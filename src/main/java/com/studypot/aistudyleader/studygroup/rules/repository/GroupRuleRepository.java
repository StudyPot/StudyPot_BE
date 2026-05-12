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

	boolean existsStudyGroup(UUID groupId);

	Optional<GroupRuleMembership> findMembership(UUID groupId, UUID userId);

	Optional<GroupRuleMembership> findMemberById(UUID groupId, UUID memberId);

	Optional<GroupRule> findRuleByGroupAndTypeForUpdate(UUID groupId, GroupRuleType ruleType);

	Optional<GroupRule> findRuleById(UUID groupId, UUID ruleId);

	boolean taskCompletionBelongsToMember(UUID taskCompletionId, UUID memberId);

	List<GroupRule> findRulesByGroupId(UUID groupId);

	void insertRule(GroupRule rule);

	void updateRule(GroupRule rule);

	boolean deactivateRule(UUID groupId, UUID ruleId, Instant updatedAt);

	boolean softDeleteRule(UUID groupId, UUID ruleId, Instant deletedAt);

	Optional<RuleViolation> findViolationById(UUID groupId, UUID violationId);

	List<RuleViolation> findViolationsByGroupId(UUID groupId);

	void insertViolation(RuleViolation violation);

	boolean updateViolationStatus(RuleViolation violation);
}
