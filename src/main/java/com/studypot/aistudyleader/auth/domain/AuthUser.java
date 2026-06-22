package com.studypot.aistudyleader.auth.domain;

import com.studypot.aistudyleader.global.domain.AggregateRoot;
import com.studypot.aistudyleader.global.domain.AuditMetadata;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AuthUser extends AggregateRoot<UUID> {

	private static final int MAX_NICKNAME_LENGTH = 80;
	private static final int MAX_PASSWORD_HASH_LENGTH = 100;
	private static final int MAX_BIO_LENGTH = 1000;
	private static final int MAX_PREFERRED_TOPIC_COUNT = 20;
	private static final int MAX_PREFERRED_TOPIC_LENGTH = 80;
	private static final Set<String> ALLOWED_SKILL_LEVELS = Set.of("beginner", "intermediate", "advanced");

	private final EmailAddress email;
	private final String passwordHash;
	private final String nickname;
	private final String profileImage;
	private final String bio;
	private final List<String> preferredTopics;
	private final String skillLevel;
	private final Instant lastLoginAt;
	private final AuditMetadata auditMetadata;

	private AuthUser(
		UUID id,
		EmailAddress email,
		String passwordHash,
		String nickname,
		String profileImage,
		String bio,
		List<String> preferredTopics,
		String skillLevel,
		Instant lastLoginAt,
		AuditMetadata auditMetadata
	) {
		super(id);
		this.email = Objects.requireNonNull(email, "email must not be null");
		this.passwordHash = requirePasswordHash(passwordHash);
		this.nickname = requireNickname(nickname);
		this.profileImage = blankToNull(profileImage);
		this.bio = requireBio(bio);
		this.preferredTopics = normalizePreferredTopics(preferredTopics);
		this.skillLevel = requireSkillLevel(skillLevel);
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
		return new AuthUser(id, email, null, nickname, profileImage, null, List.of(), null, null, AuditMetadata.created(now));
	}

	public static AuthUser createWithPassword(
		UUID id,
		EmailAddress email,
		String nickname,
		String passwordHash,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new AuthUser(id, email, passwordHash, nickname, null, null, List.of(), null, null, AuditMetadata.created(now));
	}

	public static AuthUser rehydrate(
		UUID id,
		EmailAddress email,
		String passwordHash,
		String nickname,
		String profileImage,
		String bio,
		List<String> preferredTopics,
		String skillLevel,
		Instant lastLoginAt,
		AuditMetadata auditMetadata
	) {
		return new AuthUser(id, email, passwordHash, nickname, profileImage, bio, preferredTopics, skillLevel, lastLoginAt, auditMetadata);
	}

	public AuthUser recordLogin(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return new AuthUser(id(), email, passwordHash, nickname, profileImage, bio, preferredTopics, skillLevel, now, auditMetadata.touch(now));
	}

	public AuthUser updateProfile(
		String nickname,
		String profileImage,
		String bio,
		List<String> preferredTopics,
		String skillLevel,
		Instant now
	) {
		Objects.requireNonNull(now, "now must not be null");
		return new AuthUser(id(), email, passwordHash, nickname, profileImage, bio, preferredTopics, skillLevel, lastLoginAt, auditMetadata.touch(now));
	}

	public EmailAddress email() {
		return email;
	}

	public Optional<String> passwordHash() {
		return Optional.ofNullable(passwordHash);
	}

	public String nickname() {
		return nickname;
	}

	public Optional<String> profileImage() {
		return Optional.ofNullable(profileImage);
	}

	public Optional<String> bio() {
		return Optional.ofNullable(bio);
	}

	public List<String> preferredTopics() {
		return preferredTopics;
	}

	public Optional<String> skillLevel() {
		return Optional.ofNullable(skillLevel);
	}

	public Optional<Instant> lastLoginAt() {
		return Optional.ofNullable(lastLoginAt);
	}

	public AuditMetadata auditMetadata() {
		return auditMetadata;
	}

	private static String requirePasswordHash(String passwordHash) {
		if (passwordHash == null || passwordHash.isBlank()) {
			return null;
		}
		String normalized = passwordHash.strip();
		if (normalized.length() > MAX_PASSWORD_HASH_LENGTH) {
			throw new IllegalArgumentException(
				"passwordHash length must be <= " + MAX_PASSWORD_HASH_LENGTH + ": " + normalized.length()
			);
		}
		return normalized;
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

	private static String requireBio(String bio) {
		String normalized = blankToNull(bio);
		if (normalized != null && normalized.length() > MAX_BIO_LENGTH) {
			throw new IllegalArgumentException("bio length must be <= " + MAX_BIO_LENGTH + ": " + normalized.length());
		}
		return normalized;
	}

	private static List<String> normalizePreferredTopics(List<String> preferredTopics) {
		if (preferredTopics == null || preferredTopics.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<String> normalized = new LinkedHashSet<>();
		for (String topic : preferredTopics) {
			String value = blankToNull(topic);
			if (value == null) {
				continue;
			}
			if (value.length() > MAX_PREFERRED_TOPIC_LENGTH) {
				throw new IllegalArgumentException(
					"preferred topic length must be <= " + MAX_PREFERRED_TOPIC_LENGTH + ": " + value.length()
				);
			}
			normalized.add(value);
		}
		if (normalized.size() > MAX_PREFERRED_TOPIC_COUNT) {
			throw new IllegalArgumentException("preferred topic count must be <= " + MAX_PREFERRED_TOPIC_COUNT);
		}
		return List.copyOf(new ArrayList<>(normalized));
	}

	private static String requireSkillLevel(String skillLevel) {
		String normalized = blankToNull(skillLevel);
		if (normalized == null) {
			return null;
		}
		String lowerCased = normalized.toLowerCase(java.util.Locale.ROOT);
		if (!ALLOWED_SKILL_LEVELS.contains(lowerCased)) {
			throw new IllegalArgumentException("skillLevel must be one of beginner, intermediate, advanced");
		}
		return lowerCased;
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
