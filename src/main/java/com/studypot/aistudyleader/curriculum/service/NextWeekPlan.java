package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.CurriculumTaskPlan;
import com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion;
import java.util.List;
import java.util.Objects;

/**
 * 리포트 기반으로 재생성한 다음 주차 계획: 새 TODO 목록 + 회고 설문 질문.
 */
public record NextWeekPlan(List<CurriculumTaskPlan> tasks, List<RetrospectiveQuestion> retrospectiveQuestions) {

	public NextWeekPlan {
		tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
		if (tasks.isEmpty()) {
			throw new IllegalArgumentException("tasks must not be empty.");
		}
		retrospectiveQuestions = List.copyOf(Objects.requireNonNull(retrospectiveQuestions, "retrospectiveQuestions must not be null"));
	}
}
