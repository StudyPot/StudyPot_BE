package com.studypot.aistudyleader.review.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewMembership;
import com.studypot.aistudyleader.review.domain.ReviewRatingCount;
import com.studypot.aistudyleader.review.repository.ReviewRepository;
import com.studypot.aistudyleader.review.service.ReviewService;
import com.studypot.aistudyleader.studygroup.domain.GroupMemberStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, ReviewControllerTest.TestReviewBeans.class})
@AutoConfigureMockMvc
class ReviewControllerTest {

	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000131001");
	private static final UUID STRANGER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000131009");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000131002");
	private static final UUID DUP_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000131003");
	private static final UUID NO_REVIEW_GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000131004");
	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000131005");
	private static final UUID REVIEW_ID = UUID.fromString("018f0000-0000-7000-8000-000000131006");
	private static final Instant NOW = Instant.parse("2026-06-22T03:00:00Z");

	private final MockMvc mockMvc;

	@Autowired
	ReviewControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void createReviewReturnsCreatedReview() throws Exception {
		mockMvc.perform(post(reviewsPath(GROUP_ID))
				.with(xsrf("review-xsrf"))
				.with(user(USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5,\"content\":\"좋았습니다\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(REVIEW_ID.toString()))
			.andExpect(jsonPath("$.groupId").value(GROUP_ID.toString()))
			.andExpect(jsonPath("$.userId").value(USER_ID.toString()))
			.andExpect(jsonPath("$.displayName").value("현우"))
			.andExpect(jsonPath("$.rating").value(5))
			.andExpect(jsonPath("$.content").value("좋았습니다"));
	}

	@Test
	void createReviewReturnsConflictForDuplicate() throws Exception {
		mockMvc.perform(post(reviewsPath(DUP_GROUP_ID))
				.with(xsrf("review-xsrf"))
				.with(user(USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":4}"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"));
	}

	@Test
	void createReviewReturnsValidationProblemForInvalidRating() throws Exception {
		mockMvc.perform(post(reviewsPath(GROUP_ID))
				.with(xsrf("review-xsrf"))
				.with(user(USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":0}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void createReviewRequiresAuthentication() throws Exception {
		mockMvc.perform(post(reviewsPath(GROUP_ID))
			.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void createReviewReturnsForbiddenForNonMember() throws Exception {
		mockMvc.perform(post(reviewsPath(GROUP_ID))
				.with(xsrf("review-xsrf"))
				.with(user(STRANGER_USER_ID.toString()))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"rating\":5}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void listReviewsReturnsReviews() throws Exception {
		mockMvc.perform(get(reviewsPath(GROUP_ID)).with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$[0].rating").value(5))
			.andExpect(jsonPath("$[0].displayName").value("현우"));
	}

	@Test
	void getMyReviewReturnsReview() throws Exception {
		mockMvc.perform(get(reviewsPath(GROUP_ID) + "/me").with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.rating").value(5));
	}

	@Test
	void getMyReviewReturnsNotFoundWhenAbsent() throws Exception {
		mockMvc.perform(get(reviewsPath(NO_REVIEW_GROUP_ID) + "/me").with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void getReviewStatsReturnsAggregates() throws Exception {
		mockMvc.perform(get(reviewsPath(GROUP_ID) + "/stats").with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalCount").value(3))
			.andExpect(jsonPath("$.averageRating").value(4.7))
			.andExpect(jsonPath("$.ratingDistribution.5").value(2))
			.andExpect(jsonPath("$.ratingDistribution.1").value(0));
	}

	private static String reviewsPath(UUID groupId) {
		return ApiPaths.V1 + "/groups/" + groupId + "/reviews";
	}

	private static org.springframework.test.web.servlet.request.RequestPostProcessor xsrf(String token) {
		return request -> {
			request.addHeader("X-XSRF-TOKEN", token);
			request.setCookies(new org.springframework.mock.web.MockCookie("XSRF-TOKEN", token));
			return request;
		};
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestReviewBeans {

		@Bean
		@Primary
		Clock fixedClock() {
			return Clock.fixed(NOW, ZoneOffset.UTC);
		}

		@Bean
		@Primary
		ReviewRepository reviewRepository() {
			return new StaticRepository();
		}

		@Bean
		@Primary
		ReviewService reviewService(ReviewRepository repository, Clock clock) {
			return new ReviewService(repository, clock, () -> REVIEW_ID);
		}
	}

	private static final class StaticRepository implements ReviewRepository {

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return true;
		}

		@Override
		public Optional<ReviewMembership> findMembership(UUID groupId, UUID userId) {
			if (!USER_ID.equals(userId)) {
				return Optional.empty();
			}
			return Optional.of(new ReviewMembership(groupId, MEMBER_ID, USER_ID, "현우", GroupMemberStatus.ACTIVE));
		}

		@Override
		public boolean insertReview(Review review) {
			return !DUP_GROUP_ID.equals(review.groupId());
		}

		@Override
		public Optional<Review> findReview(UUID groupId, UUID userId) {
			if (GROUP_ID.equals(groupId) && USER_ID.equals(userId)) {
				return Optional.of(review(5, "좋았습니다", groupId));
			}
			return Optional.empty();
		}

		@Override
		public List<Review> findReviews(UUID groupId) {
			return List.of(review(5, "좋았습니다", groupId), review(3, "보통", groupId));
		}

		@Override
		public List<ReviewRatingCount> findRatingCounts(UUID groupId) {
			return List.of(new ReviewRatingCount(5, 2), new ReviewRatingCount(4, 1));
		}

		private static Review review(int rating, String content, UUID groupId) {
			return Review.create(REVIEW_ID, groupId, MEMBER_ID, USER_ID, "현우", rating, content, NOW);
		}
	}
}
