package com.studypot.aistudyleader.llm.service;

import com.studypot.aistudyleader.llm.domain.LlmUsage;

public interface LlmUsageRecorder {

	void record(LlmUsage usage);
}
