package com.studypot.aistudyleader.studygroup.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class JdbcStudyGroupRepository implements StudyGroupRepository {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};
	private static final TypeReference<java.util.Map<String, Integer>> SCORE_MAP = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcStudyGroupRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	@Transactional
	public void saveCreatedGroup(StudyGroup group, GroupMember ownerMember) {
		insertStudyGroup(group);
		insertOwnerMember(ownerMember);
	}

	private void insertStudyGroup(StudyGroup group) {
		try {
			jdbcTemplate.update(
				StudyGroupJdbcSql.INSERT_STUDY_GROUP,
				uuid(group.id()),
				uuid(group.createdBy()),
				group.name(),
				group.description().orElse(null),
				group.topic(),
				detailKeywordsJson(group),
				group.status().name(),
				group.maxMembers(),
				group.isPublic(),
				group.inviteCode(),
				date(group.startsAt()),
				date(group.endsAt()),
				timestamp(group.onboardingStartedAt().orElse(null)),
				timestamp(group.startedAt().orElse(null)),
				timestamp(group.auditMetadata().createdAt()),
				timestamp(group.auditMetadata().updatedAt())
			);
		} catch (DuplicateKeyException exception) {
			if (isInviteCodeConflict(exception)) {
				throw new StudyGroupInviteCodeConflictException("study group invite code is already reserved.");
			}
			throw new StudyGroupPersistenceException("study group uniqueness conflict.", exception);
		}
	}

	private void insertOwnerMember(GroupMember ownerMember) {
		jdbcTemplate.update(
			StudyGroupJdbcSql.INSERT_GROUP_MEMBER,
			uuid(ownerMember.id()),
			uuid(ownerMember.groupId()),
			uuid(ownerMember.userId()),
			ownerMember.permission().name(),
			ownerMember.status().name(),
			ownerMember.displayName().orElse(null),
			timestamp(ownerMember.joinedAt()),
			timestamp(ownerMember.activatedAt().orElse(null)),
			timestamp(ownerMember.leftAt().orElse(null)),
			timestamp(ownerMember.auditMetadata().createdAt()),
			timestamp(ownerMember.auditMetadata().updatedAt())
		);
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			StudyGroupJdbcSql.EXISTS_STUDY_GROUP,
			Boolean.class,
			uuid(groupId)
		));
	}

	@Override
	public Optional<StudyGroup> findGroupByIdForMemberUserId(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(StudyGroupJdbcSql.SELECT_GROUP_BY_ID_FOR_MEMBER_USER_ID, this::mapStudyGroup, uuid(groupId), uuid(userId));
	}

	@Override
	public Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId) {
		return queryOne(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET, JdbcStudyGroupRepository::mapJoinTarget, uuid(groupId));
	}

	@Override
	public Optional<StudyGroupJoinTarget> findJoinTargetByInviteCode(String inviteCode) {
		Objects.requireNonNull(inviteCode, "inviteCode must not be null");
		return queryOne(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET_BY_INVITE_CODE, JdbcStudyGroupRepository::mapJoinTarget, inviteCode);
	}

	@Override
	public boolean revertReadyToStartToOnboarding(UUID groupId, Instant updatedAt) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return jdbcTemplate.update(StudyGroupJdbcSql.REVERT_READY_TO_ONBOARDING, timestamp(updatedAt), uuid(groupId)) == 1;
	}

	@Override
	public Optional<UUID> findOwnerUserId(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return queryOne(StudyGroupJdbcSql.SELECT_OWNER_USER_ID,
			(resultSet, rowNumber) -> UuidBinary.fromBytes(resultSet.getBytes("user_id")), uuid(groupId));
	}

	@Override
	public boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			StudyGroupJdbcSql.EXISTS_ACTIVE_OR_ONBOARDING_MEMBER,
			Boolean.class,
			uuid(groupId),
			uuid(userId)
		));
	}

	@Override
	public int countActiveOrOnboardingMembers(UUID groupId) {
		Integer count = jdbcTemplate.queryForObject(
			StudyGroupJdbcSql.COUNT_ACTIVE_OR_ONBOARDING_MEMBERS,
			Integer.class,
			uuid(groupId)
		);
		return count == null ? 0 : count;
	}

	@Override
	public Map<UUID, Integer> countActiveOrOnboardingMembersByGroupIds(Collection<UUID> groupIds) {
		Objects.requireNonNull(groupIds, "groupIds must not be null");
		if (groupIds.isEmpty()) {
			return Map.of();
		}
		List<UUID> ids = List.copyOf(groupIds);
		String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
		String sql = String.format(StudyGroupJdbcSql.COUNT_ACTIVE_OR_ONBOARDING_MEMBERS_BY_GROUPS, placeholders);
		Object[] args = ids.stream().map(JdbcStudyGroupRepository::uuid).toArray();
		Map<UUID, Integer> counts = new HashMap<>();
		jdbcTemplate.query(sql, resultSet -> {
			counts.put(UuidBinary.fromBytes(resultSet.getBytes("group_id")), resultSet.getInt("member_count"));
		}, args);
		return counts;
	}

	@Override
	public void saveJoinedMember(GroupMember member) {
		try {
			jdbcTemplate.update(
				StudyGroupJdbcSql.INSERT_GROUP_MEMBER,
				uuid(member.id()),
				uuid(member.groupId()),
				uuid(member.userId()),
				member.permission().name(),
				member.status().name(),
				member.displayName().orElse(null),
				timestamp(member.joinedAt()),
				timestamp(member.activatedAt().orElse(null)),
				timestamp(member.leftAt().orElse(null)),
				timestamp(member.auditMetadata().createdAt()),
				timestamp(member.auditMetadata().updatedAt())
			);
		} catch (DuplicateKeyException exception) {
			if (isGroupMemberConflict(exception)) {
				throw new GroupMemberDuplicateMembershipException("group member already exists.");
			}
			throw new StudyGroupPersistenceException("group member uniqueness conflict.", exception);
		}
	}

	@Override
	public List<StudyGroup> findGroupsByMemberUserId(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return jdbcTemplate.query(StudyGroupJdbcSql.SELECT_GROUPS_BY_MEMBER_USER_ID, this::mapStudyGroup, uuid(userId));
	}

	@Override
	public Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(StudyGroupJdbcSql.SELECT_MY_GROUP_MEMBER_PROFILE, this::mapMemberProfile, uuid(groupId), uuid(userId));
	}

	@Override
	public boolean updateGroup(StudyGroup group) {
		Objects.requireNonNull(group, "group must not be null");
		return jdbcTemplate.update(
			StudyGroupJdbcSql.UPDATE_STUDY_GROUP,
			group.name(),
			group.topic(),
			detailKeywordsJson(group),
			group.maxMembers(),
			date(group.startsAt()),
			date(group.endsAt()),
			group.description().orElse(null),
			timestamp(group.auditMetadata().updatedAt()),
			uuid(group.id())
		) == 1;
	}

	@Override
	public boolean softDeleteGroup(UUID groupId, Instant deletedAt) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(deletedAt, "deletedAt must not be null");
		Timestamp now = timestamp(deletedAt);
		return jdbcTemplate.update(StudyGroupJdbcSql.SOFT_DELETE_GROUP, now, now, uuid(groupId)) == 1;
	}

	@Override
	public List<GroupMemberSummary> findGroupMembers(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(StudyGroupJdbcSql.SELECT_GROUP_MEMBERS, JdbcStudyGroupRepository::mapGroupMemberSummary, uuid(groupId));
	}

	@Override
	public boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");
		Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		return jdbcTemplate.update(
			StudyGroupJdbcSql.UPDATE_MY_GROUP_MEMBER_DISPLAY_NAME,
			displayName,
			timestamp(updatedAt),
			uuid(groupId),
			uuid(userId)
		) == 1;
	}

	private static StudyGroupJoinTarget mapJoinTarget(ResultSet resultSet, int rowNumber) throws SQLException {
		return new StudyGroupJoinTarget(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			StudyGroupStatus.valueOf(resultSet.getString("status")),
			resultSet.getInt("max_members"),
			resultSet.getString("invite_code")
		);
	}

	private StudyGroup mapStudyGroup(ResultSet resultSet, int rowNumber) throws SQLException {
		return StudyGroup.rehydrate(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("created_by")),
			resultSet.getString("name"),
			resultSet.getString("topic"),
			readDetailKeywords(resultSet.getString("detail_keywords")),
			StudyGroupStatus.valueOf(resultSet.getString("status")),
			resultSet.getInt("max_members"),
			resultSet.getBoolean("is_public"),
			resultSet.getString("invite_code"),
			resultSet.getDate("starts_at").toLocalDate(),
			resultSet.getDate("ends_at").toLocalDate(),
			resultSet.getString("description"),
			instant(resultSet.getTimestamp("onboarding_started_at")),
			instant(resultSet.getTimestamp("started_at")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private StudyGroupMemberProfile mapMemberProfile(ResultSet resultSet, int rowNumber) throws SQLException {
		String onboardingStatus = resultSet.getString("onboarding_status");
		boolean onboardingSubmitted = "SUBMITTED".equals(onboardingStatus);
		StudyGroupMemberProfile.CurrentWeekSummary currentWeek = null;
		UUID currentWeekId = uuid(resultSet.getBytes("current_week_id"));
		if (currentWeekId != null) {
			String progressStatus = resultSet.getString("progress_status");
			currentWeek = new StudyGroupMemberProfile.CurrentWeekSummary(
				currentWeekId,
				resultSet.getInt("week_number"),
				resultSet.getString("sprint_goal"),
				instant(resultSet.getTimestamp("week_starts_at")),
				instant(resultSet.getTimestamp("week_ends_at")),
				progressStatus == null ? MemberWeekProgressStatus.NOT_STARTED : MemberWeekProgressStatus.valueOf(progressStatus)
			);
		}
		return new StudyGroupMemberProfile(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			UuidBinary.fromBytes(resultSet.getBytes("user_id")),
			resultSet.getString("display_name"),
			com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission.valueOf(resultSet.getString("permission")),
			com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus.valueOf(resultSet.getString("member_status")),
			new StudyGroupMemberProfile.OnboardingSummary(
				onboardingSubmitted,
				onboardingSubmitted ? skillLevel(readScoreMap(resultSet.getString("keyword_skill_levels"))) : null,
				instant(resultSet.getTimestamp("onboarding_submitted_at"))
			),
			currentWeek,
			new StudyGroupMemberProfile.TaskCompletionSummary(
				resultSet.getInt("task_total_count"),
				resultSet.getInt("task_done_count"),
				resultSet.getInt("task_incomplete_count"),
				resultSet.getInt("task_skipped_count")
			),
			new StudyGroupMemberProfile.RetrospectiveSummary(resultSet.getBoolean("retrospective_feedback_ready"))
		);
	}

	private static GroupMemberSummary mapGroupMemberSummary(ResultSet resultSet, int rowNumber) throws SQLException {
		return new GroupMemberSummary(
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("user_id")),
			GroupMemberPermission.valueOf(resultSet.getString("permission")),
			GroupMemberStatus.valueOf(resultSet.getString("member_status")),
			resultSet.getString("display_name"),
			resultSet.getString("nickname"),
			resultSet.getString("email"),
			resultSet.getString("onboarding_status")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private String detailKeywordsJson(StudyGroup group) {
		try {
			return objectMapper.writeValueAsString(group.detailKeywords());
		} catch (JsonProcessingException exception) {
			throw new StudyGroupPersistenceException("study group detail keywords could not be serialized.", exception);
		}
	}

	private List<String> readDetailKeywords(String value) {
		try {
			return objectMapper.readValue(value, STRING_LIST);
		} catch (JsonProcessingException exception) {
			throw new StudyGroupPersistenceException("study group detail keywords could not be deserialized.", exception);
		}
	}

	private java.util.Map<String, Integer> readScoreMap(String value) {
		if (value == null || value.isBlank()) {
			return java.util.Map.of();
		}
		try {
			return objectMapper.readValue(value, SCORE_MAP);
		} catch (JsonProcessingException exception) {
			throw new StudyGroupPersistenceException("study group skill levels could not be deserialized.", exception);
		}
	}

	private static int skillLevel(java.util.Map<String, Integer> scoreMap) {
		if (scoreMap.isEmpty()) {
			return 1;
		}
		double average = scoreMap.values().stream()
			.mapToInt(Integer::intValue)
			.average()
			.orElse(1);
		return (int) Math.round(average);
	}

	private static boolean isInviteCodeConflict(DuplicateKeyException exception) {
		String normalized = normalizedDuplicateMessage(exception);
		return normalized.contains("study_group_invite_code_live_uidx")
			|| normalized.contains("study_group_invite_code_uidx")
			|| normalized.contains("invite_code_live_key")
			|| normalized.contains("invite_code");
	}

	private static boolean isGroupMemberConflict(DuplicateKeyException exception) {
		String normalized = normalizedDuplicateMessage(exception);
		return normalized.contains("group_member_group_user_live_uidx")
			|| normalized.contains("group_member_group_user_uidx")
			|| normalized.contains("group_user_live_key");
	}

	private static String normalizedDuplicateMessage(DuplicateKeyException exception) {
		String message = (exception.getMostSpecificCause() == null ? exception : exception.getMostSpecificCause())
			.getMessage();
		if (message == null) {
			message = exception.getMessage();
		}
		return message == null ? "" : message.toLowerCase(Locale.ROOT);
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(byte[] value) {
		return value == null ? null : UuidBinary.fromBytes(value);
	}

	private static Date date(LocalDate date) {
		return Date.valueOf(date);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
