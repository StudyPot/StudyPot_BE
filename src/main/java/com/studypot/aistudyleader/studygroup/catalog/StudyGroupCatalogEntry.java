package com.studypot.aistudyleader.studygroup.catalog;

import java.time.LocalDate;
import java.util.UUID;

public record StudyGroupCatalogEntry(
	UUID id,
	String name,
	String topic,
	String status,
	LocalDate startsAt,
	LocalDate endsAt,
	int memberCount,
	double averageRating,
	boolean favorite
) {
}
