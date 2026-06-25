package com.studypot.aistudyleader.follow.repository;

import com.studypot.aistudyleader.follow.domain.FollowUserView;
import com.studypot.aistudyleader.global.persistence.UuidBinary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcFollowRepository implements FollowRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcFollowRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public boolean existsActiveUser(UUID userId) {
		Objects.requireNonNull(userId, "userId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(FollowJdbcSql.EXISTS_ACTIVE_USER, Boolean.class, uuid(userId)));
	}

	@Override
	public boolean existsFollow(UUID followerUserId, UUID followeeUserId) {
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			FollowJdbcSql.EXISTS_FOLLOW, Boolean.class, uuid(followerUserId), uuid(followeeUserId)));
	}

	@Override
	public void insertFollow(UUID id, UUID followerUserId, UUID followeeUserId, Instant createdAt) {
		jdbcTemplate.update(FollowJdbcSql.INSERT_FOLLOW, uuid(id), uuid(followerUserId), uuid(followeeUserId), Timestamp.from(createdAt));
	}

	@Override
	public void deleteFollow(UUID followerUserId, UUID followeeUserId) {
		jdbcTemplate.update(FollowJdbcSql.DELETE_FOLLOW, uuid(followerUserId), uuid(followeeUserId));
	}

	@Override
	public List<FollowUserView> findFollowing(UUID userId) {
		return jdbcTemplate.query(FollowJdbcSql.SELECT_FOLLOWING, this::mapView, uuid(userId));
	}

	@Override
	public List<FollowUserView> findFollowers(UUID userId) {
		return jdbcTemplate.query(FollowJdbcSql.SELECT_FOLLOWERS, this::mapView, uuid(userId));
	}

	private FollowUserView mapView(ResultSet resultSet, int rowNumber) throws SQLException {
		return new FollowUserView(
			UuidBinary.fromBytes(resultSet.getBytes("user_id")),
			resultSet.getString("nickname"),
			resultSet.getString("email"),
			resultSet.getString("bio"),
			resultSet.getBoolean("mutual"),
			resultSet.getTimestamp("followed_at").toInstant()
		);
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}
}
