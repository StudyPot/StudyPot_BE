package com.studypot.aistudyleader.global.persistence;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public final class UuidBinary {

	private static final int UUID_BYTE_LENGTH = 16;

	private UuidBinary() {
	}

	public static byte[] toBytes(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		return ByteBuffer.allocate(UUID_BYTE_LENGTH)
			.putLong(uuid.getMostSignificantBits())
			.putLong(uuid.getLeastSignificantBits())
			.array();
	}

	public static UUID fromBytes(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes must not be null");
		if (bytes.length != UUID_BYTE_LENGTH) {
			throw new IllegalArgumentException("uuid binary value must be exactly 16 bytes");
		}

		ByteBuffer buffer = ByteBuffer.wrap(bytes.clone());
		return new UUID(buffer.getLong(), buffer.getLong());
	}
}
