package com.studypot.aistudyleader.identity.repository;

import com.studypot.aistudyleader.identity.domain.RefreshTokenSession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	Optional<RefreshTokenSession> findByTokenHash(String tokenHash);

	RefreshTokenSession save(RefreshTokenSession session);

	boolean revoke(UUID refreshTokenId, Instant revokedAt);

	int revokeAllActiveByUserId(UUID userId, Instant revokedAt);
}
