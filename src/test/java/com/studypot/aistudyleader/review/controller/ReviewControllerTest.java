package com.studypot.aistudyleader.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = AiStudyLeaderApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReviewControllerTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000127001");
	private static final UUID AUTHOR_ID = UUID.fromString("018f0000-0000-7000-8000-000000127002");
	private static final UUID OTHER_AUTHOR_ID = UUID.fromString("018f0000-0000-7000-8000-000000127003");
	private static final String GROUP_REVIEWS_PATH = ApiPaths.V1 + "/groups/" + GROUP_ID + "/reviews";
	private static final String GROUP_STATS_PATH = GROUP_REVIEWS_PATH + "/stats";

	private final MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	ReviewControllerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void duplicateGroupReviewReturnsConflictProblemDetails() throws Exception {
		mockMvc.perform(post(GROUP_REVIEWS_PATH)
				.with(user(AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(5, "첫 리뷰입니다.")))
			.andExpect(status().isCreated());

		mockMvc.perform(post(GROUP_REVIEWS_PATH)
				.with(user(AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(4, "다시 작성합니다.")))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Conflict"))
			.andExpect(jsonPath("$.detail").value("review already exists for target and author."));
	}

	@Test
	void deleteGroupReviewAllowsOnlyOriginalAuthor() throws Exception {
		MvcResult result = mockMvc.perform(post(GROUP_REVIEWS_PATH)
				.with(user(AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(5, "삭제 권한 확인용 리뷰입니다.")))
			.andExpect(status().isCreated())
			.andReturn();
		JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
		String reviewId = body.get("id").asText();

		mockMvc.perform(delete(ApiPaths.V1 + "/reviews/" + reviewId)
				.with(user(OTHER_AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.detail").value("review author does not match requester."));

		mockMvc.perform(delete(ApiPaths.V1 + "/reviews/" + reviewId)
				.with(user(AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf")))
			.andExpect(status().isNoContent());
	}

	@Test
	void groupReviewStatsReturnsAverageCountAndFullRatingDistribution() throws Exception {
		mockMvc.perform(post(GROUP_REVIEWS_PATH)
				.with(user(AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(5, "좋은 스터디입니다.")))
			.andExpect(status().isCreated());
		mockMvc.perform(post(GROUP_REVIEWS_PATH)
				.with(user(OTHER_AUTHOR_ID.toString()))
				.with(xsrf("review-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(reviewJson(3, "보통입니다.")))
			.andExpect(status().isCreated());

		mockMvc.perform(get(GROUP_STATS_PATH)
				.with(user(AUTHOR_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.averageRating").value(4.0))
			.andExpect(jsonPath("$.totalCount").value(2))
			.andExpect(jsonPath("$.ratingDistribution.1").value(0))
			.andExpect(jsonPath("$.ratingDistribution.2").value(0))
			.andExpect(jsonPath("$.ratingDistribution.3").value(1))
			.andExpect(jsonPath("$.ratingDistribution.4").value(0))
			.andExpect(jsonPath("$.ratingDistribution.5").value(1));
	}

	@Test
	void reviewApiDocumentsDuplicateAuthorAndDistributionContracts() throws Exception {
		Class<?> requestType = Class.forName(ReviewController.class.getName() + "$CreateReviewRequest");
		Method createGroupReview = ReviewController.class.getDeclaredMethod(
			"createGroupReview",
			org.springframework.security.core.Authentication.class,
			UUID.class,
			requestType
		);
		Method deleteReview = ReviewController.class.getDeclaredMethod(
			"deleteReview",
			org.springframework.security.core.Authentication.class,
			UUID.class
		);
		Method getGroupReviewStats = ReviewController.class.getDeclaredMethod("getGroupReviewStats", UUID.class);

		assertThat(createGroupReview.getAnnotation(Operation.class).description())
			.contains("targetId+authorId")
			.contains("409 Conflict");
		assertThat(deleteReview.getAnnotation(Operation.class).description())
			.contains("작성자만")
			.contains("403 Forbidden");
		assertThat(getGroupReviewStats.getAnnotation(Operation.class).description())
			.contains("ratingDistribution")
			.contains("1~5");
	}

	private static String reviewJson(int rating, String content) {
		return """
			{
			  "rating": %d,
			  "content": "%s"
			}
			""".formatted(rating, content);
	}

	private static RequestPostProcessor xsrf(String token) {
		return request -> {
			request.addHeader("X-XSRF-TOKEN", token);
			request.setCookies(new MockCookie("XSRF-TOKEN", token));
			return request;
		};
	}
}
