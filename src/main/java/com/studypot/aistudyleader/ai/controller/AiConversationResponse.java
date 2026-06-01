package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversation;
import com.studypot.aistudyleader.ai.domain.AiConversationStatus;
import com.studypot.aistudyleader.ai.domain.AiConversationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "AI 팀장 대화 세션 응답입니다.")
record AiConversationResponse(
	@Schema(description = "AI 대화 세션 UUID입니다.", example = "018f6f55-900d-7b14-bd27-48ec1d752b8a")
	UUID id,
	@Schema(description = "대화 유형입니다.", example = "TEAM_LEAD_CHAT")
	AiConversationType conversationType,
	@Schema(description = "대화 세션 상태입니다.", example = "OPEN")
	AiConversationStatus status,
	@Schema(description = "현재까지의 대화 요약입니다.", example = "")
	String summary
) {

	static AiConversationResponse from(AiConversation conversation) {
		return new AiConversationResponse(
			conversation.id(),
			conversation.conversationType(),
			conversation.status(),
			conversation.summary()
		);
	}
}
