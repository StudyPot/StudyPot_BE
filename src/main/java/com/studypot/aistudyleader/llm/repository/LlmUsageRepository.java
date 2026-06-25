package com.studypot.aistudyleader.llm.repository;

import com.studypot.aistudyleader.llm.admin.AdminLlmUsageFilter;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageRow;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageSummary;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LlmUsageRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<LlmUsageAccessContext> findAccessContext(UUID groupId, UUID userId);

	/**
	 * 운영자 권한 판별을 위해 사용자의 이메일을 조회합니다. 탈퇴한 사용자는 제외합니다.
	 */
	Optional<String> findUserEmail(UUID userId);

	/**
	 * 모든 그룹을 가로질러 필터 조건에 맞는 사용 기록을 최신순으로 조회합니다(운영자 전용).
	 */
	List<AdminLlmUsageRow> findAdminUsage(AdminLlmUsageFilter filter);

	/**
	 * 필터 조건에 맞는 전체 기록(목록 limit과 무관)의 집계 요약을 반환합니다(운영자 전용).
	 */
	AdminLlmUsageSummary summarizeAdminUsage(AdminLlmUsageFilter filter);

	boolean insertLlmUsage(LlmUsage usage);

	/**
	 * Returns the newest usage records first, ordered by created timestamp descending and UUID descending.
	 */
	List<LlmUsage> findGroupUsage(UUID groupId, int limit);

	/**
	 * Returns the newest usage records first, ordered by created timestamp descending and UUID descending.
	 */
	List<LlmUsage> findUserUsage(UUID userId, int limit);
}
