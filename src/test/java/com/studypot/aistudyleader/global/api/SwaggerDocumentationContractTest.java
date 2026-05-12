package com.studypot.aistudyleader.global.api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SwaggerDocumentationContractTest {

	private static final Pattern HANGUL = Pattern.compile("[가-힣]");
	private static final Pattern MAPPING = Pattern.compile("@(Get|Post|Put|Patch|Delete)Mapping");
	private static final Pattern RECORD_DECLARATION = Pattern.compile("\\brecord\\s+([A-Za-z][A-Za-z0-9_]*)\\s*\\(");

	@Test
	void implementedControllersDeclareDetailedKoreanSwaggerDocs() throws IOException {
		Path projectDir = Path.of(System.getProperty("studypot.projectDir", "."));
		List<Path> controllerPaths = controllerPaths(projectDir);
		List<String> violations = new ArrayList<>();

		for (Path path : controllerPaths) {
			String text = Files.readString(path);
			if (!text.contains("@RestController")) {
				continue;
			}
			verifyControllerTag(projectDir, path, text, violations);
			verifyMappingDocs(projectDir, path, text, violations);
			verifyRecordSchemas(projectDir, path, text, violations);
		}

		assertTrue(
			violations.isEmpty(),
			() -> "Swagger Docs contract violations:\n- " + String.join("\n- ", violations)
		);
	}

	private static List<Path> controllerPaths(Path projectDir) throws IOException {
		Path sourceRoot = projectDir.resolve("src/main/java/com/studypot/aistudyleader");
		try (Stream<Path> paths = Files.find(
			sourceRoot,
			20,
			(path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().endsWith("Controller.java")
		)) {
			return paths.sorted().toList();
		}
	}

	private static void verifyControllerTag(Path projectDir, Path path, String text, List<String> violations) {
		int restControllerIndex = text.indexOf("@RestController");
		String classDocs = text.substring(Math.max(0, restControllerIndex - 500), restControllerIndex);
		if (!classDocs.contains("@Tag(") || !classDocs.contains("description =") || !HANGUL.matcher(classDocs).find()) {
			violations.add(projectDir.relativize(path) + ": @RestController must have Korean @Tag name and description");
		}
	}

	private static void verifyMappingDocs(Path projectDir, Path path, String text, List<String> violations) {
		String[] lines = text.split("\\R");
		for (int index = 0; index < lines.length; index++) {
			String line = lines[index];
			if (!MAPPING.matcher(line).find()) {
				continue;
			}
			String context = linesAround(lines, Math.max(0, index - 80), index + 1);
			if (!context.contains("@Operation(") || !context.contains("summary =") || !context.contains("description =")) {
				violations.add(projectDir.relativize(path) + ":" + (index + 1) + ": mapping must have @Operation summary and description");
			} else if (!HANGUL.matcher(context).find()) {
				violations.add(projectDir.relativize(path) + ":" + (index + 1) + ": @Operation text must be Korean");
			}
			if (!context.contains("@ApiResponse")) {
				violations.add(projectDir.relativize(path) + ":" + (index + 1) + ": mapping must document responses with @ApiResponse");
			}
			String signature = linesAround(lines, index, Math.min(lines.length, index + 30));
			if (line.contains("{") && !signature.contains("@Parameter(")) {
				violations.add(projectDir.relativize(path) + ":" + (index + 1) + ": path variables must have @Parameter descriptions");
			}
		}
	}

	private static void verifyRecordSchemas(Path projectDir, Path path, String text, List<String> violations) {
		String[] lines = text.split("\\R");
		for (int index = 0; index < lines.length; index++) {
			var matcher = RECORD_DECLARATION.matcher(lines[index]);
			if (!matcher.find()) {
				continue;
			}
			String context = linesAround(lines, Math.max(0, index - 6), index + 1);
			if (!context.contains("@Schema(") || !context.contains("description =") || !HANGUL.matcher(context).find()) {
				violations.add(
					projectDir.relativize(path) + ":" + (index + 1) + ": record " + matcher.group(1)
						+ " must have Korean @Schema description"
				);
			}
		}
	}

	private static String linesAround(String[] lines, int startInclusive, int endExclusive) {
		return String.join("\n", List.of(lines).subList(startInclusive, endExclusive));
	}
}
