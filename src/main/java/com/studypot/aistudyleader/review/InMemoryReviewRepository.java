package com.studypot.aistudyleader.review;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryReviewRepository implements ReviewRepository {

	private final Map<UUID, Review> reviewsById = new ConcurrentHashMap<>();
	private final Map<ReviewAuthorKey, UUID> reviewIdsByAuthorKey = new ConcurrentHashMap<>();

	@Override
	public boolean existsByTargetIdAndAuthorId(UUID targetId, UUID authorId) {
		return reviewIdsByAuthorKey.containsKey(new ReviewAuthorKey(targetId, authorId));
	}

	@Override
	public Review save(Review review) {
		reviewsById.put(review.id(), review);
		reviewIdsByAuthorKey.put(new ReviewAuthorKey(review.targetId(), review.authorId()), review.id());
		return review;
	}

	@Override
	public Optional<Review> findById(UUID reviewId) {
		return Optional.ofNullable(reviewsById.get(reviewId));
	}

	@Override
	public Optional<Review> findByTargetIdAndAuthorId(UUID targetId, UUID authorId) {
		return Optional.ofNullable(reviewIdsByAuthorKey.get(new ReviewAuthorKey(targetId, authorId)))
			.map(reviewsById::get);
	}

	@Override
	public List<Review> findByTargetIdOrderByCreatedAtDesc(UUID targetId) {
		return reviewsById.values()
			.stream()
			.filter(review -> review.targetId().equals(targetId))
			.sorted(Comparator.comparing(Review::createdAt).reversed())
			.toList();
	}

	@Override
	public void delete(Review review) {
		reviewsById.remove(review.id());
		reviewIdsByAuthorKey.remove(new ReviewAuthorKey(review.targetId(), review.authorId()));
	}

	private record ReviewAuthorKey(UUID targetId, UUID authorId) {

		private ReviewAuthorKey {
			Objects.requireNonNull(targetId, "targetId must not be null");
			Objects.requireNonNull(authorId, "authorId must not be null");
		}
	}
}
