package com.studypot.aistudyleader.studygroup.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StudyGroupTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002801");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002802");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002803");
	private static final Instant NOW = Instant.parse("2026-05-09T01:00:00Z");
	private static final LocalDate STARTS_AT = LocalDate.parse("2026-05-10");
	private static final LocalDate ENDS_AT = LocalDate.parse("2026-06-21");

	@Test
	void createStartsGroupInOnboardingAndCreatesPendingOwnerMember() {
		StudyGroup group = StudyGroup.create(
			GROUP_ID,
			USER_ID,
			" Backend Interview Study ",
			" Spring Boot ",
			List.of(" JPA ", "Security"),
			6,
			STARTS_AT,
			ENDS_AT,
			" Weekly backend interview prep ",
			"INVITE-2026",
			NOW
		);
		GroupMember owner = GroupMember.owner(MEMBER_ID, group.id(), USER_ID, null, NOW);

		assertThat(group.id()).isEqualTo(GROUP_ID);
		assertThat(group.createdBy()).isEqualTo(USER_ID);
		assertThat(group.name()).isEqualTo("Backend Interview Study");
		assertThat(group.topic()).isEqualTo("Spring Boot");
		assertThat(group.detailKeywords()).containsExactly("JPA", "Security");
		assertThat(group.maxMembers()).isEqualTo(6);
		assertThat(group.status()).isEqualTo(StudyGroupStatus.ONBOARDING);
		assertThat(group.inviteCode()).isEqualTo("INVITE-2026");
		assertThat(group.onboardingStartedAt()).contains(NOW);
		assertThat(group.startedAt()).isEmpty();
		assertThat(group.auditMetadata().createdAt()).isEqualTo(NOW);
		assertThat(owner.groupId()).isEqualTo(GROUP_ID);
		assertThat(owner.userId()).isEqualTo(USER_ID);
		assertThat(owner.permission()).isEqualTo(GroupMemberPermission.OWNER);
		assertThat(owner.status()).isEqualTo(GroupMemberStatus.PENDING_ONBOARDING);
		assertThat(owner.joinedAt()).isEqualTo(NOW);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "   "})
	void createRejectsBlankName(String name) {
		assertThatThrownBy(() -> validGroupBuilder().name(name).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("study group name must not be blank");
	}

	@Test
	void createRejectsEmptyDetailKeywords() {
		assertThatThrownBy(() -> validGroupBuilder().detailKeywords(List.of()).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("detailKeywords must not be empty");
	}

	@Test
	void createRejectsBlankDetailKeyword() {
		assertThatThrownBy(() -> validGroupBuilder().detailKeywords(List.of("JPA", " ")).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("detailKeywords must not contain blank values");
	}

	@Test
	void createRejectsNonPositiveMaxMembers() {
		assertThatThrownBy(() -> validGroupBuilder().maxMembers(0).build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("maxMembers must be positive");
	}

	@Test
	void createRejectsEndDateBeforeStartDate() {
		assertThatThrownBy(() -> validGroupBuilder()
				.startsAt(LocalDate.parse("2026-06-22"))
				.endsAt(LocalDate.parse("2026-06-21"))
				.build())
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("endsAt must be on or after startsAt");
	}

	private static StudyGroupBuilder validGroupBuilder() {
		return new StudyGroupBuilder();
	}

	private static final class StudyGroupBuilder {

		private String name = "Backend Interview Study";
		private String topic = "Spring Boot";
		private List<String> detailKeywords = List.of("JPA", "Security");
		private int maxMembers = 6;
		private LocalDate startsAt = STARTS_AT;
		private LocalDate endsAt = ENDS_AT;

		StudyGroupBuilder name(String value) {
			this.name = value;
			return this;
		}

		StudyGroupBuilder detailKeywords(List<String> value) {
			this.detailKeywords = value;
			return this;
		}

		StudyGroupBuilder maxMembers(int value) {
			this.maxMembers = value;
			return this;
		}

		StudyGroupBuilder startsAt(LocalDate value) {
			this.startsAt = value;
			return this;
		}

		StudyGroupBuilder endsAt(LocalDate value) {
			this.endsAt = value;
			return this;
		}

		StudyGroup build() {
			return StudyGroup.create(
				GROUP_ID,
				USER_ID,
				name,
				topic,
				detailKeywords,
				maxMembers,
				startsAt,
				endsAt,
				null,
				"INVITE-2026",
				NOW
			);
		}
	}
}
