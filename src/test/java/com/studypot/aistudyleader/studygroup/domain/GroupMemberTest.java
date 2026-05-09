package com.studypot.aistudyleader.studygroup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GroupMemberTest {

	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002901");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002902");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002903");
	private static final Instant NOW = Instant.parse("2026-05-09T03:00:00Z");

	@Test
	void memberCreatesPendingOnboardingMemberPermission() {
		GroupMember member = GroupMember.member(MEMBER_ID, GROUP_ID, USER_ID, " New Member ", NOW);

		assertThat(member.id()).isEqualTo(MEMBER_ID);
		assertThat(member.groupId()).isEqualTo(GROUP_ID);
		assertThat(member.userId()).isEqualTo(USER_ID);
		assertThat(member.permission()).isEqualTo(GroupMemberPermission.MEMBER);
		assertThat(member.status()).isEqualTo(GroupMemberStatus.PENDING_ONBOARDING);
		assertThat(member.displayName()).contains("New Member");
		assertThat(member.joinedAt()).isEqualTo(NOW);
		assertThat(member.activatedAt()).isEmpty();
		assertThat(member.leftAt()).isEmpty();
		assertThat(member.auditMetadata().createdAt()).isEqualTo(NOW);
	}

	@Test
	void memberRejectsDisplayNameThatIsTooLong() {
		assertThatThrownBy(() -> GroupMember.member(MEMBER_ID, GROUP_ID, USER_ID, "x".repeat(81), NOW))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("displayName length must be <= 80: 81");
	}
}
