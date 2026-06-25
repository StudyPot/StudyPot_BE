package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 운영자 화면용 LLM 사용 기록 한 줄입니다. 사용자 닉네임/이메일, 그룹명을 함께 담습니다.
 *
 * <p>{@code requestPayload}는 기록 시점에 이미 민감정보가 마스킹된 JSON 문자열 원본입니다.</p>
 */
public record AdminLlmUsageRow(
	UUID id,
	UUID userId,
	String userNickname,
	String userEmail,
	UUID groupId,
	String groupName,
	LlmUsagePurpose purpose,
	LlmProvider provider,
	String model,
	int inputTokens,
	int outputTokens,
	BigDecimal totalCostUsd,
	Integer latencyMs,
	LlmUsageStatus status,
	String errorCode,
	String requestPayload,
	String responseSummary,
	Instant createdAt
) {
}
