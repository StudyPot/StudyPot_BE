package com.studypot.aistudyleader.notification.domain;

import java.util.UUID;

public record NotificationRelatedResources(
	UUID onboardingResponseId,
	UUID weekId,
	UUID taskCompletionId,
	UUID retrospectiveId
) {
}
