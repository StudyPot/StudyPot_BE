package com.studypot.aistudyleader.global.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiPathsTest {

	@Test
	void v1PathMatchesLockedApiBasePath() {
		assertThat(ApiPaths.V1).isEqualTo("/api/v1");
	}
}
