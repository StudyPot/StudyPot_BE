package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class LlmUsageService {

	private static final int DEFAULT_GROUP_USAGE_LIMIT = 100;

	private final LlmUsageRepository repository;

	public LlmUsageService(LlmUsageRepository repository) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
	}

	@Transactional(readOnly = true)
	public List<LlmUsage> listGroupUsage(ListGroupLlmUsageQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		LlmUsageAccessContext context = requireAccessContext(query.groupId(), query.authenticatedUserId());
		if (!context.canReadGroupUsageLogs()) {
			throw new LlmUsageAccessDeniedException("only the study group owner can read LLM usage logs.");
		}
		return repository.findGroupUsage(query.groupId(), DEFAULT_GROUP_USAGE_LIMIT);
	}

	private LlmUsageAccessContext requireAccessContext(UUID groupId, UUID userId) {
		return repository.findAccessContext(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new LlmUsageGroupNotFoundException("study group was not found.");
				}
				throw new LlmUsageAccessDeniedException("authenticated user is not an owner of this study group.");
			});
	}
}
