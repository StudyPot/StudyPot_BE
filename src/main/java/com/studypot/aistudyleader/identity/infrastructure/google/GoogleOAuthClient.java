package com.studypot.aistudyleader.identity.infrastructure.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.studypot.aistudyleader.identity.service.GoogleOAuthCodeExchangePort;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginCommand;
import com.studypot.aistudyleader.identity.service.GoogleOAuthProfile;
import com.studypot.aistudyleader.identity.domain.EmailAddress;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

public class GoogleOAuthClient implements GoogleOAuthCodeExchangePort {

	private static final String AUTHORIZATION_CODE_GRANT = "authorization_code";

	private final RestClient restClient;
	private final GoogleOAuthProperties properties;
	private final Clock clock;

	public GoogleOAuthClient(RestClient.Builder restClientBuilder, GoogleOAuthProperties properties, Clock clock) {
		this.restClient = Objects.requireNonNull(restClientBuilder, "restClientBuilder must not be null").build();
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public GoogleOAuthProfile exchange(GoogleOAuthLoginCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		requireConfigured();

		TokenResponse token = requestToken(command);
		UserinfoResponse userinfo = requestUserinfo(token.accessToken());

		return new GoogleOAuthProfile(
			required("provider user id", userinfo.sub()),
			EmailAddress.from(required("email", userinfo.email())),
			Boolean.TRUE.equals(userinfo.emailVerified()),
			userinfo.name(),
			userinfo.picture(),
			expiresAt(token.expiresIn()),
			token.scope()
		);
	}

	private TokenResponse requestToken(GoogleOAuthLoginCommand command) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", AUTHORIZATION_CODE_GRANT);
		form.add("code", command.authorizationCode());
		form.add("client_id", properties.clientId());
		form.add("client_secret", properties.clientSecret());
		form.add("redirect_uri", command.redirectUri().toString());
		command.codeVerifier().ifPresent(codeVerifier -> form.add("code_verifier", codeVerifier));

		TokenResponse response = restClient.post()
			.uri(properties.tokenUri())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(form)
			.retrieve()
			.body(TokenResponse.class);

		if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
			throw new OAuthProviderResponseException("Google OAuth token response did not include an access token.");
		}
		return response;
	}

	private UserinfoResponse requestUserinfo(String accessToken) {
		UserinfoResponse response = restClient.get()
			.uri(properties.userinfoUri())
			.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
			.retrieve()
			.body(UserinfoResponse.class);

		if (response == null) {
			throw new OAuthProviderResponseException("Google userinfo response was empty.");
		}
		return response;
	}

	private Instant expiresAt(Long expiresIn) {
		if (expiresIn == null || expiresIn <= 0) {
			return null;
		}
		return clock.instant().plusSeconds(expiresIn);
	}

	private void requireConfigured() {
		if (properties.clientId() == null) {
			throw new GoogleOAuthConfigurationException("Google OAuth client id is not configured.");
		}
		if (properties.clientSecret() == null) {
			throw new GoogleOAuthConfigurationException("Google OAuth client secret is not configured.");
		}
	}

	private static String required(String field, String value) {
		if (value == null || value.isBlank()) {
			throw new OAuthProviderResponseException("Google userinfo response did not include " + field + ".");
		}
		return value.strip();
	}

	private record TokenResponse(
		@JsonProperty("access_token") String accessToken,
		@JsonProperty("expires_in") Long expiresIn,
		String scope,
		@JsonProperty("token_type") String tokenType
	) {
	}

	private record UserinfoResponse(
		String sub,
		String email,
		@JsonProperty("email_verified") Boolean emailVerified,
		String name,
		String picture
	) {
	}
}
