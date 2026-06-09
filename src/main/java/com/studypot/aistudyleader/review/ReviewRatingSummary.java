package com.studypot.aistudyleader.review;

import java.util.UUID;

public record ReviewRatingSummary(UUID targetId, int reviewCount, double averageRating) {
}
