package com.studypot.aistudyleader.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class AuthJdbcSqlContractTest {

	@Test
	void activeLookupsUseLiveKeysAndSoftDeleteFilters() {
		assertThat(normalize(AuthJdbcSql.FIND_ACTIVE_USER_BY_ID))
			.contains("from users")
			.contains("where id = ?")
			.contains("deleted_at is null");

		assertThat(normalize(AuthJdbcSql.FIND_ACTIVE_USER_BY_EMAIL))
			.contains("from users")
			.contains("where email_live_key = ?")
			.contains("deleted_at is null");

		assertThat(normalize(AuthJdbcSql.FIND_ACTIVE_OAUTH_ACCOUNT))
			.contains("from oauth_account")
			.contains("where provider_account_live_key = ?")
			.contains("deleted_at is null");
	}

	@Test
	void writesCanonicalLiveKeysWithoutPersistingRawProviderTokens() {
		assertThat(normalize(AuthJdbcSql.INSERT_USER))
			.contains("insert into users")
			.contains("email_live_key")
			.contains("password_hash")
			.contains("bio")
			.contains("interests")
			.contains("skill_level")
			.contains("last_login_at")
			.contains("created_at")
			.contains("updated_at")
			.doesNotContain("deleted_at");

		assertThat(normalize(AuthJdbcSql.INSERT_OAUTH_ACCOUNT))
			.contains("insert into oauth_account")
			.contains("provider_account_live_key")
			.contains("token_expires_at")
			.contains("connected_at")
			.contains("last_synced_at")
			.doesNotContain("access_token_enc")
			.doesNotContain("refresh_token_enc")
			.doesNotContain("deleted_at");

		assertThat(normalize(AuthJdbcSql.UPDATE_ACTIVE_USER))
			.contains("update users")
			.contains("password_hash")
			.contains("bio")
			.contains("interests")
			.contains("skill_level")
			.contains("where id = ?")
			.contains("deleted_at is null");

		assertThat(normalize(AuthJdbcSql.UPDATE_OAUTH_ACCOUNT_SYNC))
			.contains("update oauth_account")
			.contains("where id = ?")
			.contains("deleted_at is null")
			.doesNotContain("access_token_enc")
			.doesNotContain("refresh_token_enc");
	}

	@Test
	void upsertGuardsCanDetectSoftDeletedRowsBeforeInsert() {
		assertThat(normalize(AuthJdbcSql.EXISTS_USER_BY_ID))
			.contains("from users")
			.contains("where id = ?")
			.doesNotContain("deleted_at is null");
		assertThat(normalize(AuthJdbcSql.EXISTS_ACTIVE_USER_BY_ID))
			.contains("from users")
			.contains("where id = ?")
			.contains("deleted_at is null");
		assertThat(normalize(AuthJdbcSql.EXISTS_USER_BY_EMAIL))
			.contains("from users")
			.contains("where email_live_key = ?");
		assertThat(normalize(AuthJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_ID))
			.contains("from oauth_account")
			.contains("where id = ?")
			.doesNotContain("deleted_at is null");
		assertThat(normalize(AuthJdbcSql.EXISTS_ACTIVE_OAUTH_ACCOUNT_BY_ID))
			.contains("from oauth_account")
			.contains("where id = ?")
			.contains("deleted_at is null");
		assertThat(normalize(AuthJdbcSql.EXISTS_OAUTH_ACCOUNT_BY_PROVIDER_KEY))
			.contains("from oauth_account")
			.contains("where provider_account_live_key = ?");
	}

	@Test
	void refreshTokenSqlStoresHashesAndSupportsRevocationOnly() {
		assertThat(normalize(RefreshTokenJdbcSql.INSERT_REFRESH_TOKEN))
			.contains("insert into refresh_token")
			.contains("token_hash")
			.contains("device_info")
			.contains("ip_address")
			.contains("expires_at")
			.contains("created_at")
			.doesNotContain("raw_token")
			.doesNotContain("token_value");

		assertThat(normalize(RefreshTokenJdbcSql.FIND_REFRESH_TOKEN_BY_HASH))
			.contains("from refresh_token")
			.contains("where token_hash = ?")
			.contains("revoked_at is null");

		assertThat(normalize(RefreshTokenJdbcSql.REVOKE_REFRESH_TOKEN))
			.contains("update refresh_token")
			.contains("set revoked_at = ?")
			.contains("where id = ?")
			.contains("revoked_at is null");

		assertThat(normalize(RefreshTokenJdbcSql.REVOKE_ACTIVE_REFRESH_TOKENS_BY_USER))
			.contains("update refresh_token")
			.contains("where user_id = ?")
			.contains("revoked_at is null");
	}

	private static String normalize(String sql) {
		return sql
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase(Locale.ROOT);
	}
}
