package com.studypot.aistudyleader.studygroup.catalog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogCommand;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogEntry;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, StudyGroupCatalogControllerTest.TestCatalogBeans.class})
@AutoConfigureMockMvc
class StudyGroupCatalogControllerTest {

	private static final String CATALOG_PATH = ApiPaths.V1 + "/groups/catalog";
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000125001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000125002");

	private final MockMvc mockMvc;
	private final MutableStudyGroupCatalogMapper mapper;

	@Autowired
	StudyGroupCatalogControllerTest(MockMvc mockMvc, MutableStudyGroupCatalogMapper mapper) {
		this.mockMvc = mockMvc;
		this.mapper = mapper;
	}

	@BeforeEach
	void resetMapper() {
		mapper.clear();
	}

	@Test
	void createCatalogRejectsInvalidRequestWithProblemDetails() throws Exception {
		mockMvc.perform(post(CATALOG_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("catalog-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "",
					  "topic": "Spring",
					  "status": "ACTIVE",
					  "startsAt": "2026-06-20",
					  "endsAt": null,
					  "favorite": true
					}
					"""))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors").isArray());
	}

	@Test
	void createDetailUpdateDeleteCatalogRoundTripUsesCrudContract() throws Exception {
		mockMvc.perform(post(CATALOG_PATH)
				.with(user(USER_ID.toString()))
				.with(xsrf("catalog-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validCatalogJson("백엔드 스터디", "Spring Boot", false)))
			.andExpect(status().isCreated())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name").value("백엔드 스터디"))
			.andExpect(jsonPath("$.memberCount").value(1))
			.andExpect(jsonPath("$.averageRating").value(0.0));

		UUID createdId = mapper.onlyGroupId();

		mockMvc.perform(get(CATALOG_PATH + "/" + createdId).with(user(USER_ID.toString())))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(createdId.toString()))
			.andExpect(jsonPath("$.memberCount").value(1))
			.andExpect(jsonPath("$.averageRating").value(0.0))
			.andExpect(jsonPath("$.favorite").value(false));

		mockMvc.perform(patch(CATALOG_PATH + "/" + createdId)
				.with(user(USER_ID.toString()))
				.with(xsrf("catalog-xsrf"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(validCatalogJson("알고리즘 스터디", "CS", true)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("알고리즘 스터디"))
			.andExpect(jsonPath("$.topic").value("CS"))
			.andExpect(jsonPath("$.favorite").value(true));

		mockMvc.perform(delete(CATALOG_PATH + "/" + createdId)
				.with(user(USER_ID.toString()))
				.with(xsrf("catalog-xsrf")))
			.andExpect(status().isNoContent());

		mockMvc.perform(get(CATALOG_PATH + "/" + createdId).with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Not Found"));
	}

	@Test
	void detailMissingCatalogReturnsNotFoundProblemDetails() throws Exception {
		mockMvc.perform(get(CATALOG_PATH + "/" + GROUP_ID).with(user(USER_ID.toString())))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("study group catalog detail was not found."));
	}

	private static String validCatalogJson(String name, String topic, boolean favorite) {
		return """
			{
			  "name": "%s",
			  "topic": "%s",
			  "status": "ACTIVE",
			  "startsAt": "2026-06-20",
			  "endsAt": "2026-07-20",
			  "favorite": %s
			}
			""".formatted(name, topic, favorite);
	}

	private static RequestPostProcessor xsrf(String token) {
		return request -> {
			request.addHeader("X-XSRF-TOKEN", token);
			request.setCookies(new MockCookie("XSRF-TOKEN", token));
			return request;
		};
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestCatalogBeans {

		@Bean
		@Primary
		MutableStudyGroupCatalogMapper studyGroupCatalogMapper() {
			return new MutableStudyGroupCatalogMapper();
		}
	}

	static final class MutableStudyGroupCatalogMapper implements StudyGroupCatalogMapper {

		private final Map<UUID, StudyGroupCatalogEntry> entries = new ConcurrentHashMap<>();

		@Override
		public StudyGroupCatalogEntry insertStudyGroup(StudyGroupCatalogEntry entry) {
			StudyGroupCatalogEntry fixed = new StudyGroupCatalogEntry(
				entry.id(),
				entry.name(),
				entry.topic(),
				entry.status(),
				entry.startsAt(),
				entry.endsAt(),
				entry.memberCount(),
				entry.averageRating(),
				entry.favorite()
			);
			entries.put(fixed.id(), fixed);
			return fixed;
		}

		@Override
		public List<StudyGroupCatalogEntry> searchStudyGroups(String keyword, String status, String sort, int pageSize, String cursor) {
			return entries.values().stream().limit(pageSize).toList();
		}

		@Override
		public Optional<StudyGroupCatalogEntry> findStudyGroupDetail(UUID groupId) {
			return Optional.ofNullable(entries.get(groupId));
		}

		@Override
		public StudyGroupCatalogEntry updateStudyGroup(UUID groupId, StudyGroupCatalogCommand command) {
			StudyGroupCatalogEntry updated = new StudyGroupCatalogEntry(
				groupId,
				command.name().strip(),
				command.topic().strip(),
				command.status().strip().toUpperCase(),
				command.startsAt(),
				command.endsAt(),
				3,
				4.5,
				command.favorite()
			);
			entries.put(groupId, updated);
			return updated;
		}

		@Override
		public boolean deleteStudyGroup(UUID groupId) {
			return entries.remove(groupId) != null;
		}

		private UUID onlyGroupId() {
			return entries.keySet().stream().findFirst().orElseThrow();
		}

		private void clear() {
			entries.clear();
		}
	}
}
