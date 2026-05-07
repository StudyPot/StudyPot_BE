package com.studypot.aistudyleader.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GoogleOAuthLoginServiceTest {

	private static final UUID FIRST_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001");
	private static final UUID FIRST_ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002");
	private static final UUID SECOND_ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003");
	private static final Instant NOW = Instant.parse("2026-05-07T04:00:00Z");

	private final MutableClock clock = new MutableClock(NOW);
	private final FakeGoogleOAuthCodeExchange google = new FakeGoogleOAuthCodeExchange();
	private final FakeIdentityAccountRepository repository = new FakeIdentityAccountRepository();
	private final GoogleOAuthLoginService service = new GoogleOAuthLoginService(
		google,
		repository,
		clock,
		new DeterministicIds(FIRST_USER_ID, FIRST_ACCOUNT_ID, SECOND_ACCOUNT_ID)
	);

	@Test
	void firstLoginCreatesUserAndOAuthAccountFromVerifiedGoogleProfile() {
		google.nextProfile = verifiedProfile("google-123", "MEMBER@Example.COM", "Study Member");

		GoogleOAuthLoginResult result = service.login(command("code-123"));

		assertThat(result.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(result.email()).isEqualTo("member@example.com");
		assertThat(result.nickname()).isEqualTo("Study Member");
		assertThat(result.profileImage()).isEqualTo("https://cdn.example.com/member.png");

		IdentityUser user = repository.usersById.get(FIRST_USER_ID);
		assertThat(user.email().value()).isEqualTo("member@example.com");
		assertThat(user.lastLoginAt()).contains(NOW);
		assertThat(user.auditMetadata().createdAt()).isEqualTo(NOW);
		assertThat(user.auditMetadata().updatedAt()).isEqualTo(NOW);

		OAuthAccount account = repository.onlyActiveAccount();
		assertThat(account.id()).isEqualTo(FIRST_ACCOUNT_ID);
		assertThat(account.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(account.provider()).isEqualTo(OAuthProvider.GOOGLE);
		assertThat(account.providerUserId()).isEqualTo("google-123");
		assertThat(account.providerAccountLiveKey()).isEqualTo("GOOGLE:google-123");
		assertThat(account.email()).contains(EmailAddress.from("member@example.com"));
		assertThat(account.tokenExpiresAt()).contains(NOW.plus(Duration.ofHours(1)));
		assertThat(account.scope()).contains("openid email profile");
		assertThat(account.connectedAt()).isEqualTo(NOW);
		assertThat(account.lastSyncedAt()).contains(NOW);
	}

	@Test
	void reloginUpdatesExistingOAuthAccountWithoutDuplicatingLiveRows() {
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");
		GoogleOAuthLoginResult first = service.login(command("first-code"));

		clock.current = NOW.plus(Duration.ofMinutes(5));
		google.nextProfile = verifiedProfile("google-123", "member+new@example.com", "Updated Member");
		GoogleOAuthLoginResult second = service.login(command("second-code"));

		assertThat(second.userId()).isEqualTo(first.userId());
		assertThat(repository.usersById).hasSize(1);
		assertThat(repository.activeAccounts).hasSize(1);

		IdentityUser user = repository.usersById.get(FIRST_USER_ID);
		assertThat(user.email().value()).isEqualTo("member@example.com");
		assertThat(user.nickname()).isEqualTo("Study Member");
		assertThat(user.profileImage()).contains("https://cdn.example.com/member.png");
		assertThat(user.lastLoginAt()).contains(NOW.plus(Duration.ofMinutes(5)));
		assertThat(user.auditMetadata().updatedAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));

		OAuthAccount account = repository.onlyActiveAccount();
		assertThat(account.email()).contains(EmailAddress.from("member+new@example.com"));
		assertThat(account.lastSyncedAt()).contains(NOW.plus(Duration.ofMinutes(5)));
	}

	@Test
	void loginLinksNewGoogleAccountToExistingLiveUserWithSameEmail() {
		IdentityUser existingUser = IdentityUser.create(
			FIRST_USER_ID,
			EmailAddress.from("member@example.com"),
			"Existing Member",
			null,
			NOW.minus(Duration.ofDays(1))
		);
		repository.save(existingUser);
		google.nextProfile = verifiedProfile("google-456", "member@example.com", "Google Name");

		GoogleOAuthLoginResult result = service.login(command("code-456"));

		assertThat(result.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(repository.usersById).hasSize(1);
		assertThat(repository.onlyActiveAccount().userId()).isEqualTo(FIRST_USER_ID);
		assertThat(repository.onlyActiveAccount().providerUserId()).isEqualTo("google-456");
	}

	@Test
	void softDeletedProviderAccountDoesNotBlockNewLiveAccountForSameGoogleSubject() {
		repository.deletedProviderAccountKeys.add("GOOGLE:google-123");
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");

		service.login(command("code-123"));

		OAuthAccount account = repository.onlyActiveAccount();
		assertThat(account.id()).isEqualTo(FIRST_ACCOUNT_ID);
		assertThat(account.providerAccountLiveKey()).isEqualTo("GOOGLE:google-123");
		assertThat(repository.deletedProviderAccountKeys).contains("GOOGLE:google-123");
	}

	@Test
	void concurrentEmailInsertConflictRetriesByReadingExistingLiveUser() {
		repository.conflictOnNextUserSave = true;
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");

		GoogleOAuthLoginResult result = service.login(command("code-123"));

		assertThat(result.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(repository.usersById).hasSize(1);
		assertThat(repository.onlyActiveAccount().userId()).isEqualTo(FIRST_USER_ID);
	}

	@Test
	void concurrentOAuthAccountInsertConflictRetriesByReadingExistingLiveAccount() {
		repository.conflictOnNextAccountSave = true;
		google.nextProfile = verifiedProfile("google-123", "member@example.com", "Study Member");

		GoogleOAuthLoginResult result = service.login(command("code-123"));

		assertThat(result.userId()).isEqualTo(FIRST_USER_ID);
		assertThat(repository.usersById).hasSize(1);
		assertThat(repository.onlyActiveAccount().providerUserId()).isEqualTo("google-123");
		assertThat(repository.onlyActiveAccount().lastSyncedAt()).contains(NOW);
	}

	@Test
	void loginRejectsBlankCodeInvalidRedirectAndUnverifiedGoogleEmail() {
		assertThatThrownBy(() -> new GoogleOAuthLoginCommand(" ", "https://app.studypot.example/auth/callback", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("authorizationCode");

		assertThatThrownBy(() -> new GoogleOAuthLoginCommand("code", "notaurl", null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("redirectUri");

		google.nextProfile = new GoogleOAuthProfile(
			"google-123",
			EmailAddress.from("member@example.com"),
			false,
			"Study Member",
			null,
			NOW.plus(Duration.ofHours(1)),
			"openid email profile"
		);

		assertThatThrownBy(() -> service.login(command("code-123")))
			.isInstanceOf(OAuthLoginRejectedException.class)
			.hasMessageContaining("verified Google email");
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
			assertThat(command.authorizationCode()).isNotBlank();
			assertThat(command.redirectUri().toString()).isEqualTo("https://app.studypot.example/auth/callback");
			assertThat(command.codeVerifier()).contains("pkce-verifier");
			return nextProfile;
		}
	}

	private static final class FakeIdentityAccountRepository implements IdentityAccountRepository {

		private final Map<UUID, IdentityUser> usersById = new HashMap<>();
		private final Map<String, UUID> userIdsByEmailLiveKey = new HashMap<>();
		private final Map<String, OAuthAccount> activeAccounts = new HashMap<>();
		private final List<String> deletedProviderAccountKeys = new ArrayList<>();
		private boolean conflictOnNextUserSave;
		private boolean conflictOnNextAccountSave;

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
			if (conflictOnNextUserSave) {
				conflictOnNextUserSave = false;
				IdentityUser concurrentUser = IdentityUser.create(
					FIRST_USER_ID,
					user.email(),
					"Concurrent Member",
					null,
					NOW
				);
				usersById.put(concurrentUser.id(), concurrentUser);
				userIdsByEmailLiveKey.put(concurrentUser.email().liveKey(), concurrentUser.id());
				throw new IdentityUniquenessConflictException("users.email_live_key");
			}
			usersById.put(user.id(), user);
			userIdsByEmailLiveKey.put(user.email().liveKey(), user.id());
			return user;
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			if (conflictOnNextAccountSave) {
				conflictOnNextAccountSave = false;
				IdentityUser concurrentUser = usersById.get(account.userId());
				activeAccounts.put(account.providerAccountLiveKey(), OAuthAccount.connect(
					FIRST_ACCOUNT_ID,
					concurrentUser.id(),
					account.provider(),
					account.providerUserId(),
					concurrentUser.email(),
					account.tokenExpiresAt().orElse(null),
					account.scope().orElse(null),
					NOW
				));
				throw new IdentityUniquenessConflictException("oauth_account.provider_account_live_key");
			}
			activeAccounts.put(account.providerAccountLiveKey(), account);
			return account;
		}

		private OAuthAccount onlyActiveAccount() {
			assertThat(activeAccounts).hasSize(1);
			return activeAccounts.values().iterator().next();
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
