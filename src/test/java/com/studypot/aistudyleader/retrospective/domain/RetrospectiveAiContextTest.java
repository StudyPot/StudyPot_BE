package com.studypot.aistudyleader.retrospective.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RetrospectiveAiContextTest {

	@Test
	void copiesNestedContextValuesAsImmutableSnapshots() {
		Map<String, Object> onboarding = new LinkedHashMap<>();
		Map<String, Object> skillLevels = new LinkedHashMap<>();
		List<String> preferredTasks = new ArrayList<>(List.of("PRACTICE"));
		skillLevels.put("JPA", 2);
		onboarding.put("keywordSkillLevels", skillLevels);
		onboarding.put("preferredTasks", preferredTasks);

		Map<String, Object> rule = new LinkedHashMap<>();
		List<String> tags = new ArrayList<>(List.of("deadline"));
		rule.put("tags", tags);

		RetrospectiveAiContext context = new RetrospectiveAiContext(
			onboarding,
			List.of(rule),
			List.of(),
			List.of(),
			Map.of("status", "AVAILABLE")
		);

		skillLevels.put("Spring", 3);
		preferredTasks.add("READING");
		tags.add("penalty");

		@SuppressWarnings("unchecked")
		Map<String, Object> capturedSkills = (Map<String, Object>) context.onboarding().get("keywordSkillLevels");
		@SuppressWarnings("unchecked")
		List<String> capturedTasks = (List<String>) context.onboarding().get("preferredTasks");
		@SuppressWarnings("unchecked")
		List<String> capturedTags = (List<String>) context.rules().get(0).get("tags");

		assertThat(capturedSkills).containsOnly(Map.entry("JPA", 2));
		assertThat(capturedTasks).containsExactly("PRACTICE");
		assertThat(capturedTags).containsExactly("deadline");
		assertThatThrownBy(() -> capturedSkills.put("Spring", 3))
			.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> capturedTasks.add("READING"))
			.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void rejectsNullNestedContextValues() {
		Map<String, Object> onboarding = new LinkedHashMap<>();
		onboarding.put("keywordSkillLevels", null);

		assertThatThrownBy(() -> new RetrospectiveAiContext(
				onboarding,
				List.of(),
				List.of(),
				List.of(),
				Map.of()
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("retrospective AI context map must not contain null values.");
	}
}
