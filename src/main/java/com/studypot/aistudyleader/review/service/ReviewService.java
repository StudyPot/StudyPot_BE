package com.studypot.aistudyleader.review.service;

import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewMembership;
import com.studypot.aistudyleader.review.domain.ReviewStats;
import com.studypot.aistudyleader.review.repository.ReviewRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class ReviewService {

	private final ReviewRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public ReviewService(ReviewRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public Review createReview(CreateReviewCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		ReviewMembership membership = requireActiveMembership(command.groupId(), command.authenticatedUserId());
		Instant now = clock.instant();
		Review review;
		try {
			review = Review.create(
				idGenerator.get(),
				command.groupId(),
				membership.memberId(),
				membership.userId(),
				membership.displayName(),
				command.rating(),
				command.content(),
				now
			);
		} catch (IllegalArgumentException exception) {
			throw new InvalidReviewRequestException("rating", exception.getMessage());
		}
		if (!repository.insertReview(review)) {
			throw new ReviewMutationRejectedException("review has already been submitted for this study group.");
		}
		return repository.findReview(command.groupId(), membership.userId()).orElse(review);
	}

	@Transactional(readOnly = true)
	public Review getMyReview(GetMyReviewQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		return repository.findReview(query.groupId(), query.authenticatedUserId())
			.orElseThrow(() -> new ReviewNotFoundException("review was not found for this study group."));
	}

	@Transactional(readOnly = true)
	public List<Review> listReviews(ListReviewsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		return repository.findReviews(query.groupId());
	}

	@Transactional(readOnly = true)
	public ReviewStats getReviewStats(GetReviewStatsQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		requireActiveMembership(query.groupId(), query.authenticatedUserId());
		return ReviewStats.from(repository.findRatingCounts(query.groupId()));
	}

	private ReviewMembership requireActiveMembership(UUID groupId, UUID userId) {
		ReviewMembership membership = repository.findMembership(groupId, userId)
			.orElseGet(() -> {
				if (!repository.existsStudyGroup(groupId)) {
					throw new ReviewNotFoundException("study group was not found.");
				}
				throw new ReviewAccessDeniedException("authenticated user is not a member of this study group.");
			});
		if (!membership.active()) {
			throw new ReviewAccessDeniedException("active group membership is required for study group reviews.");
		}
		return membership;
	}
}
