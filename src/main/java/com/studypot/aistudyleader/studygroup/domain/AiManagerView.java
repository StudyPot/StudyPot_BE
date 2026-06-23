package com.studypot.aistudyleader.studygroup.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 그룹의 AI 팀장 퍼소나 조회 결과입니다. 퍼소나 미설정 시 persona 는 빈 문자열,
 * updatedAt/updatedByNickname 은 null 입니다.
 */
public record AiManagerView(
	UUID groupId,
	String persona,
	Instant updatedAt,
	String updatedByNickname
) {

	public AiManagerView {
		Objects.requireNonNull(persona, "persona must not be null");
	}
}
