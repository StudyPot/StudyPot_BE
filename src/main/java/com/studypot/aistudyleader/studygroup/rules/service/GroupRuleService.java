package com.studypot.aistudyleader.studygroup.rules.service;

import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class GroupRuleService {

	private final GroupRuleRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public GroupRuleService(GroupRuleRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public GroupRule saveRule(SaveGroupRuleCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		requireCurrentMembership(command.groupId(), command.authenticatedUserId());
		Instant now = clock.instant();
		return repository.findRuleByGroupAndTypeForUpdate(command.groupId(), command.ruleType())
			.map(existing -> updateRule(existing, command, now))
			.orElseGet(() -> insertRule(command, now));
	}

	@Transactional(readOnly = true)
	public List<GroupRule> listRules(ListGroupRulesQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireCurrentMembership(query.groupId(), query.authenticatedUserId());
		return repository.findRulesByGroupId(query.groupId());
	}

	@Transactional
	public void deactivateRule(DeactivateGroupRuleCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		requireCurrentMembership(command.groupId(), command.authenticatedUserId());
		boolean updated = repository.deactivateRule(command.groupId(), command.ruleId(), clock.instant());
		if (!updated) {
			throw new GroupRuleNotFoundException("group rule was not found.");
		}
	}

	@Transactional
	public void deleteRule(DeleteGroupRuleCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		requireCurrentMembership(command.groupId(), command.authenticatedUserId());
		boolean updated = repository.softDeleteRule(command.groupId(), command.ruleId(), clock.instant());
		if (!updated) {
			throw new GroupRuleNotFoundException("group rule was not found.");
		}
	}

	@Transactional
	public RuleViolation recordViolation(RecordRuleViolationCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		requireCurrentMembership(command.groupId(), command.authenticatedUserId());
		GroupRuleMembership target = repository.findMemberById(command.groupId(), command.memberId())
			.orElseThrow(() -> new GroupRuleAccessDeniedException("target member is not in this study group."));
		if (!target.isCurrent()) {
			throw new GroupRuleAccessDeniedException("current group membership is required.");
		}
		GroupRule rule = repository.findRuleById(command.groupId(), command.ruleId())
			.orElseThrow(() -> new GroupRuleNotFoundException("group rule was not found."));
		if (!rule.active()) {
			throw new GroupRuleMutationRejectedException("inactive group rule cannot record violations.");
		}
		command.taskCompletionId().ifPresent(taskCompletionId -> requireTaskCompletionForMember(taskCompletionId, command.memberId()));
		if (command.details().containsKey("violationType")) {
			throw new InvalidGroupRuleRequestException("details.violationType", "details must not contain reserved key: violationType.");
		}
		Instant now = clock.instant();
		RuleViolation violation = RuleViolation.open(
			idGenerator.get(),
			rule.id(),
			command.memberId(),
			command.taskCompletionId().orElse(null),
			command.violationType(),
			command.details(),
			command.occurredAt().orElse(now),
			now
		);
		repository.insertViolation(violation);
		return violation;
	}

	@Transactional(readOnly = true)
	public List<RuleViolation> listViolations(ListRuleViolationsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireCurrentMembership(query.groupId(), query.authenticatedUserId());
		return repository.findViolationsByGroupId(query.groupId());
	}

	@Transactional
	public RuleViolation resolveViolation(HandleRuleViolationCommand command) {
		return handleViolation(command, true);
	}

	@Transactional
	public RuleViolation waiveViolation(HandleRuleViolationCommand command) {
		return handleViolation(command, false);
	}

	private RuleViolation handleViolation(HandleRuleViolationCommand command, boolean resolve) {
		Objects.requireNonNull(command, "command must not be null");
		requireCurrentMembership(command.groupId(), command.authenticatedUserId());
		RuleViolation violation = repository.findViolationById(command.groupId(), command.violationId())
			.orElseThrow(() -> new GroupRuleNotFoundException("rule violation was not found."));
		if (!violation.isOpen()) {
			throw new GroupRuleMutationRejectedException("only OPEN rule violations can be handled.");
		}
		RuleViolation handled = resolve
			? violation.resolve(command.note(), clock.instant())
			: violation.waive(command.note(), clock.instant());
		boolean updated = repository.updateViolationStatus(handled);
		if (!updated) {
			throw new GroupRuleMutationRejectedException("rule violation status could not be updated.");
		}
		return handled;
	}

	private GroupRule insertRule(SaveGroupRuleCommand command, Instant now) {
		GroupRule rule = GroupRule.create(
			idGenerator.get(),
			command.groupId(),
			command.authenticatedUserId(),
			command.ruleType(),
			command.config(),
			command.description(),
			command.active(),
			now
		);
		if (!repository.insertRule(rule)) {
			throw new GroupRuleMutationRejectedException("group rule could not be inserted.");
		}
		return rule;
	}

	private GroupRule updateRule(GroupRule existing, SaveGroupRuleCommand command, Instant now) {
		GroupRule rule = existing.update(command.config(), command.description(), command.active(), now);
		if (!repository.updateRule(rule)) {
			throw new GroupRuleNotFoundException("group rule was not found.");
		}
		return rule;
	}

	private void requireTaskCompletionForMember(UUID taskCompletionId, UUID memberId) {
		if (!repository.existsTaskCompletion(taskCompletionId)) {
			throw new GroupRuleNotFoundException("task completion was not found.");
		}
		if (!repository.taskCompletionBelongsToMember(taskCompletionId, memberId)) {
			throw new GroupRuleAccessDeniedException("task completion does not belong to the target member.");
		}
	}

	private GroupRuleMembership requireCurrentMembership(UUID groupId, UUID userId) {
		GroupRuleMembership membership = repository.findMembership(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new GroupRuleGroupNotFoundException("study group was not found.");
				}
				throw new GroupRuleAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!membership.isCurrent()) {
			throw new GroupRuleAccessDeniedException("current group membership is required.");
		}
		return membership;
	}
}
