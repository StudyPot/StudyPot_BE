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
		List<StudyRecommendations.AiSuggestion> aiSuggestions = generateAiSuggestions(authenticatedUserId, topic, keywords);
		List<StudyRecommendations.PopularTopic> popularTopics = findPopularTopics(completedGroupId, topic);
		return new StudyRecommendations(aiSuggestions, popularTopics);
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
			JsonNode array = node.path("suggestions");
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
