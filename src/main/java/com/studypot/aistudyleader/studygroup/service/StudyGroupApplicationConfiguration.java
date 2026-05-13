package com.studypot.aistudyleader.studygroup.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.llm.service.LlmProviderClient;
import com.studypot.aistudyleader.llm.service.LlmUsageRecorder;
import com.studypot.aistudyleader.studygroup.repository.StudyGroupRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class StudyGroupApplicationConfiguration {

	private static final int INVITE_CODE_RANDOM_BYTES = 12;
	private static final int INVITE_CODE_MAX_ATTEMPTS = 5;

	@Bean
	@ConditionalOnBean(StudyGroupRepository.class)
	StudyGroupService studyGroupService(
		StudyGroupRepository repository,
		Clock clock,
		@Qualifier("studyGroupUuidGenerator") Supplier<UUID> idGenerator,
		@Qualifier("studyGroupInviteCodeGenerator") Supplier<String> inviteCodeGenerator
	) {
		return new StudyGroupService(repository, clock, idGenerator, inviteCodeGenerator, INVITE_CODE_MAX_ATTEMPTS);
	}

	@Bean
	@ConditionalOnBean({LlmProviderClient.class, LlmUsageRecorder.class})
	@ConditionalOnMissingBean(DetailKeywordSuggestionService.class)
	DetailKeywordSuggestionService detailKeywordSuggestionService(
		LlmProviderClient provider,
		LlmUsageRecorder usageRecorder,
		ObjectProvider<ObjectMapper> objectMapper,
		Clock clock
	) {
		return new DetailKeywordSuggestionService(
			provider,
			objectMapper.getIfAvailable(() -> JsonMapper.builder().findAndAddModules().build()),
			usageRecorder,
			clock,
			UuidV7::generate
		);
	}

	@Bean
	@ConditionalOnMissingBean(name = "studyGroupUuidGenerator")
	Supplier<UUID> studyGroupUuidGenerator() {
		return UuidV7::generate;
	}

	@Bean
	@ConditionalOnMissingBean(name = "studyGroupInviteCodeGenerator")
	Supplier<String> studyGroupInviteCodeGenerator() {
		SecureRandom random = new SecureRandom();
		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		return () -> {
			byte[] bytes = new byte[INVITE_CODE_RANDOM_BYTES];
			random.nextBytes(bytes);
			return encoder.encodeToString(bytes);
		};
	}
}
