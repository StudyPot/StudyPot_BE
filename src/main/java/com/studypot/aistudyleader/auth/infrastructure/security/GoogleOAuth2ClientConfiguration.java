package com.studypot.aistudyleader.auth.infrastructure.security;

import com.studypot.aistudyleader.auth.infrastructure.google.GoogleOAuthConfigurationException;
import com.studypot.aistudyleader.auth.infrastructure.google.GoogleOAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({GoogleOAuthProperties.class, AuthProperties.class})
class GoogleOAuth2ClientConfiguration {

	private static final String GOOGLE_REGISTRATION_ID = "google";
	private static final String AUTHORIZATION_BASE_URI = "/api/oauth2/authorization";

	@Bean
	@ConditionalOnProperty(prefix = "studypot.oauth.google", name = "client-id", matchIfMissing = false)
	@Conditional(GoogleOAuthClientIdHasTextCondition.class)
	ClientRegistrationRepository googleClientRegistrationRepository(
		GoogleOAuthProperties google,
		AuthProperties auth
	) {
		AuthProperties.OAuth2 oauth2 = auth.oauth2();
		if (oauth2 == null) {
			throw new GoogleOAuthConfigurationException("OAuth2 properties are not configured.");
		}
		ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE_REGISTRATION_ID)
			.clientId(require(google.clientId(), "Google OAuth client id is not configured."))
			.clientSecret(require(google.clientSecret(), "Google OAuth client secret is not configured."))
			.authorizationUri(google.authorizationUri())
			.tokenUri(google.tokenUri())
			.userInfoUri(google.userinfoUri())
			.redirectUri(require(oauth2.backendCallbackUri(), "OAuth backend callback URI is not configured."))
			.scope(google.scopes())
			.clientName("Google")
			.build();
		return new InMemoryClientRegistrationRepository(registration);
	}

	@Bean
	@ConditionalOnBean(ClientRegistrationRepository.class)
	OAuth2AuthorizationRequestResolver googleOAuth2AuthorizationRequestResolver(
		ClientRegistrationRepository clientRegistrationRepository
	) {
		DefaultOAuth2AuthorizationRequestResolver resolver =
			new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_BASE_URI);
		// PKCE 유지 + 항상 구글 계정 선택창을 띄운다(prompt=select_account).
		// 브라우저에 이미 로그인된 구글 계정으로 자동 진행되어 다른 계정으로 로그인하지 못하는 문제를 막는다.
		resolver.setAuthorizationRequestCustomizer(
			OAuth2AuthorizationRequestCustomizers.withPkce()
				.andThen(builder -> builder.additionalParameters(params -> params.put("prompt", "select_account")))
		);
		return resolver;
	}

	private static String require(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new GoogleOAuthConfigurationException(message);
		}
		return value.strip();
	}

	static final class GoogleOAuthClientIdHasTextCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String clientId = context.getEnvironment().getProperty("studypot.oauth.google.client-id");
			return clientId == null || clientId.isBlank()
				? ConditionOutcome.noMatch("studypot.oauth.google.client-id is blank")
				: ConditionOutcome.match("studypot.oauth.google.client-id is configured");
		}
	}
}
