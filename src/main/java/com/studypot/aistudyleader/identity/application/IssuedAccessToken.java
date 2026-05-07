package com.studypot.aistudyleader.identity.application;

import java.time.Instant;

public record IssuedAccessToken(String token, Instant expiresAt) {
}
