package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.global.api.ApiPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(StudypotCorsProperties.class)
public class SecurityConfiguration {

	@Bean
	public SecurityFilterChain apiSecurity(
		HttpSecurity http,
		ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
		ProblemDetailAccessDeniedHandler accessDeniedHandler,
		CorsConfigurationSource corsConfigurationSource,
		BearerTokenResolver bearerTokenResolver,
		@Value("${studypot.openapi.public-docs-enabled:false}") boolean publicOpenApiDocsEnabled
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(authorize -> {
				authorize
					.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
					.requestMatchers(HttpMethod.HEAD, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
				if (publicOpenApiDocsEnabled) {
					authorize
						.requestMatchers(HttpMethod.GET, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
						.requestMatchers(HttpMethod.HEAD, "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();
				}
					authorize
						.requestMatchers(HttpMethod.POST, ApiPaths.V1 + "/auth/oauth/google").permitAll()
						.requestMatchers(HttpMethod.POST, ApiPaths.V1 + "/auth/refresh").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/oauth2/authorization/google").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/login/oauth2/code/google").permitAll()
						.requestMatchers(ApiPaths.V1 + "/**").authenticated()
						.anyRequest().denyAll();
				})
				.oauth2ResourceServer(oauth2 -> oauth2
					.bearerTokenResolver(bearerTokenResolver)
					.jwt(Customizer.withDefaults()))
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(StudypotCorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedOriginPatterns(properties.allowedOriginPatterns());
		configuration.setAllowedMethods(properties.allowedMethods());
		configuration.setAllowedHeaders(properties.allowedHeaders());
		configuration.setExposedHeaders(properties.exposedHeaders());
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
