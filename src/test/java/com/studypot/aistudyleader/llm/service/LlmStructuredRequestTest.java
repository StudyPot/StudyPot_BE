package com.studypot.aistudyleader.llm.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.llm.domain.LlmUsagePurpose;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LlmStructuredRequestTest {

	@Test
	void rejectsNullEntriesWithParameterName() {
		Map<String, Object> input = new HashMap<>();
		input.put("topic", null);

		assertThatThrownBy(() -> new LlmStructuredRequest(
				LlmUsagePurpose.CURRICULUM_GENERATE,
				"Return JSON.",
				input,
				Map.of("type", "json_schema"),
				Map.of("purpose", "CURRICULUM_GENERATE")
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("input must not contain null key or value");
	}
}
