package com.studypot.aistudyleader.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, SecurityConfigurationTest.TestApiController.class})
@AutoConfigureMockMvc
class SecurityConfigurationTest {

	private final MockMvc mockMvc;

	@Autowired
	SecurityConfigurationTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void unauthenticatedApiRequestReturnsProblemDetail() throws Exception {
		mockMvc.perform(get(ApiPaths.V1 + "/test/protected"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.title").value("Unauthorized"));
	}

	@Test
	void authenticatedApiRequestCanReachProtectedApiPath() throws Exception {
		mockMvc.perform(get(ApiPaths.V1 + "/test/protected").with(user("member")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void authenticatedNonApiRequestIsDeniedWithProblemDetail() throws Exception {
		mockMvc.perform(get("/internal").with(user("member")))
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403))
			.andExpect(jsonPath("$.title").value("Forbidden"));
	}

	@RestController
	static class TestApiController {

		@GetMapping(ApiPaths.V1 + "/test/protected")
		Map<String, String> protectedEndpoint() {
			return Map.of("status", "ok");
		}

		@GetMapping("/internal")
		Map<String, String> internalEndpoint() {
			return Map.of("status", "internal");
		}
	}
}
