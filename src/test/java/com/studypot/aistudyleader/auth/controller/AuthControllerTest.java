package com.studypot.aistudyleader.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.auth.service.AccessTokenIssuer;
import com.studypot.aistudyleader.auth.service.AuthSessionMetadata;
import com.studypot.aistudyleader.auth.service.AuthSessionService;
import com.studypot.aistudyleader.auth.service.AuthTokenResult;
import com.studypot.aistudyleader.auth.service.GoogleOAuthCodeExchangePort;
import com.studypot.aistudyleader.auth.service.GoogleOAuthLoginService;
import com.studypot.aistudyleader.auth.service.GoogleOAuthProfile;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.auth.repository.RefreshTokenRepository;
import com.studypot.aistudyleader.auth.domain.RefreshTokenSession;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import com.studypot.aistudyleader.global.ratelimit.RateLimitDecision;
import com.studypot.aistudyleader.global.ratelimit.RateLimiter;
import jakarta.servlet.http.Cookie;
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
import org.junit.jupiter.api.BeforeEach;
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

@SpringBootTest(
	classes = {AiStudyLeaderApplication.class, AuthControllerTest.TestAuthBeans.class},
	properties = {
		"studypot.rate-limit.enabled=true",
		"studypot.cors.allowed-origins=https://studypot.netlify.app",
		"studypot.cors.allow-credentials=true",
		"studypot.auth.cookie.domain=studypot.rumiclean.com",
		"studypot.auth.cookie.secure=true",
		"studypot.auth.cookie.same-site=None"
	}
)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AuthControllerTest {

	private static final String REFRESH_PATH = ApiPaths.V1 + "/auth/refresh";
	private static final String CSRF_PATH = ApiPaths.V1 + "/auth/csrf";
	private static final String LOGOUT_PATH = ApiPaths.V1 + "/auth/logout";
	private static final String LOGOUT_ALL_PATH = ApiPaths.V1 + "/auth/logout-all";
	private static final String ME_PATH = ApiPaths.V1 + "/users/me";
	private static final String NETLIFY_ORIGIN = "https://studypot.netlify.app";

	private static final GoogleOAuthProfile TEST_PROFILE = new GoogleOAuthProfile(
		"google-123",
		EmailAddress.from("member@example.com"),
		true,
		"Study Member",
		null,
		null,
		"openid email profile"
	);
	private static final AuthSessionMetadata TEST_METADATA = new AuthSessionMetadata("test-agent", "127.0.0.1");

	private final MockMvc mockMvc;
	private final AuthSessionService authSessionService;
	private final ConfigurableRateLimiter rateLimiter;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	AuthControllerTest(MockMvc mockMvc, AuthSessionService authSessionService, ConfigurableRateLimiter rateLimiter) {
		this.mockMvc = mockMvc;
		this.authSessionService = authSessionService;
		this.rateLimiter = rateLimiter;
	}

	@BeforeEach
	void resetRateLimiter() {
		rateLimiter.allow();
	}

	@Test
	void sessionRefreshAndLogoutFlowUsesLockedApiShape() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(get(ME_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TestAuthBeans.FIRST_USER_ID.toString()))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.nickname").value("Study Member"));

		MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
		assertThat(body.has("accessToken")).isFalse();
		assertThat(body.has("refreshToken")).isFalse();
		assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
		assertThat(body.get("expiresIn").asLong()).isPositive();
		assertThat(body.at("/user/email").asText()).isEqualTo("member@example.com");

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfBootstrapReturnsReadableTokenAndCrossSiteCookiePolicy() throws Exception {
		MvcResult result = mockMvc.perform(get(CSRF_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, NETLIFY_ORIGIN))
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andReturn();

		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		String token = body.get("token").asText();
		assertThat(token).isNotBlank();
		assertThat(body.get("cookieName").asText()).isEqualTo("XSRF-TOKEN");
		assertThat(body.get("headerName").asText()).isEqualTo("X-XSRF-TOKEN");
		assertThat(result.getResponse().getHeader("X-XSRF-TOKEN")).isEqualTo(token);
		Cookie xsrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrfCookie).isNotNull();
		assertThat(xsrfCookie.getValue()).isEqualTo(token);
		assertThat(xsrfCookie.getAttribute("SameSite")).isEqualTo("None");
		assertThat(cookieHeaderFromSetCookie(result, "XSRF-TOKEN"))
			.contains("XSRF-TOKEN=" + token)
			.contains("Path=/")
			.contains("Domain=studypot.rumiclean.com")
			.contains("Secure")
			.doesNotContain("HttpOnly");
	}

	@Test
	void crossSiteRefreshCanUseBootstrappedCsrfToken() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get(CSRF_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN))
			.andExpect(status().isOk())
			.andReturn();
		JsonNode body = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
		String token = body.get("token").asText();

		mockMvc.perform(post(REFRESH_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN)
				.header("X-XSRF-TOKEN", token)
				.with(cookie("XSRF-TOKEN", token))
				.with(cookie("studypot_refresh_token", "invalid-refresh-token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void crossSiteRefreshCanUseBootstrappedCsrfHeaderWhenXsrfCookieIsUnavailable() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get(CSRF_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN))
			.andExpect(status().isOk())
			.andReturn();
		JsonNode body = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
		String token = body.get("token").asText();

		mockMvc.perform(post(REFRESH_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN)
				.header("X-XSRF-TOKEN", token)
				.with(cookie("studypot_refresh_token", "invalid-refresh-token")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void crossSiteRefreshRejectsUntrustedOriginCsrfHeaderWithoutXsrfCookie() throws Exception {
		mockMvc.perform(post(REFRESH_PATH)
				.header(HttpHeaders.ORIGIN, "https://evil.example")
				.header("X-XSRF-TOKEN", "attacker-token")
				.with(cookie("studypot_refresh_token", "invalid-refresh-token")))
			.andExpect(status().isForbidden());
	}

	@Test
	void refreshSetsCookiesAndCookieAccessTokenAuthenticates() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(get(ME_PATH)
				.with(cookie("studypot_access_token", session.accessToken())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TestAuthBeans.FIRST_USER_ID.toString()))
			.andExpect(jsonPath("$.email").value("member@example.com"));

		MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_access_token")).isNotBlank();
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_refresh_token")).isNotBlank();
	}

	@Test
	void refreshCanUseJsonBodyWhenCookieIsMissing() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + session.refreshToken() + "\"}")
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode body = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
		assertThat(body.has("accessToken")).isFalse();
		assertThat(body.has("refreshToken")).isFalse();
		assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
		assertThat(body.at("/user/email").asText()).isEqualTo("member@example.com");
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_access_token")).isNotBlank();
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_refresh_token")).isNotBlank();
	}

	@Test
	void refreshIgnoresStaleAccessTokenCookieWhenRefreshTokenIsValid() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		MvcResult refreshResult = mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_access_token", "stale-access-token"))
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_access_token")).isNotBlank();
		assertThat(cookieValueFromSetCookie(refreshResult, "studypot_refresh_token")).isNotBlank();
	}

	@Test
	void refreshWithoutCookieIsRejected() throws Exception {
		mockMvc.perform(post(REFRESH_PATH)
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectedRefreshClearsTokenCookiesAndReturnsStableProblemCode() throws Exception {
		MvcResult result = mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_access_token", "stale-access-token"))
				.with(cookie("studypot_refresh_token", "invalid-refresh-token"))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Unauthorized"))
			.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
			.andReturn();

		assertThat(cookieHeaderFromSetCookie(result, "studypot_access_token"))
			.contains("studypot_access_token=")
			.contains("Max-Age=0")
			.contains("Path=/")
			.contains("Secure")
			.contains("SameSite=None");
		assertThat(cookieHeaderFromSetCookie(result, "studypot_refresh_token"))
			.contains("studypot_refresh_token=")
			.contains("Max-Age=0")
			.contains("Path=/")
			.contains("Secure")
			.contains("SameSite=None");
	}

	@Test
	void cookieBackedStateChangingRequestsRequireCsrfToken() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.with(cookie("studypot_access_token", session.accessToken())))
			.andExpect(status().isForbidden());

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", session.refreshToken())))
			.andExpect(status().isForbidden());
	}

	@Test
	void cookieBackedStateChangingRequestsAcceptCsrfToken() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.with(cookie("studypot_access_token", session.accessToken()))
				.with(xsrf("logout-xsrf")))
			.andExpect(status().isNoContent());
	}

	@Test
	void cookieBackedLogoutAcceptsTrustedOriginCsrfHeaderWithoutXsrfCookie() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.header(HttpHeaders.ORIGIN, NETLIFY_ORIGIN)
				.header("X-XSRF-TOKEN", "bootstrapped-token")
				.with(cookie("studypot_access_token", session.accessToken())))
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
	void currentUserProfileCanBeUpdatedWithDomainAttributes() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(patch(ME_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "nickname": "현우2",
					  "profileImage": "https://cdn.studypot.dev/profiles/hyunwoo.png",
					  "bio": "백엔드와 Vue를 함께 공부합니다.",
					  "preferredTopics": ["Spring Boot", "JPA"],
					  "skillLevel": "intermediate"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(TestAuthBeans.FIRST_USER_ID.toString()))
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.nickname").value("현우2"))
			.andExpect(jsonPath("$.profileImage").value("https://cdn.studypot.dev/profiles/hyunwoo.png"))
			.andExpect(jsonPath("$.bio").value("백엔드와 Vue를 함께 공부합니다."))
			.andExpect(jsonPath("$.preferredTopics[0]").value("Spring Boot"))
			.andExpect(jsonPath("$.preferredTopics[1]").value("JPA"))
			.andExpect(jsonPath("$.skillLevel").value("intermediate"));
	}

	@Test
	void currentUserRateLimitReturnsTooManyRequests() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);
		rateLimiter.reject(RateLimitDecision.rejected(61, 60, Duration.ofSeconds(12)));

		mockMvc.perform(get(ME_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string(HttpHeaders.RETRY_AFTER, "12"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Too Many Requests"))
			.andExpect(jsonPath("$.detail").value("current user lookup rate limit exceeded."));
	}

	@Test
	void logoutAllRevokesEverySessionForAuthenticatedUser() throws Exception {
		AuthTokenResult firstSession = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);
		AuthTokenResult secondSession = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(post(LOGOUT_ALL_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(secondSession.accessToken())))
			.andExpect(status().isNoContent());

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", firstSession.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", secondSession.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesCurrentSessionCookie() throws Exception {
		AuthTokenResult session = authSessionService.loginWithGoogleProfile(TEST_PROFILE, TEST_METADATA);

		mockMvc.perform(post(LOGOUT_PATH)
				.header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken()))
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("logout-xsrf")))
			.andExpect(status().isNoContent());

		mockMvc.perform(post(REFRESH_PATH)
				.with(cookie("studypot_refresh_token", session.refreshToken()))
				.with(xsrf("refresh-xsrf")))
			.andExpect(status().isUnauthorized());
	}

	private static String bearer(String accessToken) {
		return "Bearer " + accessToken;
	}

	private static String cookieValueFromSetCookie(MvcResult result, String name) {
		String setCookie = cookieHeaderFromSetCookie(result, name);
		String prefix = name + "=";
		int end = setCookie.indexOf(';');
		return end < 0 ? setCookie.substring(prefix.length()) : setCookie.substring(prefix.length(), end);
	}

	private static String cookieHeaderFromSetCookie(MvcResult result, String name) {
		String prefix = name + "=";
		return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)
			.stream()
			.filter(header -> header.startsWith(prefix))
			.findFirst()
			.orElseThrow();
	}

	private static RequestPostProcessor cookie(String name, String value) {
		return request -> {
			jakarta.servlet.http.Cookie[] existingCookies = request.getCookies();
			jakarta.servlet.http.Cookie[] cookies = existingCookies == null
				? new jakarta.servlet.http.Cookie[1]
				: java.util.Arrays.copyOf(existingCookies, existingCookies.length + 1);
			cookies[cookies.length - 1] = new MockCookie(name, value);
			request.setCookies(cookies);
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
	static class TestAuthBeans {

		static final UUID FIRST_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201");
		private static final UUID FIRST_ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000202");
		private static final UUID FIRST_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000203");
		private static final UUID SECOND_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000204");
		private static final UUID THIRD_REFRESH_ID = UUID.fromString("018f0000-0000-7000-8000-000000000205");
		private static final Instant NOW = Instant.now();

		@Bean
		@Primary
		GoogleOAuthCodeExchangePort testGoogleOAuthCodeExchangePort() {
			return command -> new GoogleOAuthProfile(
				"google-123",
				EmailAddress.from("member@example.com"),
				true,
				"Study Member",
				null,
				NOW.plus(Duration.ofHours(1)),
				"openid email profile"
			);
		}

		@Bean
		@Primary
		AuthAccountRepository testAuthAccountRepository() {
			return new FakeAuthAccountRepository();
		}

		@Bean
		@Primary
		RefreshTokenRepository testRefreshTokenRepository() {
			return new FakeRefreshTokenRepository();
		}

		@Bean
		@Primary
		ConfigurableRateLimiter testRateLimiter() {
			return new ConfigurableRateLimiter();
		}

		@Bean
		@Primary
		AuthSessionService testAuthSessionService(
			GoogleOAuthCodeExchangePort google,
			AuthAccountRepository authRepository,
			RefreshTokenRepository refreshTokenRepository,
			AccessTokenIssuer accessTokenIssuer
		) {
			return new AuthSessionService(
				new GoogleOAuthLoginService(
					google,
					authRepository,
					Clock.fixed(NOW, ZoneOffset.UTC),
					new DeterministicIds(FIRST_USER_ID, FIRST_ACCOUNT_ID)
				),
				authRepository,
				refreshTokenRepository,
				accessTokenIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC),
				new DeterministicIds(FIRST_REFRESH_ID, SECOND_REFRESH_ID, THIRD_REFRESH_ID),
				new DeterministicRefreshTokens("refresh-one", "refresh-two", "refresh-three"),
				Duration.ofDays(30)
			);
		}
	}

	static final class ConfigurableRateLimiter implements RateLimiter {

		private RateLimitDecision decision;

		void allow() {
			decision = RateLimitDecision.allowed(1, 60);
		}

		void reject(RateLimitDecision decision) {
			this.decision = decision;
		}

		@Override
		public RateLimitDecision check(String key, long limit, Duration window) {
			return decision;
		}
	}

	private static final class FakeAuthAccountRepository implements AuthAccountRepository {

		private final Map<UUID, AuthUser> usersById = new HashMap<>();
		private final Map<String, UUID> userIdsByEmailLiveKey = new HashMap<>();
		private final Map<String, OAuthAccount> activeAccounts = new HashMap<>();

		@Override
		public Optional<AuthUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<AuthUser> findActiveUserByEmail(EmailAddress email) {
			return Optional.ofNullable(userIdsByEmailLiveKey.get(email.liveKey()))
				.map(usersById::get);
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			return Optional.ofNullable(activeAccounts.get(provider.liveKey(providerUserId)));
		}

		@Override
		public AuthUser save(AuthUser user) {
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
