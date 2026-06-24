package com.studypot.aistudyleader.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AiConversationBoardGatewayWiringTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(AiConversationApplicationConfiguration.class);

	@Test
	void boardGatewayBeanIsAlwaysRegistered() {
		// @ConditionalOnBean 순서 함정으로 게이트웨이가 누락되어 "board action is not configured" 가
		// 나던 회귀를 방지한다. GroupBoardService 가 컨텍스트에 없어도 게이트웨이 빈은 등록되어야 한다.
		contextRunner.run(context -> assertThat(context).hasSingleBean(AiConversationBoardGateway.class));
	}
}
