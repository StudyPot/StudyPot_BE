package com.studypot.aistudyleader.auth.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAuthAccountRepositoryTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000401");
	private static final UUID ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000402");
	private static final Instant NOW = Instant.parse("2026-05-07T04:00:00Z");

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcAuthAccountRepository repository = new JdbcAuthAccountRepository(jdbcTemplate);

	@Test
	void saveUserDoesNotInsertOverSoftDeletedRowWithSameId() {
		AuthUser user = user();
		when(jdbcTemplate.update(eq(AuthJdbcSql.UPDATE_ACTIVE_USER), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_ACTIVE_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(user))
			.isInstanceOf(AuthUniquenessConflictException.class)
			.hasMessage("Auth user is soft-deleted and cannot be overwritten.");

		verify(jdbcTemplate, never()).update(eq(AuthJdbcSql.INSERT_USER), any(Object[].class));
	}

	@Test
	void saveUserReportsReservedEmailWhenInsertConflictsWithExistingRow() {
		AuthUser user = user();
		when(jdbcTemplate.update(eq(AuthJdbcSql.UPDATE_ACTIVE_USER), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_ACTIVE_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_USER_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.update(eq(AuthJdbcSql.INSERT_USER), any(Object[].class)))
			.thenThrow(new DuplicateKeyException("users_email_live_uidx"));
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_USER_BY_EMAIL), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(user))
			.isInstanceOf(AuthUniquenessConflictException.class)
			.hasMessage("Auth user email is already reserved by an existing or soft-deleted user.");
	}

	@Test
	void saveOAuthAccountDoesNotInsertOverSoftDeletedRowWithSameId() {
		OAuthAccount account = account();
		when(jdbcTemplate.update(eq(AuthJdbcSql.UPDATE_OAUTH_ACCOUNT_SYNC), any(Object[].class)))
			.thenReturn(0);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_ACTIVE_OAUTH_ACCOUNT_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(false);
		when(jdbcTemplate.queryForObject(eq(AuthJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_ID), eq(Boolean.class), any(Object[].class)))
			.thenReturn(true);

		assertThatThrownBy(() -> repository.save(account))
			.isInstanceOf(AuthUniquenessConflictException.class)
			.hasMessage("OAuth account is soft-deleted and cannot be overwritten.");

		verify(jdbcTemplate, never()).update(eq(AuthJdbcSql.INSERT_OAUTH_ACCOUNT), any(Object[].class));
	}

	private static AuthUser user() {
		return AuthUser.create(
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
