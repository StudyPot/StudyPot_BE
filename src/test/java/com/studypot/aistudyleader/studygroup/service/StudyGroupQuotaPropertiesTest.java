package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StudyGroupQuotaPropertiesTest {

	@Test
	void defaultsToThreeForFreeAndTwentyForPremium() {
		StudyGroupQuotaProperties properties = new StudyGroupQuotaProperties(null, null);

		assertThat(properties.limitForPlan("FREE")).isEqualTo(3);
		assertThat(properties.limitForPlan("PREMIUM")).isEqualTo(20);
	}

	@Test
	void treatsUnknownOrNullPlanAsFree() {
		StudyGroupQuotaProperties properties = new StudyGroupQuotaProperties(3, 20);

		assertThat(properties.limitForPlan(null)).isEqualTo(3);
		assertThat(properties.limitForPlan("legacy")).isEqualTo(3);
		assertThat(properties.limitForPlan("premium")).isEqualTo(20);
	}

	@Test
	void rejectsNonPositiveLimits() {
		assertThatThrownBy(() -> new StudyGroupQuotaProperties(0, 20))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
