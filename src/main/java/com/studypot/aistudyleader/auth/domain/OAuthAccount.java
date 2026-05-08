package com.studypot.aistudyleader.auth.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class OAuthAccount {

	private final UUID id;
	private final UUID userId;
	private final OAuthProvider provider;
	private final String providerUserId;
	private final EmailAddress email;
	private final Instant tokenExpiresAt;
	private final String scope;
	private final Instant connectedAt;
	private final Instant lastSyncedAt;
	private final Instant deletedAt;

	private OAuthAccount(
		UUID id,
		UUID userId,
		OAuthProvider provider,
		String providerUserId,
		EmailAddress email,
		Instant tokenExpiresAt,
		String scope,
		Instant connectedAt,
		Instant lastSyncedAt,
		Instant deletedAt
	) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.userId = Objects.requireNonNull(userId, "userId must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.providerUserId = requireProviderUserId(providerUserId);
		this.email = email;
		this.tokenExpiresAt = tokenExpiresAt;
		this.scope = blankToNull(scope);
		this.connectedAt = Objects.requireNonNull(connectedAt, "connectedAt must not be null");
		this.lastSyncedAt = lastSyncedAt;
		this.deletedAt = deletedAt;
	}

	public static OAuthAccount connect(
		UUID id,
		UUID userId,
		OAuthProvider provider,
		String providerUserId,
		EmailAddress email,
		Instant tokenExpiresAt,
		String scope,
		Instant now
	) {
		return new OAuthAccount(id, userId, provider, providerUserId, email, tokenExpiresAt, scope, now, now, null);
	}

	public static OAuthAccount rehydrate(
		UUID id,
		UUID userId,
		OAuthProvider provider,
		String providerUserId,
		EmailAddress email,
		Instant tokenExpiresAt,
		String scope,
		Instant connectedAt,
		Instant lastSyncedAt,
		Instant deletedAt
	) {
		return new OAuthAccount(id, userId, provider, providerUserId, email, tokenExpiresAt, scope, connectedAt, lastSyncedAt, deletedAt);
	}

	public OAuthAccount sync(EmailAddress email, Instant tokenExpiresAt, String scope, Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		if (deletedAt != null) {
			throw new IllegalStateException("deleted OAuth account cannot be synced");
		}
		return new OAuthAccount(id, userId, provider, providerUserId, email, tokenExpiresAt, scope, connectedAt, now, deletedAt);
	}

	public OAuthAccount softDelete(Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		if (deletedAt != null) {
			throw new IllegalStateException("OAuth account is already deleted");
		}
		return new OAuthAccount(id, userId, provider, providerUserId, email, tokenExpiresAt, scope, connectedAt, lastSyncedAt, now);
	}

	public UUID id() {
		return id;
	}

	public UUID userId() {
		return userId;
	}

	public OAuthProvider provider() {
		return provider;
	}

	public String providerUserId() {
		return providerUserId;
	}

	public String providerAccountLiveKey() {
		return provider.liveKey(providerUserId);
	}

	public Optional<EmailAddress> email() {
		return Optional.ofNullable(email);
	}

	public Optional<Instant> tokenExpiresAt() {
		return Optional.ofNullable(tokenExpiresAt);
	}

	public Optional<String> scope() {
		return Optional.ofNullable(scope);
	}

	public Instant connectedAt() {
		return connectedAt;
	}

	public Optional<Instant> lastSyncedAt() {
		return Optional.ofNullable(lastSyncedAt);
	}

	public Optional<Instant> deletedAt() {
		return Optional.ofNullable(deletedAt);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		return other instanceof OAuthAccount account && id.equals(account.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	private static String requireProviderUserId(String providerUserId) {
		if (providerUserId == null || providerUserId.isBlank()) {
			throw new IllegalArgumentException("providerUserId must not be blank");
		}
		return providerUserId.strip();
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
