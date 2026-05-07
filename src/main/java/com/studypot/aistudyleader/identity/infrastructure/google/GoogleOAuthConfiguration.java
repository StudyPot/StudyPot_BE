package com.studypot.aistudyleader.identity.infrastructure.google;

import com.studypot.aistudyleader.identity.service.GoogleOAuthCodeExchangePort;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GoogleOAuthProperties.class)
class GoogleOAuthConfiguration {

	@Bean
	GoogleOAuthCodeExchangePort googleOAuthCodeExchangePort(
		ObjectProvider<RestClient.Builder> restClientBuilder,
		GoogleOAuthProperties properties
	) {
		return new GoogleOAuthClient(restClientBuilder.getIfAvailable(RestClient::builder), properties, Clock.systemUTC());
	}
}
