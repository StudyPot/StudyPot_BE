package com.studypot.aistudyleader.retrospective.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 멤버가 주차 회고 설문에 답한 결과 제출 명령. answers 각 항목은 {questionId, score?, text?}.
 */
public record SubmitRetrospectiveCommand(UUID authenticatedUserId, UUID weekId, List<Map<String, Object>> answers) {

	public SubmitRetrospectiveCommand {
		Objects.requireNonNull(authenticatedUserId, "authenticatedUserId must not be null");
		Objects.requireNonNull(weekId, "weekId must not be null");
		answers = List.copyOf(Objects.requireNonNull(answers, "answers must not be null"));
		for (Map<String, Object> answer : answers) {
			Object questionId = answer.get("questionId");
			if (questionId == null || questionId.toString().isBlank()) {
				throw new InvalidRetrospectiveAnswerException("each answer must include a questionId.");
			}
		}
	}
}
