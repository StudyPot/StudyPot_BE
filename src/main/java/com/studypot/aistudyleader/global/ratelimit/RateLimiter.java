package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;

public interface RateLimiter {

	RateLimitDecision check(String key, long limit, Duration window);

	/**
	 * 카운터를 증가시키지 않고 현재 사용량/리셋 잔여시간을 조회한다.
	 * 기본 구현은 정보 없음(0)을 반환하며, 실제 백엔드(Redis)가 오버라이드한다.
	 * default 로 두어 인터페이스를 함수형(check 단일 추상 메서드)으로 유지한다.
	 */
	default RateLimitSnapshot peek(String key) {
		return new RateLimitSnapshot(0, Duration.ZERO);
	}
}
