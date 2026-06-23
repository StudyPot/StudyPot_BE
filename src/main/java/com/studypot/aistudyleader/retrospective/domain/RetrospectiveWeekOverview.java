package com.studypot.aistudyleader.retrospective.domain;

import com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 회고 화면의 주차별 개요 한 줄. unlocked=그 주차 필수 TODO 전부 완료(회고 작성 가능),
 * answered=내가 이미 회고를 제출함, questions=커리큘럼 생성 시 만들어 둔 회고 질문.
 */
public record RetrospectiveWeekOverview(
	UUID weekId,
	int weekNumber,
	String status,
	boolean unlocked,
	boolean answered,
	List<RetrospectiveQuestion> questions
) {

	public RetrospectiveWeekOverview {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		questions = List.copyOf(Objects.requireNonNull(questions, "questions must not be null"));
	}
}
