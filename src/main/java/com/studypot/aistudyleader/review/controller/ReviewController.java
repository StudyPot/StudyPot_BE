package com.studypot.aistudyleader.review.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.review.CreateReviewCommand;
import com.studypot.aistudyleader.review.Review;
import com.studypot.aistudyleader.review.ReviewRatingSummary;
import com.studypot.aistudyleader.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "리뷰", description = "리뷰 작성, 조회, 평점 집계, 중복 방지, 작성자 검증 API입니다.")
@RestController
@RequiredArgsConstructor
class ReviewController {

	private final ReviewService reviewService;

	@Operation(summary = "대상 리뷰 조회", description = "사용자 또는 스터디 대상에 작성된 리뷰 목록을 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리뷰 목록 반환"),
		@ApiResponse(responseCode = "422", description = "대상 UUID 형식 오류")
	})
	@GetMapping(ApiPaths.V1 + "/review-targets/{targetId}/reviews")
	List<ReviewResponse> listReviews(
		@Parameter(description = "리뷰 대상 UUID입니다.", required = true) @PathVariable UUID targetId
	) {
		return reviewService.listReviews(targetId).stream()
			.map(ReviewResponse::from)
			.toList();
	}

	@Operation(summary = "리뷰 평점 집계", description = "리뷰 수와 평균 평점을 집계합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "리뷰 평점 집계 반환"),
		@ApiResponse(responseCode = "422", description = "대상 UUID 형식 오류")
	})
	@GetMapping(ApiPaths.V1 + "/review-targets/{targetId}/reviews/summary")
	ReviewRatingSummaryResponse getRatingSummary(
		@Parameter(description = "평점 집계 대상 UUID입니다.", required = true) @PathVariable UUID targetId
	) {
		return ReviewRatingSummaryResponse.from(reviewService.getRatingSummary(targetId));
	}

	@Operation(summary = "리뷰 작성", description = "인증 사용자가 대상에 한 번만 리뷰를 작성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "리뷰 생성"),
		@ApiResponse(responseCode = "401", description = "인증 사용자 확인 실패"),
		@ApiResponse(responseCode = "409", description = "같은 대상에 이미 리뷰를 작성함"),
		@ApiResponse(responseCode = "422", description = "리뷰 평점 또는 본문 형식 오류")
	})
	@PostMapping(ApiPaths.V1 + "/review-targets/{targetId}/reviews")
	@ResponseStatus(HttpStatus.CREATED)
	ReviewResponse createReview(
		Authentication authentication,
		@Parameter(description = "리뷰를 작성할 대상 UUID입니다.", required = true) @PathVariable UUID targetId,
		@Valid @RequestBody CreateReviewRequest request
	) {
		Review review = reviewService.createReview(request.toCommand(targetId, authenticatedUserId(authentication)));
		return ReviewResponse.from(review);
	}

	@Operation(summary = "리뷰 삭제", description = "작성자 검증을 통과한 리뷰만 삭제합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "리뷰 삭제"),
		@ApiResponse(responseCode = "401", description = "인증 사용자 확인 실패"),
		@ApiResponse(responseCode = "403", description = "리뷰 작성자가 아님"),
		@ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음")
	})
	@DeleteMapping(ApiPaths.V1 + "/reviews/{reviewId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteReview(
		Authentication authentication,
		@Parameter(description = "삭제할 리뷰 UUID입니다.", required = true) @PathVariable UUID reviewId
	) {
		reviewService.deleteReview(reviewId, authenticatedUserId(authentication));
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

	@Schema(description = "리뷰 작성 요청입니다.")
	private record CreateReviewRequest(
		@Min(1) @Max(5) @Schema(description = "1점부터 5점까지의 리뷰 평점입니다.", example = "5") int rating,
		@NotBlank @Schema(description = "리뷰 본문입니다.", example = "스터디 진행이 안정적이었습니다.") String content
	) {

		private CreateReviewCommand toCommand(UUID targetId, UUID authorId) {
			return new CreateReviewCommand(targetId, authorId, rating, content);
		}
	}

	@Schema(description = "리뷰 응답입니다.")
	private record ReviewResponse(
		UUID id,
		UUID targetId,
		UUID authorId,
		int rating,
		String content,
		Instant createdAt,
		Instant updatedAt
	) {

		private static ReviewResponse from(Review review) {
			return new ReviewResponse(
				review.id(),
				review.targetId(),
				review.authorId(),
				review.rating(),
				review.content(),
				review.createdAt(),
				review.updatedAt()
			);
		}
	}

	@Schema(description = "리뷰 평점 집계 응답입니다.")
	private record ReviewRatingSummaryResponse(UUID targetId, int reviewCount, double averageRating) {

		private static ReviewRatingSummaryResponse from(ReviewRatingSummary summary) {
			return new ReviewRatingSummaryResponse(summary.targetId(), summary.reviewCount(), summary.averageRating());
		}
	}
}
