package com.studypot.aistudyleader.identity.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AiStudyLeaderApplication.class)
@AutoConfigureMockMvc
class AuthControllerMissingServiceTest {

	private static final String REFRESH_PATH = ApiPaths.V1 + "/auth/refresh";

	private final MockMvc mockMvc;

	@Autowired
	AuthControllerMissingServiceTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void missingAuthSessionServiceReturnsServiceUnavailable() throws Exception {
		mockMvc.perform(post(REFRESH_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"refreshToken":"refresh-token"}
					"""))
			.andExpect(status().isServiceUnavailable())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Service unavailable"));
	}
}
