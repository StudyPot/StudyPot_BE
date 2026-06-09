package com.studypot.aistudyleader.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

	private final ReviewService reviewService = new ReviewService(
		Clock.fixed(Instant.parse("2026-06-07T12:00:00Z"), ZoneOffset.UTC)
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
}
