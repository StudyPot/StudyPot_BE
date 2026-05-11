package com.studypot.aistudyleader.curriculum.service;

import com.studypot.aistudyleader.curriculum.domain.CurriculumGeneration;
import com.studypot.aistudyleader.curriculum.domain.CurriculumGenerationRequest;

@FunctionalInterface
public interface CurriculumGenerator {

	CurriculumGeneration generate(CurriculumGenerationRequest request);
}
