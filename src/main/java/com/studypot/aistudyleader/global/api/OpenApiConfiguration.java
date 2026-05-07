package com.studypot.aistudyleader.global.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
	info = @Info(
		title = "AI Study Leader API",
		version = "1.0.0",
		description = "REST API for the onboarding-based AI Study Leader backend."
	),
	security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
	name = "bearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT"
)
class OpenApiConfiguration {

	@Bean
	OpenApiCustomizer publicAuthEndpointCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			List.of(
				ApiPaths.V1 + "/auth/oauth/google",
				ApiPaths.V1 + "/auth/refresh"
			).stream()
				.map(path -> openApi.getPaths().get(path))
				.filter(pathItem -> pathItem != null)
				.flatMap(pathItem -> pathItem.readOperations().stream())
				.forEach(OpenApiConfiguration::clearSecurity);
		};
	}

	private static void clearSecurity(Operation operation) {
		operation.setSecurity(List.of());
	}
}
