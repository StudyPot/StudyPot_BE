package com.studypot.aistudyleader.identity.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcIdentityAccountRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000401");
	private static final UUID ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000402");
	private static final Instant NOW = Instant.parse("2026-05-07T04:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcIdentityAccountRepository repository = new JdbcIdentityAccountRepository(jdbcTemplate);

	@Test
	void saveUserDoesNotInsertOverSoftDeletedRowWithSameId() {
		IdentityUser user = user();
		when(jdbcTemplate.update(eq(IdentityJdbcSql.UPDATE_ACTIVE_USER), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_ACTIVE_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(user))
			.isInstanceOf(IdentityUniquenessConflictException.class)
			.hasMessage("Identity user is soft-deleted and cannot be overwritten.");

		verify(jdbcTemplate, never()).update(eq(IdentityJdbcSql.INSERT_USER), any(Object[].class));
	}

	@Test
	void saveUserReportsReservedEmailWhenInsertConflictsWithExistingRow() {
		IdentityUser user = user();
		when(jdbcTemplate.update(eq(IdentityJdbcSql.UPDATE_ACTIVE_USER), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_ACTIVE_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.update(eq(IdentityJdbcSql.INSERT_USER), any(Object[].class)))
			.thenThrow(new DuplicateKeyException("users_email_live_uidx"));
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_USER_BY_EMAIL), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(user))
			.isInstanceOf(IdentityUniquenessConflictException.class)
			.hasMessage("Identity user email is already reserved by an existing or soft-deleted user.");
	}

	@Test
	void saveOAuthAccountDoesNotInsertOverSoftDeletedRowWithSameId() {
		OAuthAccount account = account();
		when(jdbcTemplate.update(eq(IdentityJdbcSql.UPDATE_OAUTH_ACCOUNT_SYNC), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_ACTIVE_OAUTH_ACCOUNT_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(IdentityJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(account))
			.isInstanceOf(IdentityUniquenessConflictException.class)
			.hasMessage("OAuth account is soft-deleted and cannot be overwritten.");

		verify(jdbcTemplate, never()).update(eq(IdentityJdbcSql.INSERT_OAUTH_ACCOUNT), any(Object[].class));
	}

	private static IdentityUser user() {
		return IdentityUser.create(
			USER_ID,
			EmailAddress.from("member@example.com"),
			"Study Member",
			null,
			NOW
		).recordLogin(NOW.plus(Duration.ofMinutes(1)));
	}

	private static OAuthAccount account() {
		return OAuthAccount.connect(
			ACCOUNT_ID,
			USER_ID,
			OAuthProvider.GOOGLE,
			"google-123",
			EmailAddress.from("member@example.com"),
			NOW.plus(Duration.ofHours(1)),
			"openid email profile",
			NOW
		);
	}
}
