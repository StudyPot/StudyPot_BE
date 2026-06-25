package com.studypot.aistudyleader.studygroup.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredRequest;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 완료된 스터디 다음에 시작할 만한 스터디를 추천한다.
 * AI 맞춤 제안(LLM)과 다른 공개 그룹의 인기 주제(집계)를 함께 제공하며,
 * 어느 한쪽이 비어도(LLM 미구성/조회 실패) 나머지는 그대로 반환한다.
 */
public class StudyRecommendationService {

	private static final Logger log = LoggerFactory.getLogger(StudyRecommendationService.class);

	private static final int POPULAR_LIMIT = 6;
	private static final int AI_SUGGESTION_COUNT = 3;

	private static final String INSTRUCTIONS = """
		당신은 스터디 그룹의 AI 팀장입니다. 방금 한 스터디를 완료한 팀에게
		'다음에 이어서 하면 좋은 스터디 주제'를 한국어로 추천하세요.
		- 완료한 스터디 주제(topic)와 키워드(keywords)를 바탕으로, 자연스러운 다음 단계/심화/연계 주제를 제안합니다.
		- 각 제안은 title(스터디 주제, 짧고 구체적으로)과 reason(왜 추천하는지 한두 문장)으로 구성합니다.
		- 비밀키, 자격증명류는 절대 포함하지 마세요.
		- 반드시 제공된 JSON 스키마(suggestions 배열)에 맞는 JSON 만 반환하세요.
		""";

	// 개별(비공개) 그룹을 식별하지 않도록 주제 단위로 익명 집계한다. 방금 완료한 그룹/주제는 제외.
	private static final String SELECT_POPULAR_TOPICS = """
		select sg.topic as topic,
		       count(distinct gm.id) as member_count,
		       count(distinct sg.id) as group_count
		from study_group sg
		left join group_member gm on gm.group_id = sg.id
		  and gm.status in ('PENDING_ONBOARDING', 'ACTIVE')
		  and gm.deleted_at is null
		where sg.id <> ?
		  and sg.deleted_at is null
		  and sg.topic is not null
		  and sg.topic <> ''
		  and sg.topic <> ?
		  and sg.status in ('ONBOARDING', 'READY_TO_START', 'ACTIVE', 'COMPLETED')
		group by sg.topic
		order by member_count desc, max(sg.created_at) desc
		limit %d
		""".formatted(POPULAR_LIMIT);

	// 그룹별로 추천을 1회만 생성해 캐시한다(first-write-wins). 이후 요청은 LLM/집계 없이 저장값을 반환한다.
	private static final String SELECT_CACHE = """
		select ai_suggestions, popular_topics
		from study_recommendation
		where group_id = ?
		""";

	// 이미 캐시가 있으면(동시 요청 레이스 포함) 무시한다. 먼저 저장된 값이 정답이다.
	private static final String INSERT_CACHE = """
		insert ignore into study_recommendation (group_id, ai_suggestions, popular_topics)
		values (?, ?, ?)
		""";

	private final JdbcTemplate jdbcTemplate;
	private final ObjectProvider<LlmProviderClient> provider;
	private final ObjectProvider<LlmUsageRecorder> usageRecorder;
	private final ObjectMapper objectMapper;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public StudyRecommendationService(
		JdbcTemplate jdbcTemplate,
		ObjectProvider<LlmProviderClient> provider,
		ObjectProvider<LlmUsageRecorder> usageRecorder,
		ObjectMapper objectMapper,
		Clock clock,
		Supplier<UUID> idGenerator
	) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.usageRecorder = Objects.requireNonNull(usageRecorder, "usageRecorder must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	public StudyRecommendations recommend(UUID authenticatedUserId, UUID completedGroupId, String topic, List<String> keywords) {
		Objects.requireNonNull(completedGroupId, "completedGroupId must not be null");

		// 1) 캐시가 있으면 LLM/집계를 다시 돌리지 않고 그대로 반환한다.
		StudyRecommendations cached = readCache(completedGroupId);
		if (cached != null) {
			return cached;
		}

		// 2) 캐시 미스: 새로 생성한다.
		List<StudyRecommendations.AiSuggestion> aiSuggestions = generateAiSuggestions(authenticatedUserId, topic, keywords);
		List<StudyRecommendations.PopularTopic> popularTopics = findPopularTopics(completedGroupId, topic);
		StudyRecommendations generated = new StudyRecommendations(aiSuggestions, popularTopics);

		// 3) 의미 있는 결과만 캐시한다. 둘 다 비면(LLM 일시 미구성/실패) 저장하지 않아 다음 요청에서 재시도한다.
		if (aiSuggestions.isEmpty() && popularTopics.isEmpty()) {
			return generated;
		}

		// 4) first-write-wins 저장. 동시 요청이 먼저 썼다면 그 값을 정답으로 다시 읽어 반환한다.
		StudyRecommendations persisted = persist(completedGroupId, generated);
		return persisted != null ? persisted : generated;
	}

	private StudyRecommendations readCache(UUID groupId) {
		try {
			List<StudyRecommendations> rows = jdbcTemplate.query(
				SELECT_CACHE,
				(rs, rowNum) -> new StudyRecommendations(
					readSuggestions(rs.getString("ai_suggestions")),
					readPopularTopics(rs.getString("popular_topics"))
				),
				UuidBinary.toBytes(groupId)
			);
			return rows.isEmpty() ? null : rows.get(0);
		} catch (RuntimeException exception) {
			// 캐시 조회 실패는 치명적이지 않다. 생성 경로로 폴백한다.
			log.warn("study recommendation cache read failed groupId={}", groupId, exception);
			return null;
		}
	}

	private StudyRecommendations persist(UUID groupId, StudyRecommendations recommendations) {
		try {
			jdbcTemplate.update(
				INSERT_CACHE,
				UuidBinary.toBytes(groupId),
				objectMapper.writeValueAsString(recommendations.aiSuggestions()),
				objectMapper.writeValueAsString(recommendations.popularTopics())
			);
			// 레이스로 다른 요청이 먼저 저장했을 수 있으니 저장된 정답을 다시 읽어 반환한다.
			return readCache(groupId);
		} catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
			// 저장 실패해도 방금 만든 결과는 그대로 응답한다(부가 기능).
			log.warn("study recommendation cache write failed groupId={}", groupId, exception);
			return null;
		}
	}

	private List<StudyRecommendations.PopularTopic> readPopularTopics(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			JsonNode array = objectMapper.readTree(json);
			if (!array.isArray()) {
				return List.of();
			}
			List<StudyRecommendations.PopularTopic> result = new ArrayList<>();
			for (JsonNode item : array) {
				String topic = item.path("topic").asText("");
				if (topic.isBlank()) {
					continue;
				}
				result.add(new StudyRecommendations.PopularTopic(
					topic,
					item.path("memberCount").asInt(0),
					item.path("groupCount").asInt(0)
				));
			}
			return List.copyOf(result);
		} catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
			return List.of();
		}
	}

	private List<StudyRecommendations.AiSuggestion> generateAiSuggestions(UUID userId, String topic, List<String> keywords) {
		LlmProviderClient client = provider.getIfAvailable();
		if (client == null) {
			return List.of();
		}
		Instant now = clock.instant();
		try {
			LlmStructuredResponse response = client.requestStructured(new LlmStructuredRequest(
				LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST,
				INSTRUCTIONS,
				Map.of(
					"topic", topic == null ? "" : topic,
					"keywords", keywords == null ? List.of() : keywords,
					"count", AI_SUGGESTION_COUNT
				),
				schemaFormat(),
				Map.of("purpose", "STUDY_RECOMMENDATION", "topic", topic == null ? "" : topic)
			));
			List<StudyRecommendations.AiSuggestion> suggestions = readSuggestions(response.outputText());
			recordUsage(response.withResponseSummary("Generated study recommendations: " + suggestions.size()), userId, now);
			return suggestions;
		} catch (RuntimeException exception) {
			// 추천은 부가 기능이므로 AI 실패 시 조용히 빈 목록으로 폴백한다(인기 주제는 그대로 반환).
			log.warn("study recommendation AI generation failed userId={}", userId, exception);
			return List.of();
		}
	}

	private void recordUsage(LlmStructuredResponse response, UUID userId, Instant now) {
		LlmUsageRecorder recorder = usageRecorder.getIfAvailable();
		if (recorder == null) {
			return;
		}
		try {
			recorder.record(response.toUsage(idGenerator.get(), userId, null, LlmUsagePurpose.DETAIL_KEYWORD_SUGGEST, now));
		} catch (RuntimeException exception) {
			log.warn("study recommendation usage record failed", exception);
		}
	}

	private List<StudyRecommendations.AiSuggestion> readSuggestions(String outputText) {
		try {
			JsonNode node = objectMapper.readTree(outputText);
			// LLM 출력은 {"suggestions":[...]}, 캐시는 [...] 형태로 저장된다. 둘 다 허용한다.
			JsonNode array = node.isArray() ? node : node.path("suggestions");
			if (!array.isArray()) {
				return List.of();
			}
			List<StudyRecommendations.AiSuggestion> result = new ArrayList<>();
			for (JsonNode item : array) {
				String title = item.path("title").asText("");
				if (title.isBlank()) {
					continue;
				}
				result.add(new StudyRecommendations.AiSuggestion(title, item.path("reason").asText("")));
			}
			return List.copyOf(result);
		} catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
			return List.of();
		}
	}

	private List<StudyRecommendations.PopularTopic> findPopularTopics(UUID excludeGroupId, String excludeTopic) {
		try {
			return jdbcTemplate.query(
				SELECT_POPULAR_TOPICS,
				(rs, rowNum) -> new StudyRecommendations.PopularTopic(
					rs.getString("topic"),
					rs.getInt("member_count"),
					rs.getInt("group_count")
				),
				UuidBinary.toBytes(excludeGroupId),
				excludeTopic == null ? "" : excludeTopic
			);
		} catch (RuntimeException exception) {
			log.warn("popular topics query failed", exception);
			return List.of();
		}
	}

	private Map<String, Object> schemaFormat() {
		return Map.of(
			"type", "json_schema",
			"name", "study_recommendations",
			"schema", Map.of(
				"type", "object",
				"required", List.of("suggestions"),
				"properties", Map.of(
					"suggestions", Map.of(
						"type", "array",
						"items", Map.of(
							"type", "object",
							"required", List.of("title", "reason"),
							"properties", Map.of(
								"title", Map.of("type", "string"),
								"reason", Map.of("type", "string")
							)
						)
					)
				)
			)
		);
	}
}
