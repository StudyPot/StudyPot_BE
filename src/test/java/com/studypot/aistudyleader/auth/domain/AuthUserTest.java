package com.studypot.aistudyleader.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthUserTest {

	@Test
	void domainOwnsNicknameNormalizationAndLengthPolicy() {
		AuthUser user = user("  ValidNickname  ");

		assertThat(user.nickname()).isEqualTo("ValidNickname");
	}

	@Test
	void createDoesNotRecordLoginTimeUntilUserLogsIn() {
		Instant now = Instant.parse("2026-05-07T04:00:00Z");

		AuthUser user = AuthUser.create(
			UUID.fromString("018f0000-0000-7000-8000-000000000001"),
			EmailAddress.from("member@example.com"),
			"Study Member",
			null,
			now
		);

		assertThat(user.lastLoginAt()).isEmpty();
		assertThat(user.recordLogin(now).lastLoginAt()).contains(now);
	}

	@Test
	void nicknameAllowsExactlyEightyCharactersButRejectsLongerValues() {
		assertThat(user("a".repeat(80)).nickname()).hasSize(80);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> user("a".repeat(81)))
			.withMessage("nickname length must be <= 80: 81");
	}

	private static AuthUser user(String nickname) {
		return AuthUser.create(
			UUID.fromString("018f0000-0000-7000-8000-000000000001"),
			EmailAddress.from("member@example.com"),
			nickname,
			null,
			Instant.parse("2026-05-07T04:00:00Z")
		);
	}
}
