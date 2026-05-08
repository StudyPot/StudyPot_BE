package com.studypot.aistudyleader.identity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.identity.service.AccessTokenIssuer;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.GoogleOAuthCodeExchangePort;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginCommand;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginService;
import com.studypot.aistudyleader.identity.service.GoogleOAuthProfile;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.RefreshTokenRepository;
import com.studypot.aistudyleader.identity.domain.RefreshTokenSession;
import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, AuthControllerTest.TestIdentityBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

	private static final String GOOGLE_LOGIN_PATH = ApiPaths.V1 + "/auth/oauth/google";
	private static final String REFRESH_PATH = ApiPaths.V1 + "/auth/refresh";
	private static final String LOGOUT_PATH = ApiPaths.V1 + "/auth/logout";
	private static final String LOGOUT_ALL_PATH = ApiPaths.V1 + "/auth/logout-all";
	private static final String ME_PATH = ApiPaths.V1 + "/users/me";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	AuthControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void googleLoginRefreshCurrentUserAndLogoutFlowUsesLockedApiShape() throws Exception {
		JsonNode login = login();
		String accessToken = login.get("accessToken").asText();
		String refreshToken = login.get("refreshToken").asText();

		assertThat(login.get("tokenType").asText()).isEqualTo("Bearer");
		assertThat(login.get("expiresIn").asLong()).isPositive();
		assertThat(login.at("/user/email").asText()).isEqualTo("member@example.com");

		mockMvc.perform(get(ME_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TestIdentityBeans.FIRST_USER_ID.toString()))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.nickname").value("Study Member"));

		JsonNode refreshed = refresh(refreshToken, status().isOk());
		String rotatedRefreshToken = refreshed.get("refreshToken").asText();
		assertThat(rotatedRefreshToken).isEqualTo("refresh-two");

		assertThat(refresh(refreshToken, status().isUnauthorized()).at("/title").asText()).isEqualTo("Unauthorized");

		mockMvc.perform(post(LOGOUT_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"refreshToken":"refresh-two"}
					"""))
			.andExpect(status().isNoContent());

		refresh(rotatedRefreshToken, status().isUnauthorized());
	}

	@Test
	void googleLoginSetsHttpOnlyCookiesAndCookieAccessTokenAuthenticates() throws Exception {
		MvcResult loginResult = loginResult();
		String accessCookie = cookieValueFromSetCookie(loginResult, "studypot_access_token");
		String refreshCookie = cookieValueFromSetCookie(loginResult, "studypot_refresh_token");

		assertThat(accessCookie).isNotBlank();
		assertThat(refreshCookie).isNotBlank();
		assertThat(loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_access_token=")
				.contains("HttpOnly")
				.contains("SameSite=Lax"))
			.anySatisfy(cookie -> assertThat(cookie)
				.contains("studypot_refresh_token=")
				.contains("HttpOnly")
				.contains("SameSite=Lax"));

		mockMvc.perform(get(ME_PATH)
				.with(cookie("studypot_access_token", accessCookie)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TestIdentityBeans.FIRST_USER_ID.toString()))
			.andExpect(jsonPath("$.email").value("member@example.com"));

		MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", refreshCookie))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isOk())
			.andReturn();
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_access_token")).isNotBlank();
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_refresh_token")).isNotBlank();
	}

	@Test
	void cookieBackedStateChangingRequestsRequireCsrfToken() throws Exception {
		MvcResult loginResult = loginResult();
		String accessCookie = cookieValueFromSetCookie(loginResult, "studypot_access_token");
		String refreshCookie = cookieValueFromSetCookie(loginResult, "studypot_refresh_token");

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.with(cookie("studypot_access_token", accessCookie)))
			.andExpect(status().isForbidden());

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", refreshCookie)))
			.andExpect(status().isForbidden());
	}

	@Test
	void cookieBackedStateChangingRequestsAcceptCsrfToken() throws Exception {
		MvcResult loginResult = loginResult();
		String accessCookie = cookieValueFromSetCookie(loginResult, "studypot_access_token");

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.with(cookie("studypot_access_token", accessCookie))
				.with(xsrf("logout-xsrf")))
			.andExpect(status().isNoContent());
	}

	@Test
	void protectedCurrentUserRequiresBearerAccessToken() throws Exception {
		mockMvc.perform(get(ME_PATH))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void logoutAllRevokesEveryRefreshTokenForAuthenticatedUser() throws Exception {
		JsonNode firstLogin = login();
		JsonNode secondLogin = login();
		String accessToken = secondLogin.get("accessToken").asText();

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
			.andExpect(status().isNoContent());

		refresh(firstLogin.get("refreshToken").asText(), status().isUnauthorized());
		refresh(secondLogin.get("refreshToken").asText(), status().isUnauthorized());
	}

	@Test
	void invalidGoogleLoginPayloadReturnsValidationProblem() throws Exception {
		mockMvc.perform(post(GOOGLE_LOGIN_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"authorizationCode":"google-code","redirectUri":"notaurl"}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("redirectUri"));
	}

	private JsonNode login() throws Exception {
		String response = loginResult()
			.getResponse()
			.getContentAsString();
		return objectMapper.readTree(response);
	}

	private MvcResult loginResult() throws Exception {
		return mockMvc.perform(post(GOOGLE_LOGIN_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "authorizationCode": "google-code",
					  "redirectUri": "https://app.studypot.example/auth/callback",
					  "codeVerifier": "pkce-verifier"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andReturn();
	}

	private JsonNode refresh(String refreshToken, org.springframework.test.web.servlet.ResultMatcher status) throws Exception {
		String response = mockMvc.perform(post(REFRESH_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + refreshToken + "\"}")
				.with(xsrf("refresh-xsrf")))
			.andExpect(status)
			.andReturn()
			.getResponse()
			.getContentAsString();
		return response.isBlank() ? objectMapper.createObjectNode() : objectMapper.readTree(response);
	}

	private static String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private static String cookieValueFromSetCookie(MvcResult result, String name) {
		String prefix = name + "=";
		String setCookie = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
			.stream()
			.filter(header -> header.startsWith(prefix))
			.findFirst()
			.orElseThrow();
		int end = setCookie.indexOf(';');
		return end < 0 ? setCookie.substring(prefix.length()) : setCookie.substring(prefix.length(), end);
	}

	private static RequestPostProcessor cookie(String name, String value) {
		return request -> {
			request.setCookies(new MockCookie(name, value));
			return request;
		};
	}

	private static RequestPostProcessor xsrf(String value) {
		return request -> {
			jakarta.servlet.http.Cookie[] existingCookies = request.getCookies();
			jakarta.servlet.http.Cookie[] cookies = existingCookies == null
				? new jakarta.servlet.http.Cookie[1]
				: java.util.Arrays.copyOf(existingCookies, existingCookies.length + 1);
			cookies[cookies.length - 1] = new MockCookie("XSRF-TOKEN", value);
			request.setCookies(cookies);
			request.addHeader("X-XSRF-TOKEN", value);
			return request;
		};
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestIdentityBeans {

		private static final UUID FIRST_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201");
		private static final UUID FIRST_ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000202");
		private static final UUID FIRST_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000203");
		private static final UUID SECOND_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000204");
		private static final UUID THIRD_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000205");
		private static final Instant NOW = Instant.now();

		@Bean
		@Primary
		GoogleOAuthCodeExchangePort testGoogleOAuthCodeExchangePort() {
			return command -> {
				if (command.authorizationCode().equals("provider-bug")) {
					throw new IllegalArgumentException("provider bug");
				}
				return new GoogleOAuthProfile(
					"google-123",
					EmailAddress.from("member@example.com"),
					true,
					"Study Member",
					"https://cdn.example.com/member.png",
					NOW.plus(Duration.ofHours(1)),
					"openid email profile"
				);
			};
		}

		@Bean
		@Primary
		IdentityAccountRepository testIdentityAccountRepository() {
			return new FakeIdentityAccountRepository();
		}

		@Bean
		@Primary
		RefreshTokenRepository testRefreshTokenRepository() {
			return new FakeRefreshTokenRepository();
		}

		@Bean
		@Primary
		AuthSessionService testAuthSessionService(
			GoogleOAuthCodeExchangePort google,
			IdentityAccountRepository identityRepository,
			RefreshTokenRepository refreshTokenRepository,
			AccessTokenIssuer accessTokenIssuer
		) {
			return new AuthSessionService(
				new GoogleOAuthLoginService(
					google,
					identityRepository,
					Clock.fixed(NOW, ZoneOffset.UTC),
					new DeterministicIds(FIRST_USER_ID, FIRST_ACCOUNT_ID)
				),
				identityRepository,
				refreshTokenRepository,
				accessTokenIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC),
				new DeterministicIds(FIRST_REFRESH_ID, SECOND_REFRESH_ID, THIRD_REFRESH_ID),
				new DeterministicRefreshTokens("refresh-one", "refresh-two", "refresh-three"),
				Duration.ofDays(30)
			);
		}
	}

	private static final class FakeIdentityAccountRepository implements IdentityAccountRepository {

		private final Map<UUID, IdentityUser> usersById = new HashMap<>();
		private final Map<String, UUID> userIdsByEmailLiveKey = new HashMap<>();
		private final Map<String, OAuthAccount> activeAccounts = new HashMap<>();

		@Override
		public Optional<IdentityUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<IdentityUser> findActiveUserByEmail(EmailAddress email) {
			return Optional.ofNullable(userIdsByEmailLiveKey.get(email.liveKey()))
				.map(usersById::get);
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			return Optional.ofNullable(activeAccounts.get(provider.liveKey(providerUserId)));
		}

		@Override
		public IdentityUser save(IdentityUser user) {
			usersById.put(user.id(), user);
			userIdsByEmailLiveKey.put(user.email().liveKey(), user.id());
			return user;
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			activeAccounts.put(account.providerAccountLiveKey(), account);
			return account;
		}
	}

	private static final class FakeRefreshTokenRepository implements RefreshTokenRepository {

		private final Map<UUID, RefreshTokenSession> sessionsById = new HashMap<>();
		private final Map<String, UUID> sessionIdsByHash = new HashMap<>();

		@Override
		public Optional<RefreshTokenSession> findByTokenHash(String tokenHash) {
			return Optional.ofNullable(sessionIdsByHash.get(tokenHash))
				.map(sessionsById::get);
		}

		@Override
		public RefreshTokenSession save(RefreshTokenSession session) {
			sessionsById.put(session.id(), session);
			sessionIdsByHash.put(session.tokenHash(), session.id());
			return session;
		}

		@Override
		public boolean revoke(UUID refreshTokenId, Instant revokedAt) {
			RefreshTokenSession session = sessionsById.get(refreshTokenId);
			if (session == null || session.revokedAt().isPresent()) {
				return false;
			}
			sessionsById.put(refreshTokenId, session.revoke(revokedAt));
			return true;
		}

		@Override
		public int revokeAllActiveByUserId(UUID userId, Instant revokedAt) {
			int revoked = 0;
			for (RefreshTokenSession session : List.copyOf(sessionsById.values())) {
				if (session.userId().equals(userId) && session.revokedAt().isEmpty()) {
					sessionsById.put(session.id(), session.revoke(revokedAt));
					revoked++;
				}
			}
			return revoked;
		}
	}

	private static final class DeterministicIds implements Supplier<UUID> {

		private final List<UUID> ids;
		private int next;

		private DeterministicIds(UUID... ids) {
			this.ids = List.of(ids);
		}

		@Override
		public UUID get() {
			return ids.get(next++);
		}
	}

	private static final class DeterministicRefreshTokens implements Supplier<String> {

		private final List<String> tokens;
		private int next;

		private DeterministicRefreshTokens(String... tokens) {
			this.tokens = List.of(tokens);
		}

		@Override
		public String get() {
			return tokens.get(next++);
		}
	}
}
