package com.studypot.aistudyleader.curriculum.infrastructure.openai;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

class OpenAiApiKeyConfiguredCondition implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		return StringUtils.hasText(context.getEnvironment().getProperty("studypot.ai.openai.api-key"));
	}
}
