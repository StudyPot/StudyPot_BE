package com.studypot.aistudyleader.global.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
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
	security = {
		@SecurityRequirement(name = "bearerAuth"),
		@SecurityRequirement(name = "cookieAccessToken")
	}
)
@SecurityScheme(
	name = "bearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT"
)
@SecurityScheme(
	name = "cookieAccessToken",
	type = SecuritySchemeType.APIKEY,
	in = SecuritySchemeIn.COOKIE,
	paramName = "studypot_access_token"
)
@Slf4j
class OpenApiConfiguration {

	private static final List<String> PUBLIC_AUTH_PATHS = List.of(
		ApiPaths.V1 + "/auth/oauth/google",
		ApiPaths.V1 + "/auth/refresh"
	);

	@Bean
	OpenApiCustomizer publicAuthEndpointCustomizer() {
		return openApi -> {
			if (openApi.getPaths() == null) {
				return;
			}
			for (String path : PUBLIC_AUTH_PATHS) {
				var pathItem = openApi.getPaths().get(path);
				if (pathItem == null) {
					log.warn("OpenAPI public auth path was not found: {}", path);
					continue;
				}
				pathItem.readOperations().forEach(OpenApiConfiguration::clearSecurity);
			}
		};
	}

	private static void clearSecurity(Operation operation) {
		operation.setSecurity(List.of());
	}
}
