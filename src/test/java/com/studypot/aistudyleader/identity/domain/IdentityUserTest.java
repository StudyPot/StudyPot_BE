package com.studypot.aistudyleader.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentityUserTest {

	@Test
	void domainOwnsNicknameNormalizationAndLengthPolicy() {
		IdentityUser user = IdentityUser.create(
			UUID.fromString("018f0000-0000-7000-8000-000000000001"),
			EmailAddress.from("member@example.com"),
			"  " + "a".repeat(90) + "  ",
			null,
			Instant.parse("2026-05-07T04:00:00Z")
		);

		assertThat(user.nickname()).hasSize(80);
		assertThat(user.nickname()).doesNotContain(" ");
	}
}
