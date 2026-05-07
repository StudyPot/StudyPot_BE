package com.studypot.aistudyleader.identity.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class IdentityJdbcSqlContractTest {

	@Test
	void activeLookupsUseLiveKeysAndSoftDeleteFilters() {
		assertThat(normalize(IdentityJdbcSql.FIND_ACTIVE_USER_BY_ID))
			.contains("from users")
			.contains("where id = ?")
			.contains("deleted_at is null");

		assertThat(normalize(IdentityJdbcSql.FIND_ACTIVE_USER_BY_EMAIL))
			.contains("from users")
			.contains("where email_live_key = ?")
			.contains("deleted_at is null");

		assertThat(normalize(IdentityJdbcSql.FIND_ACTIVE_OAUTH_ACCOUNT))
			.contains("from oauth_account")
			.contains("where provider_account_live_key = ?")
			.contains("deleted_at is null");
	}

	@Test
	void writesCanonicalLiveKeysWithoutPersistingRawProviderTokens() {
		assertThat(normalize(IdentityJdbcSql.INSERT_USER))
			.contains("insert into users")
			.contains("email_live_key")
			.contains("last_login_at")
			.contains("created_at")
			.contains("updated_at")
			.doesNotContain("deleted_at");

		assertThat(normalize(IdentityJdbcSql.INSERT_OAUTH_ACCOUNT))
			.contains("insert into oauth_account")
			.contains("provider_account_live_key")
			.contains("token_expires_at")
			.contains("connected_at")
			.contains("last_synced_at")
			.doesNotContain("access_token_enc")
			.doesNotContain("refresh_token_enc")
			.doesNotContain("deleted_at");

		assertThat(normalize(IdentityJdbcSql.UPDATE_OAUTH_ACCOUNT_SYNC))
			.contains("update oauth_account")
			.contains("where id = ?")
			.contains("deleted_at is null")
			.doesNotContain("access_token_enc")
			.doesNotContain("refresh_token_enc");
	}

	private static String normalize(String sql) {
		return sql
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase(Locale.ROOT);
	}
}
