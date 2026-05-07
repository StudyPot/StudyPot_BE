package com.studypot.aistudyleader.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FlywayMigrationContractTest {

	private static final Path LOCKED_SCHEMA = Path.of("docs/specs/db-schema-v1.sql");
	private static final Path DB_CONTRACT = Path.of("docs/specs/db-contract-v1.md");
	private static final Path MIGRATION_DIR = Path.of("src/main/resources/db/migration");
	private static final Path V1_MIGRATION = MIGRATION_DIR.resolve("V1__erd_v0_8_mysql8_schema.sql");

	@Test
	void firstMigrationUsesLockedErdV08MySql8BaselineName() throws IOException {
		assertThat(MIGRATION_DIR).isDirectory();

		List<String> migrationNames;
		try (var paths = Files.list(MIGRATION_DIR)) {
			migrationNames = paths
				.filter(Files::isRegularFile)
				.map(path -> path.getFileName().toString())
				.sorted()
				.toList();
		}

		assertThat(migrationNames).containsExactly("V1__erd_v0_8_mysql8_schema.sql");
		assertThat(migrationNames)
			.allMatch(name -> name.matches("V\\d+__[a-z0-9_]+\\.sql"));
	}

	@Test
	void firstMigrationMatchesLockedSchemaDocument() throws IOException {
		assertThat(V1_MIGRATION).isRegularFile();

		assertThat(normalizeSql(read(V1_MIGRATION)))
			.isEqualTo(normalizeSql(read(LOCKED_SCHEMA)));
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
}
