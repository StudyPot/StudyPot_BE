package com.studypot.aistudyleader.auth.repository;

final class AuthJdbcSql {

	static final String FIND_ACTIVE_USER_BY_ID = """
		select id, email, password_hash, nickname, profile_image, bio, interests, skill_level,
		       last_login_at, created_at, updated_at, deleted_at
		from users
		where id = ?
		  and deleted_at is null
		""";

	static final String FIND_ACTIVE_USER_BY_EMAIL = """
		select id, email, password_hash, nickname, profile_image, bio, interests, skill_level,
		       last_login_at, created_at, updated_at, deleted_at
		from users
		where email_live_key = ?
		  and deleted_at is null
		""";

	static final String FIND_ACTIVE_OAUTH_ACCOUNT = """
		select id, user_id, provider, provider_user_id, email, token_expires_at, scope,
		       connected_at, last_synced_at, deleted_at
		from oauth_account
		where provider_account_live_key = ?
		  and deleted_at is null
		""";

	static final String INSERT_USER = """
		insert into users (
		  id, email, email_live_key, password_hash, nickname, profile_image, bio, interests, skill_level,
		  last_login_at, created_at, updated_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_ACTIVE_USER = """
		update users
		set email = ?,
		    email_live_key = ?,
		    password_hash = ?,
		    nickname = ?,
		    profile_image = ?,
		    bio = ?,
		    interests = ?,
		    skill_level = ?,
		    last_login_at = ?,
		    updated_at = ?
		where id = ?
		  and deleted_at is null
		""";

	static final String EXISTS_USER_BY_ID = """
		select exists(
		  select 1
		  from users
		  where id = ?
		)
		""";

	static final String EXISTS_ACTIVE_USER_BY_ID = """
		select exists(
		  select 1
		  from users
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String EXISTS_USER_BY_EMAIL = """
		select exists(
		  select 1
		  from users
		  where email_live_key = ?
		)
		""";

	static final String INSERT_OAUTH_ACCOUNT = """
		insert into oauth_account (
		  id, user_id, provider, provider_user_id, provider_account_live_key, email,
		  token_expires_at, scope, connected_at, last_synced_at
		) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		""";

	static final String UPDATE_OAUTH_ACCOUNT_SYNC = """
		update oauth_account
		set email = ?,
		    token_expires_at = ?,
		    scope = ?,
		    last_synced_at = ?
		where id = ?
		  and deleted_at is null
		""";

	static final String EXISTS_OAUTH_ACCOUNT_BY_ID = """
		select exists(
		  select 1
		  from oauth_account
		  where id = ?
		)
		""";

	static final String EXISTS_ACTIVE_OAUTH_ACCOUNT_BY_ID = """
		select exists(
		  select 1
		  from oauth_account
		  where id = ?
		    and deleted_at is null
		)
		""";

	static final String EXISTS_OAUTH_ACCOUNT_BY_PROVIDER_KEY = """
		select exists(
		  select 1
		  from oauth_account
		  where provider_account_live_key = ?
		)
		""";

	private AuthJdbcSql() {
	}
}
