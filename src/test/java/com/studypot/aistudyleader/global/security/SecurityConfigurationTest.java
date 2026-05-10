package com.studypot.aistudyleader.global.security;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, SecurityConfigurationTest.TestApiController.class})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"springdoc.api-docs.enabled=true",
	"springdoc.swagger-ui.enabled=true",
	"studypot.openapi.public-docs-enabled=true"
})
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

	@Test
	void localDiagnosticsAndOpenApiDocsArePublic() throws Exception {
		mockMvc.perform(get("/actuator/health/readiness"))
			.andExpect(status().isOk());

		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.openapi").exists())
			.andExpect(jsonPath("$.info.title").value("AI Study Leader API"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));

		mockMvc.perform(get("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection());

		mockMvc.perform(head("/swagger-ui.html"))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	void localDiagnosticsAndOpenApiDocsOnlyPermitReadMethods() throws Exception {
		mockMvc.perform(post("/swagger-ui.html").with(user("member")))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/actuator/health").with(user("member")))
			.andExpect(status().isForbidden());
	}

	@Test
	void corsRejectsWildcardOriginsWhenCredentialsAreAllowed() {
		SecurityConfiguration configuration = new SecurityConfiguration();
		StudypotCorsProperties properties = new StudypotCorsProperties(
			List.of("*"),
			List.of(),
			List.of("GET"),
			List.of("Authorization"),
			List.of("Location"),
			true
		);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> configuration.corsConfigurationSource(properties))
			.withMessage("studypot.cors.allowed-origins cannot contain '*' when credentials are allowed; use allowed-origin-patterns instead.");
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
