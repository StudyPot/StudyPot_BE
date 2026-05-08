package com.studypot.aistudyleader.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtSecurityConfigurationTest {

	private final JwtSecurityConfiguration configuration = new JwtSecurityConfiguration();

	@Test
	void jwtSecretValidationUsesUtf8ByteLength() {
		AuthProperties properties = propertiesWithSecret("가".repeat(11));

		SecretKey key = configuration.studypotJwtSecretKey(properties);

		assertThat(key.getEncoded()).hasSize(33);
	}

	@Test
	void jwtSecretRejectsBlankValuesDuringPropertyConstruction() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuthProperties.Jwt(" ", "https://api.studypot.example", Duration.ofMinutes(15)))
			.withMessage("studypot.auth.jwt.secret must not be blank");
	}

	@Test
	void jwtSecretRejectsShortUtf8ByteLength() {
		assertThatIllegalStateException()
			.isThrownBy(() -> configuration.studypotJwtSecretKey(propertiesWithSecret("short-secret")))
			.withMessage("studypot.auth.jwt.secret must be at least 32 bytes.");
	}

	private static AuthProperties propertiesWithSecret(String secret) {
		return new AuthProperties(
			new AuthProperties.Jwt(secret, "https://api.studypot.example", Duration.ofMinutes(15)),
			Duration.ofDays(30),
			new AuthProperties.Cookie("studypot_access_token", "studypot_refresh_token", null, "/", true, "Lax"),
			new AuthProperties.OAuth2(
				"https://api.studypot.example/api/login/oauth2/code/google",
				java.net.URI.create("https://app.studypot.example/auth/success"),
				java.net.URI.create("https://app.studypot.example/auth/failure")
			)
		);
	}
}
