package com.studypot.aistudyleader.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class OAuthAccountTest {

	private static final UUID ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000101");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000102");
	private static final Instant NOW = Instant.parse("2026-05-07T04:00:00Z");

	@Test
	void accountsCompareByProviderIdentity() {
		OAuthAccount first = account(ACCOUNT_ID, "google-123");
		OAuthAccount second = OAuthAccount.rehydrate(
			ACCOUNT_ID,
			UUID.fromString("018f0000-0000-7000-8000-000000000103"),
			OAuthProvider.GOOGLE,
			"google-456",
			EmailAddress.from("other@example.com"),
			NOW.plus(Duration.ofHours(2)),
			"openid",
			NOW,
			NOW,
			null
		);

		assertThat(first).isEqualTo(second);
		assertThat(first).hasSameHashCodeAs(second);
		assertThat(first).isNotEqualTo(account(UUID.fromString("018f0000-0000-7000-8000-000000000104"), "google-123"));
	}

	@Test
	void syncRejectsSoftDeletedAccount() {
		OAuthAccount deleted = OAuthAccount.rehydrate(
			ACCOUNT_ID,
			USER_ID,
			OAuthProvider.GOOGLE,
			"google-123",
			EmailAddress.from("member@example.com"),
			NOW.plus(Duration.ofHours(1)),
			"openid email profile",
			NOW,
			NOW,
			NOW.plus(Duration.ofMinutes(1))
		);

		assertThatIllegalStateException()
			.isThrownBy(() -> deleted.sync(
				EmailAddress.from("member@example.com"),
				NOW.plus(Duration.ofHours(2)),
				"openid",
				NOW.plus(Duration.ofMinutes(2))
			))
			.withMessage("deleted OAuth account cannot be synced");
	}

	@Test
	void softDeleteRecordsDeletedAtOnce() {
		OAuthAccount active = account(ACCOUNT_ID, "google-123");
		Instant deletedAt = NOW.plus(Duration.ofMinutes(3));

		OAuthAccount deleted = active.softDelete(deletedAt);

		assertThat(deleted.deletedAt()).contains(deletedAt);
		assertThat(deleted.id()).isEqualTo(active.id());
		assertThat(deleted.userId()).isEqualTo(active.userId());
		assertThat(deleted.providerUserId()).isEqualTo(active.providerUserId());
		assertThatIllegalStateException()
			.isThrownBy(() -> deleted.softDelete(deletedAt.plus(Duration.ofSeconds(1))))
			.withMessage("OAuth account is already deleted");
	}

	@Test
	void softDeleteRequiresTimestamp() {
		assertThatNullPointerException()
			.isThrownBy(() -> account(ACCOUNT_ID, "google-123").softDelete(null))
			.withMessage("now must not be null");
	}

	@Test
	void providerLiveKeyRejectsColonInProviderUserId() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> OAuthProvider.GOOGLE.liveKey("google:123"))
			.withMessage("providerUserId must not contain ':'");
	}

	@Test
	void providerFromPersistenceRejectsUnsupportedValues() {
		assertThat(OAuthProvider.fromPersistence(" GOOGLE ")).isEqualTo(OAuthProvider.GOOGLE);

		assertThatIllegalStateException()
			.isThrownBy(() -> OAuthProvider.fromPersistence("NAVER"))
			.withMessage("Unsupported OAuth provider from persistence: NAVER");
	}

	private static OAuthAccount account(UUID id, String providerUserId) {
		return OAuthAccount.connect(
			id,
			USER_ID,
			OAuthProvider.GOOGLE,
			providerUserId,
			EmailAddress.from("member@example.com"),
			NOW.plus(Duration.ofHours(1)),
			"openid email profile",
			NOW
		);
	}
}
