package com.studypot.aistudyleader.persistence;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class FlywayAutoConfigurationContractTest {

	@Test
	void springBootFlywayAutoConfigurationIsOnTheRuntimeClasspath() {
		assertThatCode(() -> Class.forName(
			"org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
			false,
			Thread.currentThread().getContextClassLoader()
		)).doesNotThrowAnyException();
	}
}
