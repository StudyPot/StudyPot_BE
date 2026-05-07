package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.service.AccessTokenIssuer;
import com.studypot.aistudyleader.identity.service.IssuedAccessToken;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;

	JwtAccessTokenIssuer(JwtEncoder jwtEncoder, AuthProperties properties) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
	}

	@Override
	public IssuedAccessToken issue(IdentityUser user, Instant now) {
		Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.jwt().issuer())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.subject(user.id().toString())
			.claim("email", user.email().value())
			.claim("nickname", user.nickname())
			.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256)
			.type("JWT")
			.build();
		return new IssuedAccessToken(jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue(), expiresAt);
	}
}
