package com.studypot.aistudyleader.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class SignupServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f7a4e-0000-7000-9000-000000000077");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-08T00:00:00Z"), ZoneOffset.UTC);
	private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

	private final AuthAccountRepository repository = org.mockito.Mockito.mock(AuthAccountRepository.class);
	private final SignupService service = new SignupService(repository, CLOCK, () -> USER_ID, PASSWORD_ENCODER);

	@Test
	void signupCreatesUserWhenEmailIsAvailable() {
		when(repository.findActiveUserByEmail(EmailAddress.from("new@example.com"))).thenReturn(Optional.empty());
		when(repository.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SignupResult result = service.signup(new SignupCommand("new@example.com", " 현우 ", "password123"));

		assertThat(result.id()).isEqualTo(USER_ID);
		assertThat(result.email()).isEqualTo("new@example.com");
		assertThat(result.nickname()).isEqualTo("현우");

		ArgumentCaptor<AuthUser> captor = ArgumentCaptor.forClass(AuthUser.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().auditMetadata().createdAt()).isEqualTo(CLOCK.instant());
		assertThat(captor.getValue().passwordHash()).isPresent();
		String passwordHash = captor.getValue().passwordHash().orElseThrow();
		assertThat(passwordHash).doesNotContain("password123");
		assertThat(PASSWORD_ENCODER.matches("password123", passwordHash)).isTrue();
	}

	@Test
	void signupRejectsDuplicateEmail() {
		AuthUser existing = AuthUser.create(USER_ID, EmailAddress.from("new@example.com"), "이미가입", null, CLOCK.instant());
		when(repository.findActiveUserByEmail(EmailAddress.from("new@example.com"))).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> service.signup(new SignupCommand("new@example.com", "현우", "password123")))
			.isInstanceOf(InvalidAuthRequestException.class)
			.hasMessageContaining("already registered");

		verify(repository, never()).save(any(AuthUser.class));
	}

	@Test
	void signupRejectsShortPassword() {
		assertThatThrownBy(() -> service.signup(new SignupCommand("new@example.com", "현우", "short")))
			.isInstanceOf(InvalidAuthRequestException.class)
			.hasMessageContaining("at least 8");
	}
}
