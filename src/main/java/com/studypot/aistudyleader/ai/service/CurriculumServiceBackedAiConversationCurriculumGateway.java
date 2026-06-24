package com.studypot.aistudyleader.ai.service;

import com.studypot.aistudyleader.curriculum.domain.TaskCompletionStatus;
import com.studypot.aistudyleader.curriculum.service.CompleteTaskCommand;
import com.studypot.aistudyleader.curriculum.service.CurriculumService;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;

/**
 * {@link AiConversationCurriculumGateway} 어댑터: 커리큘럼 서비스로 위임한다.
 * CurriculumService 를 런타임에 ObjectProvider 로 resolve(빈 등록 순서/조건 무관).
 */
class CurriculumServiceBackedAiConversationCurriculumGateway implements AiConversationCurriculumGateway {

	private final ObjectProvider<CurriculumService> curriculumService;

	CurriculumServiceBackedAiConversationCurriculumGateway(ObjectProvider<CurriculumService> curriculumService) {
		this.curriculumService = Objects.requireNonNull(curriculumService, "curriculumService must not be null");
	}

	@Override
	public void completeTask(UUID authenticatedUserId, UUID taskId, String completionStatus) {
		CurriculumService service = curriculumService.getIfAvailable();
		if (service == null) {
			throw new AiConversationServiceUnavailableException("curriculum service is not available.");
		}
		TaskCompletionStatus status;
		try {
			status = TaskCompletionStatus.valueOf(completionStatus);
		} catch (IllegalArgumentException exception) {
			throw new AiConversationMutationRejectedException("unsupported task completion status: " + completionStatus + ".");
		}
		service.completeMyTask(new CompleteTaskCommand(authenticatedUserId, taskId, status, null, null, null));
	}
}
