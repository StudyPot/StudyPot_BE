package com.studypot.aistudyleader.auth.domain;

import com.studypot.aistudyleader.global.domain.AggregateRoot;
import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class AuthUser extends AggregateRoot<UUID> {

	private static final int MAX_NICKNAME_LENGTH = 80;

	private final EmailAddress email;
	private final String nickname;
	private final String profileImage;
	private final Instant lastLoginAt;
	private final AuditMetadata auditMetadata;

	private AuthUser(
		UUID id,
		EmailAddress email,
		String nickname,
		String profileImage,
		Instant lastLoginAt,
		AuditMetadata auditMetadata
	) {
		super(id);
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.nickname = requireNickname(nickname);
		this.profileImage = blankToNull(profileImage);
		this.lastLoginAt = lastLoginAt;
		this.auditMetadata = Objects.requireNonNull(auditMetadata, "auditMetadata must not be null");
	}

	public static AuthUser create(
		UUID id,
		EmailAddress email,
		String nickname,
		String profileImage,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new AuthUser(id, email, nickname, profileImage, null, AuditMetadata.created(now));
	}

	public static AuthUser rehydrate(
		UUID id,
		EmailAddress email,
		String nickname,
		String profileImage,
		Instant lastLoginAt,
		AuditMetadata auditMetadata
	) {
		return new AuthUser(id, email, nickname, profileImage, lastLoginAt, auditMetadata);
	}

	public AuthUser recordLogin(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return new AuthUser(id(), email, nickname, profileImage, now, auditMetadata.touch(now));
	}

	public EmailAddress email() {
		return email;
	}

	public String nickname() {
		return nickname;
	}

	public Optional<String> profileImage() {
		return Optional.ofNullable(profileImage);
	}

	public Optional<Instant> lastLoginAt() {
		return Optional.ofNullable(lastLoginAt);
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	private static String requireNickname(String nickname) {
		if (nickname == null || nickname.isBlank()) {
			throw new IllegalArgumentException("nickname must not be blank");
		}
		String normalized = nickname.strip();
		if (normalized.length() > MAX_NICKNAME_LENGTH) {
			throw new IllegalArgumentException(
				"nickname length must be <= " + MAX_NICKNAME_LENGTH + ": " + normalized.length()
			);
		}
		return normalized;
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
