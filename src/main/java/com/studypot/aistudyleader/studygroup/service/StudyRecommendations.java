package com.studypot.aistudyleader.studygroup.service;

import java.util.List;
import java.util.Objects;

/**
 * 완료된 스터디 다음에 추천할 스터디 묶음:
 * - aiSuggestions: 완료한 스터디 주제 기반 AI 맞춤 제안(LLM 미구성 시 빈 목록)
 * - popularTopics: 다른 공개 그룹의 인기/최근 주제
 */
public record StudyRecommendations(List<AiSuggestion> aiSuggestions, List<PopularTopic> popularTopics) {

	public StudyRecommendations {
		aiSuggestions = List.copyOf(Objects.requireNonNull(aiSuggestions, "aiSuggestions must not be null"));
		popularTopics = List.copyOf(Objects.requireNonNull(popularTopics, "popularTopics must not be null"));
	}

	public static StudyRecommendations empty() {
		return new StudyRecommendations(List.of(), List.of());
	}

	/** AI 맞춤 제안 항목: 추천 주제 + 추천 이유. */
	public record AiSuggestion(String title, String reason) {

		public AiSuggestion {
			title = (title == null) ? "" : title.strip();
			reason = (reason == null) ? "" : reason.strip();
		}
	}

	/** 다른 그룹들의 인기 주제(익명 집계): 주제 + 학습 멤버 수 + 그룹 수. 개별 그룹은 식별하지 않는다. */
	public record PopularTopic(String topic, int memberCount, int groupCount) {

		public PopularTopic {
			topic = (topic == null) ? "" : topic.strip();
			memberCount = Math.max(memberCount, 0);
			groupCount = Math.max(groupCount, 0);
		}
	}
}
