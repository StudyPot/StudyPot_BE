package com.studypot.aistudyleader.identity.service;

import java.time.Instant;

public record IssuedAccessToken(String token, Instant expiresAt) {
}
