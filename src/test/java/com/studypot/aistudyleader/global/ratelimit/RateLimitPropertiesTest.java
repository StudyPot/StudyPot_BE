package com.studypot.aistudyleader.global.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimitPropertiesTest {

	@Test
	void defaultsToDisabledWithConfiguredPolicies() {
		RateLimitProperties properties = new RateLimitProperties(null, null, null, null, null, null);

		assertThat(properties.enabled()).isFalse();
		assertThat(properties.failClosed()).isFalse();
		assertThat(properties.usersMe().limit()).isEqualTo(60);
		assertThat(properties.usersMe().window()).isEqualTo(Duration.ofMinutes(1));
		assertThat(properties.aiConversation().limit()).isEqualTo(5);
		assertThat(properties.curriculumGeneration().window()).isEqualTo(Duration.ofMinutes(10));
		assertThat(properties.retrospectiveFeedback().limit()).isEqualTo(2);
	}

	@Test
	void rejectsInvalidPolicy() {
		assertThatThrownBy(() -> new RateLimitProperties.Policy(0, Duration.ofMinutes(1)))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("rate limit must be positive");

		assertThatThrownBy(() -> new RateLimitProperties.Policy(1, Duration.ZERO))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("rate limit window must be positive");
	}
}
