package com.studypot.aistudyleader.studygroup.rules.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record GroupRule(
	UUID id,
	UUID groupId,
	UUID createdBy,
	GroupRuleType ruleType,
	Map<String, Object> config,
	String description,
	boolean active,
	Instant createdAt,
	Instant updatedAt,
	Optional<Instant> deletedAt
) {

	public GroupRule {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(createdBy, "createdBy must not be null");
		Objects.requireNonNull(ruleType, "ruleType must not be null");
		config = Map.copyOf(Objects.requireNonNull(config, "config must not be null"));
		if (config.isEmpty()) {
			throw new IllegalArgumentException("config must not be empty");
		}
		description = normalize(description);
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
	}

	public static GroupRule create(
		UUID id,
		UUID groupId,
		UUID createdBy,
		GroupRuleType ruleType,
		Map<String, Object> config,
		String description,
		boolean active,
		Instant now
	) {
		return new GroupRule(id, groupId, createdBy, ruleType, config, description, active, now, now, Optional.empty());
	}

	public GroupRule update(Map<String, Object> nextConfig, String nextDescription, boolean nextActive, Instant now) {
		return new GroupRule(id, groupId, createdBy, ruleType, nextConfig, nextDescription, nextActive, createdAt, now, deletedAt);
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.strip();
	}
}
