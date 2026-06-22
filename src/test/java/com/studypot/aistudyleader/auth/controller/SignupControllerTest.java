package com.studypot.aistudyleader.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.auth.service.SignupService;
import com.studypot.aistudyleader.global.api.ApiPaths;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, SignupControllerTest.TestSignupBeans.class})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SignupControllerTest {

	private static final String SIGNUP_PATH = ApiPaths.V1 + "/auth/signup";
	private static final String EMAIL_AVAILABILITY_PATH = ApiPaths.V1 + "/auth/signup/email-availability";

	private final MockMvc mockMvc;
	private final MutableAuthAccountRepository repository;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	SignupControllerTest(
		MockMvc mockMvc,
		MutableAuthAccountRepository repository,
		PasswordEncoder passwordEncoder
	) {
		this.mockMvc = mockMvc;
		this.repository = repository;
		this.passwordEncoder = passwordEncoder;
	}

	@BeforeEach
	void resetRepository() {
		repository.clear();
	}

	@Test
	void signupCreatesPasswordUserThroughPublicApiBoundary() throws Exception {
		mockMvc.perform(post(SIGNUP_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "New.Member@Example.com",
					  "nickname": " 현우 ",
					  "password": "password123"
					}
					""")
				.with(xsrf("signup-xsrf")))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.email").value("new.member@example.com"))
			.andExpect(jsonPath("$.nickname").value("현우"));

		AuthUser saved = repository.findActiveUserByEmail(EmailAddress.from("new.member@example.com")).orElseThrow();
		assertThat(saved.passwordHash())
			.hasValueSatisfying(hash -> {
				assertThat(hash).doesNotContain("password123");
				assertThat(passwordEncoder.matches("password123", hash)).isTrue();
			});
	}

	@Test
	void signupRejectsDuplicateEmailThroughValidationProblem() throws Exception {
		repository.save(AuthUser.createWithPassword(
			UUID.fromString("018f0000-0000-7000-8000-000000000601"),
			EmailAddress.from("new.member@example.com"),
			"이미가입",
			"stored-hash",
			TestSignupBeans.NOW
		));

		mockMvc.perform(post(SIGNUP_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "new.member@example.com",
					  "nickname": "현우",
					  "password": "password123"
					}
					""")
				.with(xsrf("signup-xsrf")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("email is already registered."));
	}

	@Test
	void signupRejectsInvalidEmailAndPasswordBeforeSaving() throws Exception {
		mockMvc.perform(post(SIGNUP_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "invalid-email",
					  "nickname": "현우",
					  "password": "short"
					}
					""")
				.with(xsrf("signup-xsrf")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("email must be valid"))
			.andExpect(jsonPath("$.fieldErrors[1].field").value("password"))
			.andExpect(jsonPath("$.fieldErrors[1].message").value("password must be at least 8 characters."));

		assertThat(repository.savedUserCount()).isZero();
	}

	@Test
	void signupRejectsBlankFieldsWithServiceConsistentProblemMessages() throws Exception {
		mockMvc.perform(post(SIGNUP_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "email": "",
					  "nickname": "",
					  "password": ""
					}
					""")
				.with(xsrf("signup-xsrf")))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
			.andExpect(jsonPath("$.fieldErrors[0].message").value("email is required."))
			.andExpect(jsonPath("$.fieldErrors[1].field").value("nickname"))
			.andExpect(jsonPath("$.fieldErrors[1].message").value("nickname is required."))
			.andExpect(jsonPath("$.fieldErrors[2].field").value("password"))
			.andExpect(jsonPath("$.fieldErrors[2].message").value("password must be at least 8 characters."));

		assertThat(repository.savedUserCount()).isZero();
	}

	@Test
	void emailAvailabilityReportsExistingEmailThroughPublicApiBoundary() throws Exception {
		repository.save(AuthUser.createWithPassword(
			UUID.fromString("018f0000-0000-7000-8000-000000000602"),
			EmailAddress.from("member@example.com"),
			"회원",
			"stored-hash",
			TestSignupBeans.NOW
		));

		mockMvc.perform(get(EMAIL_AVAILABILITY_PATH)
				.param("email", "member@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value("member@example.com"))
			.andExpect(jsonPath("$.available").value(false));
	}

	private static RequestPostProcessor xsrf(String value) {
		return request -> {
			request.setCookies(new MockCookie("XSRF-TOKEN", value));
			request.addHeader("X-XSRF-TOKEN", value);
			return request;
		};
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestSignupBeans {

		private static final Instant NOW = Instant.parse("2026-06-09T00:00:00Z");

		@Bean
		@Primary
		MutableAuthAccountRepository testAuthAccountRepository() {
			return new MutableAuthAccountRepository();
		}

		@Bean
		@Primary
		SignupService testSignupService(AuthAccountRepository repository, PasswordEncoder passwordEncoder) {
			AtomicLong sequence = new AtomicLong(0);
			Supplier<UUID> idGenerator = () -> new UUID(0x018f000000007000L, 0x8000000000000600L + sequence.incrementAndGet());
			return new SignupService(repository, Clock.fixed(NOW, ZoneOffset.UTC), idGenerator, passwordEncoder);
		}
	}

	static final class MutableAuthAccountRepository implements AuthAccountRepository {

		private final Map<UUID, AuthUser> usersById = new ConcurrentHashMap<>();
		private final Map<String, UUID> userIdsByEmail = new ConcurrentHashMap<>();
		private final Map<String, OAuthAccount> oauthAccountsByLiveKey = new ConcurrentHashMap<>();

		@Override
		public Optional<AuthUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<AuthUser> findActiveUserByEmail(EmailAddress email) {
			return Optional.ofNullable(userIdsByEmail.get(email.liveKey()))
				.map(usersById::get);
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			return Optional.ofNullable(oauthAccountsByLiveKey.get(provider.liveKey(providerUserId)));
		}

		@Override
		public AuthUser save(AuthUser user) {
			usersById.put(user.id(), user);
			userIdsByEmail.put(user.email().liveKey(), user.id());
			return user;
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			oauthAccountsByLiveKey.put(account.providerAccountLiveKey(), account);
			return account;
		}

		private int savedUserCount() {
			return usersById.size();
		}

		private void clear() {
			usersById.clear();
			userIdsByEmail.clear();
			oauthAccountsByLiveKey.clear();
		}
	}
}
