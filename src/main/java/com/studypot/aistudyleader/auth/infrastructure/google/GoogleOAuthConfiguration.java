package com.studypot.aistudyleader.auth.infrastructure.google;

import com.studypot.aistudyleader.auth.service.GoogleOAuthCodeExchangePort;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GoogleOAuthProperties.class)
class GoogleOAuthConfiguration {

	@Bean
	GoogleOAuthCodeExchangePort googleOAuthCodeExchangePort(
		ObjectProvider<RestClient.Builder> restClientBuilder,
		GoogleOAuthProperties properties
	) {
		RestClient.Builder builder = restClientBuilder.getIfAvailable(RestClient::builder)
			.requestFactory(requestFactory(properties));
		return new GoogleOAuthClient(builder, properties, Clock.systemUTC());
	}

	private static JdkClientHttpRequestFactory requestFactory(GoogleOAuthProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(properties.connectTimeout())
			.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return requestFactory;
	}
}
