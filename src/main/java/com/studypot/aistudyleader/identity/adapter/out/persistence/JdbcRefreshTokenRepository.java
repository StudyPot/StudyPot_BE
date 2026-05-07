package com.studypot.aistudyleader.identity.adapter.out.persistence;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.identity.application.RefreshTokenRepository;
import com.studypot.aistudyleader.identity.application.RefreshTokenSession;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcRefreshTokenRepository implements RefreshTokenRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<RefreshTokenSession> findByTokenHash(String tokenHash) {
		return queryOne(RefreshTokenJdbcSql.FIND_REFRESH_TOKEN_BY_HASH, this::mapRefreshToken, tokenHash);
	}

	@Override
	public RefreshTokenSession save(RefreshTokenSession session) {
		jdbcTemplate.update(
			RefreshTokenJdbcSql.INSERT_REFRESH_TOKEN,
			UuidBinary.toBytes(session.id()),
			UuidBinary.toBytes(session.userId()),
			session.tokenHash(),
			session.deviceInfo().orElse(null),
			session.ipAddress().orElse(null),
			timestamp(session.expiresAt()),
			timestamp(session.createdAt())
		);
		return session;
	}

	@Override
	public boolean revoke(UUID refreshTokenId, Instant revokedAt) {
		return jdbcTemplate.update(
			RefreshTokenJdbcSql.REVOKE_REFRESH_TOKEN,
			timestamp(revokedAt),
			UuidBinary.toBytes(refreshTokenId)
		) > 0;
	}

	@Override
	public int revokeAllActiveByUserId(UUID userId, Instant revokedAt) {
		return jdbcTemplate.update(
			RefreshTokenJdbcSql.REVOKE_ACTIVE_REFRESH_TOKENS_BY_USER,
			timestamp(revokedAt),
			UuidBinary.toBytes(userId)
		);
	}

	private RefreshTokenSession mapRefreshToken(ResultSet resultSet, int rowNumber) throws SQLException {
		return RefreshTokenSession.rehydrate(
			uuid(resultSet, "id"),
			uuid(resultSet, "user_id"),
			resultSet.getString("token_hash"),
			resultSet.getString("device_info"),
			resultSet.getString("ip_address"),
			instant(resultSet, "expires_at"),
			instant(resultSet, "revoked_at"),
			instant(resultSet, "created_at")
		);
	}

	private <T> Optional<T> queryOne(String sql, ThrowingRowMapper<T> mapper, Object... args) {
		List<T> results = jdbcTemplate.query(sql, (resultSet, rowNumber) -> mapper.map(resultSet, rowNumber), args);
		return results.stream().findFirst();
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
