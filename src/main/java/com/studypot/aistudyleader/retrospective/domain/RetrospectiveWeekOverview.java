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
	boolean reportPosted,
	List<RetrospectiveQuestion> questions
) {

	public RetrospectiveWeekOverview {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(status, "status must not be null");
		questions = List.copyOf(Objects.requireNonNull(questions, "questions must not be null"));
	}

	/**
	 * 회고 작성 가능(unlock) 규칙. 두 군데(개요 조회/제출 가드)에서 동일하게 쓰기 위해 한곳에 둔다.
	 * 작성 가능 = 리포트 미게시 AND ( 주차 종료(COMPLETED) OR (시작됨 AND 전체 TODO 처리 완료) ).
	 * - 진행 중에 그 주차 TODO를 모두 처리(완료/건너뜀)하면 미리 작성 가능(조건1).
	 * - 끝까지 못 끝냈어도 주차가 종료되면 리포트 전까지 작성 가능(조건2).
	 * - 주차 리포트가 게시되면 닫힘(이후 제출/수정 불가).
	 * 시작 전(PENDING) 미래 주차는 잠금(vacuous-truth 방지).
	 * (AI 생성 과제는 required 플래그가 false 라 '필수'가 아닌 '전체' TODO 기준으로 본다. doneTasks=DONE+SKIPPED)
	 */
	public static boolean unlocked(String status, long totalTasks, long doneTasks, boolean reportPosted) {
		boolean started = "IN_PROGRESS".equals(status) || "COMPLETED".equals(status);
		boolean ended = "COMPLETED".equals(status);
		boolean allDone = totalTasks == 0 || doneTasks >= totalTasks;
		return !reportPosted && (ended || (started && allDone));
	}
}
