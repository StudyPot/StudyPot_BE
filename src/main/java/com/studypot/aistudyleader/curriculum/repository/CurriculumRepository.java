package com.studypot.aistudyleader.curriculum.repository;

import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
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
}
