package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
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
		if (!target.isAcceptingJoins()) {
			throw new StudyGroupJoinRejectedException("study group is not accepting new members.");
		}
		if (repository.existsActiveOrOnboardingMember(target.id(), command.authenticatedUserId())) {
			throw new StudyGroupJoinRejectedException("user is already a member of this study group.");
		}
		if (!target.hasCapacity(repository.countActiveOrOnboardingMembers(target.id()))) {
			throw new StudyGroupJoinRejectedException("study group member capacity has been reached.");
		}

		GroupMember member = GroupMember.member(idGenerator.get(), target.id(), command.authenticatedUserId(), null, clock.instant());
		try {
			repository.saveJoinedMember(member);
		} catch (GroupMemberDuplicateMembershipException exception) {
			throw new StudyGroupJoinRejectedException("user is already a member of this study group.");
		}
		publishNotification(() -> notificationEvents.publishOnboardingRequested(member.groupId(), member.userId()));
		return new StudyGroupJoinResult(member);
	}

	@Transactional(readOnly = true)
	public List<StudyGroup> listMyGroups(ListStudyGroupsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return repository.findGroupsByMemberUserId(query.authenticatedUserId());
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
		if (!repository.softDeleteGroup(command.groupId(), clock.instant())) {
			throw new StudyGroupNotFoundException("study group was not found.");
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
