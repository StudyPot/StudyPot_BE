package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import java.util.UUID;

public record SignupResult(
	UUID id,
	String email,
	String nickname
) {

	public static SignupResult from(AuthUser user) {
		return new SignupResult(user.id(), user.email().value(), user.nickname());
	}
}
