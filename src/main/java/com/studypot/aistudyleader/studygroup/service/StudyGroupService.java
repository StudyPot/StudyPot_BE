package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import com.studypot.aistudyleader.studygroup.repository.GroupMemberDuplicateMembershipException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupInviteCodeConflictException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class StudyGroupService {

	private static final Logger log = LoggerFactory.getLogger(StudyGroupService.class);

	private final StudyGroupRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final Supplier<String> inviteCodeGenerator;
	private final int inviteCodeMaxAttempts;
	private final NotificationEventPublisher notificationEvents;

	public StudyGroupService(
		StudyGroupRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		Supplier<String> inviteCodeGenerator,
		int inviteCodeMaxAttempts
	) {
		this(
			repository,
			clock,
			idGenerator,
			inviteCodeGenerator,
			inviteCodeMaxAttempts,
			NotificationEventPublisher.noop()
		);
	}

	public StudyGroupService(
		StudyGroupRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		Supplier<String> inviteCodeGenerator,
		int inviteCodeMaxAttempts,
		NotificationEventPublisher notificationEvents
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.inviteCodeGenerator = Objects.requireNonNull(inviteCodeGenerator, "inviteCodeGenerator must not be null");
		if (inviteCodeMaxAttempts <= 0) {
			throw new IllegalArgumentException("inviteCodeMaxAttempts must be positive");
		}
		this.inviteCodeMaxAttempts = inviteCodeMaxAttempts;
		this.notificationEvents = Objects.requireNonNull(notificationEvents, "notificationEvents must not be null");
	}

	@Transactional
	public StudyGroupCreationResult createGroup(CreateStudyGroupCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		for (int attempt = 0; attempt < inviteCodeMaxAttempts; attempt++) {
			StudyGroupCreationResult result = createCandidate(command);
			try {
				repository.saveCreatedGroup(result.group(), result.ownerMember());
				publishNotification(() -> notificationEvents.publishOnboardingRequested(
					result.group().id(),
					result.ownerMember().userId()
				));
				return result;
			} catch (StudyGroupInviteCodeConflictException exception) {
				// Try another generated invite code until the bounded retry budget is exhausted.
			}
		}
		throw new StudyGroupServiceUnavailableException("could not allocate a unique invite code.");
	}

	@Transactional
	public StudyGroupJoinResult joinGroup(JoinStudyGroupCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		StudyGroupJoinTarget target = repository.findJoinTargetByIdForUpdate(command.groupId())
			.orElseThrow(() -> new StudyGroupNotFoundException("study group was not found."));
		if (!target.matchesInviteCode(command.inviteCode())) {
			throw new StudyGroupJoinRejectedException("invite code does not match the study group.");
		}
		return joinResolvedTarget(target, command.authenticatedUserId());
	}

	@Transactional
	public StudyGroupJoinResult joinGroupByInviteCode(JoinStudyGroupByInviteCodeCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		StudyGroupJoinTarget target = repository.findJoinTargetByInviteCode(command.inviteCode())
			.orElseThrow(() -> new StudyGroupJoinRejectedException("invite code does not match any study group."));
		return joinResolvedTarget(target, command.authenticatedUserId());
	}

	private StudyGroupJoinResult joinResolvedTarget(StudyGroupJoinTarget target, UUID authenticatedUserId) {
		if (!target.isAcceptingJoins()) {
			throw new StudyGroupJoinRejectedException("study group is not accepting new members.");
		}
		if (repository.existsActiveOrOnboardingMember(target.id(), authenticatedUserId)) {
			throw new StudyGroupJoinRejectedException("user is already a member of this study group.");
		}
		if (!target.hasCapacity(repository.countActiveOrOnboardingMembers(target.id()))) {
			throw new StudyGroupJoinRejectedException("study group member capacity has been reached.");
		}

		GroupMember member = GroupMember.member(idGenerator.get(), target.id(), authenticatedUserId, null, clock.instant());
		try {
			repository.saveJoinedMember(member);
		} catch (GroupMemberDuplicateMembershipException exception) {
			throw new StudyGroupJoinRejectedException("user is already a member of this study group.");
		}
		if (target.status() == StudyGroupStatus.READY_TO_START) {
			// 시작 대기 상태에서 새 멤버가 들어오면, 그 멤버 온보딩 전까지 시작을 막도록 온보딩 상태로 되돌린다.
			repository.revertReadyToStartToOnboarding(target.id(), clock.instant());
		}
		publishNotification(() -> notificationEvents.publishOnboardingRequested(member.groupId(), member.userId()));
		publishNotification(() -> repository.findOwnerUserId(member.groupId())
			.filter(ownerUserId -> !ownerUserId.equals(member.userId()))
			.ifPresent(ownerUserId -> notificationEvents.publishMemberJoined(member.groupId(), ownerUserId, member.userId())));
		return new StudyGroupJoinResult(member);
	}

	@Transactional(readOnly = true)
	public List<StudyGroup> listMyGroups(ListStudyGroupsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		List<StudyGroup> groups = repository.findGroupsByMemberUserId(query.authenticatedUserId());
		java.util.stream.Stream<StudyGroup> stream = groups.stream();
		if (query.statusFilter().isPresent()) {
			StudyGroupStatus status = query.statusFilter().get();
			stream = stream.filter(group -> group.status() == status);
		}
		if (query.queryFilter().isPresent()) {
			String keyword = query.queryFilter().get().toLowerCase(java.util.Locale.ROOT);
			stream = stream.filter(group ->
				group.name().toLowerCase(java.util.Locale.ROOT).contains(keyword)
					|| group.topic().toLowerCase(java.util.Locale.ROOT).contains(keyword));
		}
		java.util.Comparator<StudyGroup> comparator = comparatorFor(query.sortField());
		if (comparator != null) {
			if (query.descending()) {
				comparator = comparator.reversed();
			}
			stream = stream.sorted(comparator);
		}
		return stream.toList();
	}

	private static java.util.Comparator<StudyGroup> comparatorFor(String sortField) {
		if (sortField == null) {
			return null;
		}
		return switch (sortField) {
			case "startsAt" -> java.util.Comparator.comparing(StudyGroup::startsAt);
			case "endsAt" -> java.util.Comparator.comparing(StudyGroup::endsAt);
			case "name" -> java.util.Comparator.comparing(group -> group.name().toLowerCase(java.util.Locale.ROOT));
			default -> null;
		};
	}

	@Transactional(readOnly = true)
	public StudyGroup getGroup(GetStudyGroupQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return repository.findGroupByIdForMemberUserId(query.groupId(), query.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(query.groupId())) {
					throw new StudyGroupNotFoundException("study group was not found.");
				}
				throw new StudyGroupAccessDeniedException("authenticated user is not a member of this study group.");
			});
	}

	@Transactional(readOnly = true)
	public StudyGroupMemberProfile getMyGroupMemberProfile(GetMyGroupMemberProfileQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return repository.findMyGroupMemberProfile(query.groupId(), query.authenticatedUserId())
			.orElseGet(() -> profileAccessFailure(query.groupId()));
	}

	@Transactional(readOnly = true)
	public int countActiveMembers(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return repository.countActiveOrOnboardingMembers(groupId);
	}

	@Transactional(readOnly = true)
	public java.util.Map<UUID, Integer> countActiveMembers(java.util.Collection<UUID> groupIds) {
		Objects.requireNonNull(groupIds, "groupIds must not be null");
		return repository.countActiveOrOnboardingMembersByGroupIds(groupIds);
	}

	@Transactional(readOnly = true)
	public List<GroupMemberSummary> listGroupMembers(ListGroupMembersQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		if (!repository.existsActiveOrOnboardingMember(query.groupId(), query.authenticatedUserId())) {
			if (!repository.existsStudyGroup(query.groupId())) {
				throw new StudyGroupNotFoundException("study group was not found.");
			}
			throw new StudyGroupAccessDeniedException("authenticated user is not a member of this study group.");
		}
		return repository.findGroupMembers(query.groupId());
	}

	@Transactional
	public StudyGroupMemberProfile updateMyGroupMemberProfile(UpdateMyGroupMemberProfileCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		boolean updated = repository.updateMyGroupMemberDisplayName(
			command.groupId(),
			command.authenticatedUserId(),
			command.displayName(),
			clock.instant()
		);
		if (!updated) {
			profileAccessFailure(command.groupId());
		}
		return getMyGroupMemberProfile(new GetMyGroupMemberProfileQuery(command.authenticatedUserId(), command.groupId()));
	}

	@Transactional
	public StudyGroup updateGroup(UpdateStudyGroupCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		StudyGroup group = repository.findGroupByIdForMemberUserId(command.groupId(), command.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(command.groupId())) {
					throw new StudyGroupNotFoundException("study group was not found.");
				}
				throw new StudyGroupAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!group.createdBy().equals(command.authenticatedUserId())) {
			throw new StudyGroupAccessDeniedException("only the study group owner can update the study group.");
		}
		int currentMembers = repository.countActiveOrOnboardingMembers(command.groupId());
		if (command.maxMembers() < currentMembers) {
			throw new InvalidStudyGroupMemberProfileRequestException(
				"maxMembers",
				"maxMembers must not be smaller than the current member count (" + currentMembers + ")."
			);
		}
		StudyGroup updated;
		try {
			updated = group.update(
				command.name(),
				command.topic(),
				command.detailKeywords(),
				command.maxMembers(),
				command.startsAt(),
				command.endsAt(),
				command.description(),
				clock.instant()
			);
		} catch (IllegalArgumentException exception) {
			throw new InvalidStudyGroupMemberProfileRequestException("name", exception.getMessage());
		}
		if (!repository.updateGroup(updated)) {
			throw new StudyGroupNotFoundException("study group was not found.");
		}
		return updated;
	}

	@Transactional
	public void deleteGroup(DeleteStudyGroupCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		StudyGroup group = repository.findGroupByIdForMemberUserId(command.groupId(), command.authenticatedUserId())
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(command.groupId())) {
					throw new StudyGroupNotFoundException("study group was not found.");
				}
				throw new StudyGroupAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!group.createdBy().equals(command.authenticatedUserId())) {
			throw new StudyGroupAccessDeniedException("only the study group owner can delete the study group.");
		}
		// 삭제 전에 알림을 받을 멤버(그룹장 본인 제외)의 userId 를 미리 수집한다.
		List<UUID> recipientUserIds = repository.findGroupMembers(command.groupId()).stream()
			.map(GroupMemberSummary::userId)
			.filter(userId -> !userId.equals(command.authenticatedUserId()))
			.distinct()
			.toList();
		if (!repository.softDeleteGroup(command.groupId(), clock.instant())) {
			throw new StudyGroupNotFoundException("study group was not found.");
		}
		String groupName = group.name();
		for (UUID recipientUserId : recipientUserIds) {
			publishNotification(() -> notificationEvents.publishGroupDeleted(command.groupId(), recipientUserId, groupName));
		}
	}

	private StudyGroupMemberProfile profileAccessFailure(UUID groupId) {
		if (!repository.existsStudyGroup(groupId)) {
			throw new StudyGroupNotFoundException("study group was not found.");
		}
		throw new StudyGroupAccessDeniedException("authenticated user is not a current member of this study group.");
	}

	private StudyGroupCreationResult createCandidate(CreateStudyGroupCommand command) {
		Instant now = clock.instant();
		StudyGroup group = StudyGroup.create(
			idGenerator.get(),
			command.authenticatedUserId(),
			command.name(),
			command.topic(),
			command.detailKeywords(),
			command.maxMembers(),
			command.startsAt(),
			command.endsAt(),
			command.description(),
			inviteCodeGenerator.get(),
			now
		);
		GroupMember ownerMember = GroupMember.owner(idGenerator.get(), group.id(), command.authenticatedUserId(), null, now);
		return new StudyGroupCreationResult(group, ownerMember);
	}

	private static void publishNotification(Runnable task) {
		try {
			task.run();
		} catch (RuntimeException exception) {
			log.warn("study group notification publishing failed", exception);
			// Notification creation must not roll back the primary study-group command.
		}
	}
}
