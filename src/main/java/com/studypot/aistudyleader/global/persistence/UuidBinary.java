package com.studypot.aistudyleader.global.persistence;

import java.util.Objects;
import java.util.UUID;

public final class UuidBinary {

	private static final int UUID_BYTE_LENGTH = 16;

	private UuidBinary() {
	}

	public static byte[] toBytes(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		byte[] bytes = new byte[UUID_BYTE_LENGTH];
		writeLong(bytes, 0, uuid.getMostSignificantBits());
		writeLong(bytes, Long.BYTES, uuid.getLeastSignificantBits());
		return bytes;
	}

	public static UUID fromBytes(byte[] bytes) {
		Objects.requireNonNull(bytes, "bytes must not be null");
		if (bytes.length != UUID_BYTE_LENGTH) {
			throw new IllegalArgumentException("uuid binary value must be exactly 16 bytes");
		}

		return new UUID(readLong(bytes, 0), readLong(bytes, Long.BYTES));
	}

	private static void writeLong(byte[] bytes, int offset, long value) {
		for (int index = Long.BYTES - 1; index >= 0; index--) {
			bytes[offset + index] = (byte) value;
			value >>>= Byte.SIZE;
		}
	}

	private static long readLong(byte[] bytes, int offset) {
		long value = 0;
		for (int index = 0; index < Long.BYTES; index++) {
			value = (value << Byte.SIZE) | (bytes[offset + index] & 0xFFL);
		}
		return value;
	}
}
