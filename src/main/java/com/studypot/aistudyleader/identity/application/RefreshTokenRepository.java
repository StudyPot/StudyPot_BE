package com.studypot.aistudyleader.identity.application;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

	RefreshTokenSession save(RefreshTokenSession session);

	void revoke(UUID refreshTokenId, Instant revokedAt);

	int revokeAllActiveByUserId(UUID userId, Instant revokedAt);
}
