package com.studypot.aistudyleader.bookmark.service;

import com.studypot.aistudyleader.bookmark.repository.BookmarkRepository;
import com.studypot.aistudyleader.global.domain.UuidV7;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class BookmarkApplicationConfiguration {

	@Bean
	@ConditionalOnBean({BookmarkRepository.class, Clock.class})
	BookmarkService bookmarkService(BookmarkRepository repository, Clock clock) {
		return new BookmarkService(repository, clock, UuidV7::generate);
	}
}
