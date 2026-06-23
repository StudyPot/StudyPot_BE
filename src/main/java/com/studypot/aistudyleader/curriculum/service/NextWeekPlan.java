package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import java.util.List;
import java.util.Objects;

/**
 * 리포트 기반으로 재생성한 다음 주차 계획: 새 TODO 목록 + 회고 프롬프트.
 */
public record NextWeekPlan(List<CurriculumTaskPlan> tasks, String retrospectivePrompt) {

	public NextWeekPlan {
		tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
		if (tasks.isEmpty()) {
			throw new IllegalArgumentException("tasks must not be empty.");
		}
		retrospectivePrompt = retrospectivePrompt == null || retrospectivePrompt.isBlank() ? null : retrospectivePrompt.strip();
	}
}
