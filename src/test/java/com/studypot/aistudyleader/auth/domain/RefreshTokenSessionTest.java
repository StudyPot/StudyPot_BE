package com.studypot.aistudyleader.auth.domain;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenSessionTest {

	private static final UUID SESSION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000111");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000112");
	private static final Instant CREATED_AT = Instant.parse("2026-05-07T04:00:00Z");
	private static final Instant EXPIRES_AT = CREATED_AT.plus(Duration.ofDays(30));

	@Test
	void createRejectsExpiredTemporalOrder() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> RefreshTokenSession.create(
				SESSION_ID,
				USER_ID,
				"token-hash",
				null,
				null,
				CREATED_AT,
				CREATED_AT
			))
			.withMessage("expiresAt must be after createdAt");
	}

	@Test
	void rehydrateRejectsRevocationOutsideSessionLifetime() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> RefreshTokenSession.rehydrate(
				SESSION_ID,
				USER_ID,
				"token-hash",
				null,
				null,
				EXPIRES_AT,
				CREATED_AT.minusSeconds(1),
				CREATED_AT
			))
			.withMessage("revokedAt must be between createdAt and expiresAt");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> RefreshTokenSession.rehydrate(
				SESSION_ID,
				USER_ID,
				"token-hash",
				null,
				null,
				EXPIRES_AT,
				EXPIRES_AT.plusSeconds(1),
				CREATED_AT
			))
			.withMessage("revokedAt must be between createdAt and expiresAt");
	}
}
