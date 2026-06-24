package com.studypot.aistudyleader.curriculum.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 그룹 홈 '최근 활동' 카드용: 최근에 완료(DONE)된 과제 한 건입니다.
 * 누가(memberId/nickname) 어떤 과제(taskTitle)를 언제(completedAt) 완료했는지를 나타냅니다.
 */
public record RecentTaskActivity(
	UUID memberId,
	String memberNickname,
	String taskTitle,
	Instant completedAt
) {

	public RecentTaskActivity {
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(taskTitle, "taskTitle must not be null");
		Objects.requireNonNull(completedAt, "completedAt must not be null");
	}
}
