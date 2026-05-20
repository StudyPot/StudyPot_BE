package com.studypot.aistudyleader.llm.service;

import java.util.List;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class LlmProviderConfiguredCondition implements Condition {

	private static final List<String> PROVIDER_KEY_PROPERTIES = List.of(
		"studypot.ai.openai.api-key"
	);

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return PROVIDER_KEY_PROPERTIES.stream()
			.anyMatch(property -> StringUtils.hasText(context.getEnvironment().getProperty(property)));
	}
}
