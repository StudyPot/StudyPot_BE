package com.studypot.aistudyleader.review.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.review.domain.Review;
import com.studypot.aistudyleader.review.domain.ReviewStats;
import com.studypot.aistudyleader.review.service.CreateReviewCommand;
import com.studypot.aistudyleader.review.service.GetMyReviewQuery;
import com.studypot.aistudyleader.review.service.GetReviewStatsQuery;
import com.studypot.aistudyleader.review.service.ListReviewsQuery;
import com.studypot.aistudyleader.review.service.ReviewService;
import com.studypot.aistudyleader.review.service.ReviewServiceUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스터디 그룹 리뷰", description = "스터디 그룹 종료 후 멤버가 남기는 리뷰와 평점 통계를 제공하는 API입니다.")
@RestController
@RequiredArgsConstructor
class ReviewController {

	private final ObjectProvider<ReviewService> reviewService;

	@Operation(
		summary = "그룹 리뷰 작성",
		description = "활성 그룹 멤버가 1~5 평점과 선택적 내용을 담은 리뷰를 작성합니다. 그룹당 한 번만 작성할 수 있습니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "리뷰가 작성되고 반환됨"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "409", description = "이미 작성한 리뷰가 있음"),
		@ApiResponse(responseCode = "422", description = "평점 검증 실패"),
		@ApiResponse(responseCode = "503", description = "리뷰 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	ReviewResponse createReview(
		Authentication authentication,
		@Parameter(description = "리뷰를 작성할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Valid @RequestBody CreateReviewRequest request
	) {
		Review review = service().createReview(request.toCommand(authenticatedUserId(authentication), groupId));
		return ReviewResponse.from(review);
	}

	@Operation(
		summary = "그룹 리뷰 목록 조회",
		description = "활성 그룹 멤버가 그룹에 작성된 모든 리뷰를 최신순으로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리뷰 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "리뷰 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/reviews")
	List<ReviewResponse> listReviews(
		Authentication authentication,
		@Parameter(description = "리뷰 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listReviews(new ListReviewsQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(ReviewResponse::from)
			.toList();
	}

	@Operation(
		summary = "내 그룹 리뷰 조회",
		description = "활성 그룹 멤버가 본인이 작성한 리뷰를 조회합니다. 작성한 리뷰가 없으면 404를 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "내 리뷰 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹이 없거나 작성한 리뷰가 없음"),
		@ApiResponse(responseCode = "503", description = "리뷰 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/reviews/me")
	ReviewResponse getMyReview(
		Authentication authentication,
		@Parameter(description = "내 리뷰를 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		Review review = service().getMyReview(new GetMyReviewQuery(authenticatedUserId(authentication), groupId));
		return ReviewResponse.from(review);
	}

	@Operation(
		summary = "그룹 리뷰 통계 조회",
		description = "활성 그룹 멤버가 그룹 리뷰의 평균 평점, 총 개수, 평점별 분포를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리뷰 통계 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "리뷰 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/reviews/stats")
	ReviewStatsResponse getReviewStats(
		Authentication authentication,
		@Parameter(description = "리뷰 통계를 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		ReviewStats stats = service().getReviewStats(new GetReviewStatsQuery(authenticatedUserId(authentication), groupId));
		return ReviewStatsResponse.from(stats);
	}

	private ReviewService service() {
		return reviewService.getIfAvailable(() -> {
			throw new ReviewServiceUnavailableException("review service is not configured.");
		});
	}

	private static UUID authenticatedUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		String subject = authenticatedSubject(authentication);
		if (subject == null || subject.isBlank()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}

	private static String authenticatedSubject(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return authentication.getName();
	}

	@Schema(description = "그룹 리뷰 작성 요청입니다.")
	private record CreateReviewRequest(
		@Schema(description = "1~5 사이의 평점입니다.", example = "5")
		@NotNull
		@Min(1)
		@Max(5)
		Integer rating,
		@Schema(description = "선택적인 리뷰 내용입니다.", example = "팀원들과 함께 많이 배웠습니다.")
		@Size(max = 2000)
		String content
	) {

		CreateReviewCommand toCommand(UUID authenticatedUserId, UUID groupId) {
			return new CreateReviewCommand(authenticatedUserId, groupId, rating, content);
		}
	}

	@Schema(description = "그룹 리뷰 응답입니다.")
	private record ReviewResponse(
		@Schema(description = "리뷰 UUID입니다.", example = "018f6f55-8f6c-7334-a781-84152e57e4f4")
		UUID id,
		@Schema(description = "리뷰가 속한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "리뷰를 작성한 사용자 UUID입니다.", example = "018f6f55-6f42-7e11-b479-120c5f2e9d42")
		UUID userId,
		@Schema(description = "작성자 표시 이름입니다. 없으면 null입니다.", example = "현우")
		String displayName,
		@Schema(description = "평점입니다.", example = "5")
		int rating,
		@Schema(description = "리뷰 내용입니다. 없으면 null입니다.", example = "팀원들과 함께 많이 배웠습니다.")
		String content,
		@Schema(description = "작성 시각입니다.", example = "2026-06-22T10:20:30Z")
		Instant createdAt
	) {

		private static ReviewResponse from(Review review) {
			return new ReviewResponse(
				review.id(),
				review.groupId(),
				review.userId(),
				review.displayName(),
				review.rating(),
				review.content(),
				review.createdAt()
			);
		}
	}

	@Schema(description = "그룹 리뷰 통계 응답입니다.")
	private record ReviewStatsResponse(
		@Schema(description = "평균 평점입니다.", example = "4.3")
		double averageRating,
		@Schema(description = "총 리뷰 개수입니다.", example = "7")
		int totalCount,
		@Schema(description = "평점(1~5)별 리뷰 개수입니다.", example = "{\"1\":0,\"2\":1,\"3\":1,\"4\":2,\"5\":3}")
		Map<String, Integer> ratingDistribution
	) {

		private static ReviewStatsResponse from(ReviewStats stats) {
			Map<String, Integer> distribution = new TreeMap<>();
			stats.ratingDistribution().forEach((rating, count) -> distribution.put(String.valueOf(rating), count));
			return new ReviewStatsResponse(stats.averageRating(), stats.totalCount(), distribution);
		}
	}
}
