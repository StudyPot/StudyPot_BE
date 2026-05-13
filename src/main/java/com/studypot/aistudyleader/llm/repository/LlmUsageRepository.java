package com.studypot.aistudyleader.llm.repository;

import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LlmUsageRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<LlmUsageAccessContext> findAccessContext(UUID groupId, UUID userId);

	boolean insertLlmUsage(LlmUsage usage);

	List<LlmUsage> findGroupUsage(UUID groupId, int limit);

	List<LlmUsage> findUserUsage(UUID userId, int limit);
}
