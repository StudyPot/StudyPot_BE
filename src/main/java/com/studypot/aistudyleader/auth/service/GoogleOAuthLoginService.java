package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.auth.repository.AuthUniquenessConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class GoogleOAuthLoginService {

	private final GoogleOAuthCodeExchangePort googleOAuth;
	private final AuthAccountRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public GoogleOAuthLoginService(
		GoogleOAuthCodeExchangePort googleOAuth,
		AuthAccountRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this.googleOAuth = Objects.requireNonNull(googleOAuth, "googleOAuth must not be null");
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public GoogleOAuthLoginResult login(GoogleOAuthLoginCommand command) {
		Objects.requireNonNull(command, "command must not be null");

		GoogleOAuthProfile profile = Objects.requireNonNull(googleOAuth.exchange(command), "Google OAuth profile must not be null");
		return login(profile);
	}

	@Transactional
	public GoogleOAuthLoginResult login(GoogleOAuthProfile profile) {
		Objects.requireNonNull(profile, "profile must not be null");

		if (!profile.emailVerified()) {
			throw new OAuthLoginRejectedException("Google OAuth login requires a verified Google email.");
		}

		Instant now = clock.instant();
		AuthUser user = loginWithProfile(profile, now, true);

		return new GoogleOAuthLoginResult(
			user.id(),
			user.email().value(),
			user.nickname(),
			user.profileImage().orElse(null)
		);
	}

	private AuthUser loginWithProfile(GoogleOAuthProfile profile, Instant now, boolean retryOnConflict) {
		try {
			return repository.findActiveOAuthAccount(OAuthProvider.GOOGLE, profile.providerUserId())
				.map(account -> updateExistingAccount(account, profile, now))
				.orElseGet(() -> createOrLinkAccount(profile, now));
		} catch (AuthUniquenessConflictException exception) {
			if (!retryOnConflict) {
				throw exception;
			}
			return loginWithProfile(profile, now, false);
		}
	}

	private AuthUser updateExistingAccount(OAuthAccount account, GoogleOAuthProfile profile, Instant now) {
		AuthUser user = repository.findActiveUser(account.userId())
			.orElseThrow(() -> new OAuthLoginRejectedException("OAuth account is not linked to a live user."));
		AuthUser loggedInUser = repository.save(user.recordLogin(now));
		repository.save(account.sync(profile.email(), profile.tokenExpiresAt(), profile.scope(), now));
		return loggedInUser;
	}

	private AuthUser createOrLinkAccount(GoogleOAuthProfile profile, Instant now) {
		AuthUser user = repository.findActiveUserByEmail(profile.email())
			.map(existing -> existing.recordLogin(now))
			.orElseGet(() -> AuthUser.create(
				idGenerator.get(),
				profile.email(),
				nicknameFrom(profile),
				profile.picture(),
				now
			).recordLogin(now));
		AuthUser savedUser = repository.save(user);
		repository.save(OAuthAccount.connect(
			idGenerator.get(),
			savedUser.id(),
			OAuthProvider.GOOGLE,
			profile.providerUserId(),
			profile.email(),
			profile.tokenExpiresAt(),
			profile.scope(),
			now
		));
		return savedUser;
	}

	private static String nicknameFrom(GoogleOAuthProfile profile) {
		String candidate = profile.name();
		if (candidate == null || candidate.isBlank()) {
			candidate = localPart(profile.email());
		}
		return candidate.strip();
	}

	private static String localPart(EmailAddress email) {
		String value = email.value();
		int at = value.indexOf('@');
		return at <= 0 ? "Google User" : value.substring(0, at);
	}
}
