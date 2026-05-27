package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.auth.infrastructure.security.AuthProperties;
import com.studypot.aistudyleader.global.api.ApiPaths;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(StudypotCorsProperties.class)
public class SecurityConfiguration {

	private static final Set<String> CSRF_SAFE_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

	@Bean
	public SecurityFilterChain apiSecurity(
		HttpSecurity http,
		ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
		ProblemDetailAccessDeniedHandler accessDeniedHandler,
		CorsConfigurationSource corsConfigurationSource,
		BearerTokenResolver bearerTokenResolver,
		ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
		ObjectProvider<OAuth2AuthorizationRequestResolver> authorizationRequestResolver,
		@Qualifier("googleOAuth2LoginSuccessHandler") ObjectProvider<AuthenticationSuccessHandler> googleOAuth2LoginSuccessHandler,
		@Qualifier("googleOAuth2LoginFailureHandler") ObjectProvider<AuthenticationFailureHandler> googleOAuth2LoginFailureHandler,
		AuthProperties authProperties,
		@Value("${studypot.openapi.public-docs-enabled:false}") boolean publicOpenApiDocsEnabled
	) throws Exception {
		ClientRegistrationRepository oauth2Registrations = clientRegistrationRepository.getIfAvailable();
		SessionCreationPolicy sessionCreationPolicy = oauth2Registrations == null
			? SessionCreationPolicy.STATELESS
			: SessionCreationPolicy.IF_REQUIRED;

		http
			.csrf(csrf -> {
				csrf.spa();
				csrf.csrfTokenRepository(csrfTokenRepository(authProperties));
				csrf.requireCsrfProtectionMatcher(SecurityConfiguration::requiresCsrfProtection);
			})
			.cors(cors -> cors.configurationSource(corsConfigurationSource))
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(sessionCreationPolicy))
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
					.requestMatchers(HttpMethod.POST, ApiPaths.V1 + "/auth/refresh").permitAll()
					.requestMatchers(HttpMethod.GET, ApiPaths.V1 + "/auth/csrf").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/oauth2/authorization/google").permitAll()
					.requestMatchers(HttpMethod.GET, "/api/login/oauth2/code/google").permitAll()
					.requestMatchers(ApiPaths.V1 + "/**").authenticated()
					.anyRequest().denyAll();
				})
				.oauth2ResourceServer(oauth2 -> oauth2
					.bearerTokenResolver(bearerTokenResolver)
					.jwt(Customizer.withDefaults()))
				.addFilterAfter(
					new BrowserCsrfProtectionFilter(accessDeniedHandler, SecurityConfiguration::requiresCsrfProtection),
					CsrfFilter.class
				);

		if (oauth2Registrations != null) {
			OAuth2AuthorizationRequestResolver requestResolver = authorizationRequestResolver.getIfAvailable();
			AuthenticationSuccessHandler successHandler = googleOAuth2LoginSuccessHandler.getIfAvailable();
			AuthenticationFailureHandler failureHandler = googleOAuth2LoginFailureHandler.getIfAvailable();
			http.oauth2Login(oauth2 -> {
				oauth2.clientRegistrationRepository(oauth2Registrations);
				oauth2.authorizationEndpoint(endpoint -> {
					endpoint.baseUri("/api/oauth2/authorization");
					if (requestResolver != null) {
						endpoint.authorizationRequestResolver(requestResolver);
					}
				});
				oauth2.redirectionEndpoint(endpoint -> endpoint.baseUri("/api/login/oauth2/code/*"));
				if (successHandler != null) {
					oauth2.successHandler(successHandler);
				}
				if (failureHandler != null) {
					oauth2.failureHandler(failureHandler);
				}
			});
		}

		return http.build();
	}

	private static boolean requiresCsrfProtection(HttpServletRequest request) {
		return !CSRF_SAFE_METHODS.contains(request.getMethod())
			&& !hasBearerAuthorization(request);
	}

	private static boolean hasBearerAuthorization(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
	}

	private static CookieCsrfTokenRepository csrfTokenRepository(AuthProperties properties) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer(cookie -> {
			cookie.path(properties.cookie().path())
				.secure(properties.cookie().secure())
				.sameSite(properties.cookie().sameSite());
			if (properties.cookie().domain() != null) {
				cookie.domain(properties.cookie().domain());
			}
		});
		return repository;
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(StudypotCorsProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.allowedOrigins());
		configuration.setAllowedOriginPatterns(properties.allowedOriginPatterns());
		configuration.setAllowedMethods(properties.allowedMethods());
		configuration.setAllowedHeaders(properties.allowedHeaders());
		configuration.setExposedHeaders(properties.exposedHeaders());
		configuration.setAllowCredentials(properties.allowCredentials());
		if (Boolean.TRUE.equals(configuration.getAllowCredentials()) && configuration.getAllowedOrigins().contains("*")) {
			throw new IllegalArgumentException(
				"studypot.cors.allowed-origins cannot contain '*' when credentials are allowed; use allowed-origin-patterns instead."
			);
		}

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
