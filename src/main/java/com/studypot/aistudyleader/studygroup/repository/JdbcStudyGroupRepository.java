package com.studypot.aistudyleader.studygroup.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcStudyGroupRepository implements StudyGroupRepository {

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcStudyGroupRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
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
			if (existsActiveGroupByInviteCode(group.inviteCode())) {
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

	private boolean existsActiveGroupByInviteCode(String inviteCode) {
		return Boolean.TRUE.equals(
			jdbcTemplate.queryForObject(StudyGroupJdbcSql.EXISTS_ACTIVE_GROUP_BY_INVITE_CODE, Boolean.class, inviteCode)
		);
	}

	private String detailKeywordsJson(StudyGroup group) {
		try {
			return objectMapper.writeValueAsString(group.detailKeywords());
		} catch (JsonProcessingException exception) {
			throw new StudyGroupPersistenceException("study group detail keywords could not be serialized.", exception);
		}
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
}
