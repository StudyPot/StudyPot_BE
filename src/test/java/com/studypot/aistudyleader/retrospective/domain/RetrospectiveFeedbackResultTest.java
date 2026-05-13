package com.studypot.aistudyleader.retrospective.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetrospectiveFeedbackResultTest {

	private static final UUID MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000006401");

	@Test
	void mapsAiFeedbackAndNextWeekAdjustmentShape() {
		RetrospectiveFeedbackResult result = RetrospectiveFeedbackResult.of(
			"미완료 사유가 실습 시간 부족에 집중되어 있습니다.",
			List.of("완료한 읽기 과제 정리가 명확합니다."),
			List.of("실습 과제가 마감 직전에 밀렸습니다."),
			List.of("다음 주 필수 실습을 하나 줄이고 보충 자료를 먼저 제공합니다."),
			Map.of(
				"difficulty", "slightly_lower",
				"taskChanges", List.of("필수 실습 1개를 선택 과제로 전환"),
				"supportMaterials", List.of("JPA 실습 전 체크리스트"),
				"memberNotes", List.of(Map.of("memberId", MEMBER_ID.toString(), "note", "실습 시간을 먼저 확보합니다."))
			)
		);

		assertThat(result.aiFeedback())
			.containsEntry("summary", "미완료 사유가 실습 시간 부족에 집중되어 있습니다.")
			.containsEntry("strengths", List.of("완료한 읽기 과제 정리가 명확합니다."))
			.containsEntry("risks", List.of("실습 과제가 마감 직전에 밀렸습니다."))
			.containsEntry("actionItems", List.of("다음 주 필수 실습을 하나 줄이고 보충 자료를 먼저 제공합니다."));
		assertThat(result.nextWeekAdjustment())
			.containsEntry("difficulty", "slightly_lower")
			.containsEntry("taskChanges", List.of("필수 실습 1개를 선택 과제로 전환"))
			.containsEntry("supportMaterials", List.of("JPA 실습 전 체크리스트"))
			.containsEntry("memberNotes", List.of(Map.of("memberId", MEMBER_ID.toString(), "note", "실습 시간을 먼저 확보합니다.")));
	}

	@Test
	void rejectsBlankSummary() {
		assertThatThrownBy(() -> RetrospectiveFeedbackResult.of(
				" ",
				List.of("강점"),
				List.of(),
				List.of(),
				Map.of()
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("summary must not be blank.");
	}

	@Test
	void rejectsInvalidNextWeekAdjustmentShape() {
		assertThatThrownBy(() -> RetrospectiveFeedbackResult.of(
				"요약",
				List.of(),
				List.of(),
				List.of(),
				Map.of("taskChanges", "문자열은 허용하지 않음")
			))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("nextWeekAdjustment.taskChanges must be a list of strings.");
	}
}
