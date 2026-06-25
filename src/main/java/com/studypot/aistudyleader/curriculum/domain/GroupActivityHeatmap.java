package com.studypot.aistudyleader.curriculum.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 그룹 멤버별 일자 활동량(완료한 과제 수) 히트맵입니다.
 * days 는 startDate~endDate(포함) 의 연속된 날짜이고, 각 멤버의 counts 는 days 와 같은 순서로 정렬됩니다.
 */
public record GroupActivityHeatmap(
	LocalDate startDate,
	LocalDate endDate,
	List<LocalDate> days,
	List<MemberActivity> members
) {

	public record MemberActivity(
		UUID memberId,
		UUID userId,
		String displayName,
		String nickname,
		List<Integer> counts,
		List<Integer> todoCounts,
		List<Integer> postCounts
	) {
	}

	public static GroupActivityHeatmap of(LocalDate startDate, LocalDate endDate, List<GroupActivityCount> rows) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate must not be before startDate.");
		}
		List<LocalDate> days = buildDays(startDate, endDate);
		Map<LocalDate, Integer> dayIndex = new LinkedHashMap<>();
		for (int i = 0; i < days.size(); i++) {
			dayIndex.put(days.get(i), i);
		}

		Map<UUID, MemberAccumulator> accumulators = new LinkedHashMap<>();
		for (GroupActivityCount row : rows) {
			MemberAccumulator accumulator = accumulators.computeIfAbsent(
				row.memberId(),
				ignored -> new MemberAccumulator(row, days.size())
			);
			if (row.date() != null) {
				Integer index = dayIndex.get(row.date());
				if (index != null) {
					accumulator.counts.set(index, accumulator.counts.get(index) + row.count());
					accumulator.todoCounts.set(index, accumulator.todoCounts.get(index) + row.todoCount());
					accumulator.postCounts.set(index, accumulator.postCounts.get(index) + row.postCount());
				}
			}
		}

		List<MemberActivity> members = new ArrayList<>(accumulators.size());
		for (MemberAccumulator accumulator : accumulators.values()) {
			members.add(new MemberActivity(
				accumulator.memberId,
				accumulator.userId,
				accumulator.displayName,
				accumulator.nickname,
				List.copyOf(accumulator.counts),
				List.copyOf(accumulator.todoCounts),
				List.copyOf(accumulator.postCounts)
			));
		}
		return new GroupActivityHeatmap(startDate, endDate, days, members);
	}

	private static List<LocalDate> buildDays(LocalDate startDate, LocalDate endDate) {
		int size = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
		List<LocalDate> days = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			days.add(startDate.plusDays(i));
		}
		return days;
	}

	private static final class MemberAccumulator {

		private final UUID memberId;
		private final UUID userId;
		private final String displayName;
		private final String nickname;
		private final List<Integer> counts;
		private final List<Integer> todoCounts;
		private final List<Integer> postCounts;

		private MemberAccumulator(GroupActivityCount row, int dayCount) {
			this.memberId = row.memberId();
			this.userId = row.userId();
			this.displayName = row.displayName();
			this.nickname = row.nickname();
			this.counts = new ArrayList<>(dayCount);
			this.todoCounts = new ArrayList<>(dayCount);
			this.postCounts = new ArrayList<>(dayCount);
			for (int i = 0; i < dayCount; i++) {
				this.counts.add(0);
				this.todoCounts.add(0);
				this.postCounts.add(0);
			}
		}
	}
}
