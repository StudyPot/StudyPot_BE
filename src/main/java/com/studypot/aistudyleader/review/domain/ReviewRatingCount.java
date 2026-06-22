package com.studypot.aistudyleader.review.domain;

public record ReviewRatingCount(
	int rating,
	int count
) {

	public ReviewRatingCount {
		if (rating < Review.MIN_RATING || rating > Review.MAX_RATING) {
			throw new IllegalArgumentException("rating must be between " + Review.MIN_RATING + " and " + Review.MAX_RATING + ".");
		}
		if (count < 0) {
			throw new IllegalArgumentException("count must not be negative.");
		}
	}
}
