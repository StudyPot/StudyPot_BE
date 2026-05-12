package com.studypot.aistudyleader.studygroup.rules.repository;

import com.studypot.aistudyleader.studygroup.rules.domain.GroupRule;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleMembership;
import com.studypot.aistudyleader.studygroup.rules.domain.GroupRuleType;
import com.studypot.aistudyleader.studygroup.rules.domain.RuleViolation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRuleRepository {

	/**
	 * Returns true when the study group exists as a live row.
	 */
	boolean existsStudyGroup(UUID groupId);

	/**
	 * Looks up the authenticated user's current membership by users.id.
	 */
	Optional<GroupRuleMembership> findMembership(UUID groupId, UUID userId);

	/**
	 * Looks up a target membership by group_member.id.
	 */
	Optional<GroupRuleMembership> findMemberById(UUID groupId, UUID memberId);

	/**
	 * Selects one live rule with a pessimistic row lock. Call inside a short
	 * transaction; the lock is held until commit or rollback.
	 */
	Optional<GroupRule> findRuleByGroupAndTypeForUpdate(UUID groupId, GroupRuleType ruleType);

	Optional<GroupRule> findRuleById(UUID groupId, UUID ruleId);

	boolean existsTaskCompletion(UUID taskCompletionId);

	boolean taskCompletionBelongsToMember(UUID taskCompletionId, UUID memberId);

	List<GroupRule> findRulesByGroupId(UUID groupId);

	boolean insertRule(GroupRule rule);

	boolean updateRule(GroupRule rule);

	boolean deactivateRule(UUID groupId, UUID ruleId, Instant updatedAt);

	boolean softDeleteRule(UUID groupId, UUID ruleId, Instant deletedAt);

	Optional<RuleViolation> findViolationById(UUID groupId, UUID violationId);

	List<RuleViolation> findViolationsByGroupId(UUID groupId);

	void insertViolation(RuleViolation violation);

	boolean updateViolationStatus(RuleViolation violation);
}
