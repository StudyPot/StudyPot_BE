package com.studypot.aistudyleader.identity.service;

import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.RefreshTokenSession;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class AuthSessionService {

	private final GoogleOAuthLoginService googleOAuthLoginService;
	private final IdentityAccountRepository identityRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final Supplier<String> refreshTokenGenerator;
	private final Duration refreshTokenTtl;

	public AuthSessionService(
		GoogleOAuthLoginService googleOAuthLoginService,
		IdentityAccountRepository identityRepository,
		RefreshTokenRepository refreshTokenRepository,
		AccessTokenIssuer accessTokenIssuer,
		Clock clock,
		Supplier<UUID> idGenerator,
		Supplier<String> refreshTokenGenerator,
		Duration refreshTokenTtl
	) {
		this.googleOAuthLoginService = Objects.requireNonNull(googleOAuthLoginService, "googleOAuthLoginService must not be null");
		this.identityRepository = Objects.requireNonNull(identityRepository, "identityRepository must not be null");
		this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository, "refreshTokenRepository must not be null");
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer, "accessTokenIssuer must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.refreshTokenGenerator = Objects.requireNonNull(refreshTokenGenerator, "refreshTokenGenerator must not be null");
		if (refreshTokenTtl == null || refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
			throw new IllegalArgumentException("refreshTokenTtl must be positive");
		}
		this.refreshTokenTtl = refreshTokenTtl;
	}

	@Transactional
	public AuthTokenResult loginWithGoogle(GoogleOAuthLoginCommand command, AuthSessionMetadata metadata) {
		GoogleOAuthLoginResult loginResult = googleOAuthLoginService.login(command);
		IdentityUser user = findActiveUser(loginResult.userId());
		return issueTokenPair(user, metadata);
	}

	@Transactional
	public AuthTokenResult loginWithGoogleProfile(GoogleOAuthProfile profile, AuthSessionMetadata metadata) {
		GoogleOAuthLoginResult loginResult = googleOAuthLoginService.login(profile);
		IdentityUser user = findActiveUser(loginResult.userId());
		return issueTokenPair(user, metadata);
	}

	@Transactional
	public AuthTokenResult refresh(String rawRefreshToken, AuthSessionMetadata metadata) {
		Instant now = clock.instant();
		RefreshTokenSession currentSession = requireActiveRefreshToken(rawRefreshToken, now);
		IdentityUser user = findActiveUser(currentSession.userId());
		revokeOrReject(currentSession.id(), now);
		return issueTokenPair(user, metadata, now);
	}

	@Transactional
	public void logout(UUID authenticatedUserId, String rawRefreshToken) {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Instant now = clock.instant();
		RefreshTokenSession session = requireActiveRefreshToken(rawRefreshToken, now);
		if (!session.userId().equals(authenticatedUserId)) {
			throw new RefreshTokenRejectedException("refresh token does not belong to the authenticated user.");
		}
		revokeOrReject(session.id(), now);
	}

	@Transactional
	public void logoutAll(UUID authenticatedUserId) {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		currentUser(authenticatedUserId);
		refreshTokenRepository.revokeAllActiveByUserId(authenticatedUserId, clock.instant());
	}

	@Transactional(readOnly = true)
	public AuthenticatedUser currentUser(UUID authenticatedUserId) {
		return AuthenticatedUser.from(findActiveUser(authenticatedUserId));
	}

	private AuthTokenResult issueTokenPair(IdentityUser user, AuthSessionMetadata metadata) {
		return issueTokenPair(user, metadata, clock.instant());
	}

	private AuthTokenResult issueTokenPair(IdentityUser user, AuthSessionMetadata metadata, Instant now) {
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user, now);
		String rawRefreshToken = refreshTokenGenerator.get();
		RefreshTokenSession refreshToken = RefreshTokenSession.create(
			idGenerator.get(),
			user.id(),
			RefreshTokenHasher.sha256Hex(rawRefreshToken),
			metadata == null ? null : metadata.deviceInfo(),
			metadata == null ? null : metadata.ipAddress(),
			now.plus(refreshTokenTtl),
			now
		);
		refreshTokenRepository.save(refreshToken);
		return new AuthTokenResult(
			accessToken.token(),
			rawRefreshToken,
			"Bearer",
			expiresInSeconds(now, accessToken.expiresAt()),
			AuthenticatedUser.from(user)
		);
	}

	private RefreshTokenSession requireActiveRefreshToken(String rawRefreshToken, Instant now) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new RefreshTokenRejectedException("refresh token is required.");
		}
		String tokenHash = RefreshTokenHasher.sha256Hex(rawRefreshToken);
		RefreshTokenSession session = refreshTokenRepository.findByTokenHash(tokenHash)
			.orElseThrow(() -> new RefreshTokenRejectedException("refresh token is invalid."));
		if (!session.isActiveAt(now)) {
			throw new RefreshTokenRejectedException("refresh token is invalid or revoked.");
		}
		return session;
	}

	private void revokeOrReject(UUID refreshTokenId, Instant revokedAt) {
		if (!refreshTokenRepository.revoke(refreshTokenId, revokedAt)) {
			throw new RefreshTokenRejectedException("refresh token is invalid or revoked.");
		}
	}

	private IdentityUser findActiveUser(UUID userId) {
		return identityRepository.findActiveUser(userId)
			.orElseThrow(() -> new AuthSessionRejectedException("active user was not found."));
	}

	private static long expiresInSeconds(Instant issuedAt, Instant expiresAt) {
		return Math.max(1, Duration.between(issuedAt, expiresAt).toSeconds());
	}
}
