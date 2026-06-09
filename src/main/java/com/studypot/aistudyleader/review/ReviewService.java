package com.studypot.aistudyleader.review;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

	private final Clock clock;
	private final Map<UUID, Review> reviewsById = new ConcurrentHashMap<>();
	private final Map<ReviewAuthorKey, UUID> reviewIdsByAuthorKey = new ConcurrentHashMap<>();

	public ReviewService() {
		this(Clock.systemUTC());
	}

	ReviewService(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public synchronized Review createReview(CreateReviewCommand command) {
		ReviewAuthorKey authorKey = new ReviewAuthorKey(command.targetId(), command.authorId());
		if (reviewIdsByAuthorKey.containsKey(authorKey)) {
			throw new DuplicateReviewException("review already exists for target and author.");
		}

		Instant now = clock.instant();
		Review review = new Review(UUID.randomUUID(), command.targetId(), command.authorId(), command.rating(), command.content(), now, now);
		reviewsById.put(review.id(), review);
		reviewIdsByAuthorKey.put(authorKey, review.id());
		return review;
	}

	public List<Review> listReviews(UUID targetId) {
		Objects.requireNonNull(targetId, "targetId must not be null");
		return reviewsById.values()
			.stream()
			.filter(review -> review.targetId().equals(targetId))
			.sorted(Comparator.comparing(Review::createdAt).reversed())
			.toList();
	}

	public ReviewRatingSummary getRatingSummary(UUID targetId) {
		List<Review> reviews = listReviews(targetId);
		double averageRating = reviews.stream()
			.mapToInt(Review::rating)
			.average()
			.orElse(0.0);
		return new ReviewRatingSummary(targetId, reviews.size(), averageRating);
	}

	public synchronized void deleteReview(UUID reviewId, UUID requesterId) {
		Review review = requireReview(reviewId);
		if (!review.writtenBy(requesterId)) {
			throw new ReviewAuthorMismatchException("review author does not match requester.");
		}
		reviewsById.remove(reviewId);
		reviewIdsByAuthorKey.remove(new ReviewAuthorKey(review.targetId(), review.authorId()));
	}

	private Review requireReview(UUID reviewId) {
		Objects.requireNonNull(reviewId, "reviewId must not be null");
		Review review = reviewsById.get(reviewId);
		if (review == null) {
			throw new ReviewNotFoundException("review was not found.");
		}
		return review;
	}

	private record ReviewAuthorKey(UUID targetId, UUID authorId) {

		private ReviewAuthorKey {
			Objects.requireNonNull(targetId, "targetId must not be null");
			Objects.requireNonNull(authorId, "authorId must not be null");
		}
	}
}

class DuplicateReviewException extends RuntimeException {

	DuplicateReviewException(String message) {
		super(message);
	}
}

class ReviewAuthorMismatchException extends RuntimeException {

	ReviewAuthorMismatchException(String message) {
		super(message);
	}
}

class ReviewNotFoundException extends RuntimeException {

	ReviewNotFoundException(String message) {
		super(message);
	}
}
