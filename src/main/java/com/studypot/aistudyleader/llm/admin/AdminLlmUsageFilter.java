package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 운영자 LLM 사용 기록 조회 필터입니다. 모든 조건은 선택 사항이며 null이면 무시합니다.
 *
 * @param groupId 특정 스터디 그룹으로 제한
 * @param userId  특정 사용자로 제한
 * @param purpose 호출 목적으로 제한
 * @param status  호출 상태(SUCCESS/FAILED 등)로 제한
 * @param from    이 시각(포함) 이후 생성된 기록만
 * @param to      이 시각(미포함) 이전 생성된 기록만
 * @param limit   목록 조회 시 최대 반환 건수(집계에는 미적용)
 */
public record AdminLlmUsageFilter(
	UUID groupId,
	UUID userId,
	LlmUsagePurpose purpose,
	LlmUsageStatus status,
	Instant from,
	Instant to,
	int limit
) {

	public static final int DEFAULT_LIMIT = 200;
	public static final int MAX_LIMIT = 1000;

	public AdminLlmUsageFilter {
		if (limit <= 0) {
			limit = DEFAULT_LIMIT;
		}
		if (limit > MAX_LIMIT) {
			limit = MAX_LIMIT;
		}
	}
}
