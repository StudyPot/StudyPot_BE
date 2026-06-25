package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class StudyRecommendationServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-00000000a001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-00000000a002");

	@Test
	@SuppressWarnings("unchecked")
	void recommendCombinesAiSuggestionsAndPopularTopics() {
		LlmProviderClient client = request -> response("""
			{"suggestions":[{"title":"Spring Security 심화","reason":"인증/인가를 더 깊게"}]}
			""");
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of(
			new StudyRecommendations.PopularTopic("Spring Boot", 12, 3)
		));

		StudyRecommendationService service = new StudyRecommendationService(
			jdbc,
			providerOf(client),
			providerOf((LlmUsageRecorder) null),
			JsonMapper.builder().findAndAddModules().build(),
			Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC),
			UuidV7::generate
		);

		StudyRecommendations result = service.recommend(USER_ID, GROUP_ID, "Spring Boot", List.of("JPA"));

		assertThat(result.aiSuggestions()).hasSize(1);
		assertThat(result.aiSuggestions().get(0).title()).isEqualTo("Spring Security 심화");
		assertThat(result.popularTopics()).hasSize(1);
		assertThat(result.popularTopics().get(0).topic()).isEqualTo("Spring Boot");
	}

	@Test
	@SuppressWarnings("unchecked")
	void recommendReturnsPopularTopicsOnlyWhenLlmUnavailable() {
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of(
			new StudyRecommendations.PopularTopic("코딩테스트", 3, 2)
		));

		StudyRecommendationService service = new StudyRecommendationService(
			jdbc,
			providerOf((LlmProviderClient) null),
			providerOf((LlmUsageRecorder) null),
			JsonMapper.builder().findAndAddModules().build(),
			Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC),
			UuidV7::generate
		);

		StudyRecommendations result = service.recommend(USER_ID, GROUP_ID, "Spring Boot", List.of());

		assertThat(result.aiSuggestions()).isEmpty();
		assertThat(result.popularTopics()).hasSize(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	void recommendReturnsCachedValueWithoutCallingLlm() {
		// 캐시(3-인자 query)가 채워져 있으면 LLM/인기주제 집계를 다시 돌리지 않고 그대로 반환한다.
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of(
			new StudyRecommendations(
				List.of(new StudyRecommendations.AiSuggestion("저장된 추천", "이전에 생성됨")),
				List.of(new StudyRecommendations.PopularTopic("저장된 주제", 9, 4))
			)
		));
		LlmProviderClient failIfCalled = request -> {
			throw new AssertionError("cache hit 시 LLM 을 호출하면 안 된다");
		};

		StudyRecommendationService service = new StudyRecommendationService(
			jdbc,
			providerOf(failIfCalled),
			providerOf((LlmUsageRecorder) null),
			JsonMapper.builder().findAndAddModules().build(),
			Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC),
			UuidV7::generate
		);

		StudyRecommendations result = service.recommend(USER_ID, GROUP_ID, "Spring Boot", List.of("JPA"));

		assertThat(result.aiSuggestions()).hasSize(1);
		assertThat(result.aiSuggestions().get(0).title()).isEqualTo("저장된 추천");
		assertThat(result.popularTopics()).hasSize(1);
		assertThat(result.popularTopics().get(0).topic()).isEqualTo("저장된 주제");
	}

	@Test
	@SuppressWarnings("unchecked")
	void recommendPersistsGeneratedResultOnCacheMiss() {
		// 캐시(3-인자 query)는 비어 있고(miss), 인기주제(4-인자 query)는 존재하는 상황.
		LlmProviderClient client = request -> response("""
			{"suggestions":[{"title":"다음 주제","reason":"심화"}]}
			""");
		JdbcTemplate jdbc = mock(JdbcTemplate.class);
		when(jdbc.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of(
			new StudyRecommendations.PopularTopic("Spring Boot", 12, 3)
		));

		StudyRecommendationService service = new StudyRecommendationService(
			jdbc,
			providerOf(client),
			providerOf((LlmUsageRecorder) null),
			JsonMapper.builder().findAndAddModules().build(),
			Clock.fixed(Instant.parse("2026-06-24T00:00:00Z"), ZoneOffset.UTC),
			UuidV7::generate
		);

		service.recommend(USER_ID, GROUP_ID, "Spring Boot", List.of("JPA"));

		// 새로 생성한 추천을 캐시에 저장(insert)한다.
		verify(jdbc).update(anyString(), any(), any(), any());
	}

	private static <T> ObjectProvider<T> providerOf(T value) {
		@SuppressWarnings("unchecked")
		ObjectProvider<T> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(value);
		return provider;
	}

	private static LlmStructuredResponse response(String outputText) {
		return new LlmStructuredResponse(
			LlmProvider.OPENAI,
			"gpt-4o-mini",
			outputText,
			80,
			60,
			BigDecimal.ZERO,
			120,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("purpose", "STUDY_RECOMMENDATION"),
			"raw provider response"
		);
	}
}
