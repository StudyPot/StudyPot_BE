package com.studypot.aistudyleader.review.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 그룹 리뷰 통계입니다. ratingDistribution 은 1~5 평점별 개수를 모두 포함합니다.
 */
public record ReviewStats(
	double averageRating,
	int totalCount,
	Map<Integer, Integer> ratingDistribution
) {

	public ReviewStats {
		ratingDistribution = Map.copyOf(ratingDistribution);
	}

	public static ReviewStats from(List<ReviewRatingCount> ratingCounts) {
		Map<Integer, Integer> distribution = new LinkedHashMap<>();
		for (int rating = Review.MIN_RATING; rating <= Review.MAX_RATING; rating++) {
			distribution.put(rating, 0);
		}
		int total = 0;
		long ratingSum = 0;
		for (ReviewRatingCount ratingCount : ratingCounts) {
			distribution.merge(ratingCount.rating(), ratingCount.count(), Integer::sum);
			total += ratingCount.count();
			ratingSum += (long) ratingCount.rating() * ratingCount.count();
		}
		double average = total == 0 ? 0.0 : Math.round((double) ratingSum / total * 10.0) / 10.0;
		return new ReviewStats(average, total, distribution);
	}
}
