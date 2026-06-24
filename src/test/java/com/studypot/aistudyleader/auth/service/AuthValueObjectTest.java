package com.studypot.aistudyleader.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthValueObjectTest {

	@Test
	void issuedAccessTokenRejectsMissingValues() {
		assertThatNullPointerException()
			.isThrownBy(() -> new IssuedAccessToken(null, Instant.parse("2026-05-07T04:15:00Z")))
			.withMessage("token must not be null");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new IssuedAccessToken(" ", Instant.parse("2026-05-07T04:15:00Z")))
			.withMessage("token must not be blank");

		assertThatNullPointerException()
			.isThrownBy(() -> new IssuedAccessToken("access-token", null))
			.withMessage("expiresAt must not be null");
	}

	@Test
	void googleOAuthLoginCommandComparesByValueAndRedactsSecrets() {
		GoogleOAuthLoginCommand first = new GoogleOAuthLoginCommand(
			"authorization-code",
			"https://app.studypot.example/auth/callback",
			"pkce-verifier"
		);
		GoogleOAuthLoginCommand second = new GoogleOAuthLoginCommand(
			" authorization-code ",
			"https://app.studypot.example/auth/callback",
			" pkce-verifier "
		);

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSameHashCodeAs(second);
		assertThat(first.toString())
			.contains("redirectUri=https://app.studypot.example/auth/callback")
			.doesNotContain("authorization-code")
			.doesNotContain("pkce-verifier");
	}

	@Test
	void googleOAuthLoginCommandAllowsHttpOnlyForLocalRedirects() {
		assertThat(new GoogleOAuthLoginCommand("code", "HTTP://localhost:3000/auth/callback", null)
			.redirectUri()
			.getScheme()).isEqualTo("HTTP");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new GoogleOAuthLoginCommand("code", "http://app.studypot.example/auth/callback", null))
			.withMessage("redirectUri must be HTTPS unless it targets localhost");
	}

	@Test
	void googleOAuthLoginResultToStringDoesNotExposePii() {
		GoogleOAuthLoginResult result = new GoogleOAuthLoginResult(
			java.util.UUID.fromString("018f0000-0000-7000-8000-000000000201"),
			"member@example.com",
			"Study Member",
			"https://cdn.example.com/member.png"
		);

		assertThat(result.toString())
			.contains("userId=018f0000-0000-7000-8000-000000000201")
			.doesNotContain("member@example.com")
			.doesNotContain("Study Member")
			.doesNotContain("https://cdn.example.com/member.png");
	}

	@Test
	void googleOAuthProfilePreservesExplicitEmptyScope() {
		GoogleOAuthProfile profile = new GoogleOAuthProfile(
			"google-123",
			com.studypot.aistudyleader.auth.domain.EmailAddress.from("member@example.com"),
			true,
			"Study Member",
			null,
			null,
			"   "
		);

		assertThat(profile.scope()).isEmpty();
	}

	@Test
	void authenticatedUserRejectsNullDomainUser() {
		assertThatNullPointerException()
			.isThrownBy(() -> AuthenticatedUser.from(null))
			.withMessage("user must not be null");
	}

	@Test
	void authenticatedUserRejectsMissingRecordComponents() {
		UUID id = UUID.fromString("018f0000-0000-7000-8000-000000000301");

		assertThatNullPointerException()
			.isThrownBy(() -> new AuthenticatedUser(null, "member@example.com", "Study Member", null))
			.withMessage("id must not be null");
		assertThatNullPointerException()
			.isThrownBy(() -> new AuthenticatedUser(id, null, "Study Member", null))
			.withMessage("email must not be null");
		assertThatNullPointerException()
			.isThrownBy(() -> new AuthenticatedUser(id, "member@example.com", null, null))
			.withMessage("nickname must not be null");
	}

	@Test
	void authSessionMetadataDoesNotTruncateInvalidIpAddresses() {
		AuthSessionMetadata metadata = new AuthSessionMetadata("  Chrome  ", "203.0.113.999999999999999999999999999999999999");

		assertThat(metadata.deviceInfo()).isEqualTo("Chrome");
		assertThat(metadata.ipAddress()).isNull();
		assertThat(new AuthSessionMetadata(null, "not:a:hostname").ipAddress()).isNull();
	}

	@Test
	void authSessionMetadataKeepsValidIpv4AndIpv6Addresses() {
		assertThat(new AuthSessionMetadata(null, " 203.0.113.10 ").ipAddress())
			.isEqualTo("203.0.113.10");
		assertThat(new AuthSessionMetadata(null, " 2001:db8::1 ").ipAddress())
			.isEqualTo("2001:db8::1");
	}
}
