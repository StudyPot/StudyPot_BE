package com.studypot.aistudyleader.studygroup.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcStudyGroupRepositoryTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002841");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002842");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002843");
	private static final Instant NOW = Instant.parse("2026-05-09T02:00:00Z");
	private static final LocalDate STARTS_AT = LocalDate.parse("2026-05-10");
	private static final LocalDate ENDS_AT = LocalDate.parse("2026-06-21");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcStudyGroupRepository repository = new JdbcStudyGroupRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void saveCreatedGroupInsertsStudyGroupAndOwnerMemberRows() {
		StudyGroup group = group();
		GroupMember owner = owner(group);

		repository.saveCreatedGroup(group, owner);

		ArgumentCaptor<Object[]> groupArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(StudyGroupJdbcSql.INSERT_STUDY_GROUP), groupArgs.capture());
		assertThat((byte[]) groupArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) groupArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
		assertThat(groupArgs.getValue()[2]).isEqualTo("Backend Interview Study");
		assertThat(groupArgs.getValue()[5]).isEqualTo("[\"JPA\",\"Security\"]");
		assertThat(groupArgs.getValue()[6]).isEqualTo("ONBOARDING");
		assertThat(groupArgs.getValue()[8]).isEqualTo(false);
		assertThat(groupArgs.getValue()[9]).isEqualTo("INVITE-2026");
		assertThat(groupArgs.getValue()[10]).isEqualTo(Date.valueOf(STARTS_AT));
		assertThat(groupArgs.getValue()[11]).isEqualTo(Date.valueOf(ENDS_AT));
		assertThat(groupArgs.getValue()[12]).isEqualTo(Timestamp.from(NOW));

		ArgumentCaptor<Object[]> ownerArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(StudyGroupJdbcSql.INSERT_GROUP_MEMBER), ownerArgs.capture());
		assertThat((byte[]) ownerArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat((byte[]) ownerArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) ownerArgs.getValue()[2]).containsExactly(UuidBinary.toBytes(USER_ID));
		assertThat(ownerArgs.getValue()[3]).isEqualTo("OWNER");
		assertThat(ownerArgs.getValue()[4]).isEqualTo("PENDING_ONBOARDING");
		assertThat(ownerArgs.getValue()[6]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void saveCreatedGroupTranslatesInviteCodeDuplicateIntoRetryableConflict() {
		when(jdbcTemplate.update(eq(StudyGroupJdbcSql.INSERT_STUDY_GROUP), any(Object[].class)))
			.thenThrow(new DuplicateKeyException("study_group_invite_code_live_uidx"));

		assertThatThrownBy(() -> repository.saveCreatedGroup(group(), owner(group())))
			.isInstanceOf(StudyGroupInviteCodeConflictException.class)
			.hasMessage("study group invite code is already reserved.");

		verify(jdbcTemplate, never()).update(eq(StudyGroupJdbcSql.INSERT_GROUP_MEMBER), any(Object[].class));
	}

	@Test
	void findJoinTargetByIdMapsActiveStudyGroupSnapshot() {
		StudyGroupJoinTarget joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 6, "INVITE-2026");
		when(jdbcTemplate.query(eq(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(joinTarget));

		Optional<StudyGroupJoinTarget> result = repository.findJoinTargetById(GROUP_ID);

		assertThat(result).contains(joinTarget);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void findJoinTargetByIdReturnsEmptyWhenNoGroupExists() {
		when(jdbcTemplate.query(eq(StudyGroupJdbcSql.SELECT_STUDY_GROUP_JOIN_TARGET), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		assertThat(repository.findJoinTargetById(GROUP_ID)).isEmpty();
	}

	@Test
	void countActiveOrOnboardingMembersUsesCapacityStatuses() {
		when(jdbcTemplate.queryForObject(eq(StudyGroupJdbcSql.COUNT_ACTIVE_OR_ONBOARDING_MEMBERS), eq(Integer.class), any(Object[].class)))
			.thenReturn(2);

		assertThat(repository.countActiveOrOnboardingMembers(GROUP_ID)).isEqualTo(2);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(StudyGroupJdbcSql.COUNT_ACTIVE_OR_ONBOARDING_MEMBERS), eq(Integer.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void existsActiveOrOnboardingMemberChecksCurrentMembershipOnly() {
		when(jdbcTemplate.queryForObject(eq(StudyGroupJdbcSql.EXISTS_ACTIVE_OR_ONBOARDING_MEMBER), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.existsActiveOrOnboardingMember(GROUP_ID, USER_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(StudyGroupJdbcSql.EXISTS_ACTIVE_OR_ONBOARDING_MEMBER), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void saveJoinedMemberInsertsMemberRow() {
		GroupMember member = GroupMember.member(MEMBER_ID, GROUP_ID, USER_ID, null, NOW);

		repository.saveJoinedMember(member);

		ArgumentCaptor<Object[]> memberArgs = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(StudyGroupJdbcSql.INSERT_GROUP_MEMBER), memberArgs.capture());
		assertThat((byte[]) memberArgs.getValue()[0]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat((byte[]) memberArgs.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) memberArgs.getValue()[2]).containsExactly(UuidBinary.toBytes(USER_ID));
		assertThat(memberArgs.getValue()[3]).isEqualTo("MEMBER");
		assertThat(memberArgs.getValue()[4]).isEqualTo("PENDING_ONBOARDING");
		assertThat(memberArgs.getValue()[6]).isEqualTo(Timestamp.from(NOW));
	}

	@Test
	void saveJoinedMemberTranslatesDuplicateMembershipConflict() {
		when(jdbcTemplate.update(eq(StudyGroupJdbcSql.INSERT_GROUP_MEMBER), any(Object[].class)))
			.thenThrow(new DuplicateKeyException("group_member_group_user_live_uidx"));

		assertThatThrownBy(() -> repository.saveJoinedMember(GroupMember.member(MEMBER_ID, GROUP_ID, USER_ID, null, NOW)))
			.isInstanceOf(GroupMemberDuplicateMembershipException.class)
			.hasMessage("group member already exists.");
	}

	private static StudyGroup group() {
		return StudyGroup.create(
			GROUP_ID,
			USER_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			6,
			STARTS_AT,
			ENDS_AT,
			"Weekly backend interview prep",
			"INVITE-2026",
			NOW
		);
	}

	private static GroupMember owner(StudyGroup group) {
		return GroupMember.owner(MEMBER_ID, group.id(), USER_ID, null, NOW);
	}
}
