package com.studypot.aistudyleader.auth.infrastructure.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.studypot.aistudyleader.auth.service.GoogleOAuthLoginCommand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleOAuthClientTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-07T04:00:00Z"), ZoneOffset.UTC);

	@Test
	void exchangesAuthorizationCodeAndFetchesUserinfoWithoutReturningProviderTokens() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		GoogleOAuthClient client = new GoogleOAuthClient(restClientBuilder, properties(), CLOCK);

		server.expect(requestTo("https://oauth2.googleapis.com/token"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
			.andExpect(content().string(allOf(
				containsString("grant_type=authorization_code"),
				containsString("code=google-code"),
				containsString("client_id=client-id"),
				containsString("client_secret=client-secret"),
				containsString("redirect_uri=https%3A%2F%2Fapp.studypot.example%2Fauth%2Fcallback"),
				containsString("code_verifier=pkce-verifier")
			)))
			.andRespond(withSuccess("""
				{
				  "access_token": "provider-access-token",
				  "expires_in": 3600,
				  "scope": "openid email profile",
				  "token_type": "Bearer"
				}
				""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
			.andExpect(method(HttpMethod.GET))
			.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer provider-access-token"))
			.andRespond(withSuccess("""
				{
				  "sub": "google-123",
				  "email": "MEMBER@Example.COM",
				  "email_verified": true,
				  "name": "Study Member",
				  "picture": "https://cdn.example.com/member.png"
				}
				""", MediaType.APPLICATION_JSON));

		var profile = client.exchange(new GoogleOAuthLoginCommand(
			"google-code",
			"https://app.studypot.example/auth/callback",
			"pkce-verifier"
		));

		assertThat(profile.providerUserId()).isEqualTo("google-123");
		assertThat(profile.email().value()).isEqualTo("member@example.com");
		assertThat(profile.emailVerified()).isTrue();
		assertThat(profile.name()).isEqualTo("Study Member");
		assertThat(profile.picture()).isEqualTo("https://cdn.example.com/member.png");
		assertThat(profile.tokenExpiresAt()).isEqualTo(Instant.parse("2026-05-07T05:00:00Z"));
		assertThat(profile.scope()).isEqualTo("openid email profile");
		server.verify();
	}

	@Test
	void rejectsMissingClientCredentialsWithoutLeakingSecretValues() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		GoogleOAuthProperties properties = new GoogleOAuthProperties(
			"",
			"client-secret",
			"https://oauth2.googleapis.com/token",
			"https://openidconnect.googleapis.com/v1/userinfo",
			null,
			null,
			Duration.ofSeconds(5),
			Duration.ofSeconds(10)
		);
		GoogleOAuthClient client = new GoogleOAuthClient(restClientBuilder, properties, CLOCK);

		assertThatThrownBy(() -> client.exchange(new GoogleOAuthLoginCommand(
			"google-code",
			"https://app.studypot.example/auth/callback",
			null
		)))
			.isInstanceOf(GoogleOAuthConfigurationException.class)
			.hasMessageContaining("client id")
			.hasMessageNotContaining("client-secret");
	}

	@Test
	void tokenHttpErrorsAreTranslatedWithoutLeakingSecrets() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		GoogleOAuthClient client = new GoogleOAuthClient(restClientBuilder, properties(), CLOCK);

		server.expect(requestTo("https://oauth2.googleapis.com/token"))
			.andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":\"invalid_grant\"}"));

		assertThatThrownBy(() -> client.exchange(new GoogleOAuthLoginCommand(
			"google-code",
			"https://app.studypot.example/auth/callback",
			"pkce-verifier"
		)))
			.isInstanceOf(OAuthProviderResponseException.class)
			.hasMessageContaining("Google OAuth token request failed: status 400")
			.hasMessageContaining("invalid_grant")
			.hasMessageNotContaining("client-secret")
			.hasMessageNotContaining("pkce-verifier")
			.hasMessageNotContaining("google-code");
		server.verify();
	}

	@Test
	void userinfoHttpErrorsAreTranslated() {
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
		GoogleOAuthClient client = new GoogleOAuthClient(restClientBuilder, properties(), CLOCK);

		server.expect(requestTo("https://oauth2.googleapis.com/token"))
			.andRespond(withSuccess("""
				{
				  "access_token": "provider-access-token",
				  "expires_in": 3600,
				  "scope": "openid email profile",
				  "token_type": "Bearer"
				}
				""", MediaType.APPLICATION_JSON));
		server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
			.andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.contentType(MediaType.APPLICATION_JSON)
				.body("{\"error\":\"invalid_token\"}"));

		assertThatThrownBy(() -> client.exchange(new GoogleOAuthLoginCommand(
			"google-code",
			"https://app.studypot.example/auth/callback",
			null
		)))
			.isInstanceOf(OAuthProviderResponseException.class)
			.hasMessageContaining("Google userinfo request failed: status 401")
			.hasMessageContaining("invalid_token")
			.hasMessageNotContaining("provider-access-token");
		server.verify();
	}

	@Test
	void propertiesToStringMasksClientSecret() {
		assertThat(properties().toString())
			.contains("clientId=client-id")
			.contains("clientSecret=****")
			.doesNotContain("client-secret");
	}

	@Test
	void rejectsInsecureProviderEndpointsBeforeRequestingGoogle() {
		assertThatThrownBy(() -> new GoogleOAuthProperties(
			"client-id",
			"client-secret",
			"http://oauth2.googleapis.com/token",
			"https://openidconnect.googleapis.com/v1/userinfo",
			null,
			null,
			Duration.ofSeconds(5),
			Duration.ofSeconds(10)
		))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("studypot.oauth.google.token-uri");
	}

	private static GoogleOAuthProperties properties() {
		return new GoogleOAuthProperties(
			"client-id",
			"client-secret",
			"https://oauth2.googleapis.com/token",
			"https://openidconnect.googleapis.com/v1/userinfo",
			null,
			null,
			Duration.ofSeconds(5),
			Duration.ofSeconds(10)
		);
	}
}
