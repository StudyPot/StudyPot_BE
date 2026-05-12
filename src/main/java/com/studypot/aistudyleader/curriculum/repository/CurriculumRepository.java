package com.studypot.aistudyleader.curriculum.repository;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<CurriculumStartContext> findStartContext(UUID groupId, UUID userId);

	List<SubmittedOnboardingResponse> findSubmittedOnboardingResponses(UUID groupId);

	void saveStartedCurriculum(UUID groupId, Instant startedAt, LlmUsage llmUsage, Curriculum curriculum);

	Optional<CurriculumStartContext> findReadContext(UUID groupId, UUID userId);

	Optional<Curriculum> findActiveCurriculumByGroupId(UUID groupId);

	Optional<CurriculumWeek> findCurrentWeekByGroupId(UUID groupId);

	boolean existsCurriculumWeek(UUID weekId);

	Optional<CurriculumStartContext> findReadContextByWeekId(UUID weekId, UUID userId);

	List<WeeklyTask> findWeeklyTasksByWeekId(UUID weekId);

	Optional<MemberWeekProgress> findMemberWeekProgress(UUID weekId, UUID memberId);

	Optional<Instant> findWeekDueAt(UUID weekId);

	boolean insertMemberWeekProgress(MemberWeekProgress progress);

	boolean updateMemberWeekProgress(MemberWeekProgress progress);
}
