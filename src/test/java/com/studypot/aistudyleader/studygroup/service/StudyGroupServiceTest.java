package com.studypot.aistudyleader.studygroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.studygroup.domain.GroupMember;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupInviteCodeConflictException;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class StudyGroupServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002821");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002822");
	private static final UUID OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002823");
	private static final UUID RETRY_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002824");
	private static final UUID RETRY_OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002825");
	private static final UUID THIRD_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000002826");
	private static final UUID THIRD_OWNER_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000002827");
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

	private static StudyGroupService service(CapturingRepository repository, List<String> inviteCodes, UUID... ids) {
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
			3
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

	private static final class CapturingRepository implements StudyGroupRepository {

		private final Set<String> collidingCodes;
		private final AtomicInteger attempts = new AtomicInteger();
		private StudyGroup savedGroup;
		private GroupMember savedOwnerMember;

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

		int attempts() {
			return attempts.get();
		}

		StudyGroup savedGroup() {
			return savedGroup;
		}

		GroupMember savedOwnerMember() {
			return savedOwnerMember;
		}
	}
}
