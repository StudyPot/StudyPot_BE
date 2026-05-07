package com.studypot.aistudyleader.identity.repository;

final class RefreshTokenJdbcSql {

	static final String FIND_REFRESH_TOKEN_BY_HASH = """
		select id, user_id, token_hash, device_info, ip_address, expires_at, revoked_at, created_at
		from refresh_token
		where token_hash = ?
		""";

	static final String INSERT_REFRESH_TOKEN = """
		insert into refresh_token (
		  id, user_id, token_hash, device_info, ip_address, expires_at, created_at
		) values (?, ?, ?, ?, ?, ?, ?)
		""";

	static final String REVOKE_REFRESH_TOKEN = """
		update refresh_token
		set revoked_at = ?
		where id = ?
		  and revoked_at is null
		""";

	static final String REVOKE_ACTIVE_REFRESH_TOKENS_BY_USER = """
		update refresh_token
		set revoked_at = ?
		where user_id = ?
		  and revoked_at is null
		""";

	private RefreshTokenJdbcSql() {
	}
}
