package com.studypot.aistudyleader.global.domain;

import java.time.Instant;

/**
 * Domain event raised by an aggregate when a meaningful business fact occurs.
 * Implementations should be immutable and safe to hand across application
 * boundaries for publication or follow-up processing.
 */
public interface DomainEvent {

	/**
	 * Returns the time the event occurred as a non-null {@link Instant}.
	 * Events should use UTC-based instants and preserve the precision needed by
	 * the domain and persistence layer.
	 */
	Instant occurredAt();
}
