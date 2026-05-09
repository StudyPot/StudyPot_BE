package com.studypot.aistudyleader.studygroup.service;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupInviteCodeConflictException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class StudyGroupService {

	private final StudyGroupRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;
	private final Supplier<String> inviteCodeGenerator;
	private final int inviteCodeMaxAttempts;

	public StudyGroupService(
		StudyGroupRepository repository,
		Clock clock,
		Supplier<UUID> idGenerator,
		Supplier<String> inviteCodeGenerator,
		int inviteCodeMaxAttempts
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
		this.inviteCodeGenerator = Objects.requireNonNull(inviteCodeGenerator, "inviteCodeGenerator must not be null");
		if (inviteCodeMaxAttempts <= 0) {
			throw new IllegalArgumentException("inviteCodeMaxAttempts must be positive");
		}
		this.inviteCodeMaxAttempts = inviteCodeMaxAttempts;
	}

	@Transactional
	public StudyGroupCreationResult createGroup(CreateStudyGroupCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		for (int attempt = 0; attempt < inviteCodeMaxAttempts; attempt++) {
			StudyGroupCreationResult result = createCandidate(command);
			try {
				repository.saveCreatedGroup(result.group(), result.ownerMember());
				return result;
			} catch (StudyGroupInviteCodeConflictException exception) {
				// Try another generated invite code until the bounded retry budget is exhausted.
			}
		}
		throw new StudyGroupServiceUnavailableException("could not allocate a unique invite code.");
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
}
