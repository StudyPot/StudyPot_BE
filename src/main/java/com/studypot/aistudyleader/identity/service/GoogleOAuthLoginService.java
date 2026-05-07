package com.studypot.aistudyleader.identity.service;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import com.studypot.aistudyleader.identity.repository.IdentityAccountRepository;
import com.studypot.aistudyleader.identity.repository.IdentityUniquenessConflictException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class GoogleOAuthLoginService {

	private final GoogleOAuthCodeExchangePort googleOAuth;
	private final IdentityAccountRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public GoogleOAuthLoginService(
		GoogleOAuthCodeExchangePort googleOAuth,
		IdentityAccountRepository repository,
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
		if (!profile.emailVerified()) {
			throw new OAuthLoginRejectedException("Google OAuth login requires a verified Google email.");
		}

		Instant now = clock.instant();
		IdentityUser user = loginWithProfile(profile, now, true);

		return new GoogleOAuthLoginResult(
			user.id(),
			user.email().value(),
			user.nickname(),
			user.profileImage().orElse(null)
		);
	}

	private IdentityUser loginWithProfile(GoogleOAuthProfile profile, Instant now, boolean retryOnConflict) {
		try {
			return repository.findActiveOAuthAccount(OAuthProvider.GOOGLE, profile.providerUserId())
				.map(account -> updateExistingAccount(account, profile, now))
				.orElseGet(() -> createOrLinkAccount(profile, now));
		} catch (IdentityUniquenessConflictException exception) {
			if (!retryOnConflict) {
				throw exception;
			}
			return loginWithProfile(profile, now, false);
		}
	}

	private IdentityUser updateExistingAccount(OAuthAccount account, GoogleOAuthProfile profile, Instant now) {
		IdentityUser user = repository.findActiveUser(account.userId())
			.orElseThrow(() -> new OAuthLoginRejectedException("OAuth account is not linked to a live user."));
		IdentityUser loggedInUser = repository.save(user.recordLogin(now));
		repository.save(account.sync(profile.email(), profile.tokenExpiresAt(), profile.scope(), now));
		return loggedInUser;
	}

	private IdentityUser createOrLinkAccount(GoogleOAuthProfile profile, Instant now) {
		IdentityUser user = repository.findActiveUserByEmail(profile.email())
			.map(existing -> existing.recordLogin(now))
			.orElseGet(() -> IdentityUser.create(
				idGenerator.get(),
				profile.email(),
				nicknameFrom(profile),
				profile.picture(),
				now
			));
		IdentityUser savedUser = repository.save(user);
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
