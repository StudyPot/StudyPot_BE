package com.studypot.aistudyleader.global.security;

import com.studypot.aistudyleader.global.api.ApiPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfiguration {

	@Bean
	public SecurityFilterChain apiSecurity(
		HttpSecurity http,
		ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
		ProblemDetailAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		return http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler))
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
				.requestMatchers(ApiPaths.V1 + "/auth/oauth/google").permitAll()
				.requestMatchers(ApiPaths.V1 + "/auth/refresh").permitAll()
				.requestMatchers(ApiPaths.V1 + "/**").authenticated()
				.anyRequest().denyAll())
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
			.build();
	}
}
