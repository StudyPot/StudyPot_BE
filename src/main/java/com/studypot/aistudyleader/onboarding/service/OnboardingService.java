package com.studypot.aistudyleader.onboarding.service;

import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class OnboardingService {

	private final OnboardingRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public OnboardingService(OnboardingRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public GroupOnboardingResponse saveMyDraft(SaveMyOnboardingCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		OnboardingMemberContext context = requireMemberContext(command.authenticatedUserId(), command.groupId());
		Instant now = clock.instant();
		UUID responseId = repository.findResponseByMemberId(context.memberId())
			.map(GroupOnboardingResponse::id)
			.orElseGet(idGenerator);
		GroupOnboardingResponse response;
		try {
			response = GroupOnboardingResponse.draft(
				responseId,
				context,
				command.keywordSkillLevels(),
				command.taskPreferences(),
				command.additionalNote(),
				now
			);
		} catch (IllegalArgumentException exception) {
			throw invalidRequest(exception);
		}
		repository.saveDraft(response);
		return repository.findResponseByMemberId(context.memberId()).orElse(response);
	}

	@Transactional(readOnly = true)
	public GroupOnboardingResponse getMyResponse(GetMyOnboardingQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		OnboardingMemberContext context = requireMemberContext(query.authenticatedUserId(), query.groupId());
		return repository.findResponseByMemberId(context.memberId())
			.orElseThrow(() -> new OnboardingResponseNotFoundException("onboarding response was not found."));
	}

	private OnboardingMemberContext requireMemberContext(UUID authenticatedUserId, UUID groupId) {
		return repository.findMemberContext(groupId, authenticatedUserId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new OnboardingGroupNotFoundException("study group was not found.");
				}
				throw new OnboardingMembershipRequiredException("authenticated user is not a member of this study group.");
			});
	}

	private static InvalidOnboardingRequestException invalidRequest(IllegalArgumentException exception) {
		String message = exception.getMessage();
		String field = "onboarding";
		if (message != null && message.startsWith("keywordSkillLevels")) {
			field = "keywordSkillLevels";
		} else if (message != null && message.startsWith("taskPreferences")) {
			field = "taskPreferences";
		}
		return new InvalidOnboardingRequestException(field, message);
	}
}
