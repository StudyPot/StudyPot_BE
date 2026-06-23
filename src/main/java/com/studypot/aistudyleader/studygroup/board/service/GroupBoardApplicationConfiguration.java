package com.studypot.aistudyleader.studygroup.board.service;

import com.studypot.aistudyleader.global.domain.UuidV7;
import com.studypot.aistudyleader.notification.service.NotificationEventPublisher;
import com.studypot.aistudyleader.studygroup.board.repository.GroupBoardRepository;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GroupBoardApplicationConfiguration {

	@Bean
	@ConditionalOnBean(GroupBoardRepository.class)
	GroupBoardService groupBoardService(
		GroupBoardRepository repository,
		Clock clock,
		ObjectProvider<NotificationEventPublisher> notificationEvents
	) {
		return new GroupBoardService(
			repository,
			clock,
			UuidV7::generate,
			notificationEvents.getIfAvailable(NotificationEventPublisher::noop)
		);
	}
}
