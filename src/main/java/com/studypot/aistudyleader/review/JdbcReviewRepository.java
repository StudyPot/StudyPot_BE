package com.studypot.aistudyleader.review;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

class JdbcReviewRepository implements ReviewRepository {

	private final JdbcTemplate jdbcTemplate;

	JdbcReviewRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
	}

	@Override
	public boolean existsByTargetIdAndAuthorId(UUID targetId, UUID authorId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		Objects.requireNonNull(authorId, "authorId must not be null");
		return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
			ReviewJdbcSql.EXISTS_BY_TARGET_ID_AND_AUTHOR_ID,
			Boolean.class,
			uuid(targetId),
			uuid(authorId)
		));
	}

	@Override
	public Review save(Review review) {
		Objects.requireNonNull(review, "review must not be null");
		jdbcTemplate.update(
			ReviewJdbcSql.INSERT_REVIEW,
			uuid(review.id()),
			uuid(review.targetId()),
			uuid(review.authorId()),
			review.rating(),
			review.content(),
			timestamp(review.createdAt()),
			timestamp(review.updatedAt())
		);
		return review;
	}

	@Override
	public Optional<Review> findById(UUID reviewId) {
		Objects.requireNonNull(reviewId, "reviewId must not be null");
		return queryOne(ReviewJdbcSql.SELECT_BY_ID, uuid(reviewId));
	}

	@Override
	public Optional<Review> findByTargetIdAndAuthorId(UUID targetId, UUID authorId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		Objects.requireNonNull(authorId, "authorId must not be null");
		return queryOne(ReviewJdbcSql.SELECT_BY_TARGET_ID_AND_AUTHOR_ID, uuid(targetId), uuid(authorId));
	}

	@Override
	public List<Review> findByTargetIdOrderByCreatedAtDesc(UUID targetId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		return jdbcTemplate.query(
			ReviewJdbcSql.SELECT_BY_TARGET_ID_ORDER_BY_CREATED_AT_DESC,
			this::mapReview,
			uuid(targetId)
		);
	}

	@Override
	public void delete(Review review) {
		Objects.requireNonNull(review, "review must not be null");
		jdbcTemplate.update(
			ReviewJdbcSql.SOFT_DELETE_REVIEW,
			timestamp(review.updatedAt()),
			timestamp(review.updatedAt()),
			uuid(review.id())
		);
	}

	private Optional<Review> queryOne(String sql, Object... args) {
		List<Review> reviews = jdbcTemplate.query(sql, this::mapReview, args);
		return reviews.stream().findFirst();
	}

	private Review mapReview(ResultSet resultSet, int rowNumber) throws SQLException {
		return new Review(
			uuid(resultSet, "id"),
			uuid(resultSet, "group_id"),
			uuid(resultSet, "author_id"),
			resultSet.getInt("rating"),
			resultSet.getString("content"),
			instant(resultSet, "created_at"),
			instant(resultSet, "updated_at")
		);
	}

	private static byte[] uuid(UUID uuid) {
		return UuidBinary.toBytes(uuid);
	}

	private static UUID uuid(ResultSet resultSet, String column) throws SQLException {
		return UuidBinary.fromBytes(resultSet.getBytes(column));
	}

	private static Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private static Instant instant(ResultSet resultSet, String column) throws SQLException {
		return resultSet.getTimestamp(column).toInstant();
	}
}
