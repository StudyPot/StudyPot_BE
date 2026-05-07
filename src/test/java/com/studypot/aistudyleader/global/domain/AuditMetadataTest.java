package com.studypot.aistudyleader.global.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditMetadataTest {

	@Test
	void createdMetadataUsesMysqlTimestamp6Precision() {
		Instant now = Instant.parse("2026-05-07T04:00:00.123456789Z");

		AuditMetadata metadata = AuditMetadata.created(now);

		assertThat(metadata.createdAt()).isEqualTo(Instant.parse("2026-05-07T04:00:00.123456Z"));
		assertThat(metadata.updatedAt()).isEqualTo(Instant.parse("2026-05-07T04:00:00.123456Z"));
		assertThat(metadata.deletedAt()).isNull();
		assertThat(metadata.deleted()).isFalse();
	}

	@Test
	void touchUpdatesTimestampWithMysqlPrecision() {
		AuditMetadata metadata = AuditMetadata.created(Instant.parse("2026-05-07T04:00:00Z"));

		AuditMetadata touched = metadata.touch(Instant.parse("2026-05-07T04:00:01.987654321Z"));

		assertThat(touched.createdAt()).isEqualTo(metadata.createdAt());
		assertThat(touched.updatedAt()).isEqualTo(Instant.parse("2026-05-07T04:00:01.987654Z"));
		assertThat(touched.deletedAt()).isNull();
	}

	@Test
	void softDeleteRecordsDeletedAtOnce() {
		AuditMetadata metadata = AuditMetadata.created(Instant.parse("2026-05-07T04:00:00Z"));

		AuditMetadata deleted = metadata.softDelete(Instant.parse("2026-05-07T04:00:02.000000999Z"));

		assertThat(deleted.deleted()).isTrue();
		assertThat(deleted.deletedAt()).isEqualTo(Instant.parse("2026-05-07T04:00:02Z"));
		assertThat(deleted.softDelete(Instant.parse("2026-05-07T04:00:03Z"))).isSameAs(deleted);
	}

	@Test
	void auditMetadataRejectsMissingOrBackwardsTimestamps() {
		assertThatNullPointerException()
			.isThrownBy(() -> AuditMetadata.created(null))
			.withMessage("createdAt must not be null");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuditMetadata(
				Instant.parse("2026-05-07T04:00:01Z"),
				Instant.parse("2026-05-07T04:00:00Z"),
				null
			))
			.withMessage("updatedAt must not be before createdAt");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new AuditMetadata(
				Instant.parse("2026-05-07T04:00:00Z"),
				Instant.parse("2026-05-07T04:00:02Z"),
				Instant.parse("2026-05-07T04:00:01Z")
			))
			.withMessage("deletedAt must not be before updatedAt");
	}

	@Test
	void auditMetadataRejectsMovingCurrentTimestampBackwards() {
		AuditMetadata metadata = AuditMetadata.created(Instant.parse("2026-05-07T04:00:00Z"))
			.touch(Instant.parse("2026-05-07T04:00:02Z"));

		assertThatIllegalArgumentException()
			.isThrownBy(() -> metadata.touch(Instant.parse("2026-05-07T04:00:01Z")))
			.withMessage("updatedAt must not be before current updatedAt");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> metadata.softDelete(Instant.parse("2026-05-07T04:00:01Z")))
			.withMessage("deletedAt must not be before current updatedAt");
	}
}
