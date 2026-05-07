package com.studypot.aistudyleader.identity.application;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class RefreshTokenSession {

	private final UUID id;
	private final UUID userId;
	private final String tokenHash;
	private final String deviceInfo;
	private final String ipAddress;
	private final Instant expiresAt;
	private final Instant revokedAt;
	private final Instant createdAt;

	private RefreshTokenSession(
		UUID id,
		UUID userId,
		String tokenHash,
		String deviceInfo,
		String ipAddress,
		Instant expiresAt,
		Instant revokedAt,
		Instant createdAt
	) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.tokenHash = requireHash(tokenHash);
		this.deviceInfo = blankToNull(deviceInfo);
		this.ipAddress = blankToNull(ipAddress);
		this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		this.revokedAt = revokedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	public static RefreshTokenSession create(
		UUID id,
		UUID userId,
		String tokenHash,
		String deviceInfo,
		String ipAddress,
		Instant expiresAt,
		Instant createdAt
	) {
		return rehydrate(id, userId, tokenHash, deviceInfo, ipAddress, expiresAt, null, createdAt);
	}

	public static RefreshTokenSession rehydrate(
		UUID id,
		UUID userId,
		String tokenHash,
		String deviceInfo,
		String ipAddress,
		Instant expiresAt,
		Instant revokedAt,
		Instant createdAt
	) {
		return new RefreshTokenSession(id, userId, tokenHash, deviceInfo, ipAddress, expiresAt, revokedAt, createdAt);
	}

	public RefreshTokenSession revoke(Instant revokedAt) {
		Objects.requireNonNull(revokedAt, "revokedAt must not be null");
		if (this.revokedAt != null) {
			return this;
		}
		return new RefreshTokenSession(id, userId, tokenHash, deviceInfo, ipAddress, expiresAt, revokedAt, createdAt);
	}

	public boolean isActiveAt(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public UUID id() {
		return id;
	}

	public UUID userId() {
		return userId;
	}

	public String tokenHash() {
		return tokenHash;
	}

	public Optional<String> deviceInfo() {
		return Optional.ofNullable(deviceInfo);
	}

	public Optional<String> ipAddress() {
		return Optional.ofNullable(ipAddress);
	}

	public Instant expiresAt() {
		return expiresAt;
	}

	public Optional<Instant> revokedAt() {
		return Optional.ofNullable(revokedAt);
	}

	public Instant createdAt() {
		return createdAt;
	}

	private static String requireHash(String tokenHash) {
		if (tokenHash == null || tokenHash.isBlank()) {
			throw new IllegalArgumentException("tokenHash must not be blank");
		}
		return tokenHash.strip();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
