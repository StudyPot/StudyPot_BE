package com.studypot.aistudyleader.global.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "studypot.cors")
public record StudypotCorsProperties(
	List<String> allowedOrigins,
	List<String> allowedOriginPatterns,
	List<String> allowedMethods,
	List<String> allowedHeaders,
	List<String> exposedHeaders
) {

	private static final List<String> DEFAULT_ALLOWED_METHODS = List.of(
		"GET",
		"POST",
		"PUT",
		"PATCH",
		"DELETE",
		"OPTIONS",
		"HEAD"
	);
	private static final List<String> DEFAULT_ALLOWED_HEADERS = List.of(
		"Authorization",
		"Content-Type",
		"Accept",
		"Origin",
		"X-Requested-With"
	);
	private static final List<String> DEFAULT_EXPOSED_HEADERS = List.of("Location");

	public StudypotCorsProperties {
		allowedOrigins = normalize(allowedOrigins);
		allowedOriginPatterns = normalize(allowedOriginPatterns);
		allowedMethods = defaultIfEmpty(allowedMethods, DEFAULT_ALLOWED_METHODS);
		allowedHeaders = defaultIfEmpty(allowedHeaders, DEFAULT_ALLOWED_HEADERS);
		exposedHeaders = defaultIfEmpty(exposedHeaders, DEFAULT_EXPOSED_HEADERS);
	}

	private static List<String> defaultIfEmpty(List<String> values, List<String> defaultValues) {
		List<String> normalized = normalize(values);
		return normalized.isEmpty() ? defaultValues : normalized;
	}

	private static List<String> normalize(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
			.filter(value -> value != null && !value.isBlank())
			.map(String::strip)
			.toList();
	}
}
