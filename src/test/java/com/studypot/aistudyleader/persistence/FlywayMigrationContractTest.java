package com.studypot.aistudyleader.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationContractTest {

	private static final Path PROJECT_ROOT = projectRoot();
	private static final Path LOCKED_SCHEMA = PROJECT_ROOT.resolve("docs/specs/db-schema-v1.sql");
	private static final Path DB_CONTRACT = PROJECT_ROOT.resolve("docs/specs/db-contract-v1.md");
	private static final Path MIGRATION_DIR = PROJECT_ROOT.resolve("src/main/resources/db/migration");
	private static final Path V1_MIGRATION = MIGRATION_DIR.resolve("V1__erd_v0_8_mysql8_schema.sql");
	private static final Path V4_READY_TO_START_STATUS = MIGRATION_DIR.resolve("V4__study_group_ready_to_start_status.sql");
	private static final Pattern MIGRATION_NAME_PATTERN = Pattern.compile("V([1-9][0-9]*)__[a-z0-9_]+\\.sql");

	@Test
	void firstMigrationUsesLockedErdV08MySql8BaselineAsLowestVersion() throws IOException {
		assertThat(MIGRATION_DIR).isDirectory();

		List<FlywayMigration> migrations;
		try (var paths = Files.list(MIGRATION_DIR)) {
			migrations = paths
				.filter(Files::isRegularFile)
				.map(path -> path.getFileName().toString())
				.sorted()
				.map(FlywayMigration::from)
				.toList();
		}

		assertThat(migrations)
			.extracting(FlywayMigration::fileName)
			.contains("V1__erd_v0_8_mysql8_schema.sql");
		assertThat(migrations.stream().min(Comparator.comparingInt(FlywayMigration::version)))
			.hasValueSatisfying(migration -> assertThat(migration.fileName())
				.isEqualTo("V1__erd_v0_8_mysql8_schema.sql"));
	}

	@Test
	void firstMigrationMatchesLockedSchemaDocument() throws IOException {
		assertThat(V1_MIGRATION).isRegularFile();

		assertThat(normalizeSql(read(V1_MIGRATION)))
			.isEqualTo(normalizeSql(read(LOCKED_SCHEMA)));
	}

	@Test
	void readyToStartStatusIsAppliedByPostBaselineConstraintMigration() throws IOException {
		String baseline = read(V1_MIGRATION);
		String readyMigration = read(V4_READY_TO_START_STATUS);
		String contract = read(DB_CONTRACT);

		assertThat(baseline)
			.contains("status in ('DRAFT','ONBOARDING','ACTIVE','COMPLETED','ARCHIVED')")
			.doesNotContain("READY_TO_START");
		assertThat(readyMigration)
			.contains("drop check study_group_status_check")
			.contains("add constraint study_group_status_check")
			.contains("READY_TO_START");
		assertThat(contract)
			.contains("READY_TO_START")
			.contains("V4__study_group_ready_to_start_status.sql");
	}

	@Test
	void firstMigrationCoversMvpTablesAndExcludesDeferredTables() throws IOException {
		String contract = read(DB_CONTRACT);
		String migration = read(V1_MIGRATION);

		List<String> expectedTables = matches(
			Pattern.compile("^\\|\\s*\\d+\\s*\\|\\s*`([^`]+)`\\s*\\|", Pattern.MULTILINE),
			contract
		);
		List<String> actualTables = matches(
			Pattern.compile("^create\\s+table\\s+`?([a-z0-9_]+)`?\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
			migration
		);

		assertThat(expectedTables).hasSize(19);
		assertThat(actualTables).containsExactlyInAnyOrderElementsOf(expectedTables);

		for (String table : expectedTables) {
			assertThat(createTableBody(migration, table))
				.as("%s.id", table)
				.containsPattern("(?im)^\\s*id\\s+binary\\(16\\)\\s+not\\s+null\\b");
		}

		assertThat(migration.toLowerCase())
			.contains("binary(16)")
			.contains("timestamp(6)")
			.contains(" json ")
			.contains("engine=innodb default charset=utf8mb4");

		assertThat(actualTables)
			.doesNotContain("discord_integration", "study_session", "study_group_invitation");
	}

	private static String read(Path path) throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	private static String normalizeSql(String sql) {
		return sql
			.replace("\r\n", "\n")
			.replaceAll("[ \\t]+\\n", "\n")
			.strip();
	}

	private static List<String> matches(Pattern pattern, String text) {
		return pattern.matcher(text)
			.results()
			.map(result -> result.group(1))
			.toList();
	}

	private static String createTableBody(String migration, String table) {
		var matcher = Pattern.compile(
			"^create\\s+table\\s+`?" + Pattern.quote(table) + "`?\\s*\\((.*?)\\)\\s*engine=",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
		).matcher(migration);

		assertThat(matcher.find()).as("create table block for %s", table).isTrue();
		return matcher.group(1);
	}

	private static Path projectRoot() {
		String projectDir = System.getProperty("studypot.projectDir");
		if (projectDir != null && !projectDir.isBlank()) {
			return Path.of(projectDir).toAbsolutePath().normalize();
		}

		Path current = Path.of("").toAbsolutePath().normalize();
		while (current != null) {
			if (Files.isRegularFile(current.resolve("settings.gradle"))
				&& Files.isRegularFile(current.resolve("docs/specs/db-schema-v1.sql"))) {
				return current;
			}
			current = current.getParent();
		}

		throw new IllegalStateException("Unable to locate project root for Flyway migration contract test.");
	}

	private record FlywayMigration(int version, String fileName) {

		private static FlywayMigration from(String fileName) {
			var matcher = MIGRATION_NAME_PATTERN.matcher(fileName);
			assertThat(matcher.matches())
				.as("Flyway migration name %s must match V<positive version>__<snake_case>.sql", fileName)
				.isTrue();
			return new FlywayMigration(Integer.parseInt(matcher.group(1)), fileName);
		}
	}
}
