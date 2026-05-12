package com.studypot.aistudyleader.global.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class ApiTimingInterceptor implements HandlerInterceptor {

	private static final String START_TIME_ATTRIBUTE = ApiTimingInterceptor.class.getName() + ".startTime";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		request.setAttribute(START_TIME_ATTRIBUTE, System.nanoTime());
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
		@Nullable Exception exception) throws Exception {
		Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
		if (!(startTime instanceof Long startedAt)) {
			return;
		}
		request.removeAttribute(START_TIME_ATTRIBUTE);

		long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

		log.info("api_request_timing method={} uri={} status={} elapsedMs={}ms exception={}", request.getMethod(),
			request.getRequestURI(), response.getStatus(), elapsedMs,
			exception == null ? "None" : exception.getClass().getSimpleName());
	}
}
