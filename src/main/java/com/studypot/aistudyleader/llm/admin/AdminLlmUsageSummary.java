package com.studypot.aistudyleader.llm.admin;

import java.math.BigDecimal;

/**
 * 필터 조건에 해당하는 전체 기록(목록 limit과 무관)에 대한 집계 요약입니다.
 *
 * @param totalCalls   전체 호출 수
 * @param successCalls 성공(SUCCESS) 호출 수
 * @param failedCalls  실패(SUCCESS 외) 호출 수
 * @param inputTokens  입력 토큰 합계
 * @param outputTokens 출력 토큰 합계
 * @param totalCostUsd 비용 합계(USD)
 */
public record AdminLlmUsageSummary(
	long totalCalls,
	long successCalls,
	long failedCalls,
	long inputTokens,
	long outputTokens,
	BigDecimal totalCostUsd
) {

	public static final AdminLlmUsageSummary EMPTY =
		new AdminLlmUsageSummary(0L, 0L, 0L, 0L, 0L, BigDecimal.ZERO);
}
