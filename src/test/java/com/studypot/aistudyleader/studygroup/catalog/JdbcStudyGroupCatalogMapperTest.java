package com.studypot.aistudyleader.studygroup.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.global.persistence.UuidBinary;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class JdbcStudyGroupCatalogMapperTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-00000000c701");
	private static final StudyGroupCatalogEntry ENTRY = new StudyGroupCatalogEntry(
		GROUP_ID,
		"백엔드 스터디",
		"Spring Boot",
		"ACTIVE",
		LocalDate.parse("2026-06-09"),
		LocalDate.parse("2026-07-09"),
		3,
		4.5,
		true
	);

	private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
	private final JdbcStudyGroupCatalogMapper mapper = new JdbcStudyGroupCatalogMapper(jdbcTemplate);

	@Test
	void insertStudyGroupPersistsCatalogRow() {
		when(jdbcTemplate.update(eq(StudyGroupCatalogJdbcSql.INSERT_STUDY_GROUP), any(Object[].class))).thenReturn(1);

		assertThat(mapper.insertStudyGroup(ENTRY)).isEqualTo(ENTRY);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(StudyGroupCatalogJdbcSql.INSERT_STUDY_GROUP), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat(args.getValue()[1]).isEqualTo("백엔드 스터디");
		assertThat(args.getValue()[6]).isEqualTo(3);
		assertThat(args.getValue()[8]).isEqualTo(true);
	}

	@Test
	void findStudyGroupDetailUsesBinaryIdAndMapsOptionalResult() {
		when(jdbcTemplate.query(eq(StudyGroupCatalogJdbcSql.SELECT_DETAIL), any(RowMapper.class), any(Object[].class)))
			.thenReturn(java.util.List.of(ENTRY));

		Optional<StudyGroupCatalogEntry> detail = mapper.findStudyGroupDetail(GROUP_ID);

		assertThat(detail).contains(ENTRY);
		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(StudyGroupCatalogJdbcSql.SELECT_DETAIL), any(RowMapper.class), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}

	@Test
	void searchBindsCursorAndLimitToDatabaseQuery() {
		when(jdbcTemplate.query(eq(StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(java.util.List.of(ENTRY));

		assertThat(mapper.searchStudyGroups(" Spring ", " ACTIVE ", " name ", 3, GROUP_ID.toString()))
			.containsExactly(ENTRY);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS), any(RowMapper.class), args.capture());
		assertThat(StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS)
			.contains("id > ?")
			.contains("limit ?");
		assertThat(args.getValue()).hasSize(12);
		assertThat(args.getValue()[0]).isEqualTo("Spring");
		assertThat(args.getValue()[4]).isEqualTo("ACTIVE");
		assertThat((byte[]) args.getValue()[7]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat((byte[]) args.getValue()[8]).containsExactly(UuidBinary.toBytes(GROUP_ID));
		assertThat(args.getValue()[9]).isEqualTo("name");
		assertThat(args.getValue()[11]).isEqualTo(3);
	}

	@Test
	void searchBindsNullDatabaseCursorWhenCursorIsBlank() {
		when(jdbcTemplate.query(eq(StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS), any(RowMapper.class), any(Object[].class)))
			.thenReturn(java.util.List.of(ENTRY));

		assertThat(mapper.searchStudyGroups(null, null, null, 10, "  "))
			.containsExactly(ENTRY);

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).query(eq(StudyGroupCatalogJdbcSql.SEARCH_STUDY_GROUPS), any(RowMapper.class), args.capture());
		assertThat(args.getValue()[7]).isNull();
		assertThat(args.getValue()[8]).isNull();
		assertThat(args.getValue()[11]).isEqualTo(10);
	}

	@Test
	void deleteStudyGroupSoftDeletesCatalogRow() {
		when(jdbcTemplate.update(eq(StudyGroupCatalogJdbcSql.SOFT_DELETE_STUDY_GROUP), any(Object[].class))).thenReturn(1);

		assertThat(mapper.deleteStudyGroup(GROUP_ID)).isTrue();

		ArgumentCaptor<Object[]> args = ArgumentCaptor.forClass(Object[].class);
		verify(jdbcTemplate).update(eq(StudyGroupCatalogJdbcSql.SOFT_DELETE_STUDY_GROUP), args.capture());
		assertThat((byte[]) args.getValue()[0]).containsExactly(UuidBinary.toBytes(GROUP_ID));
	}
}
