package com.studypot.aistudyleader.curriculum.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record CurrentLearningActivity(
	UUID groupId,
	CurriculumWeek currentWeek,
	Optional<MemberWeekProgress> progress,
	MemberWeekProgressStatus progressStatus,
	List<Task> tasks,
	TaskCompletionSummary taskCompletion
) {

	public CurrentLearningActivity {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(currentWeek, "currentWeek must not be null");
		progress = Objects.requireNonNull(progress, "progress must not be null");
		Objects.requireNonNull(progressStatus, "progressStatus must not be null");
		tasks = List.copyOf(Objects.requireNonNull(tasks, "tasks must not be null"));
		if (tasks.stream().anyMatch(Objects::isNull)) {
			throw new IllegalArgumentException("tasks must not contain null elements.");
		}
		Objects.requireNonNull(taskCompletion, "taskCompletion must not be null");
	}

	public static CurrentLearningActivity of(
		UUID groupId,
		CurriculumWeek currentWeek,
		Optional<MemberWeekProgress> progress,
		List<TaskCompletion> completions
	) {
		Objects.requireNonNull(completions, "completions must not be null");
		Map<UUID, TaskCompletion> completionsByTaskId = new LinkedHashMap<>();
		for (TaskCompletion completion : completions) {
			Objects.requireNonNull(completion, "completions must not contain null elements");
			completionsByTaskId.putIfAbsent(completion.weeklyTaskId(), completion);
		}
		List<Task> tasks = currentWeek.tasks().stream()
			.map(task -> new Task(task, Optional.ofNullable(completionsByTaskId.get(task.id()))))
			.toList();
		return new CurrentLearningActivity(
			groupId,
			currentWeek,
			progress,
			progress.map(MemberWeekProgress::status).orElse(MemberWeekProgressStatus.NOT_STARTED),
			tasks,
			TaskCompletionSummary.from(tasks)
		);
	}

	public record Task(
		WeeklyTask task,
		Optional<TaskCompletion> completion,
		TaskCompletionStatus completionStatus
	) {

		public Task(WeeklyTask task, Optional<TaskCompletion> completion) {
			this(
				task,
				completion,
				completion.map(TaskCompletion::status).orElse(TaskCompletionStatus.TODO)
			);
		}

		public Task {
			Objects.requireNonNull(task, "task must not be null");
			completion = Objects.requireNonNull(completion, "completion must not be null");
			Objects.requireNonNull(completionStatus, "completionStatus must not be null");
		}
	}

	public record TaskCompletionSummary(
		int totalCount,
		int doneCount,
		int incompleteCount,
		int skippedCount
	) {

		private static TaskCompletionSummary from(List<Task> tasks) {
			int doneCount = 0;
			int incompleteCount = 0;
			int skippedCount = 0;
			for (Task task : tasks) {
				switch (task.completionStatus()) {
					case DONE -> doneCount++;
					case INCOMPLETE -> incompleteCount++;
					case SKIPPED -> skippedCount++;
					case TODO -> {
					}
				}
			}
			return new TaskCompletionSummary(tasks.size(), doneCount, incompleteCount, skippedCount);
		}

		public TaskCompletionSummary {
			requireNonNegative(totalCount, "totalCount");
			requireNonNegative(doneCount, "doneCount");
			requireNonNegative(incompleteCount, "incompleteCount");
			requireNonNegative(skippedCount, "skippedCount");
			if (doneCount + incompleteCount + skippedCount > totalCount) {
				throw new IllegalArgumentException("status counts must not exceed totalCount.");
			}
		}

		private static void requireNonNegative(int value, String fieldName) {
			if (value < 0) {
				throw new IllegalArgumentException(fieldName + " must be non-negative.");
			}
		}
	}
}
