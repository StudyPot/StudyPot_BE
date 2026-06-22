package com.studypot.aistudyleader.review.repository;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewMembership;
import com.studypot.aistudyleader.review.domain.ReviewRatingCount;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcReviewRepository implements ReviewRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcReviewRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public boolean existsStudyGroup(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(ReviewJdbcSql.EXISTS_STUDY_GROUP, Boolean.class, uuid(groupId)));
	}

	@Override
	public Optional<ReviewMembership> findMembership(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(ReviewJdbcSql.SELECT_MEMBERSHIP, JdbcReviewRepository::mapMembership, uuid(groupId), uuid(userId));
	}

	@Override
	public boolean insertReview(Review review) {
		Objects.requireNonNull(review, "review must not be null");
		try {
			return jdbcTemplate.update(
				ReviewJdbcSql.INSERT_REVIEW,
				uuid(review.id()),
				uuid(review.groupId()),
				uuid(review.memberId()),
				uuid(review.userId()),
				review.rating(),
				review.content(),
				timestamp(review.createdAt()),
				timestamp(review.updatedAt())
			) == 1;
		} catch (DuplicateKeyException exception) {
			return false;
		}
	}

	@Override
	public Optional<Review> findReview(UUID groupId, UUID userId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(userId, "userId must not be null");
		return queryOne(ReviewJdbcSql.SELECT_REVIEW_BY_GROUP_AND_USER, JdbcReviewRepository::mapReview, uuid(groupId), uuid(userId));
	}

	@Override
	public List<Review> findReviews(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(ReviewJdbcSql.SELECT_REVIEWS_BY_GROUP, JdbcReviewRepository::mapReview, uuid(groupId));
	}

	@Override
	public List<ReviewRatingCount> findRatingCounts(UUID groupId) {
		Objects.requireNonNull(groupId, "groupId must not be null");
		return jdbcTemplate.query(ReviewJdbcSql.SELECT_RATING_COUNTS_BY_GROUP, JdbcReviewRepository::mapRatingCount, uuid(groupId));
	}

	private static ReviewMembership mapMembership(ResultSet resultSet, int rowNumber) throws SQLException {
		return new ReviewMembership(
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			UuidBinary.fromBytes(resultSet.getBytes("user_id")),
			resultSet.getString("display_name"),
			GroupMemberStatus.valueOf(resultSet.getString("member_status"))
		);
	}

	private static Review mapReview(ResultSet resultSet, int rowNumber) throws SQLException {
		return new Review(
			UuidBinary.fromBytes(resultSet.getBytes("id")),
			UuidBinary.fromBytes(resultSet.getBytes("group_id")),
			UuidBinary.fromBytes(resultSet.getBytes("member_id")),
			UuidBinary.fromBytes(resultSet.getBytes("user_id")),
			resultSet.getString("display_name"),
			resultSet.getInt("rating"),
			resultSet.getString("content"),
			instant(resultSet.getTimestamp("created_at")),
			instant(resultSet.getTimestamp("updated_at"))
		);
	}

	private static ReviewRatingCount mapRatingCount(ResultSet resultSet, int rowNumber) throws SQLException {
		return new ReviewRatingCount(resultSet.getInt("rating"), resultSet.getInt("rating_count"));
	}

	private <T> Optional<T> queryOne(String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapper, args));
		} catch (EmptyResultDataAccessException exception) {
			return Optional.empty();
		}
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static Timestamp timestamp(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}

	private static Instant instant(Timestamp timestamp) {
		return timestamp == null ? null : timestamp.toInstant();
	}
}
