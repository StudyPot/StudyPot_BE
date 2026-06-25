package com.studypot.aistudyleader.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.llm.admin.AdminLlmUsageFilter;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageRow;
import com.studypot.aistudyleader.llm.admin.AdminLlmUsageSummary;
import com.studypot.aistudyleader.llm.domain.LlmProvider;
import com.studypot.aistudyleader.llm.domain.LlmUsage;
import com.studypot.aistudyleader.llm.domain.LlmUsageAccessContext;
import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import com.studypot.aistudyleader.llm.domain.LlmUsageStatus;
import com.studypot.aistudyleader.llm.repository.LlmUsageRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberPermission;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LlmUsageServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007101");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000007102");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000007103");
	private static final UUID USAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000007104");
	private static final Instant NOW = Instant.parse("2026-05-13T02:20:00Z");

	@Test
	void listGroupUsageReturnsOwnerVisibleRecords() {
		FakeRepository repository = new FakeRepository();
		LlmUsage usage = usage();
		repository.groupUsage = List.of(usage);
		LlmUsageService service = new LlmUsageService(repository);

		List<LlmUsage> result = service.listGroupUsage(new ListGroupLlmUsageQuery(USER_ID, GROUP_ID));

		assertThat(result).containsExactly(usage);
		assertThat(repository.requestedGroupId).isEqualTo(GROUP_ID);
		assertThat(repository.requestedLimit).isEqualTo(100);
	}

	@Test
	void listGroupUsageRejectsNonOwnerAndLeftOwner() {
		FakeRepository repository = new FakeRepository();
		repository.accessContext = access(GroupMemberPermission.MEMBER, GroupMemberStatus.ACTIVE);
		LlmUsageService service = new LlmUsageService(repository);

		assertThatThrownBy(() -> service.listGroupUsage(new ListGroupLlmUsageQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(LlmUsageAccessDeniedException.class)
			.hasMessage("only the study group owner can read LLM usage logs.");

		repository.accessContext = access(GroupMemberPermission.OWNER, GroupMemberStatus.LEFT);
		assertThatThrownBy(() -> service.listGroupUsage(new ListGroupLlmUsageQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(LlmUsageAccessDeniedException.class)
			.hasMessage("only the study group owner can read LLM usage logs.");
	}

	@Test
	void listGroupUsageDistinguishesMissingGroupFromCrossGroupAccess() {
		FakeRepository repository = new FakeRepository();
		repository.accessContext = null;
		repository.groupExists = false;
		LlmUsageService service = new LlmUsageService(repository);

		assertThatThrownBy(() -> service.listGroupUsage(new ListGroupLlmUsageQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(LlmUsageGroupNotFoundException.class)
			.hasMessage("study group was not found.");

		repository.groupExists = true;
		assertThatThrownBy(() -> service.listGroupUsage(new ListGroupLlmUsageQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(LlmUsageAccessDeniedException.class)
			.hasMessage("authenticated user is not an owner of this study group.");
	}

	private static LlmUsageAccessContext access(GroupMemberPermission permission, GroupMemberStatus memberStatus) {
		return new LlmUsageAccessContext(GROUP_ID, MEMBER_ID, StudyGroupStatus.ACTIVE, permission, memberStatus);
	}

	private static LlmUsage usage() {
		return LlmUsage.record(
			USAGE_ID,
			USER_ID,
			GROUP_ID,
			LlmUsagePurpose.CURRICULUM_GENERATE,
			LlmProvider.OPENAI,
			"gpt-4.1-mini",
			300,
			120,
			new BigDecimal("0.004200"),
			900,
			LlmUsageStatus.SUCCESS,
			null,
			Map.of("source", "curriculum_start"),
			"generated curriculum",
			NOW
		);
	}

	private static final class FakeRepository implements LlmUsageRepository {

		private LlmUsageAccessContext accessContext = access(GroupMemberPermission.OWNER, GroupMemberStatus.ACTIVE);
		private boolean groupExists = true;
		private List<LlmUsage> groupUsage = List.of();
		private UUID requestedGroupId;
		private int requestedLimit;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<LlmUsageAccessContext> findAccessContext(UUID groupId, UUID userId) {
			return Optional.ofNullable(accessContext);
		}

		@Override
		public boolean insertLlmUsage(LlmUsage usage) {
			return true;
		}

		@Override
		public List<LlmUsage> findGroupUsage(UUID groupId, int limit) {
			requestedGroupId = groupId;
			requestedLimit = limit;
			return groupUsage;
		}

		@Override
		public List<LlmUsage> findUserUsage(UUID userId, int limit) {
			return List.of();
		}

		@Override
		public Optional<String> findUserEmail(UUID userId) {
			return Optional.empty();
		}

		@Override
		public List<AdminLlmUsageRow> findAdminUsage(AdminLlmUsageFilter filter) {
			return List.of();
		}

		@Override
		public AdminLlmUsageSummary summarizeAdminUsage(AdminLlmUsageFilter filter) {
			return AdminLlmUsageSummary.EMPTY;
		}
	}
}
