package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.curriculum.domain.MemberWeekProgressStatus;
import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberSummary;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupJoinTarget;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupMemberProfile;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.repository.GroupMemberDuplicateMembershipException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupInviteCodeConflictException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class StudyGroupServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002821");
	private static final UUID JOINER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002820");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002822");
	private static final UUID OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002823");
	private static final UUID RETRY_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002824");
	private static final UUID RETRY_OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002825");
	private static final UUID THIRD_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002826");
	private static final UUID THIRD_OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002827");
	private static final UUID JOINED_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002828");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000002829");
	private static final Instant NOW = Instant.parse("2026-05-09T01:30:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void createGroupPersistsOnboardingGroupAndPendingOwnerMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("INVITE-0001"), GROUP_ID, OWNER_MEMBER_ID);

		StudyGroupCreationResult result = service.createGroup(command());

		assertThat(repository.attempts()).isEqualTo(1);
		assertThat(repository.savedGroup()).isSameAs(result.group());
		assertThat(repository.savedOwnerMember()).isSameAs(result.ownerMember());
		assertThat(result.group().createdBy()).isEqualTo(USER_ID);
		assertThat(result.group().status()).isEqualTo(StudyGroupStatus.ONBOARDING);
		assertThat(result.group().inviteCode()).isEqualTo("INVITE-0001");
		assertThat(result.group().onboardingStartedAt()).contains(NOW);
		assertThat(result.ownerMember().groupId()).isEqualTo(result.group().id());
		assertThat(result.ownerMember().userId()).isEqualTo(USER_ID);
		assertThat(result.ownerMember().permission()).isEqualTo(GroupMemberPermission.OWNER);
		assertThat(result.ownerMember().status()).isEqualTo(GroupMemberStatus.PENDING_ONBOARDING);
	}

	@Test
	void createGroupPublishesOnboardingNotificationForOwner() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		CapturingNotificationPublisher notifications = new CapturingNotificationPublisher();
		StudyGroupService service = service(repository, List.of("INVITE-0001"), notifications, GROUP_ID, OWNER_MEMBER_ID);

		StudyGroupCreationResult result = service.createGroup(command());

		assertThat(notifications.onboardingRequests)
			.containsExactly(new OnboardingRequest(result.group().id(), result.ownerMember().userId()));
	}

	@Test
	void createGroupRetriesWhenGeneratedInviteCodeAlreadyExists() {
		CapturingRepository repository = new CapturingRepository(Set.of("DUPLICATE"));
		StudyGroupService service = service(
			repository,
			List.of("DUPLICATE", "INVITE-0002"),
			GROUP_ID,
			OWNER_MEMBER_ID,
			RETRY_GROUP_ID,
			RETRY_OWNER_MEMBER_ID
		);

		StudyGroupCreationResult result = service.createGroup(command());

		assertThat(repository.attempts()).isEqualTo(2);
		assertThat(result.group().id()).isEqualTo(RETRY_GROUP_ID);
		assertThat(result.ownerMember().id()).isEqualTo(RETRY_OWNER_MEMBER_ID);
		assertThat(result.group().inviteCode()).isEqualTo("INVITE-0002");
	}

	@Test
	void createGroupFailsAfterInviteCodeRetryBudgetIsExhausted() {
		CapturingRepository repository = new CapturingRepository(Set.of("DUPLICATE"));
		StudyGroupService service = service(
			repository,
			List.of("DUPLICATE", "DUPLICATE", "DUPLICATE"),
			GROUP_ID,
			OWNER_MEMBER_ID,
			RETRY_GROUP_ID,
			RETRY_OWNER_MEMBER_ID,
			THIRD_GROUP_ID,
			THIRD_OWNER_MEMBER_ID
		);

		assertThatThrownBy(() -> service.createGroup(command()))
			.isInstanceOf(StudyGroupServiceUnavailableException.class)
			.hasMessage("could not allocate a unique invite code.");
		assertThat(repository.attempts()).isEqualTo(3);
	}

	@Test
	void joinGroupRevertsReadyToStartGroupToOnboarding() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.READY_TO_START, 6, "INVITE-0001");
		repository.currentMemberCount = 1;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001"));

		assertThat(repository.revertedToOnboardingGroupId).isEqualTo(GROUP_ID);
	}

	@Test
	void joinGroupDoesNotRevertOnboardingGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 6, "INVITE-0001");
		repository.currentMemberCount = 1;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001"));

		assertThat(repository.revertedToOnboardingGroupId).isNull();
	}

	@Test
	void joinGroupPersistsPendingMemberWhenInviteCodeMatchesAndCapacityAvailable() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");
		repository.currentMemberCount = 1;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		StudyGroupJoinResult result = service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, " INVITE-0001 "));

		assertThat(result.member()).isSameAs(repository.savedJoinedMember());
		assertThat(result.member().id()).isEqualTo(JOINED_MEMBER_ID);
		assertThat(result.member().groupId()).isEqualTo(GROUP_ID);
		assertThat(result.member().userId()).isEqualTo(JOINER_ID);
		assertThat(result.member().permission()).isEqualTo(GroupMemberPermission.MEMBER);
		assertThat(result.member().status()).isEqualTo(GroupMemberStatus.PENDING_ONBOARDING);
		assertThat(result.member().joinedAt()).isEqualTo(NOW);
	}

	@Test
	void joinGroupPublishesOnboardingNotificationForJoinedMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");
		repository.currentMemberCount = 1;
		CapturingNotificationPublisher notifications = new CapturingNotificationPublisher();
		StudyGroupService service = service(repository, List.of("UNUSED"), notifications, JOINED_MEMBER_ID);

		StudyGroupJoinResult result = service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001"));

		assertThat(notifications.onboardingRequests)
			.containsExactly(new OnboardingRequest(result.member().groupId(), result.member().userId()));
	}

	@Test
	void notificationFailureDoesNotBreakGroupCreation() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(
			repository,
			List.of("INVITE-0001"),
			new ThrowingNotificationPublisher(),
			GROUP_ID,
			OWNER_MEMBER_ID
		);

		StudyGroupCreationResult result = service.createGroup(command());

		assertThat(result.group()).isSameAs(repository.savedGroup());
		assertThat(repository.savedOwnerMember()).isNotNull();
	}

	@Test
	void joinGroupRejectsMissingGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001")))
			.isInstanceOf(StudyGroupNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void joinGroupRejectsMismatchedInviteCode() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "WRONG-CODE")))
			.isInstanceOf(StudyGroupJoinRejectedException.class)
			.hasMessage("invite code does not match the study group.");
		assertThat(repository.savedJoinedMember()).isNull();
	}

	@Test
	void joinGroupAcceptsActiveGroupForLateJoiner() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ACTIVE, 3, "INVITE-0001");
		repository.currentMemberCount = 1;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		StudyGroupJoinResult result = service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001"));

		assertThat(result.member()).isSameAs(repository.savedJoinedMember());
		assertThat(result.member().id()).isEqualTo(JOINED_MEMBER_ID);
		assertThat(result.member().groupId()).isEqualTo(GROUP_ID);
		assertThat(result.member().userId()).isEqualTo(JOINER_ID);
		assertThat(result.member().permission()).isEqualTo(GroupMemberPermission.MEMBER);
		assertThat(result.member().status()).isEqualTo(GroupMemberStatus.PENDING_ONBOARDING);
		assertThat(result.member().joinedAt()).isEqualTo(NOW);
	}

	@Test
	void joinGroupRejectsCompletedGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.COMPLETED, 3, "INVITE-0001");
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001")))
			.isInstanceOf(StudyGroupJoinRejectedException.class)
			.hasMessage("study group is not accepting new members.");
		assertThat(repository.savedJoinedMember()).isNull();
	}

	@Test
	void joinGroupRejectsDuplicateActiveOrOnboardingMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");
		repository.existingActiveOrOnboardingMember = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001")))
			.isInstanceOf(StudyGroupJoinRejectedException.class)
			.hasMessage("user is already a member of this study group.");
		assertThat(repository.savedJoinedMember()).isNull();
	}

	@Test
	void joinGroupRejectsWhenCapacityIsFull() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 2, "INVITE-0001");
		repository.currentMemberCount = 2;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001")))
			.isInstanceOf(StudyGroupJoinRejectedException.class)
			.hasMessage("study group member capacity has been reached.");
		assertThat(repository.savedJoinedMember()).isNull();
	}

	@Test
	void joinGroupTranslatesRacingDuplicateMembershipConflict() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.joinTarget = new StudyGroupJoinTarget(GROUP_ID, StudyGroupStatus.ONBOARDING, 3, "INVITE-0001");
		repository.throwDuplicateMembershipOnJoin = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), JOINED_MEMBER_ID);

		assertThatThrownBy(() -> service.joinGroup(new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, "INVITE-0001")))
			.isInstanceOf(StudyGroupJoinRejectedException.class)
			.hasMessage("user is already a member of this study group.");
	}

	@Test
	void listMyGroupsReturnsCurrentUserGroupsFromRepository() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroup listedGroup = group();
		repository.listedGroups = List.of(listedGroup);
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		List<StudyGroup> result = service.listMyGroups(new ListStudyGroupsQuery(USER_ID));

		assertThat(repository.listRequestedUserId).isEqualTo(USER_ID);
		assertThat(result).containsExactly(listedGroup);
	}

	@Test
	void listMyGroupsReturnsEmptyListWhenUserHasNoCurrentMemberships() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		List<StudyGroup> result = service.listMyGroups(new ListStudyGroupsQuery(USER_ID));

		assertThat(repository.listRequestedUserId).isEqualTo(USER_ID);
		assertThat(result).isEmpty();
	}

	@Test
	void listMyGroupsFiltersByStatus() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroup onboarding = groupWithStatus("온보딩 그룹", StudyGroupStatus.ONBOARDING);
		StudyGroup active = groupWithStatus("진행중 그룹", StudyGroupStatus.ACTIVE);
		repository.listedGroups = List.of(onboarding, active);
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		List<StudyGroup> result = service.listMyGroups(
			new ListStudyGroupsQuery(USER_ID, StudyGroupStatus.ACTIVE, null, null, null)
		);

		assertThat(result).containsExactly(active);
	}

	@Test
	void listMyGroupsSortsByNameDescending() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroup alpha = groupWithStatus("AAA", StudyGroupStatus.ACTIVE);
		StudyGroup zeta = groupWithStatus("ZZZ", StudyGroupStatus.ACTIVE);
		repository.listedGroups = List.of(alpha, zeta);
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		List<StudyGroup> result = service.listMyGroups(
			new ListStudyGroupsQuery(USER_ID, null, null, "name", "desc")
		);

		assertThat(result).containsExactly(zeta, alpha);
	}

	@Test
	void getGroupReturnsCurrentMemberGroupFromRepository() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroup group = group();
		repository.readGroup = group;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		StudyGroup result = service.getGroup(new GetStudyGroupQuery(USER_ID, GROUP_ID));

		assertThat(repository.getRequestedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.getRequestedUserId).isEqualTo(USER_ID);
		assertThat(result).isSameAs(group);
	}

	@Test
	void getGroupRejectsMissingGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.getGroup(new GetStudyGroupQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupNotFoundException.class)
			.hasMessage("study group was not found.");
		assertThat(repository.existsRequestedGroupId).isEqualTo(GROUP_ID);
	}

	@Test
	void getGroupRejectsExistingGroupWhenUserIsNotMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.groupExists = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.getGroup(new GetStudyGroupQuery(JOINER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
		assertThat(repository.getRequestedUserId).isEqualTo(JOINER_ID);
		assertThat(repository.existsRequestedGroupId).isEqualTo(GROUP_ID);
	}

	@Test
	void deleteGroupSoftDeletesForOwner() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		service.deleteGroup(new DeleteStudyGroupCommand(USER_ID, GROUP_ID));

		assertThat(repository.softDeletedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.softDeletedAt).isEqualTo(NOW);
	}

	@Test
	void deleteGroupNotifiesOtherMembers() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		GroupMemberSummary owner = new GroupMemberSummary(
			OWNER_MEMBER_ID, GROUP_ID, USER_ID,
			GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE,
			"현우", "hyunwoo", "hyunwoo@example.com", "SUBMITTED"
		);
		GroupMemberSummary joiner = new GroupMemberSummary(
			JOINED_MEMBER_ID, GROUP_ID, JOINER_ID,
			GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE,
			"민수", "minsu", "minsu@example.com", "SUBMITTED"
		);
		repository.groupMembers = List.of(owner, joiner);
		CapturingNotificationPublisher notifications = new CapturingNotificationPublisher();
		StudyGroupService service = service(repository, List.of("UNUSED"), notifications, GROUP_ID, OWNER_MEMBER_ID);

		service.deleteGroup(new DeleteStudyGroupCommand(USER_ID, GROUP_ID));

		// 그룹장(USER_ID) 은 제외하고 다른 멤버(JOINER_ID) 에게만 삭제 알림이 간다.
		assertThat(notifications.groupDeletedRecipients).containsExactly(JOINER_ID);
	}

	@Test
	void deleteGroupRejectsNonOwnerMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.deleteGroup(new DeleteStudyGroupCommand(JOINER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("only the study group owner can delete the study group.");
		assertThat(repository.softDeletedGroupId).isNull();
	}

	@Test
	void deleteGroupRejectsNonMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.groupExists = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.deleteGroup(new DeleteStudyGroupCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
		assertThat(repository.softDeletedGroupId).isNull();
	}

	@Test
	void deleteGroupReturnsNotFoundWhenGroupMissing() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.deleteGroup(new DeleteStudyGroupCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void updateGroupUpdatesForOwner() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		repository.currentMemberCount = 1;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		StudyGroup result = service.updateGroup(new UpdateStudyGroupCommand(
			USER_ID, GROUP_ID, "새 스터디 이름", "Spring", List.of("JPA", "Security"),
			6, LocalDate.parse("2026-05-10"), LocalDate.parse("2026-06-21"), "수정된 소개"
		));

		assertThat(result.name()).isEqualTo("새 스터디 이름");
		assertThat(result.detailKeywords()).containsExactly("JPA", "Security");
		assertThat(repository.updatedGroup).isNotNull();
		assertThat(repository.updatedGroup.name()).isEqualTo("새 스터디 이름");
	}

	@Test
	void updateGroupRejectsNonOwnerMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.updateGroup(new UpdateStudyGroupCommand(
			JOINER_ID, GROUP_ID, "이름", "Spring", List.of("JPA"),
			6, LocalDate.parse("2026-05-10"), LocalDate.parse("2026-06-21"), null
		)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("only the study group owner can update the study group.");
		assertThat(repository.updatedGroup).isNull();
	}

	@Test
	void updateGroupRejectsMaxMembersBelowCurrentCount() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.readGroup = group();
		repository.currentMemberCount = 5;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.updateGroup(new UpdateStudyGroupCommand(
			USER_ID, GROUP_ID, "이름", "Spring", List.of("JPA"),
			2, LocalDate.parse("2026-05-10"), LocalDate.parse("2026-06-21"), null
		)))
			.isInstanceOf(InvalidStudyGroupMemberProfileRequestException.class);
		assertThat(repository.updatedGroup).isNull();
	}

	@Test
	void getMyGroupMemberProfileReturnsRepositoryProfile() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupMemberProfile profile = profile("현우");
		repository.profile = profile;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		StudyGroupMemberProfile result = service.getMyGroupMemberProfile(new GetMyGroupMemberProfileQuery(USER_ID, GROUP_ID));

		assertThat(repository.profileRequestedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.profileRequestedUserId).isEqualTo(USER_ID);
		assertThat(result).isSameAs(profile);
	}

	@Test
	void getMyGroupMemberProfileRejectsMissingGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.getMyGroupMemberProfile(new GetMyGroupMemberProfileQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupNotFoundException.class)
			.hasMessage("study group was not found.");
		assertThat(repository.existsRequestedGroupId).isEqualTo(GROUP_ID);
	}

	@Test
	void getMyGroupMemberProfileRejectsExistingGroupWhenUserIsNotCurrentMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.groupExists = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.getMyGroupMemberProfile(new GetMyGroupMemberProfileQuery(JOINER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("authenticated user is not a current member of this study group.");
		assertThat(repository.profileRequestedUserId).isEqualTo(JOINER_ID);
	}

	@Test
	void updateMyGroupMemberProfileUpdatesDisplayNameAndReturnsRefreshedProfile() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.profile = profile("현우2");
		repository.updateProfileSucceeds = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		StudyGroupMemberProfile result = service.updateMyGroupMemberProfile(new UpdateMyGroupMemberProfileCommand(
			USER_ID,
			GROUP_ID,
			" 현우2 "
		));

		assertThat(repository.updatedProfileGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.updatedProfileUserId).isEqualTo(USER_ID);
		assertThat(repository.updatedDisplayName).isEqualTo("현우2");
		assertThat(repository.updatedAt).isEqualTo(NOW);
		assertThat(result.displayName()).isEqualTo("현우2");
	}

	@Test
	void listGroupMembersReturnsMembersForActiveMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.existingActiveOrOnboardingMember = true;
		GroupMemberSummary owner = new GroupMemberSummary(
			OWNER_MEMBER_ID, GROUP_ID, USER_ID,
			GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE,
			"현우", "hyunwoo", "hyunwoo@example.com", "SUBMITTED"
		);
		repository.groupMembers = List.of(owner);
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		List<GroupMemberSummary> result = service.listGroupMembers(new ListGroupMembersQuery(USER_ID, GROUP_ID));

		assertThat(repository.membersRequestedGroupId).isEqualTo(GROUP_ID);
		assertThat(result).containsExactly(owner);
	}

	@Test
	void listGroupMembersRejectsNonMemberOfExistingGroup() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.groupExists = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.listGroupMembers(new ListGroupMembersQuery(JOINER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void listGroupMembersReturnsNotFoundWhenGroupMissing() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.listGroupMembers(new ListGroupMembersQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(StudyGroupNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void updateMyGroupMemberProfileRejectsBlankDisplayName() {
		assertThatThrownBy(() -> new UpdateMyGroupMemberProfileCommand(USER_ID, GROUP_ID, " "))
			.isInstanceOf(InvalidStudyGroupMemberProfileRequestException.class)
			.hasMessage("displayName must not be blank.");
	}

	@Test
	void taskCompletionSummaryAllowsUnattemptedTasksButRejectsImpossibleCounts() {
		StudyGroupMemberProfile.TaskCompletionSummary summary =
			new StudyGroupMemberProfile.TaskCompletionSummary(5, 2, 1, 1);

		assertThat(summary.totalCount()).isEqualTo(5);
		assertThatThrownBy(() -> new StudyGroupMemberProfile.TaskCompletionSummary(3, 2, 1, 1))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("status counts must not exceed totalCount.");
	}

	@Test
	void updateMyGroupMemberProfileRejectsExistingGroupWhenUserIsNotCurrentMember() {
		CapturingRepository repository = new CapturingRepository(Set.of());
		repository.groupExists = true;
		StudyGroupService service = service(repository, List.of("UNUSED"), GROUP_ID, OWNER_MEMBER_ID);

		assertThatThrownBy(() -> service.updateMyGroupMemberProfile(new UpdateMyGroupMemberProfileCommand(
				JOINER_ID,
				GROUP_ID,
				"민수"
			)))
			.isInstanceOf(StudyGroupAccessDeniedException.class)
			.hasMessage("authenticated user is not a current member of this study group.");
	}

	@Test
	void listQueryRejectsMissingAuthenticatedUserId() {
		assertThatThrownBy(() -> new ListStudyGroupsQuery(null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage("authenticatedUserId must not be null");
	}

	@Test
	void createCommandRejectsEndDateBeforeStartDate() {
		assertThatThrownBy(() -> new CreateStudyGroupCommand(
				USER_ID,
				"Backend Interview Study",
				"Spring Boot",
				List.of("JPA", "Security"),
				6,
				LocalDate.parse("2026-06-21"),
				LocalDate.parse("2026-05-10"),
				null
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("endsAt must be on or after startsAt");
	}

	@Test
	void joinCommandRejectsBlankInviteCode() {
		assertThatThrownBy(() -> new JoinStudyGroupCommand(JOINER_ID, GROUP_ID, " "))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("inviteCode must not be blank");
	}

	private static StudyGroupService service(CapturingRepository repository, List<String> inviteCodes, UUID... ids) {
		return service(repository, inviteCodes, NotificationEventPublisher.noop(), ids);
	}

	private static StudyGroupService service(
		CapturingRepository repository,
		List<String> inviteCodes,
		NotificationEventPublisher notificationEvents,
		UUID... ids
	) {
		Queue<String> codes = new ArrayDeque<>(inviteCodes);
		Queue<UUID> uuidQueue = new ArrayDeque<>(List.of(ids));
		Supplier<UUID> idGenerator = () -> {
			UUID id = uuidQueue.poll();
			if (id == null) {
				throw new IllegalStateException("No test UUID left.");
			}
			return id;
		};
		return new StudyGroupService(
			repository,
			CLOCK,
			idGenerator,
			() -> {
				String code = codes.poll();
				if (code == null) {
					throw new IllegalStateException("No test invite code left.");
				}
				return code;
			},
			3,
			notificationEvents
		);
	}

	private static CreateStudyGroupCommand command() {
		return new CreateStudyGroupCommand(
			USER_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			6,
			LocalDate.parse("2026-05-10"),
			LocalDate.parse("2026-06-21"),
			"Weekly backend interview prep"
		);
	}

	private static StudyGroup groupWithStatus(String name, StudyGroupStatus status) {
		return StudyGroup.rehydrate(
			UUID.randomUUID(),
			USER_ID,
			name,
			"Spring Boot",
			List.of("JPA"),
			status,
			6,
			false,
			"INVITE-" + name,
			LocalDate.parse("2026-05-10"),
			LocalDate.parse("2026-06-21"),
			null,
			NOW,
			status == StudyGroupStatus.ACTIVE ? NOW : null,
			NOW,
			NOW
		);
	}

	private static StudyGroup group() {
		return StudyGroup.create(
			GROUP_ID,
			USER_ID,
			"Backend Interview Study",
			"Spring Boot",
			List.of("JPA", "Security"),
			6,
			LocalDate.parse("2026-05-10"),
			LocalDate.parse("2026-06-21"),
			"Weekly backend interview prep",
			"INVITE-0001",
			NOW
		);
	}

	private static StudyGroupMemberProfile profile(String displayName) {
		return new StudyGroupMemberProfile(
			GROUP_ID,
			OWNER_MEMBER_ID,
			USER_ID,
			displayName,
			GroupMemberPermission.OWNER,
			GroupMemberStatus.ACTIVE,
			new StudyGroupMemberProfile.OnboardingSummary(true, 3, Instant.parse("2026-05-10T01:00:00Z")),
			new StudyGroupMemberProfile.CurrentWeekSummary(
				WEEK_ID,
				2,
				"JPA 실습",
				Instant.parse("2026-05-17T00:00:00Z"),
				Instant.parse("2026-05-24T00:00:00Z"),
				MemberWeekProgressStatus.IN_PROGRESS
			),
			new StudyGroupMemberProfile.TaskCompletionSummary(4, 2, 1, 1),
			new StudyGroupMemberProfile.RetrospectiveSummary(true)
		);
	}

	private static final class CapturingRepository implements StudyGroupRepository {

		private final Set<String> collidingCodes;
		private final AtomicInteger attempts = new AtomicInteger();
		private StudyGroupJoinTarget joinTarget;
		private int currentMemberCount;
		private boolean existingActiveOrOnboardingMember;
		private UUID ownerUserId;
		private UUID revertedToOnboardingGroupId;
		private boolean throwDuplicateMembershipOnJoin;
		private boolean groupExists;
		private UUID getRequestedGroupId;
		private UUID getRequestedUserId;
		private UUID existsRequestedGroupId;
		private UUID listRequestedUserId;
		private UUID profileRequestedGroupId;
		private UUID profileRequestedUserId;
		private UUID updatedProfileGroupId;
		private UUID updatedProfileUserId;
		private String updatedDisplayName;
		private Instant updatedAt;
		private StudyGroup readGroup;
		private StudyGroupMemberProfile profile;
		private UUID membersRequestedGroupId;
		private List<GroupMemberSummary> groupMembers = List.of();
		private List<StudyGroup> listedGroups = List.of();
		private StudyGroup savedGroup;
		private GroupMember savedOwnerMember;
		private GroupMember savedJoinedMember;
		private boolean updateProfileSucceeds;
		private boolean softDeleteSucceeds = true;
		private UUID softDeletedGroupId;
		private Instant softDeletedAt;
		private boolean updateGroupSucceeds = true;
		private StudyGroup updatedGroup;

		private CapturingRepository(Set<String> collidingCodes) {
			this.collidingCodes = collidingCodes;
		}

		@Override
		public void saveCreatedGroup(StudyGroup group, GroupMember ownerMember) {
			attempts.incrementAndGet();
			if (collidingCodes.contains(group.inviteCode())) {
				throw new StudyGroupInviteCodeConflictException("invite code already exists.");
			}
			this.savedGroup = group;
			this.savedOwnerMember = ownerMember;
		}

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			this.existsRequestedGroupId = groupId;
			return groupExists;
		}

		@Override
		public java.util.Optional<StudyGroup> findGroupByIdForMemberUserId(UUID groupId, UUID userId) {
			this.getRequestedGroupId = groupId;
			this.getRequestedUserId = userId;
			return java.util.Optional.ofNullable(readGroup);
		}

		@Override
		public java.util.Optional<StudyGroupJoinTarget> findJoinTargetByIdForUpdate(UUID groupId) {
			if (joinTarget == null) {
				return java.util.Optional.empty();
			}
			return java.util.Optional.of(joinTarget);
		}

		@Override
		public java.util.Optional<StudyGroupJoinTarget> findJoinTargetByInviteCode(String inviteCode) {
			return java.util.Optional.ofNullable(joinTarget);
		}

		@Override
		public java.util.Optional<UUID> findOwnerUserId(UUID groupId) {
			return java.util.Optional.ofNullable(ownerUserId);
		}

		@Override
		public boolean revertReadyToStartToOnboarding(UUID groupId, Instant updatedAt) {
			this.revertedToOnboardingGroupId = groupId;
			return true;
		}

		@Override
		public boolean existsActiveOrOnboardingMember(UUID groupId, UUID userId) {
			return existingActiveOrOnboardingMember;
		}

		@Override
		public int countActiveOrOnboardingMembers(UUID groupId) {
			return currentMemberCount;
		}

		@Override
		public void saveJoinedMember(GroupMember member) {
			if (throwDuplicateMembershipOnJoin) {
				throw new GroupMemberDuplicateMembershipException("group member already exists.");
			}
			this.savedJoinedMember = member;
		}

		@Override
		public List<StudyGroup> findGroupsByMemberUserId(UUID userId) {
			this.listRequestedUserId = userId;
			return listedGroups;
		}

		@Override
		public java.util.Optional<StudyGroupMemberProfile> findMyGroupMemberProfile(UUID groupId, UUID userId) {
			this.profileRequestedGroupId = groupId;
			this.profileRequestedUserId = userId;
			return java.util.Optional.ofNullable(profile);
		}

		@Override
		public List<GroupMemberSummary> findGroupMembers(UUID groupId) {
			this.membersRequestedGroupId = groupId;
			return groupMembers;
		}

		@Override
		public boolean updateMyGroupMemberDisplayName(UUID groupId, UUID userId, String displayName, Instant updatedAt) {
			this.updatedProfileGroupId = groupId;
			this.updatedProfileUserId = userId;
			this.updatedDisplayName = displayName;
			this.updatedAt = updatedAt;
			return updateProfileSucceeds;
		}

		@Override
		public boolean softDeleteGroup(UUID groupId, Instant deletedAt) {
			this.softDeletedGroupId = groupId;
			this.softDeletedAt = deletedAt;
			return softDeleteSucceeds;
		}

		@Override
		public boolean updateGroup(StudyGroup group) {
			this.updatedGroup = group;
			return updateGroupSucceeds;
		}

		int attempts() {
			return attempts.get();
		}

		StudyGroup savedGroup() {
			return savedGroup;
		}

		GroupMember savedOwnerMember() {
			return savedOwnerMember;
		}

		GroupMember savedJoinedMember() {
			return savedJoinedMember;
		}
	}

	private record OnboardingRequest(UUID groupId, UUID recipientUserId) {
	}

	private static final class CapturingNotificationPublisher implements NotificationEventPublisher {

		private final List<OnboardingRequest> onboardingRequests = new ArrayList<>();
		private final List<UUID> memberJoinedRecipients = new ArrayList<>();
		private final List<UUID> groupDeletedRecipients = new ArrayList<>();

		@Override
		public void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
			groupDeletedRecipients.add(recipientUserId);
		}

		@Override
		public void publishNoticePosted(UUID groupId, UUID actorUserId, UUID postId, String title) {
		}

		@Override
		public void publishLeaderReportPosted(UUID groupId, UUID postId, String title) {
		}

		@Override
		public void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
		}

		@Override
		public void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
			memberJoinedRecipients.add(ownerUserId);
		}

		@Override
		public void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		}

		@Override
		public void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		}

		@Override
		public void publishOnboardingRequested(UUID groupId, UUID recipientUserId) {
			onboardingRequests.add(new OnboardingRequest(groupId, recipientUserId));
		}

		@Override
		public void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
		}

		@Override
		public void publishTaskDueReminder(
			UUID groupId,
			UUID recipientUserId,
			UUID weekId,
			UUID taskCompletionId,
			String taskTitle,
			Instant dueAt
		) {
		}

		@Override
		public void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}

		@Override
		public void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}
	}

	private static final class ThrowingNotificationPublisher implements NotificationEventPublisher {

		@Override
		public void publishOnboardingRequested(UUID groupId, UUID recipientUserId) {
			throw new IllegalStateException("notification backend unavailable");
		}

		@Override
		public void publishGroupDeleted(UUID groupId, UUID recipientUserId, String groupName) {
		}

		@Override
		public void publishNoticePosted(UUID groupId, UUID actorUserId, UUID postId, String title) {
		}

		@Override
		public void publishLeaderReportPosted(UUID groupId, UUID postId, String title) {
		}

		@Override
		public void publishOnboardingCompleted(UUID groupId, UUID ownerUserId) {
		}

		@Override
		public void publishMemberJoined(UUID groupId, UUID ownerUserId, UUID joinedUserId) {
			throw new IllegalStateException("notification backend unavailable");
		}

		@Override
		public void publishOnboardingSubmitted(UUID groupId, UUID recipientUserId, UUID submitterMemberId) {
		}

		@Override
		public void publishRetrospectiveReminder(UUID groupId, UUID recipientUserId, UUID weekId) {
		}

		@Override
		public void publishWeekStarted(UUID groupId, UUID weekId, int weekNumber, String weekTitle) {
		}

		@Override
		public void publishTaskDueReminder(
			UUID groupId,
			UUID recipientUserId,
			UUID weekId,
			UUID taskCompletionId,
			String taskTitle,
			Instant dueAt
		) {
		}

		@Override
		public void publishTaskOverdueCheck(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishIncompleteReasonRequested(UUID groupId, UUID recipientUserId, UUID taskCompletionId, String taskTitle) {
		}

		@Override
		public void publishRetrospectiveReady(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}

		@Override
		public void publishNextWeekAdjusted(UUID groupId, UUID recipientUserId, UUID retrospectiveId, UUID weekId) {
		}
	}
}
