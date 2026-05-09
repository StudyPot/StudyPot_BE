package com.studypot.aistudyleader.studygroup.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class JdbcStudyGroupRepository implements StudyGroupRepository {

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
	public Optional<StudyGroupJoinTarget> findJoinTargetById(UUID groupId) {
		return queryOne(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET, JdbcStudyGroupRepository::mapJoinTarget, uuid(groupId));
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

	private static StudyGroupJoinTarget mapJoinTarget(ResultSet resultSet, int rowNumber) throws SQLException {
		return new StudyGroupJoinTarget(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			StudyGroupStatus.valueOf(resultSet.getString("status")),
			resultSet.getInt("max_members"),
			resultSet.getString("invite_code")
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

	private static Date date(LocalDate date) {
		return Date.valueOf(date);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
