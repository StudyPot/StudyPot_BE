package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.global.api.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

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
		@Value("${studypot.openapi.public-docs-enabled:false}") boolean publicOpenApiDocsEnabled
	) throws Exception {
		ClientRegistrationRepository oauth2Registrations = clientRegistrationRepository.getIfAvailable();
		SessionCreationPolicy sessionCreationPolicy = oauth2Registrations == null
			? SessionCreationPolicy.STATELESS
			: SessionCreationPolicy.IF_REQUIRED;

		http
			.csrf(csrf -> {
				csrf.spa();
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
				.addFilterAfter(new BrowserCsrfProtectionFilter(accessDeniedHandler), CsrfFilter.class);

		if (oauth2Registrations != null) {
			http.oauth2Login(oauth2 -> oauth2
				.clientRegistrationRepository(oauth2Registrations)
				.authorizationEndpoint(endpoint -> endpoint
					.baseUri("/api/oauth2/authorization")
					.authorizationRequestResolver(authorizationRequestResolver.getObject()))
				.redirectionEndpoint(endpoint -> endpoint.baseUri("/api/login/oauth2/code/*"))
				.successHandler(googleOAuth2LoginSuccessHandler.getObject())
				.failureHandler(googleOAuth2LoginFailureHandler.getObject()));
		}

		return http.build();
	}

	private static boolean requiresCsrfProtection(HttpServletRequest request) {
		return !CSRF_SAFE_METHODS.contains(request.getMethod())
			&& !hasBearerAuthorization(request)
			&& !isGoogleCodeLoginRequest(request);
	}

	private static boolean isGoogleCodeLoginRequest(HttpServletRequest request) {
		return request.getMethod().equals(HttpMethod.POST.name())
			&& request.getRequestURI().equals(ApiPaths.V1 + "/auth/oauth/google");
	}

	private static boolean hasBearerAuthorization(HttpServletRequest request) {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, "Bearer ".length());
	}

	private static final class BrowserCsrfProtectionFilter extends OncePerRequestFilter {

		private static final String XSRF_COOKIE = "XSRF-TOKEN";
		private static final String X_XSRF_TOKEN = "X-XSRF-TOKEN";
		private static final String X_CSRF_TOKEN = "X-CSRF-TOKEN";

		private final AccessDeniedHandler accessDeniedHandler;

		private BrowserCsrfProtectionFilter(AccessDeniedHandler accessDeniedHandler) {
			this.accessDeniedHandler = accessDeniedHandler == null ? new AccessDeniedHandlerImpl() : accessDeniedHandler;
		}

		@Override
		protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
		) throws ServletException, IOException {
			if (!requiresCsrfProtection(request) || xsrfTokenMatches(request)) {
				filterChain.doFilter(request, response);
				return;
			}
			accessDeniedHandler.handle(
				request,
				response,
				new AccessDeniedException("CSRF token is required.")
			);
		}

		private static boolean xsrfTokenMatches(HttpServletRequest request) {
			String expected = cookieValue(request, XSRF_COOKIE);
			String actual = headerValue(request, X_XSRF_TOKEN);
			if (actual == null) {
				actual = headerValue(request, X_CSRF_TOKEN);
			}
			if (actual == null) {
				actual = parameterValue(request, "_csrf");
			}
			return expected != null
				&& actual != null
				&& MessageDigest.isEqual(
					expected.getBytes(StandardCharsets.UTF_8),
					actual.getBytes(StandardCharsets.UTF_8)
				);
		}

		private static String cookieValue(HttpServletRequest request, String name) {
			if (request.getCookies() == null) {
				return null;
			}
			for (Cookie cookie : request.getCookies()) {
				if (cookie.getName().equals(name) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
					return cookie.getValue();
				}
			}
			return null;
		}

		private static String headerValue(HttpServletRequest request, String name) {
			String value = request.getHeader(name);
			return value == null || value.isBlank() ? null : value;
		}

		private static String parameterValue(HttpServletRequest request, String name) {
			String value = request.getParameter(name);
			return value == null || value.isBlank() ? null : value;
		}
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
