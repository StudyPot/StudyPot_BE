package com.studypot.aistudyleader.identity.application;

import java.util.UUID;

public record GoogleOAuthLoginResult(UUID userId, String email, String nickname, String profileImage) {
}
