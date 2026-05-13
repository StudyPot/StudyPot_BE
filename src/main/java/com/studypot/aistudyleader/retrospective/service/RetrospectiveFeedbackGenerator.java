package com.studypot.aistudyleader.retrospective.service;

import com.studypot.aistudyleader.retrospective.domain.Retrospective;

interface RetrospectiveFeedbackGenerator {

	RetrospectiveFeedbackGeneration generate(Retrospective retrospective);
}
