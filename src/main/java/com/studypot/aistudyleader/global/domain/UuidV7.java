package com.studypot.aistudyleader.global.domain;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class UuidV7 {

	private static final long MAX_UNIX_TIMESTAMP_MILLIS = 0xFFFF_FFFF_FFFFL;

	private UuidV7() {
	}

	public static UUID generate() {
		return generate(Clock.systemUTC(), ThreadLocalRandom.current());
	}

	public static UUID generate(Clock clock, RandomGenerator random) {
		Objects.requireNonNull(clock, "clock must not be null");
		Objects.requireNonNull(random, "random must not be null");

		long timestampMillis = clock.millis();
		if (timestampMillis < 0 || timestampMillis > MAX_UNIX_TIMESTAMP_MILLIS) {
			throw new IllegalArgumentException("uuid v7 timestamp must fit in 48 bits");
		}

		long randA = random.nextInt(1 << 12);
		long mostSignificantBits = (timestampMillis << 16) | (0x7L << 12) | randA;
		long leastSignificantBits = (random.nextLong() >>> 2) | 0x8000_0000_0000_0000L;
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	public static long unixTimestampMillis(UUID uuid) {
		return requireVersion7(uuid).getMostSignificantBits() >>> 16;
	}

	public static UUID requireVersion7(UUID uuid) {
		Objects.requireNonNull(uuid, "uuid must not be null");
		if (uuid.version() != 7) {
			throw new IllegalArgumentException("uuid must be version 7");
		}
		if (uuid.variant() != 2) {
			throw new IllegalArgumentException("uuid must use RFC 4122 variant");
		}
		return uuid;
	}
}
