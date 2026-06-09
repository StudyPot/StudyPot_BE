package com.studypot.aistudyleader.studygroup.catalog;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

class InMemoryStudyGroupCatalogMapper implements StudyGroupCatalogMapper {

	private final Map<UUID, StudyGroupCatalogEntry> entries = new ConcurrentHashMap<>();

	InMemoryStudyGroupCatalogMapper() {
		StudyGroupCatalogEntry seed = new StudyGroupCatalogEntry(
			UUID.fromString("018f0000-0000-7000-8000-00000000c001"),
			"백엔드 인터뷰 스터디",
			"Spring Boot",
			"ACTIVE",
			LocalDate.parse("2026-06-08"),
			LocalDate.parse("2026-07-08"),
			4,
			4.8,
			true
		);
		entries.put(seed.id(), seed);
	}

	@Override
	public StudyGroupCatalogEntry insertStudyGroup(StudyGroupCatalogEntry entry) {
		entries.put(entry.id(), entry);
		return entry;
	}

	@Override
	public List<StudyGroupCatalogEntry> searchStudyGroups(String keyword, String status, String sort, int pageSize, String cursor) {
		String normalizedKeyword = normalize(keyword);
		String normalizedStatus = normalize(status);
		return entries.values().stream()
			.filter(entry -> normalizedKeyword.isBlank()
				|| entry.name().toLowerCase().contains(normalizedKeyword)
				|| entry.topic().toLowerCase().contains(normalizedKeyword))
			.filter(entry -> normalizedStatus.isBlank() || entry.status().equalsIgnoreCase(normalizedStatus))
			.sorted(comparator(sort))
			.dropWhile(entry -> cursor != null && !cursor.isBlank() && !entry.id().toString().equals(cursor))
			.skip(cursor == null || cursor.isBlank() ? 0 : 1)
			.limit(Math.clamp(pageSize, 1, 50) + 1L)
			.toList();
	}

	@Override
	public Optional<StudyGroupCatalogEntry> findStudyGroupDetail(UUID groupId) {
		return Optional.ofNullable(entries.get(groupId));
	}

	@Override
	public StudyGroupCatalogEntry updateStudyGroup(UUID groupId, StudyGroupCatalogCommand command) {
		StudyGroupCatalogEntry previous = entries.get(groupId);
		StudyGroupCatalogEntry updated = new StudyGroupCatalogEntry(
			groupId,
			command.name().strip(),
			command.topic().strip(),
			command.status().strip().toUpperCase(),
			command.startsAt(),
			command.endsAt(),
			previous.memberCount(),
			previous.averageRating(),
			command.favorite()
		);
		entries.put(groupId, updated);
		return updated;
	}

	@Override
	public boolean deleteStudyGroup(UUID groupId) {
		return entries.remove(groupId) != null;
	}

	private static Comparator<StudyGroupCatalogEntry> comparator(String sort) {
		if ("name".equalsIgnoreCase(sort)) {
			return Comparator.comparing(StudyGroupCatalogEntry::name);
		}
		if ("startDate".equalsIgnoreCase(sort)) {
			return Comparator.comparing(StudyGroupCatalogEntry::startsAt).reversed();
		}
		return Comparator.comparing(StudyGroupCatalogEntry::favorite).reversed()
			.thenComparing(StudyGroupCatalogEntry::startsAt, Comparator.reverseOrder());
	}

	private static String normalize(String value) {
		return value == null ? "" : value.strip().toLowerCase();
	}
}
