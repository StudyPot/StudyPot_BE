package com.studypot.aistudyleader.auth.service;

import java.util.UUID;

public record GoogleOAuthLoginResult(UUID userId, String email, String nickname, String profileImage) {

	@Override
	public String toString() {
		return "GoogleOAuthLoginResult[userId=%s, email=****, nickname=****, profileImage=****]"
			.formatted(userId);
	}
}
