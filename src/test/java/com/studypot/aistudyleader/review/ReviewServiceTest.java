package com.studypot.aistudyleader.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

	private static final Instant FIXED_NOW = Instant.parse("2026-06-07T12:00:00Z");
	private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

	private final ReviewService reviewService = new ReviewService(
		FIXED_CLOCK
	);

	@Test
	void createsReviewAndAggregatesTargetRating() {
		UUID targetId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();

		Review review = reviewService.createReview(new CreateReviewCommand(targetId, authorId, 5, "협업 리듬이 안정적입니다."));

		assertThat(review.id()).isNotNull();
		assertThat(reviewService.listReviews(targetId)).containsExactly(review);
		assertThat(reviewService.getRatingSummary(targetId))
			.extracting(ReviewRatingSummary::reviewCount, ReviewRatingSummary::averageRating)
			.containsExactly(1, 5.0);
	}

	@Test
	void rejectsDuplicateReviewFromSameAuthorToSameTarget() {
		UUID targetId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		reviewService.createReview(new CreateReviewCommand(targetId, authorId, 4, "첫 리뷰"));

		assertThatThrownBy(() ->
			reviewService.createReview(new CreateReviewCommand(targetId, authorId, 3, "중복 리뷰"))
		)
			.isInstanceOf(DuplicateReviewException.class)
			.hasMessageContaining("review already exists");
	}

	@Test
	void findsMyReviewByGroupAndAuthor() {
		UUID groupId = UUID.randomUUID();
		UUID authorId = UUID.randomUUID();
		Review review = reviewService.createReview(new CreateReviewCommand(groupId, authorId, 5, "내 리뷰"));

		assertThat(reviewService.getMyReview(groupId, authorId)).isEqualTo(review);
	}

	@Test
	void validatesAuthorBeforeMutation() {
		Review review = reviewService.createReview(
			new CreateReviewCommand(UUID.randomUUID(), UUID.randomUUID(), 4, "작성자 검증 대상 리뷰")
		);

		assertThatThrownBy(() -> reviewService.deleteReview(review.id(), UUID.randomUUID()))
			.isInstanceOf(ReviewAuthorMismatchException.class)
			.hasMessageContaining("review author does not match");
	}

	@Test
	void deleteReviewUsesCurrentClockTimestampForSoftDelete() {
		CapturingReviewRepository repository = new CapturingReviewRepository();
		ReviewService service = new ReviewService(repository, FIXED_CLOCK);
		Review review = new Review(
			UUID.randomUUID(),
			UUID.randomUUID(),
			UUID.randomUUID(),
			4,
			"삭제 시간 검증 리뷰",
			Instant.parse("2026-06-01T00:00:00Z"),
			Instant.parse("2026-06-01T00:00:00Z")
		);
		repository.save(review);

		service.deleteReview(review.id(), review.authorId());

		assertThat(repository.deletedReview())
			.extracting(Review::updatedAt)
			.isEqualTo(FIXED_NOW);
	}

	@Test
	void deleteReviewRefreshesAggregateViewByRemovingDeletedReview() {
		UUID targetId = UUID.randomUUID();
		Review firstReview = reviewService.createReview(
			new CreateReviewCommand(targetId, UUID.randomUUID(), 5, "첫 리뷰")
		);
		reviewService.createReview(new CreateReviewCommand(targetId, UUID.randomUUID(), 3, "둘째 리뷰"));

		reviewService.deleteReview(firstReview.id(), firstReview.authorId());

		assertThat(reviewService.getRatingSummary(targetId))
			.extracting(ReviewRatingSummary::reviewCount, ReviewRatingSummary::averageRating)
			.containsExactly(1, 3.0);
	}

	private static final class CapturingReviewRepository extends InMemoryReviewRepository {

		private Review deletedReview;

		@Override
		public void delete(Review review) {
			deletedReview = review;
			super.delete(review);
		}

		private Review deletedReview() {
			return deletedReview;
		}
	}
}
