package com.studypot.aistudyleader.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DbCorePolicyContractTest {

	private static final Path PROJECT_ROOT = projectRoot();
	private static final Path MIGRATION_DIR = PROJECT_ROOT.resolve("src/main/resources/db/migration");
	private static final Path V1_MIGRATION = MIGRATION_DIR.resolve("V1__erd_v0_8_mysql8_schema.sql");
	private static final Path V2_MIGRATION = MIGRATION_DIR.resolve("V2__db_core_live_row_unique_constraints.sql");

	@Test
	void allPrimaryAndForeignKeyColumnsUseUuidBinary16() throws IOException {
		String migration = read(V1_MIGRATION);

		for (String table : tables(migration)) {
			String body = createTableBody(migration, table);
			assertThat(body)
				.as("%s primary id", table)
				.containsPattern("(?im)^\\s*`?id`?\\s+binary\\(16\\)\\s+not\\s+null\\b");

			for (String column : foreignKeyColumns(body)) {
				assertThat(body)
					.as("%s foreign key column: %s", table, column)
					.containsPattern("(?im)^\\s*`?" + Pattern.quote(column) + "`?\\s+binary\\(16\\)\\s+");
			}
		}
	}

	@Test
	void auditAndSoftDeleteColumnsUseMysqlTimestamp6Policy() throws IOException {
		String migration = read(V1_MIGRATION);

		for (String table : tables(migration)) {
			String body = createTableBody(migration, table);
			if (containsColumn(body, "created_at")) {
				assertThat(body)
					.as("%s created_at", table)
					.containsPattern("(?im)^\\s*`?created_at`?\\s+timestamp\\(6\\)\\s+not\\s+null\\s+default\\s+current_timestamp\\(6\\)");
			}
			if (containsColumn(body, "updated_at")) {
				assertThat(body)
					.as("%s updated_at", table)
					.containsPattern("(?im)^\\s*`?updated_at`?\\s+timestamp\\(6\\)\\s+not\\s+null\\s+default\\s+current_timestamp\\(6\\)\\s+on\\s+update\\s+current_timestamp\\(6\\)");
			}
			if (containsColumn(body, "deleted_at")) {
				assertThat(body)
					.as("%s deleted_at", table)
					.containsPattern("(?im)^\\s*`?deleted_at`?\\s+timestamp\\(6\\)\\s+null\\b");
			}
		}
	}

	@Test
	void v2AddsLiveRowUniqueConstraintsForSoftDeletedDomains() throws IOException {
		String migration = normalize(read(V2_MIGRATION));

		assertThat(migration)
			.contains("alter table study_group")
			.contains("drop index study_group_invite_code_uidx")
			.contains("add column invite_code_live_key varchar(80) generated always as (case when deleted_at is null then invite_code else null end) stored")
			.contains("add unique key study_group_invite_code_live_uidx (invite_code_live_key)");

		assertThat(migration)
			.contains("alter table group_member")
			.contains("drop index group_member_group_user_uidx")
			.contains("add column group_user_live_key varbinary(32) generated always as (case when deleted_at is null then concat(group_id, user_id) else null end) stored")
			.contains("add unique key group_member_group_user_live_uidx (group_user_live_key)");

		assertThat(migration)
			.contains("alter table group_onboarding_response")
			.contains("drop index onboarding_member_uidx")
			.contains("add column member_live_key binary(16) generated always as (case when deleted_at is null then member_id else null end) stored")
			.contains("add unique key onboarding_member_live_uidx (member_live_key)");

		assertThat(migration)
			.contains("alter table curriculum")
			.contains("drop index curriculum_group_uidx")
			.contains("add column group_live_key binary(16) generated always as (case when deleted_at is null then group_id else null end) stored")
			.contains("add unique key curriculum_group_live_uidx (group_live_key)");
	}

	private static List<String> tables(String migration) {
		return Pattern.compile("^create\\s+table\\s+`?([a-z0-9_]+)`?\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
			.matcher(migration)
			.results()
			.map(result -> result.group(1))
			.toList();
	}

	private static Set<String> foreignKeyColumns(String tableBody) {
		return Pattern.compile("foreign\\s+key\\s*\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE)
			.matcher(tableBody)
			.results()
			.flatMap(result -> Arrays.stream(result.group(1).split(",")))
			.map(DbCorePolicyContractTest::unquoteIdentifier)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String unquoteIdentifier(String identifier) {
		String stripped = identifier.strip();
		if (stripped.startsWith("`") && stripped.endsWith("`")) {
			return stripped.substring(1, stripped.length() - 1);
		}
		return stripped;
	}

	private static boolean containsColumn(String tableBody, String column) {
		return Pattern.compile("^\\s*`?" + Pattern.quote(column) + "`?\\s+", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
			.matcher(tableBody)
			.find();
	}

	private static String createTableBody(String migration, String table) {
		var matcher = Pattern.compile(
			"^create\\s+table\\s+`?" + Pattern.quote(table) + "`?\\s*\\((.*?)\\)\\s*engine\\s*=",
			Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
		).matcher(migration);

		assertThat(matcher.find()).as("create table block for %s", table).isTrue();
		return matcher.group(1);
	}

	private static String read(Path path) throws IOException {
		return Files.readString(path, StandardCharsets.UTF_8);
	}

	private static String normalize(String sql) {
		return sql
			.replace("\r\n", "\n")
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase();
	}

	private static Path projectRoot() {
		String projectDir = System.getProperty("studypot.projectDir");
		if (projectDir != null && !projectDir.isBlank()) {
			return Path.of(projectDir).toAbsolutePath().normalize();
		}

		Path current = Path.of("").toAbsolutePath().normalize();
		while (current != null) {
			if ((Files.isRegularFile(current.resolve("settings.gradle"))
				|| Files.isRegularFile(current.resolve("settings.gradle.kts")))
				&& Files.isRegularFile(current.resolve("docs/specs/db-schema-v1.sql"))) {
				return current;
			}
			current = current.getParent();
		}

		throw new IllegalStateException("Unable to locate project root for DB core policy contract test.");
	}
}
