package com.studypot.aistudyleader.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AggregateRootTest {

	@Test
	void aggregateRequiresIdentifier() {
		assertThatNullPointerException()
			.isThrownBy(() -> new TestAggregate(null))
			.withMessage("id must not be null");
	}

	@Test
	void aggregatePullsAndClearsDomainEvents() {
		var aggregate = new TestAggregate("aggregate-1");

		aggregate.markChanged();

		assertThat(aggregate.id()).isEqualTo("aggregate-1");
		assertThat(aggregate.pullDomainEvents()).containsExactly(new TestDomainEvent(Instant.EPOCH));
		assertThat(aggregate.pullDomainEvents()).isEmpty();
	}

	private static final class TestAggregate extends AggregateRoot<String> {

		private TestAggregate(String id) {
			super(id);
		}

		private void markChanged() {
			registerEvent(new TestDomainEvent(Instant.EPOCH));
		}
	}

	private record TestDomainEvent(Instant occurredAt) implements DomainEvent {
	}
}
