package com.studypot.aistudyleader.global.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AggregateRoot<ID> {

	private final List<DomainEvent> domainEvents = new ArrayList<>();

	private final ID id;

	protected AggregateRoot(ID id) {
		this.id = Objects.requireNonNull(id, "id must not be null");
	}

	public ID id() {
		return id;
	}

	public List<DomainEvent> pullDomainEvents() {
		var events = List.copyOf(domainEvents);
		domainEvents.clear();
		return events;
	}

	protected void registerEvent(DomainEvent domainEvent) {
		domainEvents.add(Objects.requireNonNull(domainEvent, "domainEvent must not be null"));
	}
}
