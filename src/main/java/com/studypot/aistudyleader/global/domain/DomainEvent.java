package com.studypot.aistudyleader.global.domain;

import java.time.Instant;

public interface DomainEvent {

	Instant occurredAt();
}
