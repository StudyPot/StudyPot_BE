package com.studypot.aistudyleader.curriculum.repository;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.GroupActivityCount;
import com.studypot.aistudyleader.curriculum.domain.GroupWeekProgress;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.TaskCompletion;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumRepository {

	boolean existsStudyGroup(UUID groupId);

	List<GroupWeekProgress> findWeekProgressByGroupIds(Collection<UUID> groupIds);

	Optional<CurriculumStartContext> findStartContext(UUID groupId, UUID userId);

	List<SubmittedOnboardingResponse> findSubmittedOnboardingResponses(UUID groupId);

	void saveStartedCurriculum(UUID groupId, Instant startedAt, LlmUsage llmUsage, Curriculum curriculum);

	void saveFailedLlmUsage(LlmUsage llmUsage);

	Optional<CurriculumStartContext> findReadContext(UUID groupId, UUID userId);

	Optional<Curriculum> findActiveCurriculumByGroupId(UUID groupId);

	Optional<CurriculumWeek> findCurrentWeekByGroupId(UUID groupId);

	Optional<CurriculumWeek> findWeekById(UUID weekId);

	boolean existsCurriculumWeek(UUID weekId);

	Optional<CurriculumStartContext> findReadContextByWeekId(UUID weekId, UUID userId);

	Optional<com.studypot.aistudyleader.curriculum.domain.NextWeekTarget> findNextPendingWeek(UUID currentWeekId);

	Optional<com.studypot.aistudyleader.curriculum.domain.NextWeekTarget> findNextRegenerableWeek(UUID currentWeekId, Instant now);

	Optional<String> findLatestWeeklyReportBody(UUID groupId);

	CurriculumWeek replaceNextWeekTasks(
		UUID weekId,
		List<WeeklyTask> tasks,
		List<com.studypot.aistudyleader.curriculum.domain.RetrospectiveQuestion> retrospectiveQuestions,
		Instant now
	);

	Optional<CurriculumStartContext> findReadContextByTaskId(UUID taskId, UUID userId);

	List<WeeklyTask> findWeeklyTasksByWeekId(UUID weekId);

	/** 점진 생성: 기존 커리큘럼에 다음 주차(+TODO)를 새로 삽입한다. */
	void insertNextWeek(CurriculumWeek week);

	/** 다음 주차 생성 입력용: 해당 주차의 완료된 회고 답변 요약(멤버별)을 조회한다. 없으면 빈 목록. */
	List<String> findCompletedRetrospectiveSummaries(UUID weekId);

	/** 다음 주차 생성 입력용: 해당 주차의 완료된 회고가 만든 구조화된 '다음 주차 조정 제안'(JSON)을 조회한다. 없으면 빈 목록. */
	List<String> findCompletedRetrospectiveAdjustments(UUID weekId);

	/** 다음 주차 생성 입력용: 그룹의 팀장 대화에서 since 이후 도출된 '다음 주차 조정 후보'(JSON)를 조회한다. 없으면 빈 목록. */
	List<String> findTeamLeadAdjustmentCandidates(UUID groupId, Instant since);

	boolean existsWeeklyTask(UUID taskId);

	Optional<WeeklyTask> findWeeklyTaskById(UUID taskId);

	Optional<Instant> findCurriculumWeekStartsAt(UUID weekId);

	Optional<MemberWeekProgress> findMemberWeekProgress(UUID weekId, UUID memberId);

	Optional<Instant> findWeekDueAt(UUID weekId);

	boolean insertMemberWeekProgress(MemberWeekProgress progress);

	boolean updateMemberWeekProgress(MemberWeekProgress progress);

	Optional<TaskCompletion> findTaskCompletion(UUID taskId, UUID memberId);

	List<TaskCompletion> findTaskCompletionsByWeekIdAndMemberId(UUID weekId, UUID memberId);

	List<GroupActivityCount> findGroupDoneActivityCounts(UUID groupId, Instant fromInclusive, Instant toExclusive);

	List<com.studypot.aistudyleader.curriculum.domain.RecentTaskActivity> findRecentTaskActivity(UUID groupId, int limit);

	int countActiveOrOnboardingMembers(UUID groupId);

	int countMemberDoneActivity(UUID userId, Instant fromInclusive, Instant toExclusive);

	List<CurriculumWeek> findWeeksByGroupId(UUID groupId);

	boolean insertTaskCompletion(TaskCompletion completion);

	boolean updateTaskCompletion(TaskCompletion completion);
}
