package com.studypot.aistudyleader.ai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiConversationTest {

	private static final UUID CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000009001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000009002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000009003");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000009004");
	private static final UUID RETROSPECTIVE_ID = UUID.fromString("018f0000-0000-7000-8000-000000009005");
	private static final Instant NOW = Instant.parse("2026-05-13T00:30:00Z");

	@Test
	void openCreatesOpenConversationWithEmptySummary() {
		AiConversation conversation = AiConversation.open(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			WEEK_ID,
			RETROSPECTIVE_ID,
			AiConversationType.RETROSPECTIVE,
			NOW
		);

		assertThat(conversation.id()).isEqualTo(CONVERSATION_ID);
		assertThat(conversation.groupId()).isEqualTo(GROUP_ID);
		assertThat(conversation.memberId()).isEqualTo(MEMBER_ID);
		assertThat(conversation.curriculumWeekId()).isEqualTo(WEEK_ID);
		assertThat(conversation.retrospectiveId()).isEqualTo(RETROSPECTIVE_ID);
		assertThat(conversation.conversationType()).isEqualTo(AiConversationType.RETROSPECTIVE);
		assertThat(conversation.status()).isEqualTo(AiConversationStatus.OPEN);
		assertThat(conversation.summary()).isEmpty();
		assertThat(conversation.openedAt()).isEqualTo(NOW);
		assertThat(conversation.closedAt()).isNull();
		assertThat(conversation.createdAt()).isEqualTo(NOW);
		assertThat(conversation.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void constructorNormalizesNullSummaryToEmptyText() {
		AiConversation conversation = new AiConversation(
			CONVERSATION_ID,
			GROUP_ID,
			MEMBER_ID,
			null,
			null,
			AiConversationType.TEAM_LEAD_CHAT,
			AiConversationStatus.OPEN,
			null,
			NOW,
			null,
			NOW,
			NOW
		);

		assertThat(conversation.summary()).isEmpty();
	}

	@Test
	void constructorRequiresCoreFields() {
		assertThatThrownBy(() -> AiConversation.open(
				CONVERSATION_ID,
				GROUP_ID,
				MEMBER_ID,
				null,
				null,
				null,
				NOW
			))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("conversationType must not be null");
	}
}
