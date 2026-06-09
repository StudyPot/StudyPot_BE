package com.studypot.aistudyleader.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcReviewRepositoryTest {

	private static final UUID REVIEW_ID = UUID.fromString("018f0000-0000-7000-8000-00000000a701");
	private static final UUID TARGET_ID = UUID.fromString("018f0000-0000-7000-8000-00000000a702");
	private static final UUID AUTHOR_ID = UUID.fromString("018f0000-0000-7000-8000-00000000a703");
	private static final Instant CREATED_AT = Instant.parse("2026-06-09T09:00:00Z");
	private static final Review REVIEW = new Review(REVIEW_ID, TARGET_ID, AUTHOR_ID, 5, "좋은 스터디입니다.", CREATED_AT, CREATED_AT);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcReviewRepository repository = new JdbcReviewRepository(jdbcTemplate);

	@Test
	void savePersistsReviewWithBinaryIdsAndTimestamps() {
		when(jdbcTemplate.update(eq(ReviewJdbcSql.INSERT_REVIEW), any(Object[].class))).thenReturn(1);

		assertThat(repository.save(REVIEW)).isEqualTo(REVIEW);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(ReviewJdbcSql.INSERT_REVIEW), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(REVIEW_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(TARGET_ID));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(AUTHOR_ID));
		assertThat(args.getValue()[3]).isEqualTo(5);
		assertThat(args.getValue()[4]).isEqualTo("좋은 스터디입니다.");
		assertThat(args.getValue()[5]).isEqualTo(Timestamp.from(CREATED_AT));
		verify(jdbcTemplate).update(eq(ReviewJdbcSql.REFRESH_CATALOG_REVIEW_AGGREGATE), any(Object[].class));
	}

	@Test
	void deleteSoftDeletesTheReviewInsteadOfDroppingRows() {
		when(jdbcTemplate.update(eq(ReviewJdbcSql.SOFT_DELETE_REVIEW), any(Object[].class))).thenReturn(1);

		repository.delete(REVIEW);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(ReviewJdbcSql.SOFT_DELETE_REVIEW), args.capture());
		assertThat(args.getValue()[0]).isEqualTo(Timestamp.from(CREATED_AT));
		assertThat((byte[]) args.getValue()[2]).containsExactly(UuidBinary.toBytes(REVIEW_ID));
		verify(jdbcTemplate).update(eq(ReviewJdbcSql.REFRESH_CATALOG_REVIEW_AGGREGATE), any(Object[].class));
	}

	@Test
	void findByTargetAndAuthorMapsStoredReview() {
		when(jdbcTemplate.query(eq(ReviewJdbcSql.SELECT_BY_TARGET_ID_AND_AUTHOR_ID), any(RowMapper.class), any(Object[].class)))
			.thenReturn(java.util.List.of(REVIEW));

		Optional<Review> found = repository.findByTargetIdAndAuthorId(TARGET_ID, AUTHOR_ID);

		assertThat(found).contains(REVIEW);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(ReviewJdbcSql.SELECT_BY_TARGET_ID_AND_AUTHOR_ID), any(RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(TARGET_ID));
		assertThat((byte[]) args.getValue()[1]).containsExactly(UuidBinary.toBytes(AUTHOR_ID));
	}
}
