package com.studypot.aistudyleader.review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ReviewRepository {

	boolean existsByTargetIdAndAuthorId(UUID targetId, UUID authorId);

	Review save(Review review);

	Optional<Review> findById(UUID reviewId);

	Optional<Review> findByTargetIdAndAuthorId(UUID targetId, UUID authorId);

	List<Review> findByTargetIdOrderByCreatedAtDesc(UUID targetId);

	void delete(Review review);
}
