package com.studypot.aistudyleader.studygroup.catalog;

import java.time.LocalDate;

public record StudyGroupCatalogCommand(
	String name,
	String topic,
	String status,
	LocalDate startsAt,
	LocalDate endsAt,
	boolean favorite
) {
}
