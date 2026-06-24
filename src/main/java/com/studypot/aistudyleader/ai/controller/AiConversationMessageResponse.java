package com.studypot.aistudyleader.ai.controller;

import com.studypot.aistudyleader.ai.domain.AiConversationMessage;
import com.studypot.aistudyleader.ai.domain.AiConversationMessageSenderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
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
	Instant createdAt,
	@Schema(description = "메시지에 제안된 액션입니다. 없으면 null이며, 있으면 클라이언트가 확인 버튼을 렌더링합니다.")
	MessageActionView action
) {

	static AiConversationMessageResponse from(AiConversationMessage message) {
		return new AiConversationMessageResponse(
			message.id(),
			message.senderType(),
			message.content(),
			message.createdAt(),
			MessageActionView.from(message.metadata())
		);
	}

	@Schema(description = "AI 팀장 메시지에 제안된 액션입니다(확인 후 실행).")
	record MessageActionView(
		@Schema(description = "액션 유형입니다.", example = "SHARE_QUESTION")
		String type,
		@Schema(description = "액션 상태입니다.", example = "PENDING")
		String status,
		@Schema(description = "공유 제안된 질문 제목입니다.")
		String title,
		@Schema(description = "공유 제안된 질문 요약입니다.")
		String summary
	) {

		static MessageActionView from(Map<String, Object> metadata) {
			if (metadata == null) {
				return null;
			}
			if (!(metadata.get("pendingAction") instanceof Map<?, ?> pendingAction)) {
				return null;
			}
			String type = stringOrNull(pendingAction.get("type"));
			String status = stringOrNull(pendingAction.get("status"));
			String title = null;
			String summary = null;
			if (pendingAction.get("question") instanceof Map<?, ?> question) {
				title = stringOrNull(question.get("title"));
				summary = stringOrNull(question.get("summary"));
			}
			return new MessageActionView(type, status, title, summary);
		}

		private static String stringOrNull(Object value) {
			return value == null ? null : value.toString();
		}
	}
}
