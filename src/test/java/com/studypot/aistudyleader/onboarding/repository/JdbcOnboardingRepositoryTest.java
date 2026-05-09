package com.studypot.aistudyleader.onboarding.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcOnboardingRepositoryTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000003081");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003082");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000003083");
	private static final UUID RESPONSE_ID = UUID.fromString("018f0000-0000-7000-8000-000000003084");
	private static final Instant NOW = Instant.parse("2026-05-09T08:40:00Z");
	private static final OnboardingMemberContext CONTEXT = new OnboardingMemberContext(
		GROUP_ID,
		MEMBER_ID,
		List.of("JPA", "Security")
	);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcOnboardingRepository repository = new JdbcOnboardingRepository(jdbcTemplate, new ObjectMapper());

	@Test
	void existsStudyGroupReturnsTrueWhenGroupExists() {
		when(jdbcTemplate.queryForObject(eq(OnboardingJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThat(repository.existsStudyGroup(GROUP_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).queryForObject(eq(OnboardingJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void existsStudyGroupReturnsFalseWhenGroupDoesNotExist() {
		when(jdbcTemplate.queryForObject(eq(OnboardingJdbcSql.EXISTS_STUDY_GROUP), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);

		assertThat(repository.existsStudyGroup(GROUP_ID)).isFalse();
	}

	@Test
	void existsStudyGroupRejectsNullGroupId() {
		assertThatThrownBy(() -> repository.existsStudyGroup(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("groupId must not be null");
	}

	@Test
	void findMemberContextReturnsCurrentMemberContext() {
		when(jdbcTemplate.query(eq(OnboardingJdbcSql.SELECT_MEMBER_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(CONTEXT));

		Optional<OnboardingMemberContext> result = repository.findMemberContext(GROUP_ID, USER_ID);

		assertThat(result).contains(CONTEXT);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(OnboardingJdbcSql.SELECT_MEMBER_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(USER_ID));
	}

	@Test
	void findMemberContextReturnsEmptyWhenCurrentMemberDoesNotExist() {
		when(jdbcTemplate.query(eq(OnboardingJdbcSql.SELECT_MEMBER_CONTEXT), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		assertThat(repository.findMemberContext(GROUP_ID, USER_ID)).isEmpty();
	}

	@Test
	void findMemberContextRejectsNullIds() {
		assertThatThrownBy(() -> repository.findMemberContext(null, USER_ID))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("groupId must not be null");
		assertThatThrownBy(() -> repository.findMemberContext(GROUP_ID, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("userId must not be null");
	}

	@Test
	void findResponseByMemberIdReturnsStoredResponse() {
		GroupOnboardingResponse response = response();
		when(jdbcTemplate.query(eq(OnboardingJdbcSql.SELECT_RESPONSE_BY_MEMBER), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of(response));

		assertThat(repository.findResponseByMemberId(MEMBER_ID)).contains(response);
	}

	@Test
	void findResponseByMemberIdReturnsEmptyWhenResponseDoesNotExist() {
		when(jdbcTemplate.query(eq(OnboardingJdbcSql.SELECT_RESPONSE_BY_MEMBER), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
			.thenReturn(List.of());

		assertThat(repository.findResponseByMemberId(MEMBER_ID)).isEmpty();
	}

	@Test
	void findResponseByMemberIdRejectsNullMemberId() {
		assertThatThrownBy(() -> repository.findResponseByMemberId(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("memberId must not be null");
	}

	@Test
	void saveDraftUpsertsResponseJsonFields() {
		GroupOnboardingResponse response = response();

		GroupOnboardingResponse saved = repository.saveDraft(response);

		assertThat(saved).isSameAs(response);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(OnboardingJdbcSql.UPSERT_ONBOARDING_RESPONSE_DRAFT), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(RESPONSE_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(MEMBER_ID));
		assertThat(args.getValue()[3]).isEqualTo("{\"JPA\":2}");
		assertThat(args.getValue()[4]).isEqualTo("{\"READING\":4}");
		assertThat(args.getValue()[5]).isEqualTo("실습 위주가 좋아요.");
		assertThat(args.getValue()[6]).isEqualTo("DRAFT");
	}

	@Test
	void saveDraftRejectsNullResponse() {
		assertThatThrownBy(() -> repository.saveDraft(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("response must not be null");
	}

	private static GroupOnboardingResponse response() {
		return GroupOnboardingResponse.draft(
			RESPONSE_ID,
			CONTEXT,
			Map.of("JPA", 2),
			Map.of("READING", 4),
			"실습 위주가 좋아요.",
			NOW
		);
	}
}
