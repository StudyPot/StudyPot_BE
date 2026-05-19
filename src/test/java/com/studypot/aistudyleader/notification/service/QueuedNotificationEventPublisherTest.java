package com.studypot.aistudyleader.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.studypot.aistudyleader.notification.domain.NotificationType;
import com.studypot.aistudyleader.notification.repository.NotificationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class QueuedNotificationEventPublisherTest {

	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000008401");
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008402");
	private static final UUID OTHER_USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000008403");
	private static final UUID WEEK_ID = UUID.fromString("018f0000-0000-7000-8000-000000008404");

	private final NotificationRepository repository = mock(NotificationRepository.class);
	private final CapturingNotificationJobPublisher jobs = new CapturingNotificationJobPublisher();
	private final QueuedNotificationEventPublisher publisher = new QueuedNotificationEventPublisher(repository, jobs);

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void onboardingRequestedEnqueuesExistingNotificationCommandAfterCommit() {
		TransactionSynchronizationManager.initSynchronization();

		publisher.publishOnboardingRequested(GROUP_ID, USER_ID);

		assertThat(jobs.commands).isEmpty();
		TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

		CreateNotificationCommand command = singleCommand();
		assertThat(command.groupId()).isEqualTo(GROUP_ID);
		assertThat(command.recipientUserId()).isEqualTo(USER_ID);
		assertThat(command.notificationType()).isEqualTo(NotificationType.ONBOARDING_REQUESTED);
		assertThat(command.idempotencyKey())
			.isEqualTo("idempotency:notification:onboarding-requested:group:%s:recipient:%s".formatted(GROUP_ID, USER_ID));
		assertThat(command.payload()).containsEntry("deepLink", "/groups/%s/onboarding".formatted(GROUP_ID));
	}

	@Test
	void weekStartedEnqueuesOneJobPerActiveRecipient() {
		when(repository.findActiveGroupRecipientUserIds(GROUP_ID)).thenReturn(List.of(USER_ID, OTHER_USER_ID));

		publisher.publishWeekStarted(GROUP_ID, WEEK_ID, 3, "JPA 심화");

		assertThat(jobs.commands).hasSize(2);
		assertThat(jobs.commands)
			.extracting(CreateNotificationCommand::recipientUserId)
			.containsExactly(USER_ID, OTHER_USER_ID);
		assertThat(jobs.commands)
			.allSatisfy(command -> {
				assertThat(command.notificationType()).isEqualTo(NotificationType.WEEK_STARTED);
				assertThat(command.relatedResources().weekId()).isEqualTo(WEEK_ID);
				assertThat(command.idempotencyKey()).contains(WEEK_ID.toString(), command.recipientUserId().toString());
				assertThat(command.payload()).containsEntry("weekTitle", "JPA 심화");
			});
	}

	@Test
	void queueFailureDoesNotEscapeEventPublisher() {
		jobs.throwOnPublish = true;

		assertThatCode(() -> publisher.publishTaskDueReminder(
			GROUP_ID,
			USER_ID,
			WEEK_ID,
			UUID.fromString("018f0000-0000-7000-8000-000000008405"),
			"테스트 작성",
			Instant.parse("2026-05-20T12:00:00Z")
		)).doesNotThrowAnyException();

		assertThat(jobs.attempts).isEqualTo(1);
		assertThat(jobs.commands).isEmpty();
	}

	private CreateNotificationCommand singleCommand() {
		assertThat(jobs.commands).hasSize(1);
		return jobs.commands.getFirst();
	}

	private static final class CapturingNotificationJobPublisher implements NotificationJobPublisher {

		private final List<CreateNotificationCommand> commands = new ArrayList<>();
		private int attempts;
		private boolean throwOnPublish;

		@Override
		public void publish(CreateNotificationCommand command) {
			attempts++;
			if (throwOnPublish) {
				throw new IllegalStateException("rabbitmq unavailable");
			}
			commands.add(command);
		}
	}
}
