package com.studypot.aistudyleader.onboarding.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingResponse;
import com.studypot.aistudyleader.onboarding.domain.GroupOnboardingStatus;
import com.studypot.aistudyleader.onboarding.domain.OnboardingMemberContext;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcOnboardingRepository implements OnboardingRepository {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};
	private static final TypeReference<Map<String, Integer>> SCORE_MAP = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcOnboardingRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(OnboardingJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<OnboardingMemberContext> findMemberContext(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(OnboardingJdbcSql.SELECT_MEMBER_CONTEXT, this::mapMemberContext, uuid(groupId), uuid(userId));
	}

	@Override
	public Optional<GroupOnboardingResponse> findResponseByMemberId(UUID memberId) {
		Objects.requireNonNull(memberId, "memberId must not be null");
		return queryOne(OnboardingJdbcSql.SELECT_RESPONSE_BY_MEMBER, this::mapResponse, uuid(memberId));
	}

	@Override
	public GroupOnboardingResponse saveDraft(GroupOnboardingResponse response) {
		Objects.requireNonNull(response, "response must not be null");
		jdbcTemplate.update(
			OnboardingJdbcSql.UPSERT_ONBOARDING_RESPONSE_DRAFT,
			uuid(response.id()),
			uuid(response.groupId()),
			uuid(response.memberId()),
			json(response.keywordSkillLevels(), "keyword skill levels"),
			json(response.taskPreferences(), "task preferences"),
			response.additionalNote().orElse(null),
			response.status().name(),
			timestamp(response.submittedAt().orElse(null)),
			timestamp(response.auditMetadata().createdAt()),
			timestamp(response.auditMetadata().updatedAt())
		);
		return response;
	}

	private OnboardingMemberContext mapMemberContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new OnboardingMemberContext(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			read(resultSet.getString("detail_keywords"), STRING_LIST, "study group detail keywords")
		);
	}

	private GroupOnboardingResponse mapResponse(ResultSet resultSet, int rowNumber) throws SQLException {
		return GroupOnboardingResponse.rehydrate(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			read(resultSet.getString("keyword_skill_levels"), SCORE_MAP, "keyword skill levels"),
			read(resultSet.getString("task_preferences"), SCORE_MAP, "task preferences"),
			resultSet.getString("additional_note"),
			GroupOnboardingStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("submitted_at")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private String json(Object value, String fieldName) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (JsonProcessingException exception) {
			throw new OnboardingPersistenceException("onboarding " + fieldName + " could not be serialized.", exception);
		}
	}

	private <T> T read(String value, TypeReference<T> type, String fieldName) {
		try {
			return objectMapper.readValue(value, type);
		} catch (JsonProcessingException exception) {
			throw new OnboardingPersistenceException("onboarding " + fieldName + " could not be deserialized.", exception);
		}
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
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
