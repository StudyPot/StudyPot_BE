package com.studypot.aistudyleader.studygroup.catalog;

import com.studypot.aistudyleader.auth.service.InvalidAuthRequestException;
import com.studypot.aistudyleader.studygroup.service.StudyGroupNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StudyGroupCatalogService {

	private final StudyGroupCatalogMapper mapper;

	public StudyGroupCatalogService(StudyGroupCatalogMapper mapper) {
		this.mapper = mapper;
	}

	public StudyGroupCatalogEntry create(StudyGroupCatalogCommand command) {
		StudyGroupCatalogCommand validCommand = validate(command);
		return mapper.insertStudyGroup(new StudyGroupCatalogEntry(
			UUID.randomUUID(),
			validCommand.name().strip(),
			validCommand.topic().strip(),
			validCommand.status().strip().toUpperCase(),
			validCommand.startsAt(),
			validCommand.endsAt(),
			1,
			0,
			validCommand.favorite()
		));
	}

	public StudyGroupCatalogPage search(String keyword, String status, String sort, int pageSize, String cursor) {
		int boundedPageSize = Math.clamp(pageSize, 1, 50);
		List<StudyGroupCatalogEntry> fetched = mapper.searchStudyGroups(keyword, status, sort, boundedPageSize, cursor);
		if (fetched.size() <= boundedPageSize) {
			return new StudyGroupCatalogPage(fetched, null);
		}
		List<StudyGroupCatalogEntry> items = fetched.subList(0, boundedPageSize);
		return new StudyGroupCatalogPage(items, items.getLast().id().toString());
	}

	public StudyGroupCatalogEntry detail(UUID groupId) {
		return mapper.findStudyGroupDetail(groupId)
			.orElseThrow(() -> new StudyGroupNotFoundException("study group catalog detail was not found."));
	}

	public StudyGroupCatalogEntry update(UUID groupId, StudyGroupCatalogCommand command) {
		detail(groupId);
		return mapper.updateStudyGroup(groupId, validate(command));
	}

	public void delete(UUID groupId) {
		if (!mapper.deleteStudyGroup(groupId)) {
			throw new StudyGroupNotFoundException("study group catalog detail was not found.");
		}
	}

	private static StudyGroupCatalogCommand validate(StudyGroupCatalogCommand command) {
		if (command == null) {
			throw new InvalidAuthRequestException("request", "study group request is required.");
		}
		if (command.name() == null || command.name().isBlank()) {
			throw new InvalidAuthRequestException("name", "name is required.");
		}
		if (command.topic() == null || command.topic().isBlank()) {
			throw new InvalidAuthRequestException("topic", "topic is required.");
		}
		if (command.status() == null || command.status().isBlank()) {
			throw new InvalidAuthRequestException("status", "status is required.");
		}
		if (command.startsAt() == null || command.endsAt() == null || command.endsAt().isBefore(command.startsAt())) {
			throw new InvalidAuthRequestException("period", "startsAt and endsAt are required in order.");
		}
		return new StudyGroupCatalogCommand(
			command.name(),
			command.topic(),
			command.status(),
			LocalDate.from(command.startsAt()),
			LocalDate.from(command.endsAt()),
			command.favorite()
		);
	}
}
