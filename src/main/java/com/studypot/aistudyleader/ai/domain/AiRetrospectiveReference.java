package com.studypot.aistudyleader.ai.domain;

import java.util.Objects;
import java.util.UUID;

public record AiRetrospectiveReference(UUID groupId, UUID memberId, UUID curriculumWeekId) {

	public AiRetrospectiveReference {
		Objects.requireNonNull(groupId, "groupId must not be null");
		Objects.requireNonNull(memberId, "memberId must not be null");
		Objects.requireNonNull(curriculumWeekId, "curriculumWeekId must not be null");
	}
}
