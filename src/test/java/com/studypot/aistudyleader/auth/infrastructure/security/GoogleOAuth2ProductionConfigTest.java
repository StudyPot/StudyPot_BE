package com.studypot.aistudyleader.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GoogleOAuth2ProductionConfigTest {

	@Test
	void applicationConfigMapsDeploymentGoogleClientEnvironmentNames() throws IOException {
		String applicationConfig = Files.readString(Path.of("src/main/resources/application.yml"));

		assertThat(applicationConfig)
			.contains("client-id: ${STUDYPOT_GOOGLE_CLIENT_ID:}")
			.contains("client-secret: ${STUDYPOT_GOOGLE_CLIENT_SECRET:}");
	}

	@Test
	void rumicleanComposePassesSameGoogleClientEnvironmentNames() throws IOException {
		String rumicleanCompose = Files.readString(Path.of("deploy/rumiclean/docker-compose.yml"));

		assertThat(rumicleanCompose)
			.contains("STUDYPOT_GOOGLE_CLIENT_ID: \"${STUDYPOT_GOOGLE_CLIENT_ID:-}\"")
			.contains("STUDYPOT_GOOGLE_CLIENT_SECRET: \"${STUDYPOT_GOOGLE_CLIENT_SECRET:-}\"");
	}
}
