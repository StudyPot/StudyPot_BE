package com.studypot.aistudyleader.shared.domain;

import java.time.Instant;

public interface DomainEvent {

	Instant occurredAt();
}
