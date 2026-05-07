package com.studypot.aistudyleader.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidBinaryTest {

	@Test
	void uuidRoundTripsThroughSixteenBytes() {
		UUID uuid = UUID.fromString("0196ad97-b27b-7f64-8e95-9b2541212d2f");

		byte[] bytes = UuidBinary.toBytes(uuid);
		UUID restored = UuidBinary.fromBytes(bytes);

		assertThat(bytes).hasSize(16);
		assertThat(restored).isEqualTo(uuid);
	}

	@Test
	void conversionDefensivelyCopiesBytes() {
		UUID uuid = UUID.fromString("0196ad97-b27b-7f64-8e95-9b2541212d2f");
		byte[] bytes = UuidBinary.toBytes(uuid);

		bytes[0] = 0;

		assertThat(UuidBinary.toBytes(uuid)[0]).isNotZero();
	}

	@Test
	void conversionRejectsNullAndInvalidLength() {
		assertThatNullPointerException()
			.isThrownBy(() -> UuidBinary.toBytes(null))
			.withMessage("uuid must not be null");
		assertThatNullPointerException()
			.isThrownBy(() -> UuidBinary.fromBytes(null))
			.withMessage("bytes must not be null");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> UuidBinary.fromBytes(new byte[15]))
			.withMessage("uuid binary value must be exactly 16 bytes");
	}
}
