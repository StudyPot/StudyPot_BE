package com.studypot.aistudyleader.onboarding.domain;

import java.util.Arrays;
import java.util.Optional;

public enum TaskPreferenceType {
	READING,
	PRACTICE,
	ASSIGNMENT,
	PROJECT,
	CUSTOM;

	public static Optional<TaskPreferenceType> parse(String value) {
		if (value == null || value.isBlank()) {
			return Optional.empty();
		}
		String normalized = value.strip();
		return Arrays.stream(values())
			.filter(type -> type.name().equals(normalized))
			.findFirst();
	}
}
