package com.studypot.aistudyleader.auth.repository;

import com.studypot.aistudyleader.global.domain.AuditMetadata;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcAuthAccountRepository implements AuthAccountRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcAuthAccountRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<AuthUser> findActiveUser(UUID userId) {
		return queryOne(AuthJdbcSql.FIND_ACTIVE_USER_BY_ID, this::mapUser, UuidBinary.toBytes(userId));
	}

	@Override
	public Optional<AuthUser> findActiveUserByEmail(EmailAddress email) {
		return queryOne(AuthJdbcSql.FIND_ACTIVE_USER_BY_EMAIL, this::mapUser, email.liveKey());
	}

	@Override
	public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
		return queryOne(AuthJdbcSql.FIND_ACTIVE_OAUTH_ACCOUNT, this::mapOAuthAccount, provider.liveKey(providerUserId));
	}

	@Override
	public AuthUser save(AuthUser user) {
		try {
			int updatedRows = jdbcTemplate.update(
				AuthJdbcSql.UPDATE_ACTIVE_USER,
				user.email().value(),
				user.email().liveKey(),
				user.nickname(),
				user.profileImage().orElse(null),
				timestamp(user.lastLoginAt().orElse(null)),
				timestamp(user.auditMetadata().updatedAt()),
				UuidBinary.toBytes(user.id())
			);
			if (updatedRows == 0) {
				if (exists(AuthJdbcSql.EXISTS_ACTIVE_USER_BY_ID, UuidBinary.toBytes(user.id()))) {
					return user;
				}
				rejectSoftDeletedUserOverwrite(user);
				jdbcTemplate.update(
					AuthJdbcSql.INSERT_USER,
					UuidBinary.toBytes(user.id()),
					user.email().value(),
					user.email().liveKey(),
					user.nickname(),
					user.profileImage().orElse(null),
					timestamp(user.lastLoginAt().orElse(null)),
					timestamp(user.auditMetadata().createdAt()),
					timestamp(user.auditMetadata().updatedAt())
				);
			}
			return user;
		} catch (DuplicateKeyException exception) {
			throw userConflict(user, exception);
		}
	}

	@Override
	public OAuthAccount save(OAuthAccount account) {
		try {
			int updatedRows = jdbcTemplate.update(
				AuthJdbcSql.UPDATE_OAUTH_ACCOUNT_SYNC,
				account.email().map(EmailAddress::value).orElse(null),
				timestamp(account.tokenExpiresAt().orElse(null)),
				account.scope().orElse(null),
				timestamp(account.lastSyncedAt().orElse(null)),
				UuidBinary.toBytes(account.id())
			);
			if (updatedRows == 0) {
				if (exists(AuthJdbcSql.EXISTS_ACTIVE_OAUTH_ACCOUNT_BY_ID, UuidBinary.toBytes(account.id()))) {
					return account;
				}
				rejectSoftDeletedOAuthAccountOverwrite(account);
				jdbcTemplate.update(
					AuthJdbcSql.INSERT_OAUTH_ACCOUNT,
					UuidBinary.toBytes(account.id()),
					UuidBinary.toBytes(account.userId()),
					account.provider().name(),
					account.providerUserId(),
					account.providerAccountLiveKey(),
					account.email().map(EmailAddress::value).orElse(null),
					timestamp(account.tokenExpiresAt().orElse(null)),
					account.scope().orElse(null),
					timestamp(account.connectedAt()),
					timestamp(account.lastSyncedAt().orElse(null))
				);
			}
			return account;
		} catch (DuplicateKeyException exception) {
			throw oauthAccountConflict(account, exception);
		}
	}

	private void rejectSoftDeletedUserOverwrite(AuthUser user) {
		if (exists(AuthJdbcSql.EXISTS_USER_BY_ID, UuidBinary.toBytes(user.id()))) {
			throw new AuthUniquenessConflictException("Auth user is soft-deleted and cannot be overwritten.");
		}
	}

	private void rejectSoftDeletedOAuthAccountOverwrite(OAuthAccount account) {
		if (exists(AuthJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_ID, UuidBinary.toBytes(account.id()))) {
			throw new AuthUniquenessConflictException("OAuth account is soft-deleted and cannot be overwritten.");
		}
	}

	private AuthUniquenessConflictException userConflict(AuthUser user, DuplicateKeyException exception) {
		if (exists(AuthJdbcSql.EXISTS_USER_BY_EMAIL, user.email().liveKey())) {
			return new AuthUniquenessConflictException(
				"Auth user email is already reserved by an existing or soft-deleted user.",
				exception
			);
		}
		return new AuthUniquenessConflictException("Auth user uniqueness conflict.", exception);
	}

	private AuthUniquenessConflictException oauthAccountConflict(OAuthAccount account, DuplicateKeyException exception) {
		if (exists(AuthJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_PROVIDER_KEY, account.providerAccountLiveKey())) {
			return new AuthUniquenessConflictException(
				"OAuth provider account is already reserved by an existing or soft-deleted account.",
				exception
			);
		}
		return new AuthUniquenessConflictException("OAuth account uniqueness conflict.", exception);
	}

	private AuthUser mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
		return AuthUser.rehydrate(
			uuid(resultSet, "id"),
			EmailAddress.from(resultSet.getString("email")),
			resultSet.getString("nickname"),
			resultSet.getString("profile_image"),
			instant(resultSet, "last_login_at"),
			new AuditMetadata(
				instant(resultSet, "created_at"),
				instant(resultSet, "updated_at"),
				instant(resultSet, "deleted_at")
			)
		);
	}

	private OAuthAccount mapOAuthAccount(ResultSet resultSet, int rowNumber) throws SQLException {
		String email = resultSet.getString("email");
		return OAuthAccount.rehydrate(
			uuid(resultSet, "id"),
			uuid(resultSet, "user_id"),
			OAuthProvider.fromPersistence(resultSet.getString("provider")),
			resultSet.getString("provider_user_id"),
			email == null ? null : EmailAddress.from(email),
			instant(resultSet, "token_expires_at"),
			resultSet.getString("scope"),
			instant(resultSet, "connected_at"),
			instant(resultSet, "last_synced_at"),
			instant(resultSet, "deleted_at")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
	}

	private boolean exists(String sql, Object... args) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, args));
	}

	private static UUID uuid(ResultSet resultSet, String column) throws SQLException {
		return UuidBinary.fromBytes(resultSet.getBytes(column));
	}

	private static Instant instant(ResultSet resultSet, String column) throws SQLException {
		Timestamp timestamp = resultSet.getTimestamp(column);
		return timestamp == null ? null : timestamp.toInstant();
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	@FunctionalInterface
	private interface ThrowingRowMapper<T> {

		T map(ResultSet resultSet, int rowNumber) throws SQLException;
	}
}
