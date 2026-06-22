package com.studypot.aistudyleader.review.repository;

import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewMembership;
import com.studypot.aistudyleader.review.domain.ReviewRatingCount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {

	boolean existsStudyGroup(UUID groupId);

	Optional<ReviewMembership> findMembership(UUID groupId, UUID userId);

	boolean insertReview(Review review);

	Optional<Review> findReview(UUID groupId, UUID userId);

	List<Review> findReviews(UUID groupId);

	List<ReviewRatingCount> findRatingCounts(UUID groupId);
}
