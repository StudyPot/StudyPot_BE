package com.studypot.aistudyleader.studygroup.rules.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationStatus;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolationType;
import com.studypot.aistudyleader.studygroup.rules.repository.GroupRuleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupRuleServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000005002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000005003");
	private static final UUID RULE_ID = UUID.fromString("018f0000-0000-7000-8000-000000005004");
	private static final UUID VIOLATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005005");
	private static final UUID TASK_COMPLETION_ID = UUID.fromString("018f0000-0000-7000-8000-000000005006");
	private static final Instant NOW = Instant.parse("2026-05-12T00:30:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void saveRuleCreatesNewRuleForCurrentGroupMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		GroupRuleService service = service(repository, RULE_ID);

		GroupRule result = service.saveRule(new SaveGroupRuleCommand(
			USER_ID,
			GROUP_ID,
			GroupRuleType.TASK_DEADLINE,
			Map.of("dueDayOfWeek", 5, "dueTime", "23:59", "timezone", "Asia/Seoul"),
			"금요일 자정 전까지 제출",
			true
		));

		assertThat(result.id()).isEqualTo(RULE_ID);
		assertThat(result.groupId()).isEqualTo(GROUP_ID);
		assertThat(result.createdBy()).isEqualTo(USER_ID);
		assertThat(result.ruleType()).isEqualTo(GroupRuleType.TASK_DEADLINE);
		assertThat(result.config()).containsEntry("dueTime", "23:59");
		assertThat(result.description()).contains("금요일");
		assertThat(result.active()).isTrue();
		assertThat(result.createdAt()).isEqualTo(NOW);
		assertThat(repository.insertedRule).isSameAs(result);
	}

	@Test
	void saveRuleUpdatesExistingLiveRuleForSameType() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.existingRule = rule(GroupRuleType.RETROSPECTIVE_REQUIRED, true);
		GroupRuleService service = service(repository);

		GroupRule result = service.saveRule(new SaveGroupRuleCommand(
			USER_ID,
			GROUP_ID,
			GroupRuleType.RETROSPECTIVE_REQUIRED,
			Map.of("required", true),
			"매주 회고 필수",
			true
		));

		assertThat(result.id()).isEqualTo(RULE_ID);
		assertThat(result.config()).containsEntry("required", true);
		assertThat(result.description()).contains("회고");
		assertThat(result.updatedAt()).isEqualTo(NOW);
		assertThat(repository.updatedRule).isSameAs(result);
		assertThat(repository.insertedRule).isNull();
	}

	@Test
	void listRulesRejectsNonMemberOfExistingGroup() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;
		GroupRuleService service = service(repository);

		assertThatThrownBy(() -> service.listRules(new ListGroupRulesQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(GroupRuleAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void deactivateRuleRequiresCurrentMemberAndMarksRuleInactive() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.deactivateResult = true;
		GroupRuleService service = service(repository);

		service.deactivateRule(new DeactivateGroupRuleCommand(USER_ID, GROUP_ID, RULE_ID));

		assertThat(repository.deactivatedRuleId).isEqualTo(RULE_ID);
		assertThat(repository.deactivatedAt).isEqualTo(NOW);
	}

	@Test
	void deleteRuleRejectsMissingRule() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.deleteResult = false;
		GroupRuleService service = service(repository);

		assertThatThrownBy(() -> service.deleteRule(new DeleteGroupRuleCommand(USER_ID, GROUP_ID, RULE_ID)))
			.isInstanceOf(GroupRuleNotFoundException.class)
			.hasMessage("group rule was not found.");
	}

	@Test
	void recordViolationCreatesOpenViolationForActiveTargetMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.targetMembership = activeMember();
		repository.ruleForViolation = rule(GroupRuleType.TASK_DEADLINE, true);
		GroupRuleService service = service(repository, VIOLATION_ID);

		RuleViolation result = service.recordViolation(new RecordRuleViolationCommand(
			USER_ID,
			GROUP_ID,
			RULE_ID,
			MEMBER_ID,
			TASK_COMPLETION_ID,
			RuleViolationType.INCOMPLETE_REASON_MISSING,
			Map.of("reason", "마감 후 미완료 사유 없음"),
			NOW.minusSeconds(60)
		));

		assertThat(result.id()).isEqualTo(VIOLATION_ID);
		assertThat(result.ruleId()).isEqualTo(RULE_ID);
		assertThat(result.memberId()).isEqualTo(MEMBER_ID);
		assertThat(result.taskCompletionId()).contains(TASK_COMPLETION_ID);
		assertThat(result.violationType()).isEqualTo(RuleViolationType.INCOMPLETE_REASON_MISSING);
		assertThat(result.details()).containsEntry("reason", "마감 후 미완료 사유 없음");
		assertThat(result.status()).isEqualTo(RuleViolationStatus.OPEN);
		assertThat(result.occurredAt()).isEqualTo(NOW.minusSeconds(60));
		assertThat(repository.insertedViolation).isSameAs(result);
	}

	@Test
	void recordViolationRejectsLeftActor() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = leftMember();
		repository.targetMembership = activeMember();
		repository.ruleForViolation = rule(GroupRuleType.TASK_DEADLINE, true);
		GroupRuleService service = service(repository, VIOLATION_ID);

		assertThatThrownBy(() -> service.recordViolation(new RecordRuleViolationCommand(
				USER_ID,
				GROUP_ID,
				RULE_ID,
				MEMBER_ID,
				null,
				RuleViolationType.CUSTOM,
				Map.of("memo", "운영 메모 위반"),
				NOW
			)))
			.isInstanceOf(GroupRuleAccessDeniedException.class)
			.hasMessage("current group membership is required.");
		assertThat(repository.insertedViolation).isNull();
	}

	@Test
	void recordViolationRejectsTaskCompletionThatDoesNotBelongToTargetMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.targetMembership = activeMember();
		repository.ruleForViolation = rule(GroupRuleType.TASK_DEADLINE, true);
		repository.taskCompletionBelongsToMember = false;
		GroupRuleService service = service(repository, VIOLATION_ID);

		assertThatThrownBy(() -> service.recordViolation(new RecordRuleViolationCommand(
				USER_ID,
				GROUP_ID,
				RULE_ID,
				MEMBER_ID,
				TASK_COMPLETION_ID,
				RuleViolationType.INCOMPLETE_REASON_MISSING,
				Map.of("reason", "마감 후 미완료 사유 없음"),
				NOW
			)))
			.isInstanceOf(GroupRuleAccessDeniedException.class)
			.hasMessage("task completion does not belong to the target member.");
		assertThat(repository.insertedViolation).isNull();
	}

	@Test
	void resolveViolationMovesOpenViolationToResolved() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.existingViolation = violation(RuleViolationStatus.OPEN);
		repository.updateViolationResult = true;
		GroupRuleService service = service(repository);

		RuleViolation result = service.resolveViolation(new HandleRuleViolationCommand(
			USER_ID,
			GROUP_ID,
			VIOLATION_ID,
			"미완료 사유를 제출해서 해결"
		));

		assertThat(result.status()).isEqualTo(RuleViolationStatus.RESOLVED);
		assertThat(result.resolvedAt()).contains(NOW);
		assertThat(result.resolvedNote()).hasValueSatisfying(note -> assertThat(note).contains("미완료 사유"));
		assertThat(repository.updatedViolation).isSameAs(result);
	}

	@Test
	void waiveViolationRejectsAlreadyHandledViolation() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMember();
		repository.existingViolation = violation(RuleViolationStatus.RESOLVED);
		GroupRuleService service = service(repository);

		assertThatThrownBy(() -> service.waiveViolation(new HandleRuleViolationCommand(
				USER_ID,
				GROUP_ID,
				VIOLATION_ID,
				"운영상 면제"
			)))
			.isInstanceOf(GroupRuleMutationRejectedException.class)
			.hasMessage("only OPEN rule violations can be handled.");
	}

	private static GroupRuleService service(CapturingRepository repository, UUID... ids) {
		Queue<UUID> idQueue = new ArrayDeque<>(List.of(ids));
		return new GroupRuleService(
			repository,
			CLOCK,
			() -> {
				UUID id = idQueue.poll();
				if (id == null) {
					throw new AssertionError("no deterministic id left");
				}
				return id;
			}
		);
	}

	private static GroupRuleMembership activeMember() {
		return new GroupRuleMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
	}

	private static GroupRuleMembership leftMember() {
		return new GroupRuleMembership(GROUP_ID, MEMBER_ID, GroupMemberPermission.MEMBER, GroupMemberStatus.LEFT);
	}

	private static GroupRule rule(GroupRuleType type, boolean active) {
		return new GroupRule(
			RULE_ID,
			GROUP_ID,
			USER_ID,
			type,
			Map.of("seed", true),
			"기존 규칙",
			active,
			NOW.minusSeconds(3600),
			NOW.minusSeconds(3600),
			Optional.empty()
		);
	}

	private static RuleViolation violation(RuleViolationStatus status) {
		return new RuleViolation(
			VIOLATION_ID,
			RULE_ID,
			MEMBER_ID,
			Optional.of(TASK_COMPLETION_ID),
			RuleViolationType.INCOMPLETE_REASON_MISSING,
			Map.of("reason", "missing"),
			status,
			Optional.empty(),
			Optional.empty(),
			NOW.minusSeconds(60),
			NOW.minusSeconds(60)
		);
	}

	private static final class CapturingRepository implements GroupRuleRepository {

		private boolean groupExists;
		private GroupRuleMembership membership;
		private GroupRuleMembership targetMembership;
		private GroupRule existingRule;
		private GroupRule ruleForViolation;
		private GroupRule insertedRule;
		private GroupRule updatedRule;
		private boolean deactivateResult;
		private UUID deactivatedRuleId;
		private Instant deactivatedAt;
		private boolean deleteResult;
		private RuleViolation existingViolation;
		private RuleViolation insertedViolation;
		private RuleViolation updatedViolation;
		private boolean updateViolationResult;
		private boolean taskCompletionBelongsToMember = true;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists || membership != null;
		}

		@Override
		public Optional<GroupRuleMembership> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public Optional<GroupRuleMembership> findMemberById(UUID groupId, UUID memberId) {
			return Optional.ofNullable(targetMembership);
		}

		@Override
		public Optional<GroupRule> findRuleByGroupAndTypeForUpdate(UUID groupId, GroupRuleType ruleType) {
			return Optional.ofNullable(existingRule);
		}

		@Override
		public Optional<GroupRule> findRuleById(UUID groupId, UUID ruleId) {
			return Optional.ofNullable(ruleForViolation);
		}

		@Override
		public boolean taskCompletionBelongsToMember(UUID taskCompletionId, UUID memberId) {
			return taskCompletionBelongsToMember;
		}

		@Override
		public List<GroupRule> findRulesByGroupId(UUID groupId) {
			return existingRule == null ? List.of() : List.of(existingRule);
		}

		@Override
		public void insertRule(GroupRule rule) {
			insertedRule = rule;
		}

		@Override
		public void updateRule(GroupRule rule) {
			updatedRule = rule;
		}

		@Override
		public boolean deactivateRule(UUID groupId, UUID ruleId, Instant updatedAt) {
			deactivatedRuleId = ruleId;
			deactivatedAt = updatedAt;
			return deactivateResult;
		}

		@Override
		public boolean softDeleteRule(UUID groupId, UUID ruleId, Instant deletedAt) {
			return deleteResult;
		}

		@Override
		public Optional<RuleViolation> findViolationById(UUID groupId, UUID violationId) {
			return Optional.ofNullable(existingViolation);
		}

		@Override
		public List<RuleViolation> findViolationsByGroupId(UUID groupId) {
			return existingViolation == null ? List.of() : List.of(existingViolation);
		}

		@Override
		public void insertViolation(RuleViolation violation) {
			insertedViolation = violation;
		}

		@Override
		public boolean updateViolationStatus(RuleViolation violation) {
			updatedViolation = violation;
			return updateViolationResult;
		}
	}
}
