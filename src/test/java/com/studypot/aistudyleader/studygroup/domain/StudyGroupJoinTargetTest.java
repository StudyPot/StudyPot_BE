package com.studypot.aistudyleader.studygroup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudyGroupJoinTargetTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002911");

	@Test
	void onboardingGroupAcceptsMatchingInviteCodeWhileCapacityRemains() {
		StudyGroupJoinTarget target = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");

		assertThat(target.isAcceptingJoins()).isTrue();
		assertThat(target.matchesInviteCode(" INVITE-0001 ")).isTrue();
		assertThat(target.hasCapacity(2)).isTrue();
		assertThat(target.hasCapacity(3)).isFalse();
	}

	@Test
	void activeGroupDoesNotAcceptSpt29JoinFlow() {
		StudyGroupJoinTarget target = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ACTIVE, 3, "INVITE-0001");

		assertThat(target.isAcceptingJoins()).isFalse();
	}

	@Test
	void rejectsInvalidJoinTargetSnapshot() {
		assertThatThrownBy(() -> new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 0, "INVITE-0001"))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("maxMembers must be positive");
		assertThatThrownBy(() -> new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, " "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inviteCode must not be blank");
	}
}
