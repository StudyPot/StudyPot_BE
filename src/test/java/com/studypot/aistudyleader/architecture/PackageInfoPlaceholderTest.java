package com.studypot.aistudyleader.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PackageInfoPlaceholderTest {

	@Test
	void sourceTreeDoesNotKeepEmptyPackageInfoPlaceholders() throws IOException {
		Path sourceRoot = Path.of("src/main/java");

		try (var paths = Files.walk(sourceRoot)) {
			List<Path> packageInfoFiles = paths
				.filter(path -> path.getFileName().toString().equals("package-info.java"))
				.toList();

			assertThat(packageInfoFiles).isEmpty();
		}
	}
}
