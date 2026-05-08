package com.studypot.aistudyleader.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtAccessTokenIssuerTest {

	private static final Instant NOW = Instant.parse("2026-05-07T04:00:00Z");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000121");

	@Test
	void issuedJwtDoesNotEmbedEmailOrNicknameClaims() {
		CapturingJwtEncoder encoder = new CapturingJwtEncoder();
		JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(encoder, properties());

		issuer.issue(IdentityUser.create(
			USER_ID,
			EmailAddress.from("member@example.com"),
			"Study Member",
			null,
			NOW
		), NOW);

		assertThat(encoder.claims.getClaims())
			.containsEntry("sub", USER_ID.toString())
			.doesNotContainKeys("email", "nickname");
	}

	private static AuthProperties properties() {
		return new AuthProperties(
			new AuthProperties.Jwt("0123456789abcdef0123456789abcdef", "https://api.studypot.example", Duration.ofMinutes(15)),
			Duration.ofDays(30),
			new AuthProperties.Cookie("studypot_access_token", "studypot_refresh_token", null, "/", true, "Lax"),
			new AuthProperties.OAuth2(
				"https://api.studypot.example/api/login/oauth2/code/google",
				java.net.URI.create("https://app.studypot.example/auth/success"),
				java.net.URI.create("https://app.studypot.example/auth/failure")
			)
		);
	}

	private static final class CapturingJwtEncoder implements JwtEncoder {

		private JwtClaimsSet claims;

		@Override
		public Jwt encode(JwtEncoderParameters parameters) {
			this.claims = parameters.getClaims();
			return new Jwt(
				"encoded-token",
				claims.getIssuedAt(),
				claims.getExpiresAt(),
				Map.of("alg", "HS256"),
				claims.getClaims()
			);
		}
	}
}
