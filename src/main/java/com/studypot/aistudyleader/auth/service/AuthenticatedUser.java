package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUser(
	UUID id,
	String email,
	String nickname,
	String profileImage,
	String bio,
	List<String> preferredTopics,
	String skillLevel
) {

	public AuthenticatedUser {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(nickname, "nickname must not be null");
		preferredTopics = preferredTopics == null ? List.of() : List.copyOf(preferredTopics);
	}

	public AuthenticatedUser(UUID id, String email, String nickname) {
		this(id, email, nickname, null, null, List.of(), null);
	}

	public static AuthenticatedUser from(AuthUser user) {
		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(user.id(), "user.id must not be null");
		Objects.requireNonNull(user.email(), "user.email must not be null");
		Objects.requireNonNull(user.email().value(), "user.email.value must not be null");
		Objects.requireNonNull(user.nickname(), "user.nickname must not be null");
		return new AuthenticatedUser(
			user.id(),
			user.email().value(),
			user.nickname(),
			user.profileImage().orElse(null),
			user.bio().orElse(null),
			user.preferredTopics(),
			user.skillLevel().orElse(null)
		);
	}
}
