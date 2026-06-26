package com.studypot.aistudyleader.auth.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import com.studypot.aistudyleader.auth.repository.AuthAccountRepository;
import com.studypot.aistudyleader.llm.admin.AdminProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminUserServiceTest {

	private static final Instant NOW = Instant.parse("2026-06-26T00:00:00Z");
	private static final UUID ADMIN_ID = UUID.fromString("018f6f55-0000-7000-9000-0000000000a1");
	private static final UUID MEMBER_ID = UUID.fromString("018f6f55-0000-7000-9000-0000000000b2");

	private final FakeAuthAccountRepository repository = new FakeAuthAccountRepository();
	private final AdminUserService service = new AdminUserService(
		repository,
		new AdminProperties(Set.of("admin@studypot.dev")),
		Clock.fixed(NOW, ZoneOffset.UTC)
	);

	AdminUserServiceTest() {
		repository.add(AuthUser.create(ADMIN_ID, EmailAddress.from("admin@studypot.dev"), "관리자", null, NOW), "FREE");
		repository.add(AuthUser.create(MEMBER_ID, EmailAddress.from("member@studypot.dev"), "회원", null, NOW), "FREE");
	}

	@Test
	void findByEmailReturnsUserWithPlanForAdmin() {
		AdminUserView view = service.findByEmail(ADMIN_ID, "member@studypot.dev");

		assertThat(view.id()).isEqualTo(MEMBER_ID);
		assertThat(view.email()).isEqualTo("member@studypot.dev");
		assertThat(view.nickname()).isEqualTo("회원");
		assertThat(view.plan()).isEqualTo("FREE");
	}

	@Test
	void findByEmailRejectsNonAdminRequester() {
		assertThatThrownBy(() -> service.findByEmail(MEMBER_ID, "member@studypot.dev"))
			.isInstanceOf(AdminUserAccessDeniedException.class);
	}

	@Test
	void findByEmailThrowsWhenUserMissing() {
		assertThatThrownBy(() -> service.findByEmail(ADMIN_ID, "ghost@studypot.dev"))
			.isInstanceOf(AdminUserNotFoundException.class);
	}

	@Test
	void changePlanUpgradesTargetUser() {
		AdminUserView view = service.changePlan(ADMIN_ID, MEMBER_ID, AdminUserPlan.PREMIUM);

		assertThat(view.plan()).isEqualTo("PREMIUM");
		assertThat(repository.findPlan(MEMBER_ID)).contains("PREMIUM");
	}

	@Test
	void changePlanRejectsNonAdminRequester() {
		assertThatThrownBy(() -> service.changePlan(MEMBER_ID, MEMBER_ID, AdminUserPlan.PREMIUM))
			.isInstanceOf(AdminUserAccessDeniedException.class);
		assertThat(repository.findPlan(MEMBER_ID)).contains("FREE");
	}

	@Test
	void changePlanThrowsWhenTargetMissing() {
		UUID unknown = UUID.fromString("018f6f55-0000-7000-9000-0000000000c3");

		assertThatThrownBy(() -> service.changePlan(ADMIN_ID, unknown, AdminUserPlan.PREMIUM))
			.isInstanceOf(AdminUserNotFoundException.class);
	}

	private static final class FakeAuthAccountRepository implements AuthAccountRepository {

		private final Map<UUID, AuthUser> usersById = new HashMap<>();
		private final Map<String, UUID> idByEmailLiveKey = new HashMap<>();
		private final Map<UUID, String> plansById = new HashMap<>();

		void add(AuthUser user, String plan) {
			usersById.put(user.id(), user);
			idByEmailLiveKey.put(user.email().liveKey(), user.id());
			plansById.put(user.id(), plan);
		}

		@Override
		public Optional<AuthUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<AuthUser> findActiveUserByEmail(EmailAddress email) {
			UUID id = idByEmailLiveKey.get(email.liveKey());
			return id == null ? Optional.empty() : Optional.ofNullable(usersById.get(id));
		}

		@Override
		public Optional<String> findPlan(UUID userId) {
			return Optional.ofNullable(plansById.get(userId));
		}

		@Override
		public boolean updatePlan(UUID userId, String plan, Instant updatedAt) {
			if (!usersById.containsKey(userId)) {
				return false;
			}
			plansById.put(userId, plan);
			return true;
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			throw new UnsupportedOperationException();
		}

		@Override
		public AuthUser save(AuthUser user) {
			throw new UnsupportedOperationException();
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean updateProfile(UUID userId, String nickname, String bio, Instant updatedAt) {
			throw new UnsupportedOperationException();
		}
	}
}
