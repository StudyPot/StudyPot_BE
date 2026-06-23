package com.studypot.aistudyleader.report.service;

import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.Objects;

/**
 * 주차 리포트 생성 결과: 리포트 본문 + LLM 응답(사용량 기록용).
 */
public record WeeklyReportGeneration(WeeklyReportContent content, LlmStructuredResponse response) {

	public WeeklyReportGeneration {
		Objects.requireNonNull(content, "content must not be null");
		Objects.requireNonNull(response, "response must not be null");
	}
}
