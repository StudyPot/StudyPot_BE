package com.studypot.aistudyleader.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class AuthPropertiesTest {

	@Test
	void jwtRequiresExplicitIssuer() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties.Jwt("0123456789abcdef0123456789abcdef", null, Duration.ofMinutes(15)))
			.withMessage("studypot.auth.jwt.issuer must not be blank");
	}

	@Test
	void jwtRequiresExplicitSecret() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties.Jwt(null, "https://api.studypot.example", Duration.ofMinutes(15)))
			.withMessageContaining("secret");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties.Jwt("", "https://api.studypot.example", Duration.ofMinutes(15)))
			.withMessageContaining("secret");
	}

	@Test
	void oauth2RequiresExplicitFrontendRedirectUris() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties.OAuth2(
				"https://api.studypot.example/api/login/oauth2/code/google",
				null,
				URI.create("https://app.studypot.example/auth/failure")
			))
			.withMessage("studypot.auth.oauth2.frontend-success-uri must be configured.");
	}

	@Test
	void authPropertiesRequiresOAuth2Configuration() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties(
				new AuthProperties.Jwt(
					"0123456789abcdef0123456789abcdef",
					"https://api.studypot.example",
					Duration.ofMinutes(15)
				),
				Duration.ofDays(30),
				null,
				null
			))
			.withMessage("studypot.auth.oauth2 must be configured.");
	}

	@Test
	void jwtSecretIsStrippedAfterValidation() {
		AuthProperties.Jwt jwt = new AuthProperties.Jwt(
			"  0123456789abcdef0123456789abcdef  ",
			" https://api.studypot.example ",
			Duration.ofMinutes(15)
		);

		assertThat(jwt.secret()).isEqualTo("0123456789abcdef0123456789abcdef");
		assertThat(jwt.issuer()).isEqualTo("https://api.studypot.example");
	}
}
