package com.studypot.aistudyleader.ai.service;

import java.util.UUID;

/**
 * AI 팀장 대화에서 확정된 커리큘럼 액션(과제 완료/미완료 토글, 과제 추가)을 실제 수행으로 위임하는 포트.
 * ai 모듈은 이 추상화에만 의존하고, 구현(어댑터)이 커리큘럼 서비스로 연결한다.
 */
public interface AiConversationCurriculumGateway {

	/** 멤버 본인 과제의 완료 상태를 변경한다(권한/소유 검증은 커리큘럼 서비스가 수행). */
	void completeTask(UUID authenticatedUserId, UUID taskId, String completionStatus);
}
