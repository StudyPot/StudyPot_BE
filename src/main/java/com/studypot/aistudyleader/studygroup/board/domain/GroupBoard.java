package com.studypot.aistudyleader.studygroup.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupBoard(
	UUID id,
	UUID groupId,
	GroupBoardType boardType,
	String name,
	String description,
	int displayOrder,
	boolean defaultBoard,
	Instant createdAt,
	Instant updatedAt,
	Instant deletedAt
) {

	public GroupBoard {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(boardType, "boardType must not be null");
		name = normalizeRequired(name, "name", 80);
		description = normalizeOptional(description);
		if (displayOrder <= 0) {
			throw new IllegalArgumentException("displayOrder must be positive.");
		}
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public static GroupBoard createDefault(
		UUID id,
		UUID groupId,
		GroupBoardType boardType,
		int displayOrder,
		Instant now
	) {
		return new GroupBoard(
			id,
			groupId,
			boardType,
			boardType.displayName(),
			boardType.description(),
			displayOrder,
			true,
			now,
			now,
			null
		);
	}

	static String normalizeRequired(String value, String fieldName, int maxLength) {
		String normalized = value == null ? "" : value.strip();
		if (normalized.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " length must be <= " + maxLength + ": " + normalized.length());
		}
		return normalized;
	}

	static String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
