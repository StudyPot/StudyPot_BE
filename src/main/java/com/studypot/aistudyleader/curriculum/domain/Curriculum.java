package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class Curriculum {

	private final UUID id;
	private final UUID groupId;
	private final UUID llmUsageId;
	private final String title;
	private final int totalWeeks;
	private final Map<String, Object> onboardingSummary;
	private final boolean generatedByAi;
	private final String generationPrompt;
	private final CurriculumStatus status;
	private final List<CurriculumWeek> weeks;
	private final Instant createdAt;
	private final Instant updatedAt;

	public Curriculum(
		UUID id,
		UUID groupId,
		UUID llmUsageId,
		String title,
		int totalWeeks,
		Map<String, Object> onboardingSummary,
		boolean generatedByAi,
		String generationPrompt,
		CurriculumStatus status,
		List<CurriculumWeek> weeks,
		Instant createdAt,
		Instant updatedAt
	) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.groupId = Objects.requireNonNull(groupId, "groupId must not be null");
		this.llmUsageId = llmUsageId;
		this.title = requireText(title, "title");
		if (totalWeeks <= 0) {
			throw new IllegalArgumentException("totalWeeks must be positive");
		}
		this.totalWeeks = totalWeeks;
		this.onboardingSummary = Map.copyOf(Objects.requireNonNull(onboardingSummary, "onboardingSummary must not be null"));
		this.generatedByAi = generatedByAi;
		this.generationPrompt = generationPrompt == null || generationPrompt.isBlank() ? null : generationPrompt.strip();
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.weeks = List.copyOf(Objects.requireNonNull(weeks, "weeks must not be null"));
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}

	public UUID id() {
		return id;
	}

	public UUID groupId() {
		return groupId;
	}

	public Optional<UUID> llmUsageId() {
		return Optional.ofNullable(llmUsageId);
	}

	public String title() {
		return title;
	}

	public int totalWeeks() {
		return totalWeeks;
	}

	public Map<String, Object> onboardingSummary() {
		return onboardingSummary;
	}

	public boolean generatedByAi() {
		return generatedByAi;
	}

	public Optional<String> generationPrompt() {
		return Optional.ofNullable(generationPrompt);
	}

	public CurriculumStatus status() {
		return status;
	}

	public List<CurriculumWeek> weeks() {
		return weeks;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
