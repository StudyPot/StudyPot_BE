package com.studypot.aistudyleader.identity.controller;

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
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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
				.with(xsrf("refresh-xsrf"))
				.content("""
					{"refreshToken":"refresh-token"}
					"""))
			.andExpect(status().isServiceUnavailable())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Service Unavailable"));
	}

	private static RequestPostProcessor xsrf(String value) {
		return request -> {
			request.setCookies(new MockCookie("XSRF-TOKEN", value));
			request.addHeader("X-XSRF-TOKEN", value);
			return request;
		};
	}
}
