package com.studypot.aistudyleader.onboarding.service;

import com.studypot.aistudyleader.onboarding.domain.GroupMemberOnboarding;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingStatus;
import com.studypot.aistudyleader.onboarding.domain.MemberAvailabilitySlot;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import com.studypot.aistudyleader.onboarding.repository.OnboardingRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class OnboardingService {

	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingService.class);

	private final OnboardingRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final com.studypot.aistudyleader.notification.service.NotificationEventPublisher notificationEvents;

	public OnboardingService(OnboardingRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this(repository, clock, idGenerator,
			com.studypot.aistudyleader.notification.service.NotificationEventPublisher.noop());
	}

	public OnboardingService(
		OnboardingRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		com.studypot.aistudyleader.notification.service.NotificationEventPublisher notificationEvents
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.notificationEvents = Objects.requireNonNull(notificationEvents, "notificationEvents must not be null");
	}

	@Transactional
	public GroupOnboardingResponse submitMyOnboarding(SubmitMyOnboardingCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		OnboardingMemberContext context = requireMemberContext(command.authenticatedUserId(), command.groupId());
		Instant now = clock.instant();
		Optional<GroupOnboardingResponse> existingResponse = repository.findResponseByMemberId(context.memberId());
		if (existingResponse.map(GroupOnboardingResponse::status).filter(GroupOnboardingStatus.SUBMITTED::equals).isPresent()) {
			throw new OnboardingAlreadySubmittedException("onboarding response was already submitted.");
		}
		UUID responseId = existingResponse.map(GroupOnboardingResponse::id).orElseGet(idGenerator);
		GroupOnboardingResponse response;
		try {
			Map<String, Integer> keywordSkillLevels = keywordSkillLevels(context, command.skillLevel());
			response = GroupOnboardingResponse.draft(
				responseId,
				context,
				keywordSkillLevels,
				Map.of(),
				command.additionalNote(),
				now
			).withAvailabilitySlots(availabilitySlots(command.availabilitySlots(), responseId, context.memberId(), now));
		} catch (IllegalArgumentException exception) {
			throw invalidRequest(exception);
		}
		GroupOnboardingResponse saved = repository.saveDraft(response);
		GroupOnboardingResponse submitted = repository.submit(saved.submit(now));
		if (context.memberStatus() == GroupMemberStatus.PENDING_ONBOARDING
			&& !repository.activatePendingMember(context.memberId(), now)) {
			throw new OnboardingMembershipRequiredException("current group membership is required.");
		}
		markReadyAndNotifyIfAllOnboarded(context.groupId(), now);
		return submitted;
	}

	@Transactional(readOnly = true)
	public List<GroupMemberOnboarding> listGroupOnboardings(GetGroupOnboardingsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireMemberContext(query.authenticatedUserId(), query.groupId());
		return repository.findGroupOnboardings(query.groupId());
	}

	private void markReadyAndNotifyIfAllOnboarded(UUID groupId, Instant now) {
		// 모든 활성 멤버가 온보딩을 마쳤을 때에만 그룹을 READY_TO_START 로 전환하고
		// 소유자에게 시작 안내 알림을 보낸다. (소유자 한 명만 온보딩해도 시작 가능하던 문제 수정)
		repository.findOwnerUserIdWhenAllOnboarded(groupId).ifPresent(ownerUserId -> {
			repository.markStudyGroupReadyToStart(groupId, now);
			try {
				notificationEvents.publishOnboardingCompleted(groupId, ownerUserId);
			} catch (RuntimeException exception) {
				// 알림 발행 실패가 온보딩 제출 트랜잭션을 롤백하지 않도록 한다.
				log.warn("onboarding completed notification publish failed groupId={}", groupId, exception);
			}
		});
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
		if (message != null && message.startsWith("skillLevel")) {
			field = "skillLevel";
		} else if (message != null && message.startsWith("keywordSkillLevels")) {
			field = "skillLevel";
		} else if (message != null && message.startsWith("taskPreferences")) {
			field = "onboarding";
		} else if (message != null && message.startsWith("availabilitySlots")) {
			field = "availabilitySlots";
		}
		return new InvalidOnboardingRequestException(field, message);
	}

	private static Map<String, Integer> keywordSkillLevels(OnboardingMemberContext context, int skillLevel) {
		if (skillLevel < 1 || skillLevel > 5) {
			throw new IllegalArgumentException("skillLevel must be between 1 and 5.");
		}
		Map<String, Integer> result = new LinkedHashMap<>();
		for (String keyword : context.detailKeywords()) {
			result.put(keyword, skillLevel);
		}
		return result;
	}

	private List<MemberAvailabilitySlot> availabilitySlots(
		List<AvailabilitySlotCommand> commands,
		UUID responseId,
		UUID memberId,
		Instant now
	) {
		return commands.stream()
			.map(command -> {
				command.validate();
				return MemberAvailabilitySlot.create(
					idGenerator.get(),
					responseId,
					memberId,
					command.dayOfWeek(),
					command.startTime(),
					command.endTime(),
					command.timezone(),
					now
				);
			})
			.toList();
	}
}
