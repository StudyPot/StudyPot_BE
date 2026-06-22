package com.studypot.aistudyleader.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewMembership;
import com.studypot.aistudyleader.review.domain.ReviewRatingCount;
import com.studypot.aistudyleader.review.domain.ReviewStats;
import com.studypot.aistudyleader.review.repository.ReviewRepository;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewServiceTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000130001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000130002");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000130003");
	private static final UUID REVIEW_ID = UUID.fromString("018f0000-0000-7000-8000-000000130004");
	private static final Instant NOW = Instant.parse("2026-06-22T03:00:00Z");

	private ReviewService service(ReviewRepository repository) {
		return new ReviewService(repository, Clock.fixed(NOW, ZoneOffset.UTC), () -> REVIEW_ID);
	}

	@Test
	void createReviewStoresReviewForActiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();

		Review result = service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 5, " 좋았어요 "));

		assertThat(repository.inserted).isNotNull();
		assertThat(repository.inserted.id()).isEqualTo(REVIEW_ID);
		assertThat(repository.inserted.memberId()).isEqualTo(MEMBER_ID);
		assertThat(repository.inserted.userId()).isEqualTo(USER_ID);
		assertThat(repository.inserted.rating()).isEqualTo(5);
		assertThat(repository.inserted.content()).isEqualTo("좋았어요");
		assertThat(result.rating()).isEqualTo(5);
		assertThat(result.displayName()).isEqualTo("현우");
	}

	@Test
	void createReviewRejectsDuplicateSubmission() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();
		repository.insertSucceeds = false;

		assertThatThrownBy(() -> service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 4, null)))
			.isInstanceOf(ReviewMutationRejectedException.class)
			.hasMessage("review has already been submitted for this study group.");
	}

	@Test
	void createReviewRejectsInvalidRating() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();

		assertThatThrownBy(() -> service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 0, null)))
			.isInstanceOf(InvalidReviewRequestException.class);
	}

	@Test
	void createReviewRejectsNonMemberOfExistingGroup() {
		CapturingRepository repository = new CapturingRepository();
		repository.groupExists = true;

		assertThatThrownBy(() -> service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 5, null)))
			.isInstanceOf(ReviewAccessDeniedException.class)
			.hasMessage("authenticated user is not a member of this study group.");
	}

	@Test
	void createReviewReturnsNotFoundWhenGroupMissing() {
		CapturingRepository repository = new CapturingRepository();

		assertThatThrownBy(() -> service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 5, null)))
			.isInstanceOf(ReviewNotFoundException.class)
			.hasMessage("study group was not found.");
	}

	@Test
	void createReviewRejectsInactiveMember() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = new ReviewMembership(GROUP_ID, MEMBER_ID, USER_ID, "현우", GroupMemberStatus.PENDING_ONBOARDING);

		assertThatThrownBy(() -> service(repository).createReview(new CreateReviewCommand(USER_ID, GROUP_ID, 5, null)))
			.isInstanceOf(ReviewAccessDeniedException.class)
			.hasMessage("active group membership is required for study group reviews.");
	}

	@Test
	void getMyReviewReturnsStoredReview() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();
		repository.myReview = review(5, "좋았어요");

		Review result = service(repository).getMyReview(new GetMyReviewQuery(USER_ID, GROUP_ID));

		assertThat(result.rating()).isEqualTo(5);
	}

	@Test
	void getMyReviewReturnsNotFoundWhenAbsent() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();

		assertThatThrownBy(() -> service(repository).getMyReview(new GetMyReviewQuery(USER_ID, GROUP_ID)))
			.isInstanceOf(ReviewNotFoundException.class)
			.hasMessage("review was not found for this study group.");
	}

	@Test
	void listReviewsReturnsRepositoryReviews() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();
		repository.reviews = List.of(review(5, "a"), review(3, "b"));

		List<Review> result = service(repository).listReviews(new ListReviewsQuery(USER_ID, GROUP_ID));

		assertThat(result).hasSize(2);
	}

	@Test
	void getReviewStatsComputesAverageAndDistribution() {
		CapturingRepository repository = new CapturingRepository();
		repository.membership = activeMembership();
		repository.ratingCounts = List.of(new ReviewRatingCount(5, 3), new ReviewRatingCount(4, 1), new ReviewRatingCount(2, 1));

		ReviewStats stats = service(repository).getReviewStats(new GetReviewStatsQuery(USER_ID, GROUP_ID));

		assertThat(stats.totalCount()).isEqualTo(5);
		assertThat(stats.averageRating()).isEqualTo(4.2);
		assertThat(stats.ratingDistribution()).containsEntry(5, 3).containsEntry(4, 1).containsEntry(2, 1)
			.containsEntry(1, 0).containsEntry(3, 0);
	}

	private static ReviewMembership activeMembership() {
		return new ReviewMembership(GROUP_ID, MEMBER_ID, USER_ID, "현우", GroupMemberStatus.ACTIVE);
	}

	private static Review review(int rating, String content) {
		return Review.create(REVIEW_ID, GROUP_ID, MEMBER_ID, USER_ID, "현우", rating, content, NOW);
	}

	private static final class CapturingRepository implements ReviewRepository {

		private boolean groupExists;
		private ReviewMembership membership;
		private boolean insertSucceeds = true;
		private Review inserted;
		private Review myReview;
		private List<Review> reviews = List.of();
		private List<ReviewRatingCount> ratingCounts = List.of();

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<ReviewMembership> findMembership(UUID groupId, UUID userId) {
			return Optional.ofNullable(membership);
		}

		@Override
		public boolean insertReview(Review review) {
			this.inserted = review;
			return insertSucceeds;
		}

		@Override
		public Optional<Review> findReview(UUID groupId, UUID userId) {
			if (inserted != null) {
				return Optional.of(inserted);
			}
			return Optional.ofNullable(myReview);
		}

		@Override
		public List<Review> findReviews(UUID groupId) {
			return reviews;
		}

		@Override
		public List<ReviewRatingCount> findRatingCounts(UUID groupId) {
			return ratingCounts;
		}
	}
}
