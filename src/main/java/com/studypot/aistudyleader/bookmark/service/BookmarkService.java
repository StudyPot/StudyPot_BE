package com.studypot.aistudyleader.bookmark.service;

import com.studypot.aistudyleader.bookmark.domain.BookmarkedGroup;
import com.studypot.aistudyleader.bookmark.repository.BookmarkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.transaction.annotation.Transactional;

public class BookmarkService {

	private final BookmarkRepository repository;
	private final Clock clock;
	private final Supplier<UUID> idGenerator;

	public BookmarkService(BookmarkRepository repository, Clock clock, Supplier<UUID> idGenerator) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator must not be null");
	}

	@Transactional
	public BookmarkToggleResult toggleBookmark(ToggleBookmarkCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		if (!repository.existsStudyGroup(command.groupId())) {
			throw new BookmarkGroupNotFoundException("study group was not found.");
		}
		Instant now = clock.instant();
		Optional<Boolean> active = repository.findBookmarkActive(command.authenticatedUserId(), command.groupId());
		boolean bookmarked;
		if (active.isEmpty()) {
			repository.insertBookmark(idGenerator.get(), command.authenticatedUserId(), command.groupId(), now);
			bookmarked = true;
		} else if (active.get()) {
			repository.softDeleteBookmark(command.authenticatedUserId(), command.groupId(), now);
			bookmarked = false;
		} else {
			repository.reactivateBookmark(command.authenticatedUserId(), command.groupId(), now);
			bookmarked = true;
		}
		return new BookmarkToggleResult(command.groupId(), bookmarked);
	}

	@Transactional(readOnly = true)
	public List<BookmarkedGroup> listMyBookmarks(ListMyBookmarksQuery query) {
		Objects.requireNonNull(query, "query must not be null");
		return repository.findMyBookmarks(query.authenticatedUserId());
	}
}
