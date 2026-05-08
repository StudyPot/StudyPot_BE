package com.studypot.aistudyleader.identity.infrastructure.security;

import com.studypot.aistudyleader.identity.infrastructure.google.GoogleOAuthConfigurationException;
import com.studypot.aistudyleader.identity.infrastructure.google.GoogleOAuthProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
	@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${studypot.oauth.google.client-id:}')")
	ClientRegistrationRepository googleClientRegistrationRepository(
		GoogleOAuthProperties google,
		AuthProperties auth
	) {
		ClientRegistration registration = CommonOAuth2Provider.GOOGLE.getBuilder(GOOGLE_REGISTRATION_ID)
			.clientId(require(google.clientId(), "Google OAuth client id is not configured."))
			.clientSecret(require(google.clientSecret(), "Google OAuth client secret is not configured."))
			.authorizationUri(google.authorizationUri())
			.tokenUri(google.tokenUri())
			.userInfoUri(google.userinfoUri())
			.redirectUri(require(auth.oauth2().backendCallbackUri(), "OAuth backend callback URI is not configured."))
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
		resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
		return resolver;
	}

	private static String require(String value, String message) {
		if (value == null || value.isBlank()) {
			throw new GoogleOAuthConfigurationException(message);
		}
		return value.strip();
	}
}
