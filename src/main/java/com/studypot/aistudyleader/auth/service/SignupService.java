package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

public class SignupService {

	private static final int MIN_PASSWORD_LENGTH = 8;

	private final AuthAccountRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final PasswordEncoder passwordEncoder;

	public SignupService(
		AuthAccountRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		PasswordEncoder passwordEncoder
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
	}

	@Transactional
	public SignupResult signup(SignupCommand command) {
		SignupCommand validCommand = requireValidCommand(command);
		EmailAddress email = email(validCommand.email());
		if (!isEmailAvailable(email.value())) {
			throw new InvalidAuthRequestException("email", "email is already registered.");
		}

		String passwordHash = passwordEncoder.encode(validCommand.password());
		AuthUser user = AuthUser.createWithPassword(idGenerator.get(), email, validCommand.nickname().strip(), passwordHash, clock.instant());
		AuthUser saved = repository.save(user);
		return SignupResult.from(saved);
	}

	@Transactional(readOnly = true)
	public boolean isEmailAvailable(String rawEmail) {
		EmailAddress email = email(rawEmail);
		return repository.findActiveUserByEmail(email).isEmpty();
	}

	private static SignupCommand requireValidCommand(SignupCommand command) {
		if (command == null) {
			throw new InvalidAuthRequestException("request", "signup request is required.");
		}
		if (command.email() == null || command.email().isBlank()) {
			throw new InvalidAuthRequestException("email", "email is required.");
		}
		if (command.nickname() == null || command.nickname().isBlank()) {
			throw new InvalidAuthRequestException("nickname", "nickname is required.");
		}
		if (command.password() == null || command.password().length() < MIN_PASSWORD_LENGTH) {
			throw new InvalidAuthRequestException("password", "password must be at least 8 characters.");
		}
		return command;
	}

	private static EmailAddress email(String rawEmail) {
		try {
			return EmailAddress.from(rawEmail);
		} catch (IllegalArgumentException exception) {
			throw new InvalidAuthRequestException("email", exception.getMessage());
		}
	}
}
