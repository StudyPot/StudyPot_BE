package com.studypot.aistudyleader.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, AuthControllerTest.TestIdentityBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
	"studypot.oauth.google.client-id=test-google-client",
	"studypot.oauth.google.client-secret=test-google-secret",
		"studypot.auth.oauth2.frontend-success-uri=https://frontend.studypot.local/auth/success",
		"studypot.auth.oauth2.frontend-failure-uri=https://frontend.studypot.local/auth/failure",
		"studypot.auth.cookie.secure=true",
		"studypot.auth.cookie.same-site=Lax",
		"studypot.cors.allowed-origins=https://frontend.studypot.local"
	})
class GoogleOAuthRedirectControllerTest {

	private final MockMvc mockMvc;

	@Autowired
	GoogleOAuthRedirectControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void googleAuthorizationEndpointRedirectsToGoogleAndStoresTemporaryHttpOnlyCookies() throws Exception {
		MvcResult result = startGoogleLogin();

		String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
		Map<String, String> query = query(location);
		assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(query)
			.containsEntry("client_id", "test-google-client")
			.containsEntry("response_type", "code")
			.containsEntry("scope", "openid email profile")
			.containsEntry("code_challenge_method", "S256");
		assertThat(query.get("redirect_uri")).isEqualTo("https://localhost/api/login/oauth2/code/google");
		assertThat(query.get("state")).isNotBlank();
		assertThat(query.get("code_challenge")).isNotBlank();

		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_oauth_state=")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_oauth_code_verifier=")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_oauth_redirect_uri=")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"));
	}

	@Test
	void googleCallbackIssuesTokenCookiesAndRedirectsToFrontendWithoutTokenQueryValues() throws Exception {
		MvcResult start = startGoogleLogin();
		String state = requiredCookieValue(start, "studypot_oauth_state");
		String verifier = requiredCookieValue(start, "studypot_oauth_code_verifier");
		String redirectUri = requiredCookieValue(start, "studypot_oauth_redirect_uri");

		MvcResult callback = mockMvc.perform(get("/api/login/oauth2/code/google")
				.secure(true)
				.queryParam("code", "google-code")
				.queryParam("state", state)
				.with(cookies(
					"studypot_oauth_state", state,
					"studypot_oauth_code_verifier", verifier,
					"studypot_oauth_redirect_uri", redirectUri
				)))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, "https://frontend.studypot.local/auth/success"))
			.andReturn();

		assertThat(callback.getResponse().getHeader(HttpHeaders.LOCATION))
			.doesNotContain("access")
			.doesNotContain("refresh")
			.doesNotContain("token");
		assertThat(callback.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_access_token=")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_refresh_token=")
				.contains("HttpOnly")
				.contains("Secure")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_state=;"))
			.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_code_verifier=;"))
				.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_redirect_uri=;"));
	}

	@Test
	void googleCallbackWithStateMismatchRedirectsToFailureAndDoesNotIssueTokenCookies() throws Exception {
		MvcResult start = startGoogleLogin();
		String state = requiredCookieValue(start, "studypot_oauth_state");
		String verifier = requiredCookieValue(start, "studypot_oauth_code_verifier");
		String redirectUri = requiredCookieValue(start, "studypot_oauth_redirect_uri");

		MvcResult callback = mockMvc.perform(get("/api/login/oauth2/code/google")
				.secure(true)
				.queryParam("code", "google-code")
				.queryParam("state", state + "-tampered")
				.with(cookies(
					"studypot_oauth_state", state,
					"studypot_oauth_code_verifier", verifier,
					"studypot_oauth_redirect_uri", redirectUri
				)))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string(HttpHeaders.LOCATION, "https://frontend.studypot.local/auth/failure"))
			.andReturn();

		assertThat(callback.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.noneSatisfy(cookie -> assertThat(cookie).contains("studypot_access_token="))
			.noneSatisfy(cookie -> assertThat(cookie).contains("studypot_refresh_token="))
			.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_state=;"))
			.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_code_verifier=;"))
			.anySatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_redirect_uri=;"));
	}

	@Test
	void corsPreflightAllowsConfiguredFrontendOriginWithCredentials() throws Exception {
		mockMvc.perform(options("/api/v1/users/me")
				.header(HttpHeaders.ORIGIN, "https://frontend.studypot.local")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.studypot.local"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	private MvcResult startGoogleLogin() throws Exception {
		return mockMvc.perform(get("/api/oauth2/authorization/google")
				.secure(true))
			.andExpect(status().is3xxRedirection())
			.andReturn();
	}

	private static String requiredCookieValue(MvcResult result, String name) {
		String prefix = name + "=";
		String setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
			.stream()
			.filter(header -> header.startsWith(prefix))
			.findFirst()
			.orElse(null);
		assertThat(setCookie).as(name).isNotNull();
		int end = setCookie.indexOf(';');
		return end < 0 ? setCookie.substring(prefix.length()) : setCookie.substring(prefix.length(), end);
	}

	private static RequestPostProcessor cookies(
		String firstName,
		String firstValue,
		String secondName,
		String secondValue,
		String thirdName,
		String thirdValue
	) {
		return request -> {
			request.setCookies(
				new MockCookie(firstName, firstValue),
				new MockCookie(secondName, secondValue),
				new MockCookie(thirdName, thirdValue)
			);
			return request;
		};
	}

	private static Map<String, String> query(String location) {
		return UriComponentsBuilder.fromUri(URI.create(location))
			.build()
			.getQueryParams()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> URLDecoder.decode(String.join(" ", entry.getValue()), StandardCharsets.UTF_8)
			));
	}
}
