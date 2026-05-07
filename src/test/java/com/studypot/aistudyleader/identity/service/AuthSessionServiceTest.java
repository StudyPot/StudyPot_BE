package com.studypot.aistudyleader.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class AuthSessionServiceTest {

	private static final UUID FIRST_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000101");
	private static final UUID FIRST_ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000102");
	private static final UUID FIRST_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000103");
	private static final UUID SECOND_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000104");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000105");
	private static final Instant NOW = Instant.parse("2026-05-07T05:00:00Z");
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

	private final MutableClock clock = new MutableClock(NOW);
	private final FakeGoogleOAuthCodeExchange google = new FakeGoogleOAuthCodeExchange();
	private final FakeIdentityAccountRepository identityRepository = new FakeIdentityAccountRepository();
	private final FakeRefreshTokenRepository refreshTokenRepository = new FakeRefreshTokenRepository();
	private final FakeAccessTokenIssuer accessTokenIssuer = new FakeAccessTokenIssuer();
	private final AuthSessionService service = new AuthSessionService(
		new GoogleOAuthLoginService(
			google,
			identityRepository,
			clock,
			new DeterministicIds(FIRST_USER_ID, FIRST_ACCOUNT_ID)
		),
		identityRepository,
		refreshTokenRepository,
		accessTokenIssuer,
		clock,
		new DeterministicIds(FIRST_REFRESH_ID, SECOND_REFRESH_ID),
		new DeterministicRefreshTokens("refresh-one", "refresh-two"),
		REFRESH_TOKEN_TTL
	);

	@Test
	void googleLoginIssuesAccessTokenAndStoresOnlyRefreshTokenHash() {
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");

		AuthTokenResult result = service.loginWithGoogle(command("code-123"), metadata("Chrome", "203.0.113.10"));

		assertThat(result.accessToken()).isEqualTo("access-token:" + FIRST_USER_ID);
		assertThat(result.refreshToken()).isEqualTo("refresh-one");
		assertThat(result.tokenType()).isEqualTo("Bearer");
		assertThat(result.expiresIn()).isEqualTo(900);
		assertThat(result.user().id()).isEqualTo(FIRST_USER_ID);
		assertThat(result.user().email()).isEqualTo("member@example.com");
		assertThat(result.user().nickname()).isEqualTo("Study Member");

		RefreshTokenSession stored = refreshTokenRepository.onlySession();
		assertThat(stored.id()).isEqualTo(FIRST_REFRESH_ID);
		assertThat(stored.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(stored.tokenHash()).isEqualTo(RefreshTokenHasher.sha256Hex("refresh-one"));
		assertThat(stored.tokenHash()).doesNotContain("refresh-one");
		assertThat(stored.deviceInfo()).contains("Chrome");
		assertThat(stored.ipAddress()).contains("203.0.113.10");
		assertThat(stored.expiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_TTL));
		assertThat(stored.revokedAt()).isEmpty();
	}

	@Test
	void refreshRotatesRefreshTokenAndRejectsOldTokenReuse() {
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");
		AuthTokenResult issued = service.loginWithGoogle(command("code-123"), metadata("Chrome", "203.0.113.10"));

		clock.current = NOW.plus(Duration.ofMinutes(3));
		AuthTokenResult refreshed = service.refresh(issued.refreshToken(), metadata("Safari", "203.0.113.11"));

		assertThat(refreshed.refreshToken()).isEqualTo("refresh-two");
		assertThat(refreshed.accessToken()).isEqualTo("access-token:" + FIRST_USER_ID);
		assertThat(refreshTokenRepository.sessionsById.get(FIRST_REFRESH_ID).revokedAt()).contains(clock.current);
		assertThat(refreshTokenRepository.sessionsById.get(SECOND_REFRESH_ID).revokedAt()).isEmpty();

		assertThatThrownBy(() -> service.refresh(issued.refreshToken(), metadata("Safari", "203.0.113.11")))
			.isInstanceOf(RefreshTokenRejectedException.class)
			.hasMessageContaining("refresh token");
	}

	@Test
	void refreshRejectsConcurrentReuseWhenRevokeLosesRace() {
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");
		AuthTokenResult issued = service.loginWithGoogle(command("code-123"), metadata("Chrome", "203.0.113.10"));

		refreshTokenRepository.loseNextRevokeRace = true;
		clock.current = NOW.plus(Duration.ofMinutes(3));

		assertThatThrownBy(() -> service.refresh(issued.refreshToken(), metadata("Safari", "203.0.113.11")))
			.isInstanceOf(RefreshTokenRejectedException.class)
			.hasMessageContaining("refresh token");
		assertThat(refreshTokenRepository.sessionsById).hasSize(1);
		assertThat(refreshTokenRepository.sessionsById.get(FIRST_REFRESH_ID).revokedAt()).isEmpty();
	}

	@Test
	void logoutRevokesOnlySubmittedRefreshTokenForAuthenticatedUser() {
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");
		AuthTokenResult issued = service.loginWithGoogle(command("code-123"), metadata("Chrome", "203.0.113.10"));

		clock.current = NOW.plus(Duration.ofMinutes(5));
		service.logout(FIRST_USER_ID, issued.refreshToken());

		assertThat(refreshTokenRepository.sessionsById.get(FIRST_REFRESH_ID).revokedAt()).contains(clock.current);
		assertThatThrownBy(() -> service.logout(OTHER_USER_ID, issued.refreshToken()))
			.isInstanceOf(RefreshTokenRejectedException.class);
	}

	@Test
	void logoutAllRevokesEveryActiveRefreshTokenForCurrentUser() {
		identityRepository.save(IdentityUser.create(
			FIRST_USER_ID,
			EmailAddress.from("member@example.com"),
			"Study Member",
			null,
			NOW
		));
		refreshTokenRepository.save(session(FIRST_REFRESH_ID, FIRST_USER_ID, "refresh-one"));
		refreshTokenRepository.save(session(SECOND_REFRESH_ID, FIRST_USER_ID, "refresh-two"));
		refreshTokenRepository.save(session(UUID.randomUUID(), OTHER_USER_ID, "other-refresh"));

		clock.current = NOW.plus(Duration.ofMinutes(7));
		service.logoutAll(FIRST_USER_ID);

		assertThat(refreshTokenRepository.sessionsById.get(FIRST_REFRESH_ID).revokedAt()).contains(clock.current);
		assertThat(refreshTokenRepository.sessionsById.get(SECOND_REFRESH_ID).revokedAt()).contains(clock.current);
		assertThat(refreshTokenRepository.findByTokenHash(RefreshTokenHasher.sha256Hex("other-refresh")))
			.get()
			.extracting(RefreshTokenSession::revokedAt)
			.satisfies(revokedAt -> assertThat(revokedAt).isEmpty());
	}

	@Test
	void currentUserReadsActiveUserAndRejectsMissingUser() {
		IdentityUser user = IdentityUser.create(
			FIRST_USER_ID,
			EmailAddress.from("member@example.com"),
			"Study Member",
			null,
			NOW
		);
		identityRepository.save(user);

		AuthenticatedUser currentUser = service.currentUser(FIRST_USER_ID);

		assertThat(currentUser.email()).isEqualTo("member@example.com");
		assertThat(currentUser.nickname()).isEqualTo("Study Member");
		assertThatThrownBy(() -> service.currentUser(OTHER_USER_ID))
			.isInstanceOf(AuthSessionRejectedException.class)
			.hasMessageContaining("user");
	}

	private static RefreshTokenSession session(UUID id, UUID userId, String rawToken) {
		return RefreshTokenSession.create(
			id,
			userId,
			RefreshTokenHasher.sha256Hex(rawToken),
			null,
			null,
			NOW.plus(REFRESH_TOKEN_TTL),
			NOW
		);
	}

	private static AuthSessionMetadata metadata(String deviceInfo, String ipAddress) {
		return new AuthSessionMetadata(deviceInfo, ipAddress);
	}

	private static GoogleOAuthLoginCommand command(String authorizationCode) {
		return new GoogleOAuthLoginCommand(
			authorizationCode,
			"https://app.studypot.example/auth/callback",
			"pkce-verifier"
		);
	}

	private static GoogleOAuthProfile verifiedProfile(String providerUserId, String email, String name) {
		return new GoogleOAuthProfile(
			providerUserId,
			EmailAddress.from(email),
			true,
			name,
			"https://cdn.example.com/member.png",
			NOW.plus(Duration.ofHours(1)),
			"openid email profile"
		);
	}

	private static final class FakeGoogleOAuthCodeExchange implements GoogleOAuthCodeExchangePort {

		private GoogleOAuthProfile nextProfile;

		@Override
		public GoogleOAuthProfile exchange(GoogleOAuthLoginCommand command) {
			return nextProfile;
		}
	}

	private static final class FakeIdentityAccountRepository implements IdentityAccountRepository {

		private final Map<UUID, IdentityUser> usersById = new HashMap<>();
		private final Map<String, UUID> userIdsByEmailLiveKey = new HashMap<>();
		private final Map<String, OAuthAccount> activeAccounts = new HashMap<>();

		@Override
		public Optional<IdentityUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<IdentityUser> findActiveUserByEmail(EmailAddress email) {
			return Optional.ofNullable(userIdsByEmailLiveKey.get(email.liveKey()))
				.map(usersById::get);
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			return Optional.ofNullable(activeAccounts.get(provider.liveKey(providerUserId)));
		}

		@Override
		public IdentityUser save(IdentityUser user) {
			usersById.put(user.id(), user);
			userIdsByEmailLiveKey.put(user.email().liveKey(), user.id());
			return user;
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			activeAccounts.put(account.providerAccountLiveKey(), account);
			return account;
		}
	}

	private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {

		private final Map<UUID, RefreshTokenSession> sessionsById = new HashMap<>();
		private final Map<String, UUID> sessionIdsByHash = new HashMap<>();
		private boolean loseNextRevokeRace;

		@Override
		public Optional<RefreshTokenSession> findByTokenHash(String tokenHash) {
			return Optional.ofNullable(sessionIdsByHash.get(tokenHash))
				.map(sessionsById::get);
		}

		@Override
		public RefreshTokenSession save(RefreshTokenSession session) {
			sessionsById.put(session.id(), session);
			sessionIdsByHash.put(session.tokenHash(), session.id());
			return session;
		}

		@Override
		public boolean revoke(UUID refreshTokenId, Instant revokedAt) {
			if (loseNextRevokeRace) {
				loseNextRevokeRace = false;
				return false;
			}
			RefreshTokenSession session = sessionsById.get(refreshTokenId);
			if (session == null || session.revokedAt().isPresent()) {
				return false;
			}
			sessionsById.put(refreshTokenId, session.revoke(revokedAt));
			return true;
		}

		@Override
		public int revokeAllActiveByUserId(UUID userId, Instant revokedAt) {
			int revoked = 0;
			for (RefreshTokenSession session : List.copyOf(sessionsById.values())) {
				if (session.userId().equals(userId) && session.revokedAt().isEmpty()) {
					sessionsById.put(session.id(), session.revoke(revokedAt));
					revoked++;
				}
			}
			return revoked;
		}

		private RefreshTokenSession onlySession() {
			assertThat(sessionsById).hasSize(1);
			return sessionsById.values().iterator().next();
		}
	}

	private static final class FakeAccessTokenIssuer implements AccessTokenIssuer {

		@Override
		public IssuedAccessToken issue(IdentityUser user, Instant now) {
			return new IssuedAccessToken("access-token:" + user.id(), now.plus(Duration.ofMinutes(15)));
		}
	}

	private static final class DeterministicIds implements Supplier<UUID> {

		private final List<UUID> ids;
		private int next;

		private DeterministicIds(UUID... ids) {
			this.ids = List.of(ids);
		}

		@Override
		public UUID get() {
			return ids.get(next++);
		}
	}

	private static final class DeterministicRefreshTokens implements Supplier<String> {

		private final List<String> tokens;
		private int next;

		private DeterministicRefreshTokens(String... tokens) {
			this.tokens = List.of(tokens);
		}

		@Override
		public String get() {
			return tokens.get(next++);
		}
	}

	private static final class MutableClock extends Clock {

		private Instant current;

		private MutableClock(Instant current) {
			this.current = current;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return Clock.fixed(current, zone);
		}

		@Override
		public Instant instant() {
			return current;
		}
	}
}
