package com.studypot.aistudyleader.studygroup.domain;

import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record StudyGroupMemberProfile(
	UUID groupId,
	UUID memberId,
	UUID userId,
	String displayName,
	GroupMemberPermission permission,
	GroupMemberStatus status,
	OnboardingSummary onboarding,
	CurrentWeekSummary currentWeek,
	TaskCompletionSummary taskCompletion,
	RetrospectiveSummary retrospective
) {

	public StudyGroupMemberProfile {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		displayName = GroupMember.normalizeDisplayName(displayName);
		Objects.requireNonNull(permission, "permission must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(onboarding, "onboarding must not be null");
		Objects.requireNonNull(taskCompletion, "taskCompletion must not be null");
		Objects.requireNonNull(retrospective, "retrospective must not be null");
	}

	public record OnboardingSummary(
		boolean submitted,
		Integer skillLevel,
		Instant submittedAt
	) {

		public OnboardingSummary {
			if (!submitted) {
				skillLevel = null;
				submittedAt = null;
			}
			if (skillLevel != null && (skillLevel < 1 || skillLevel > 5)) {
				throw new IllegalArgumentException("skillLevel must be between 1 and 5.");
			}
		}
	}

	public record CurrentWeekSummary(
		UUID weekId,
		int weekNumber,
		String sprintGoal,
		Instant startsAt,
		Instant endsAt,
		MemberWeekProgressStatus progressStatus
	) {

		public CurrentWeekSummary {
			Objects.requireNonNull(weekId, "weekId must not be null");
			if (weekNumber <= 0) {
				throw new IllegalArgumentException("weekNumber must be positive.");
			}
			Objects.requireNonNull(progressStatus, "progressStatus must not be null");
		}
	}

	public record TaskCompletionSummary(
		int totalCount,
		int doneCount,
		int incompleteCount,
		int skippedCount
	) {

		public TaskCompletionSummary {
			requireNonNegative(totalCount, "totalCount");
			requireNonNegative(doneCount, "doneCount");
			requireNonNegative(incompleteCount, "incompleteCount");
			requireNonNegative(skippedCount, "skippedCount");
			int statusCount = doneCount + incompleteCount + skippedCount;
			if (statusCount > totalCount) {
				throw new IllegalArgumentException("status counts must not exceed totalCount.");
			}
		}

		private static void requireNonNegative(int value, String fieldName) {
			if (value < 0) {
				throw new IllegalArgumentException(fieldName + " must be non-negative.");
			}
		}
	}

	public record RetrospectiveSummary(boolean feedbackReady) {
	}
}
