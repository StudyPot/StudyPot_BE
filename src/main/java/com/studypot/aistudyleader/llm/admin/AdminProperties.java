package com.studypot.aistudyleader.llm.admin;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 운영(admin) 권한을 가진 사용자 이메일 허용목록입니다.
 *
 * <p>역할 시스템 대신 환경변수 기반 이메일 허용목록으로 운영자 권한을 판별합니다.
 * 비교는 공백 제거 후 소문자로 정규화하여 수행합니다.</p>
 */
@ConfigurationProperties(prefix = "studypot.admin")
public record AdminProperties(Set<String> emails) {

	public AdminProperties {
		emails = normalize(emails);
	}

	private static Set<String> normalize(Set<String> emails) {
		if (emails == null) {
			return Set.of();
		}
		return emails.stream()
			.filter(email -> email != null && !email.isBlank())
			.map(email -> email.strip().toLowerCase(Locale.ROOT))
			.collect(Collectors.toUnmodifiableSet());
	}

	public boolean isAdmin(String email) {
		if (email == null || email.isBlank()) {
			return false;
		}
		return emails.contains(email.strip().toLowerCase(Locale.ROOT));
	}
}
