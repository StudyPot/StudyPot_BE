package com.studypot.aistudyleader.global.ratelimit;

import java.time.Duration;

/**
 * 한도 카운터의 읽기 전용 스냅샷. 증가 없이 현재 사용량과 리셋까지 남은 시간을 조회한다.
 *
 * @param count        현재 윈도우에서 누적된 호출 수(키가 없으면 0)
 * @param timeToReset  현재 윈도우가 리셋(키 만료)되기까지 남은 시간(없으면 ZERO)
 */
public record RateLimitSnapshot(long count, Duration timeToReset) {

	public RateLimitSnapshot {
		if (count < 0) {
			count = 0;
		}
		if (timeToReset == null || timeToReset.isNegative()) {
			timeToReset = Duration.ZERO;
		}
	}
}
