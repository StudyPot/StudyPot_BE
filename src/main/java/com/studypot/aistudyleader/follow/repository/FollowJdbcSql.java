package com.studypot.aistudyleader.follow.repository;

final class FollowJdbcSql {

	static final String EXISTS_ACTIVE_USER = """
		select exists(
		  select 1 from users where id = ? and deleted_at is null
		)
		""";

	static final String EXISTS_FOLLOW = """
		select exists(
		  select 1 from user_follow where follower_user_id = ? and followee_user_id = ?
		)
		""";

	static final String INSERT_FOLLOW = """
		insert into user_follow (id, follower_user_id, followee_user_id, created_at)
		values (?, ?, ?, ?)
		""";

	static final String DELETE_FOLLOW = """
		delete from user_follow where follower_user_id = ? and followee_user_id = ?
		""";

	// 내가 팔로우하는 사용자. mutual = 상대도 나를 팔로우하는지.
	static final String SELECT_FOLLOWING = """
		select u.id as user_id, u.nickname, u.email, u.bio,
		       f.created_at as followed_at,
		       exists(
		         select 1 from user_follow r
		         where r.follower_user_id = f.followee_user_id and r.followee_user_id = f.follower_user_id
		       ) as mutual
		from user_follow f
		join users u on u.id = f.followee_user_id and u.deleted_at is null
		where f.follower_user_id = ?
		order by f.created_at desc, f.id desc
		""";

	// 나를 팔로우하는 사용자. mutual = 내가 그 사용자를 팔로우하는지.
	static final String SELECT_FOLLOWERS = """
		select u.id as user_id, u.nickname, u.email, u.bio,
		       f.created_at as followed_at,
		       exists(
		         select 1 from user_follow r
		         where r.follower_user_id = f.followee_user_id and r.followee_user_id = f.follower_user_id
		       ) as mutual
		from user_follow f
		join users u on u.id = f.follower_user_id and u.deleted_at is null
		where f.followee_user_id = ?
		order by f.created_at desc, f.id desc
		""";

	private FollowJdbcSql() {
	}
}
