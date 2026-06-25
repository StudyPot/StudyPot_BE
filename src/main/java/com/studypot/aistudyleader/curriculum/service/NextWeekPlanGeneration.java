package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.llm.service.LlmStructuredResponse;
import java.util.Objects;

public record NextWeekPlanGeneration(NextWeekPlan plan, LlmStructuredResponse response) {

	public NextWeekPlanGeneration {
		Objects.requireNonNull(plan, "plan must not be null");
		Objects.requireNonNull(response, "response must not be null");
	}
}
