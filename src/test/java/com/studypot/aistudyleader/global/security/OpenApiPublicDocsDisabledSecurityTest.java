package com.studypot.aistudyleader.global.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.studypot.aistudyleader.AiStudyLeaderApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AiStudyLeaderApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
	"springdoc.api-docs.enabled=true",
	"springdoc.swagger-ui.enabled=true",
	"studypot.openapi.public-docs-enabled=false"
})
class OpenApiPublicDocsDisabledSecurityTest {

	private final MockMvc mockMvc;

	@Autowired
	OpenApiPublicDocsDisabledSecurityTest(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void openApiAndSwaggerRoutesAreDeniedWhenPublicDocsAreDisabled() throws Exception {
		mockMvc.perform(get("/v3/api-docs").with(user("member")))
			.andExpect(status().isForbidden());

		mockMvc.perform(get("/swagger-ui.html").with(user("member")))
			.andExpect(status().isForbidden());
	}
}
