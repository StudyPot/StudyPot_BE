package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "AI 팀장 대화 메시지 응답입니다.")
record AiConversationMessageResponse(
	@Schema(description = "AI 대화 메시지 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8c")
	UUID id,
	@Schema(description = "메시지 발신자 유형입니다.", example = "USER")
	AiConversationMessageSenderType senderType,
	@Schema(description = "메시지 본문입니다.", example = "이번 주 과제 양을 조금 줄이고 싶어요.")
	String content,
	@Schema(description = "메시지 생성 시각입니다.")
	Instant createdAt
) {

	static AiConversationMessageResponse from(AiConversationMessage message) {
		return new AiConversationMessageResponse(
			message.id(),
			message.senderType(),
			message.content(),
			message.createdAt()
		);
	}
}
