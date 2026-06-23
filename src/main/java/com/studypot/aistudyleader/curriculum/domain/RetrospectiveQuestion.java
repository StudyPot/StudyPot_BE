package com.studypot.aistudyleader.curriculum.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * 커리큘럼 생성 시 AI가 만들어 두는 주차별 회고 질문 한 개입니다.
 * curriculum_week.retrospective_questions(JSON 배열)로 저장됩니다.
 */
public record RetrospectiveQuestion(String id, String text, RetrospectiveQuestionType type) {

	@JsonCreator
	public RetrospectiveQuestion(
		@JsonProperty("id") String id,
		@JsonProperty("text") String text,
		@JsonProperty("type") RetrospectiveQuestionType type
	) {
		this.id = requireText(id, "id");
		this.text = requireText(text, "text");
		this.type = Objects.requireNonNull(type, "type must not be null");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.strip();
	}
}
