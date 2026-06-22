package com.studypot.aistudyleader.studygroup.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StudyGroupCatalogServiceTest {

	@Test
	void searchFetchesOneExtraRowToProduceNextCursor() {
		RecordingCatalogMapper mapper = new RecordingCatalogMapper();
		StudyGroupCatalogEntry first = entry("018f0000-0000-7000-8000-00000000c801", "A");
		StudyGroupCatalogEntry second = entry("018f0000-0000-7000-8000-00000000c802", "B");
		StudyGroupCatalogEntry third = entry("018f0000-0000-7000-8000-00000000c803", "C");
		mapper.results.addAll(List.of(first, second, third));

		StudyGroupCatalogPage page = new StudyGroupCatalogService(mapper).search("spring", "ACTIVE", "name", 2, null);

		assertThat(mapper.requestedPageSize).isEqualTo(3);
		assertThat(page.items()).containsExactly(first, second);
		assertThat(page.nextCursor()).isEqualTo(second.id().toString());
	}

	private static StudyGroupCatalogEntry entry(String id, String name) {
		return new StudyGroupCatalogEntry(
			UUID.fromString(id),
			name,
			"Spring Boot",
			"ACTIVE",
			LocalDate.parse("2026-06-09"),
			LocalDate.parse("2026-07-09"),
			1,
			0,
			false
		);
	}

	private static final class RecordingCatalogMapper implements StudyGroupCatalogMapper {

		private final List<StudyGroupCatalogEntry> results = new ArrayList<>();
		private int requestedPageSize;

		@Override
		public StudyGroupCatalogEntry insertStudyGroup(StudyGroupCatalogEntry entry) {
			return entry;
		}

		@Override
		public List<StudyGroupCatalogEntry> searchStudyGroups(String keyword, String status, String sort, int pageSize, String cursor) {
			requestedPageSize = pageSize;
			return results;
		}

		@Override
		public Optional<StudyGroupCatalogEntry> findStudyGroupDetail(UUID groupId) {
			return Optional.empty();
		}

		@Override
		public StudyGroupCatalogEntry updateStudyGroup(UUID groupId, StudyGroupCatalogCommand command) {
			return results.getFirst();
		}

		@Override
		public boolean deleteStudyGroup(UUID groupId) {
			return false;
		}
	}
}
