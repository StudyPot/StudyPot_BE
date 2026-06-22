package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import java.util.Objects;
import java.util.Optional;

/**
 * 주차 과제와 인증 멤버의 완료 상태를 함께 노출하기 위한 조회 결과입니다.
 * completion은 멤버가 아직 어떤 액션도 하지 않은 과제의 경우 null입니다.
 */
public record WeeklyTaskWithCompletion(
	WeeklyTask task,
	TaskCompletion completion
) {

	public WeeklyTaskWithCompletion {
		Objects.requireNonNull(task, "task must not be null");
	}

	public Optional<TaskCompletion> completionOptional() {
		return Optional.ofNullable(completion);
	}
}
