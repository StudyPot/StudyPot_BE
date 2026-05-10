package com.studypot.aistudyleader.auth.infrastructure.security;

import com.studypot.aistudyleader.auth.service.AccessTokenIssuer;
import com.studypot.aistudyleader.auth.service.IssuedAccessToken;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

@RequiredArgsConstructor
class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;

	@Override
	public IssuedAccessToken issue(AuthUser user, Instant now) {
		Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.jwt().issuer())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.subject(user.id().toString())
			.build();
		JwsHeader headers = JwsHeader.with(MacAlgorithm.HS256)
			.type("JWT")
			.build();
		return new IssuedAccessToken(jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue(), expiresAt);
	}
}
