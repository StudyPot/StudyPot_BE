package com.studypot.aistudyleader.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiConversationMessageTest {

	private static final UUID MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009401");
	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009402");
	private static final UUID LLM_USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009403");
	private static final Instant NOW = Instant.parse("2026-05-13T02:00:00Z");

	@Test
	void userMessageTrimsContentAndDefaultsMetadata() {
		AiConversationMessage message = AiConversationMessage.userMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			"  이번 주 과제 양을 줄이고 싶어요.  ",
			NOW
		);

		assertThat(message.id()).isEqualTo(MESSAGE_ID);
		assertThat(message.conversationId()).isEqualTo(CONVERSATION_ID);
		assertThat(message.llmUsageId()).isNull();
		assertThat(message.senderType()).isEqualTo(AiConversationMessageSenderType.USER);
		assertThat(message.content()).isEqualTo("이번 주 과제 양을 줄이고 싶어요.");
		assertThat(message.metadata()).isEmpty();
		assertThat(message.createdAt()).isEqualTo(NOW);
	}

	@Test
	void assistantMessageKeepsLlmUsageAndMetadata() {
		AiConversationMessage message = new AiConversationMessage(
			MESSAGE_ID,
			CONVERSATION_ID,
			LLM_USAGE_ID,
			AiConversationMessageSenderType.ASSISTANT,
			"다음 주에는 필수 과제를 하나 줄이는 방향을 추천합니다.",
			Map.of("retrievalContextVersion", "db-first-v1"),
			NOW
		);

		assertThat(message.senderType()).isEqualTo(AiConversationMessageSenderType.ASSISTANT);
		assertThat(message.llmUsageId()).isEqualTo(LLM_USAGE_ID);
		assertThat(message.metadata()).containsEntry("retrievalContextVersion", "db-first-v1");
	}

	@Test
	void messageRejectsBlankContent() {
		assertThatThrownBy(() -> AiConversationMessage.userMessage(MESSAGE_ID, CONVERSATION_ID, " ", NOW))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("content must not be blank.");
	}
}
