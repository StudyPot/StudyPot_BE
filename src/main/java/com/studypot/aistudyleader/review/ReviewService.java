package com.studypot.aistudyleader.review;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

	private final Clock clock;
	private final ReviewRepository reviewRepository;

	@Autowired
	public ReviewService(ReviewRepository reviewRepository) {
		this(reviewRepository, Clock.systemUTC());
	}

	ReviewService(Clock clock) {
		this(new InMemoryReviewRepository(), clock);
	}

	ReviewService(ReviewRepository reviewRepository, Clock clock) {
		this.reviewRepository = Objects.requireNonNull(reviewRepository, "reviewRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public synchronized Review createReview(CreateReviewCommand command) {
		if (reviewRepository.existsByTargetIdAndAuthorId(command.targetId(), command.authorId())) {
			throw new DuplicateReviewException("review already exists for target and author.");
		}

		Instant now = clock.instant();
		Review review = new Review(UUID.randomUUID(), command.targetId(), command.authorId(), command.rating(), command.content(), now, now);
		return reviewRepository.save(review);
	}

	public List<Review> listReviews(UUID targetId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		return reviewRepository.findByTargetIdOrderByCreatedAtDesc(targetId);
	}

	public ReviewRatingSummary getRatingSummary(UUID targetId) {
		List<Review> reviews = listReviews(targetId);
		double averageRating = reviews.stream()
			.mapToInt(Review::rating)
			.average()
			.orElse(0.0);
		return new ReviewRatingSummary(targetId, reviews.size(), averageRating);
	}

	public Review getMyReview(UUID targetId, UUID authorId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		Objects.requireNonNull(authorId, "authorId must not be null");
		return reviewRepository.findByTargetIdAndAuthorId(targetId, authorId)
			.orElseThrow(() -> new ReviewNotFoundException("review was not found."));
	}

	public synchronized Review updateReview(UpdateReviewCommand command) {
		Review review = requireReview(command.reviewId());
		if (!review.writtenBy(command.requesterId())) {
			throw new ReviewAuthorMismatchException("review author does not match requester.");
		}
		Review updated = review.update(command.rating(), command.content(), clock.instant());
		if (!reviewRepository.update(updated)) {
			throw new ReviewNotFoundException("review was not found.");
		}
		return updated;
	}

	public synchronized void deleteReview(UUID reviewId, UUID requesterId) {
		Review review = requireReview(reviewId);
		if (!review.writtenBy(requesterId)) {
			throw new ReviewAuthorMismatchException("review author does not match requester.");
		}
		Review reviewWithDeletionTime = new Review(
			review.id(),
			review.targetId(),
			review.authorId(),
			review.rating(),
			review.content(),
			review.createdAt(),
			clock.instant()
		);
		reviewRepository.delete(reviewWithDeletionTime);
	}

	private Review requireReview(UUID reviewId) {
		Objects.requireNonNull(reviewId, "reviewId must not be null");
		return reviewRepository.findById(reviewId)
			.orElseThrow(() -> new ReviewNotFoundException("review was not found."));
	}
}
