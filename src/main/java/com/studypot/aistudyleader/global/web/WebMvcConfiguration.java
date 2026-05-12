package com.studypot.aistudyleader.global.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.studypot.aistudyleader.global.api.ApiPaths;

@Configuration(proxyBeanMethods = false)
public class WebMvcConfiguration implements WebMvcConfigurer {
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ApiTimingInterceptor())
			.addPathPatterns(ApiPaths.V1 + "/**");
	}
}
