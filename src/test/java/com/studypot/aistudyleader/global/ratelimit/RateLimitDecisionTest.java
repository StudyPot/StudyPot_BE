package com.studypot.aistudyleader.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitDecisionTest {

	@Test
	void allowedDecisionHasNoRetryAfter() {
		RateLimitDecision decision = RateLimitDecision.allowed(3, 60);

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.retryAfter()).isZero();
	}

	@Test
	void rejectedDecisionRequiresPositiveRetryAfterAndExceededCount() {
		assertThatThrownBy(() -> RateLimitDecision.rejected(60, 60, Duration.ofSeconds(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("rejected decision must exceed limit");

		assertThatThrownBy(() -> RateLimitDecision.rejected(61, 60, Duration.ZERO))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("rejected decision must have positive retryAfter");
	}
}
