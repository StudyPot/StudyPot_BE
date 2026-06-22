package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String nickname) {

	public AuthenticatedUser {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(email, "email must not be null");
		Objects.requireNonNull(nickname, "nickname must not be null");
	}

	public static AuthenticatedUser from(AuthUser user) {
		Objects.requireNonNull(user, "user must not be null");
		Objects.requireNonNull(user.id(), "user.id must not be null");
		Objects.requireNonNull(user.email(), "user.email must not be null");
		Objects.requireNonNull(user.email().value(), "user.email.value must not be null");
		Objects.requireNonNull(user.nickname(), "user.nickname must not be null");
		return new AuthenticatedUser(user.id(), user.email().value(), user.nickname());
	}
}
