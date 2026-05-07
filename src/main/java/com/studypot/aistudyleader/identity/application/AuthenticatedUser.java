package com.studypot.aistudyleader.identity.application;

import com.studypot.aistudyleader.identity.domain.IdentityUser;
import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String nickname) {

	public static AuthenticatedUser from(IdentityUser user) {
		return new AuthenticatedUser(user.id(), user.email().value(), user.nickname());
	}
}
