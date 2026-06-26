package com.studypot.aistudyleader.auth.admin;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.llm.admin.AdminProperties;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영자(이메일 허용목록)만 접근 가능한 사용자 요금제(FREE/PREMIUM) 관리 서비스입니다.
 *
 * <p>사용자 권한(role) 모델 대신 환경변수 기반 이메일 허용목록({@link AdminProperties})으로
 * 운영자 여부를 판별합니다. v1 유료 전환을 관리자가 수동(SQL)으로 하던 것을 API로 대체합니다.</p>
 */
public class AdminUserService {

	private final AuthAccountRepository authRepository;
	private final AdminProperties adminProperties;
	private final Clock clock;

	public AdminUserService(AuthAccountRepository authRepository, AdminProperties adminProperties, Clock clock) {
		this.authRepository = Objects.requireNonNull(authRepository, "authRepository must not be null");
		this.adminProperties = Objects.requireNonNull(adminProperties, "adminProperties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	/** 이메일로 활성 사용자를 찾아 요금제 현황을 반환합니다. 운영자가 아니면 예외, 사용자가 없으면 예외. */
	@Transactional(readOnly = true)
	public AdminUserView findByEmail(UUID requesterId, String email) {
		requireAdmin(requesterId);
		String normalized = email == null ? "" : email.strip();
		if (normalized.isBlank()) {
			throw new AdminUserNotFoundException("email is required.");
		}
		AuthUser user = authRepository.findActiveUserByEmail(EmailAddress.from(normalized))
			.orElseThrow(() -> new AdminUserNotFoundException("user not found for email: " + normalized));
		return toView(user);
	}

	/** 대상 사용자의 요금제를 변경합니다. 운영자가 아니면 예외, 사용자가 없으면 예외. */
	@Transactional
	public AdminUserView changePlan(UUID requesterId, UUID targetUserId, AdminUserPlan plan) {
		requireAdmin(requesterId);
		Objects.requireNonNull(targetUserId, "targetUserId must not be null");
		Objects.requireNonNull(plan, "plan must not be null");
		boolean updated = authRepository.updatePlan(targetUserId, plan.name(), clock.instant());
		if (!updated) {
			throw new AdminUserNotFoundException("user not found: " + targetUserId);
		}
		AuthUser user = authRepository.findActiveUser(targetUserId)
			.orElseThrow(() -> new AdminUserNotFoundException("user not found: " + targetUserId));
		return new AdminUserView(user.id(), user.email().value(), user.nickname(), plan.name());
	}

	private AdminUserView toView(AuthUser user) {
		String plan = authRepository.findPlan(user.id()).orElse(AdminUserPlan.FREE.name());
		return new AdminUserView(user.id(), user.email().value(), user.nickname(), plan);
	}

	private void requireAdmin(UUID requesterId) {
		Objects.requireNonNull(requesterId, "requesterId must not be null");
		String email = authRepository.findActiveUser(requesterId)
			.map(user -> user.email().value())
			.orElse(null);
		if (email == null || !adminProperties.isAdmin(email)) {
			throw new AdminUserAccessDeniedException("only configured admins can manage user plans.");
		}
	}
}
