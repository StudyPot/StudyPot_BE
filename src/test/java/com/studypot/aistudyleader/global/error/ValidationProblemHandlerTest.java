package com.studypot.aistudyleader.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import com.studypot.aistudyleader.global.api.ApiPaths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(classes = {AiStudyLeaderApplication.class, ValidationProblemHandlerTest.TestValidationController.class})
@AutoConfigureMockMvc(addFilters = false)
class ValidationProblemHandlerTest {

	private final MockMvc mockMvc;

	@Autowired
	ValidationProblemHandlerTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void invalidJsonBodyReturnsUnprocessableProblemDetailWithFieldErrors() throws Exception {
		mockMvc.perform(post(ApiPaths.V1 + "/test/validation")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(422))
			.andExpect(jsonPath("$.title").value("Invalid request payload"))
			.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
	}

	@RestController
	static class TestValidationController {

		@PostMapping(ApiPaths.V1 + "/test/validation")
		TestValidationResponse validate(@Valid @RequestBody TestValidationRequest request) {
			return new TestValidationResponse(request.name());
		}
	}

	public record TestValidationRequest(@NotBlank String name) {
	}

	public record TestValidationResponse(String name) {
	}
}
