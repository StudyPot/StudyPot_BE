package com.studypot.aistudyleader.global.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiTimingInterceptorTest {

	private final ApiTimingInterceptor interceptor = new ApiTimingInterceptor();

	@Test
	void preHandleStoresRequestScopedStartTime() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");

		boolean proceed = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

		assertThat(proceed).isTrue();
		assertThat(Collections.list(request.getAttributeNames()))
			.anyMatch(name -> name.endsWith(".startTime"));
	}

	@Test
	void afterCompletionAllowsMissingStartTime() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
		MockHttpServletResponse response = new MockHttpServletResponse();

		assertThatCode(() -> interceptor.afterCompletion(request, response, new Object(), null))
			.doesNotThrowAnyException();
	}
}
