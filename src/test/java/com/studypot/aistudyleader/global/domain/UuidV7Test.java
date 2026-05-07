package com.studypot.aistudyleader.global.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.SplittableRandom;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

	@Test
	void generatedUuidUsesVersion7VariantAndClockMilliseconds() {
		var clock = Clock.fixed(Instant.parse("2026-05-07T04:00:00.123456Z"), ZoneOffset.UTC);

		UUID id = UuidV7.generate(clock, new SplittableRandom(23));

		assertThat(id.version()).isEqualTo(7);
		assertThat(id.variant()).isEqualTo(2);
		assertThat(UuidV7.unixTimestampMillis(id)).isEqualTo(1_778_126_400_123L);
	}

	@Test
	void generatedUuidSortsByTimestampBits() {
		var firstClock = Clock.fixed(Instant.parse("2026-05-07T04:00:00Z"), ZoneOffset.UTC);
		var secondClock = Clock.fixed(Instant.parse("2026-05-07T04:00:01Z"), ZoneOffset.UTC);

		UUID first = UuidV7.generate(firstClock, new SplittableRandom(1));
		UUID second = UuidV7.generate(secondClock, new SplittableRandom(1));

		assertThat(UuidV7.unixTimestampMillis(first)).isLessThan(UuidV7.unixTimestampMillis(second));
		assertThat(compareUnsignedBytes(first, second)).isNegative();
	}

	@Test
	void versionValidationRejectsNullAndNonV7Uuid() {
		assertThatNullPointerException()
			.isThrownBy(() -> UuidV7.requireVersion7(null))
			.withMessage("uuid must not be null");

		assertThatIllegalArgumentException()
			.isThrownBy(() -> UuidV7.requireVersion7(UUID.randomUUID()))
			.withMessage("uuid must be version 7");

		UUID nonRfcVariantV7 = new UUID((1_778_126_400_123L << 16) | (0x7L << 12), 0);
		assertThatIllegalArgumentException()
			.isThrownBy(() -> UuidV7.requireVersion7(nonRfcVariantV7))
			.withMessage("uuid must use RFC 4122 variant");
	}

	private static int compareUnsignedBytes(UUID left, UUID right) {
		for (int shift = Long.SIZE - Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
			int comparison = Integer.compare(
				(int) ((left.getMostSignificantBits() >>> shift) & 0xFF),
				(int) ((right.getMostSignificantBits() >>> shift) & 0xFF)
			);
			if (comparison != 0) {
				return comparison;
			}
		}
		for (int shift = Long.SIZE - Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
			int comparison = Integer.compare(
				(int) ((left.getLeastSignificantBits() >>> shift) & 0xFF),
				(int) ((right.getLeastSignificantBits() >>> shift) & 0xFF)
			);
			if (comparison != 0) {
				return comparison;
			}
		}
		return 0;
	}
}
