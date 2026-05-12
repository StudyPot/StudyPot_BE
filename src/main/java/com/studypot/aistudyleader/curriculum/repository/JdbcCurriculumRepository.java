package com.studypot.aistudyleader.curriculum.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.curriculum.domain.Curriculum;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStartContext;
import com.studypot.aistudyleader.curriculum.domain.CurriculumStatus;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeek;
import com.studypot.aistudyleader.curriculum.domain.CurriculumWeekStatus;
import com.studypot.aistudyleader.curriculum.domain.LlmUsage;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgress;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.curriculum.domain.SubmittedAvailabilitySlot;
import com.studypot.aistudyleader.curriculum.domain.SubmittedOnboardingResponse;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTask;
import com.studypot.aistudyleader.curriculum.domain.WeeklyTaskType;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcCurriculumRepository implements CurriculumRepository {

	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
	};
	private static final TypeReference<Map<String, Integer>> SCORE_MAP = new TypeReference<>() {
	};
	private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
	};
	private static final TypeReference<List<Map<String, String>>> RESOURCE_LIST = new TypeReference<>() {
	};

	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	JdbcCurriculumRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(CurriculumJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<CurriculumStartContext> findStartContext(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(CurriculumJdbcSql.SELECT_START_CONTEXT, this::mapStartContext, uuid(groupId), uuid(userId));
	}

	@Override
	public List<SubmittedOnboardingResponse> findSubmittedOnboardingResponses(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		List<SubmittedOnboardingResponse> responses = jdbcTemplate.query(
			CurriculumJdbcSql.SELECT_SUBMITTED_ONBOARDING_RESPONSES,
			this::mapSubmittedResponse,
			uuid(groupId)
		);
		if (responses.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<SubmittedAvailabilitySlot>> slotsByResponse = submittedAvailabilitySlotsByResponse(groupId);
		return responses.stream()
			.map(response -> new SubmittedOnboardingResponse(
				response.id(),
				response.memberId(),
				response.keywordSkillLevels(),
				response.taskPreferences(),
				response.additionalNote(),
				slotsByResponse.getOrDefault(response.id(), List.of()),
				response.submittedAt()
			))
			.toList();
	}

	@Override
	public void saveStartedCurriculum(UUID groupId, Instant startedAt, LlmUsage llmUsage, Curriculum curriculum) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(startedAt, "startedAt must not be null");
		Objects.requireNonNull(llmUsage, "llmUsage must not be null");
		Objects.requireNonNull(curriculum, "curriculum must not be null");
		int updated = jdbcTemplate.update(
			CurriculumJdbcSql.UPDATE_STUDY_GROUP_STARTED,
			timestamp(startedAt),
			timestamp(startedAt),
			uuid(groupId)
		);
		if (updated != 1) {
			throw new CurriculumPersistenceException("study group could not be transitioned to ACTIVE.");
		}
		insertLlmUsage(llmUsage);
		insertCurriculum(curriculum);
		for (CurriculumWeek week : curriculum.weeks()) {
			insertWeek(week);
			for (WeeklyTask task : week.tasks()) {
				insertTask(task);
			}
		}
	}

	@Override
	public Optional<CurriculumStartContext> findReadContext(UUID groupId, UUID userId) {
		return findStartContext(groupId, userId);
	}

	@Override
	public Optional<Curriculum> findActiveCurriculumByGroupId(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return queryOne(CurriculumJdbcSql.SELECT_ACTIVE_CURRICULUM, this::mapCurriculumRow, uuid(groupId))
			.map(row -> row.toCurriculum(findWeeks(row.id())));
	}

	@Override
	public Optional<CurriculumWeek> findCurrentWeekByGroupId(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return queryOne(
			CurriculumJdbcSql.SELECT_CURRENT_WEEK_BY_GROUP,
			(resultSet, rowNumber) -> {
				CurriculumWeekRow row = mapWeekRow(resultSet, rowNumber);
				return row.toWeek(findWeeklyTasksByWeekId(row.id()));
			},
			uuid(groupId)
		);
	}

	@Override
	public boolean existsCurriculumWeek(UUID weekId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(CurriculumJdbcSql.EXISTS_CURRICULUM_WEEK, Boolean.class, uuid(weekId)));
	}

	@Override
	public Optional<CurriculumStartContext> findReadContextByWeekId(UUID weekId, UUID userId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(CurriculumJdbcSql.SELECT_WEEK_READ_CONTEXT, this::mapStartContext, uuid(weekId), uuid(userId));
	}

	@Override
	public List<WeeklyTask> findWeeklyTasksByWeekId(UUID weekId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return jdbcTemplate.query(CurriculumJdbcSql.SELECT_WEEKLY_TASKS_BY_WEEK, this::mapTask, uuid(weekId));
	}

	@Override
	public Optional<MemberWeekProgress> findMemberWeekProgress(UUID weekId, UUID memberId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		return queryOne(CurriculumJdbcSql.SELECT_MEMBER_WEEK_PROGRESS_BY_WEEK_AND_MEMBER, this::mapProgress, uuid(weekId), uuid(memberId));
	}

	@Override
	public Optional<Instant> findWeekDueAt(UUID weekId) {
		Objects.requireNonNull(weekId, "weekId must not be null");
		return queryOne(CurriculumJdbcSql.SELECT_WEEK_DUE_AT, (resultSet, rowNumber) -> instant(resultSet.getTimestamp("ends_at")), uuid(weekId));
	}

	@Override
	public boolean insertMemberWeekProgress(MemberWeekProgress progress) {
		Objects.requireNonNull(progress, "progress must not be null");
		return jdbcTemplate.update(
			CurriculumJdbcSql.INSERT_MEMBER_WEEK_PROGRESS,
			uuid(progress.id()),
			uuid(progress.curriculumWeekId()),
			uuid(progress.memberId()),
			progress.status().name(),
			timestamp(progress.startedAt()),
			timestamp(progress.dueAt()),
			timestamp(progress.completedAt()),
			progress.completionNote(),
			progress.incompleteReason(),
			timestamp(progress.reasonSubmittedAt()),
			timestamp(progress.createdAt()),
			timestamp(progress.updatedAt())
		) == 1;
	}

	@Override
	public boolean updateMemberWeekProgress(MemberWeekProgress progress) {
		Objects.requireNonNull(progress, "progress must not be null");
		return jdbcTemplate.update(
			CurriculumJdbcSql.UPDATE_MEMBER_WEEK_PROGRESS,
			progress.status().name(),
			timestamp(progress.startedAt()),
			timestamp(progress.dueAt()),
			timestamp(progress.completedAt()),
			progress.completionNote(),
			progress.incompleteReason(),
			timestamp(progress.reasonSubmittedAt()),
			timestamp(progress.updatedAt()),
			uuid(progress.id())
		) == 1;
	}

	private Map<UUID, List<SubmittedAvailabilitySlot>> submittedAvailabilitySlotsByResponse(UUID groupId) {
		List<SubmittedAvailabilityRow> rows = jdbcTemplate.query(
			CurriculumJdbcSql.SELECT_SUBMITTED_AVAILABILITY_SLOTS,
			this::mapSubmittedAvailabilityRow,
			uuid(groupId)
		);
		Map<UUID, List<SubmittedAvailabilitySlot>> result = new LinkedHashMap<>();
		for (SubmittedAvailabilityRow row : rows) {
			result.computeIfAbsent(row.responseId(), ignored -> new ArrayList<>()).add(row.slot());
		}
		return result;
	}

	private void insertLlmUsage(LlmUsage usage) {
		jdbcTemplate.update(
			CurriculumJdbcSql.INSERT_LLM_USAGE,
			uuid(usage.id()),
			uuidOrNull(usage.userId()),
			uuidOrNull(usage.groupId()),
			usage.purpose(),
			usage.provider().name(),
			usage.model(),
			usage.inputTokens(),
			usage.outputTokens(),
			usage.totalCostUsd(),
			usage.latencyMs(),
			usage.status().name(),
			usage.errorCode(),
			json(usage.requestPayload(), "llm usage request payload"),
			usage.responseSummary(),
			Date.valueOf(usage.createdDateUtc()),
			timestamp(usage.createdAt())
		);
	}

	private void insertCurriculum(Curriculum curriculum) {
		jdbcTemplate.update(
			CurriculumJdbcSql.INSERT_CURRICULUM,
			uuid(curriculum.id()),
			uuid(curriculum.groupId()),
			uuidOrNull(curriculum.llmUsageId().orElse(null)),
			curriculum.title(),
			curriculum.totalWeeks(),
			json(curriculum.onboardingSummary(), "curriculum onboarding summary"),
			curriculum.generatedByAi(),
			curriculum.generationPrompt().orElse(null),
			curriculum.status().name(),
			timestamp(curriculum.createdAt()),
			timestamp(curriculum.updatedAt())
		);
	}

	private void insertWeek(CurriculumWeek week) {
		jdbcTemplate.update(
			CurriculumJdbcSql.INSERT_CURRICULUM_WEEK,
			uuid(week.id()),
			uuid(week.curriculumId()),
			week.weekNumber(),
			week.title(),
			week.description(),
			week.sprintGoal(),
			json(week.learningGoals(), "curriculum week learning goals"),
			json(week.resources(), "curriculum week resources"),
			week.status().name(),
			timestamp(week.startsAt()),
			timestamp(week.endsAt()),
			timestamp(week.createdAt()),
			timestamp(week.updatedAt())
		);
	}

	private void insertTask(WeeklyTask task) {
		jdbcTemplate.update(
			CurriculumJdbcSql.INSERT_WEEKLY_TASK,
			uuid(task.id()),
			uuid(task.curriculumWeekId()),
			task.displayOrder(),
			task.taskType().name(),
			task.title(),
			task.description(),
			task.required(),
			timestamp(task.dueAt()),
			task.generatedByAi(),
			json(task.sourcePayload(), "weekly task source payload"),
			timestamp(task.createdAt()),
			timestamp(task.updatedAt())
		);
	}

	private List<CurriculumWeek> findWeeks(UUID curriculumId) {
		List<CurriculumWeekRow> weekRows = jdbcTemplate.query(CurriculumJdbcSql.SELECT_CURRICULUM_WEEKS, this::mapWeekRow, uuid(curriculumId));
		Map<UUID, List<WeeklyTask>> tasksByWeek = tasksByWeek(curriculumId);
		return weekRows.stream()
			.map(row -> row.toWeek(tasksByWeek.getOrDefault(row.id(), List.of())))
			.toList();
	}

	private Map<UUID, List<WeeklyTask>> tasksByWeek(UUID curriculumId) {
		List<WeeklyTask> tasks = jdbcTemplate.query(CurriculumJdbcSql.SELECT_WEEKLY_TASKS_BY_CURRICULUM, this::mapTask, uuid(curriculumId));
		Map<UUID, List<WeeklyTask>> result = new LinkedHashMap<>();
		for (WeeklyTask task : tasks) {
			result.computeIfAbsent(task.curriculumWeekId(), ignored -> new ArrayList<>()).add(task);
		}
		return result;
	}

	private CurriculumStartContext mapStartContext(ResultSet resultSet, int rowNumber) throws SQLException {
		return new CurriculumStartContext(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			resultSet.getString("group_name"),
			resultSet.getString("topic"),
			read(resultSet.getString("detail_keywords"), STRING_LIST, "study group detail keywords"),
			StudyGroupStatus.valueOf(resultSet.getString("group_status")),
			resultSet.getDate("starts_at").toLocalDate(),
			resultSet.getDate("ends_at").toLocalDate(),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			GroupMemberPermission.valueOf(resultSet.getString("permission")),
			GroupMemberStatus.valueOf(resultSet.getString("member_status"))
		);
	}

	private SubmittedOnboardingResponse mapSubmittedResponse(ResultSet resultSet, int rowNumber) throws SQLException {
		return new SubmittedOnboardingResponse(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			read(resultSet.getString("keyword_skill_levels"), SCORE_MAP, "keyword skill levels"),
			read(resultSet.getString("task_preferences"), SCORE_MAP, "task preferences"),
			resultSet.getString("additional_note"),
			List.of(),
			instant(resultSet.getTimestamp("submitted_at"))
		);
	}

	private SubmittedAvailabilityRow mapSubmittedAvailabilityRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new SubmittedAvailabilityRow(
			UuidBinary.fromBytes(resultSet.getBytes("onboarding_response_id")),
			new SubmittedAvailabilitySlot(
				resultSet.getInt("day_of_week"),
				timeString(resultSet.getTime("start_time")),
				timeString(resultSet.getTime("end_time")),
				resultSet.getString("timezone")
			)
		);
	}

	private CurriculumRow mapCurriculumRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new CurriculumRow(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			uuid(resultSet.getBytes("llm_usage_id")),
			resultSet.getString("title"),
			resultSet.getInt("total_weeks"),
			readNullableMap(resultSet.getString("onboarding_summary"), "curriculum onboarding summary"),
			resultSet.getBoolean("generated_by_ai"),
			resultSet.getString("generation_prompt"),
			CurriculumStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private CurriculumWeekRow mapWeekRow(ResultSet resultSet, int rowNumber) throws SQLException {
		return new CurriculumWeekRow(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_id")),
			resultSet.getInt("week_number"),
			resultSet.getString("title"),
			resultSet.getString("description"),
			resultSet.getString("sprint_goal"),
			readNullableList(resultSet.getString("learning_goals"), "curriculum week learning goals"),
			readNullableResources(resultSet.getString("resources"), "curriculum week resources"),
			CurriculumWeekStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("starts_at")),
			instant(resultSet.getTimestamp("ends_at")),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private WeeklyTask mapTask(ResultSet resultSet, int rowNumber) throws SQLException {
		return new WeeklyTask(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id")),
			resultSet.getInt("display_order"),
			WeeklyTaskType.valueOf(resultSet.getString("task_type")),
			resultSet.getString("title"),
			resultSet.getString("description"),
			resultSet.getBoolean("required"),
			instant(resultSet.getTimestamp("due_at")),
			resultSet.getBoolean("generated_by_ai"),
			readNullableMap(resultSet.getString("source_payload"), "weekly task source payload"),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private MemberWeekProgress mapProgress(ResultSet resultSet, int rowNumber) throws SQLException {
		return new MemberWeekProgress(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("curriculum_week_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			MemberWeekProgressStatus.valueOf(resultSet.getString("status")),
			instant(resultSet.getTimestamp("started_at")),
			instant(resultSet.getTimestamp("due_at")),
			instant(resultSet.getTimestamp("completed_at")),
			resultSet.getString("completion_note"),
			resultSet.getString("incomplete_reason"),
			instant(resultSet.getTimestamp("reason_submitted_at")),
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
			throw new CurriculumPersistenceException(fieldName + " could not be serialized.", exception);
		}
	}

	private <T> T read(String value, TypeReference<T> type, String fieldName) {
		try {
			return objectMapper.readValue(value, type);
		} catch (JsonProcessingException exception) {
			throw new CurriculumPersistenceException(fieldName + " could not be deserialized.", exception);
		}
	}

	private Map<String, Object> readNullableMap(String value, String fieldName) {
		return value == null ? Map.of() : read(value, OBJECT_MAP, fieldName);
	}

	private List<String> readNullableList(String value, String fieldName) {
		return value == null ? List.of() : read(value, STRING_LIST, fieldName);
	}

	private List<Map<String, String>> readNullableResources(String value, String fieldName) {
		return value == null ? List.of() : read(value, RESOURCE_LIST, fieldName);
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static byte[] uuidOrNull(UUID uuid) {
		return uuid == null ? null : UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(byte[] bytes) {
		return bytes == null ? null : UuidBinary.fromBytes(bytes);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static String timeString(Time time) {
		return time == null ? null : time.toLocalTime().toString();
	}

	private record SubmittedAvailabilityRow(UUID responseId, SubmittedAvailabilitySlot slot) {
	}

	private record CurriculumRow(
		UUID id,
		UUID groupId,
		UUID llmUsageId,
		String title,
		int totalWeeks,
		Map<String, Object> onboardingSummary,
		boolean generatedByAi,
		String generationPrompt,
		CurriculumStatus status,
		Instant createdAt,
		Instant updatedAt
	) {

		Curriculum toCurriculum(List<CurriculumWeek> weeks) {
			return new Curriculum(
				id,
				groupId,
				llmUsageId,
				title,
				totalWeeks,
				onboardingSummary,
				generatedByAi,
				generationPrompt,
				status,
				weeks,
				createdAt,
				updatedAt
			);
		}
	}

	private record CurriculumWeekRow(
		UUID id,
		UUID curriculumId,
		int weekNumber,
		String title,
		String description,
		String sprintGoal,
		List<String> learningGoals,
		List<Map<String, String>> resources,
		CurriculumWeekStatus status,
		Instant startsAt,
		Instant endsAt,
		Instant createdAt,
		Instant updatedAt
	) {

		CurriculumWeek toWeek(List<WeeklyTask> tasks) {
			return new CurriculumWeek(
				id,
				curriculumId,
				weekNumber,
				title,
				description,
				sprintGoal,
				learningGoals,
				resources,
				status,
				startsAt,
				endsAt,
				tasks,
				createdAt,
				updatedAt
			);
		}
	}

	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
