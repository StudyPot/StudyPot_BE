package com.studypot.aistudyleader.global.web;

import com.studypot.aistudyleader.global.api.ApiPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

	private final ApiTimingInterceptor apiTimingInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(apiTimingInterceptor)
			.addPathPatterns(ApiPaths.V1 + "/**");
	}
}
