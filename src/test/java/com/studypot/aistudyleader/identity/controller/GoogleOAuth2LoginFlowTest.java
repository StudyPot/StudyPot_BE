package com.studypot.aistudyleader.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, AuthControllerTest.TestIdentityBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
		"studypot.oauth.google.client-id=test-google-client",
		"studypot.oauth.google.client-secret=test-google-secret",
		"studypot.auth.oauth2.backend-callback-uri=https://api.studypot.local/api/login/oauth2/code/google",
		"studypot.auth.oauth2.frontend-success-uri=https://frontend.studypot.local/auth/success",
		"studypot.auth.oauth2.frontend-failure-uri=https://frontend.studypot.local/auth/failure",
		"studypot.auth.cookie.secure=true",
		"studypot.auth.cookie.same-site=Lax",
		"studypot.cors.allowed-origins=https://frontend.studypot.local"
	})
class GoogleOAuth2LoginFlowTest {

	private final MockMvc mockMvc;

	@Autowired
	GoogleOAuth2LoginFlowTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void googleAuthorizationEndpointUsesSpringSecurityOAuth2ClientWithPkceAndFixedCallbackUri() throws Exception {
		MvcResult result = startGoogleLogin();

		String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
		Map<String, String> query = query(location);

		assertThat(location).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(query)
			.containsEntry("client_id", "test-google-client")
			.containsEntry("response_type", "code")
			.containsEntry("code_challenge_method", "S256")
			.containsEntry("redirect_uri", "https://api.studypot.local/api/login/oauth2/code/google");
		assertThat(scopeValues(query.get("scope"))).containsExactlyInAnyOrder("openid", "email", "profile");
		assertThat(query.get("state")).isNotBlank();
		assertThat(query.get("code_challenge")).isNotBlank();

		HttpSession session = result.getRequest().getSession(false);
		assertThat(session).as("Spring Security should own OAuth2 authorization request state").isNotNull();
		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.noneSatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_"));
	}

	@Test
	void googleAuthorizationEndpointIgnoresSpoofedHostWhenBuildingCallbackUri() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/oauth2/authorization/google")
				.secure(true)
				.header(HttpHeaders.HOST, "attacker.example"))
			.andExpect(status().is3xxRedirection())
			.andReturn();

		Map<String, String> query = query(result.getResponse().getHeader(HttpHeaders.LOCATION));
		assertThat(query.get("redirect_uri")).isEqualTo("https://api.studypot.local/api/login/oauth2/code/google");
		assertThat(query.get("redirect_uri")).doesNotContain("attacker.example");
		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.noneSatisfy(cookie -> assertThat(cookie).contains("studypot_oauth_"));
	}

	@Test
	void corsPreflightAllowsConfiguredFrontendOriginWithCredentials() throws Exception {
		mockMvc.perform(options("/api/v1/users/me")
				.header(HttpHeaders.ORIGIN, "https://frontend.studypot.local")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://frontend.studypot.local"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("X-XSRF-TOKEN")));
	}

	private MvcResult startGoogleLogin() throws Exception {
		return mockMvc.perform(get("/api/oauth2/authorization/google")
				.secure(true))
			.andExpect(status().is3xxRedirection())
			.andReturn();
	}

	private static String[] scopeValues(String scope) {
		assertThat(scope).isNotBlank();
		return Arrays.stream(scope.split(" "))
			.filter(value -> !value.isBlank())
			.toArray(String[]::new);
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
