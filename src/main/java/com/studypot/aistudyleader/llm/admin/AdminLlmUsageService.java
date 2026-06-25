package com.studypot.aistudyleader.llm.admin;

import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import com.studypot.aistudyleader.llm.service.LlmUsageAccessDeniedException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자(이메일 허용목록)만 접근 가능한, 모든 그룹을 가로지르는 LLM 사용 기록 조회 서비스입니다.
 */
public class AdminLlmUsageService {

	private final LlmUsageRepository repository;
	private final AdminProperties adminProperties;

	public AdminLlmUsageService(LlmUsageRepository repository, AdminProperties adminProperties) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.adminProperties = Objects.requireNonNull(adminProperties, "adminProperties must not be null");
	}

	/**
	 * 요청자가 운영자인지 여부와 이메일을 반환합니다. 운영자가 아니어도 예외를 던지지 않습니다.
	 */
	@Transactional(readOnly = true)
	public AdminIdentity identify(UUID requesterId) {
		Objects.requireNonNull(requesterId, "requesterId must not be null");
		Optional<String> email = repository.findUserEmail(requesterId);
		return new AdminIdentity(email.orElse(null), email.map(adminProperties::isAdmin).orElse(false));
	}

	@Transactional(readOnly = true)
	public List<AdminLlmUsageRow> listUsage(UUID requesterId, AdminLlmUsageFilter filter) {
		requireAdmin(requesterId);
		return repository.findAdminUsage(filter);
	}

	@Transactional(readOnly = true)
	public AdminLlmUsageSummary summarize(UUID requesterId, AdminLlmUsageFilter filter) {
		requireAdmin(requesterId);
		return repository.summarizeAdminUsage(filter);
	}

	private void requireAdmin(UUID requesterId) {
		Objects.requireNonNull(requesterId, "requesterId must not be null");
		if (!identify(requesterId).admin()) {
			throw new LlmUsageAccessDeniedException("only configured admins can read LLM usage logs.");
		}
	}

	public record AdminIdentity(String email, boolean admin) {
	}
}
