package com.studypot.aistudyleader.shared.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public record AuditMetadata(Instant createdAt, Instant updatedAt, Instant deletedAt) {

	public AuditMetadata {
		createdAt = timestamp6(createdAt, "createdAt");
		updatedAt = timestamp6(updatedAt, "updatedAt");
		deletedAt = deletedAt == null ? null : timestamp6(deletedAt, "deletedAt");

		if (updatedAt.isBefore(createdAt)) {
			throw new IllegalArgumentException("updatedAt must not be before createdAt");
		}
		if (deletedAt != null && deletedAt.isBefore(updatedAt)) {
			throw new IllegalArgumentException("deletedAt must not be before updatedAt");
		}
	}

	public static AuditMetadata created(Instant createdAt) {
		Instant timestamp = timestamp6(createdAt, "createdAt");
		return new AuditMetadata(timestamp, timestamp, null);
	}

	public AuditMetadata touch(Instant updatedAt) {
		Instant timestamp = timestamp6(updatedAt, "updatedAt");
		if (timestamp.isBefore(this.updatedAt)) {
			throw new IllegalArgumentException("updatedAt must not be before current updatedAt");
		}
		return new AuditMetadata(createdAt, timestamp, deletedAt);
	}

	public AuditMetadata softDelete(Instant deletedAt) {
		if (deleted()) {
			return this;
		}
		Instant timestamp = timestamp6(deletedAt, "deletedAt");
		if (timestamp.isBefore(updatedAt)) {
			throw new IllegalArgumentException("deletedAt must not be before current updatedAt");
		}
		return new AuditMetadata(createdAt, timestamp, timestamp);
	}

	public boolean deleted() {
		return deletedAt != null;
	}

	private static Instant timestamp6(Instant value, String fieldName) {
		return Objects.requireNonNull(value, fieldName + " must not be null")
			.truncatedTo(ChronoUnit.MICROS);
	}
}
