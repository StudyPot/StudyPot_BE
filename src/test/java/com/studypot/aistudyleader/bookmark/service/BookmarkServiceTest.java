package com.studypot.aistudyleader.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studypot.aistudyleader.bookmark.domain.BookmarkedGroup;
import com.studypot.aistudyleader.bookmark.repository.BookmarkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class BookmarkServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-22T00:00:00Z"), ZoneOffset.UTC);
	private static final UUID USER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001");
	private static final UUID GROUP_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002");

	@Test
	void togglingUnbookmarkedGroupInsertsBookmark() {
		FakeRepository repository = new FakeRepository();
		repository.groupExists = true;
		BookmarkService service = service(repository);

		BookmarkToggleResult result = service.toggleBookmark(new ToggleBookmarkCommand(USER_ID, GROUP_ID));

		assertThat(result.bookmarked()).isTrue();
		assertThat(repository.inserts).isEqualTo(1);
		assertThat(repository.active).isTrue();
	}

	@Test
	void togglingBookmarkedGroupSoftDeletesBookmark() {
		FakeRepository repository = new FakeRepository();
		repository.groupExists = true;
		repository.active = true;
		BookmarkService service = service(repository);

		BookmarkToggleResult result = service.toggleBookmark(new ToggleBookmarkCommand(USER_ID, GROUP_ID));

		assertThat(result.bookmarked()).isFalse();
		assertThat(repository.softDeletes).isEqualTo(1);
	}

	@Test
	void togglingPreviouslyRemovedBookmarkReactivatesIt() {
		FakeRepository repository = new FakeRepository();
		repository.groupExists = true;
		repository.active = false;
		BookmarkService service = service(repository);

		BookmarkToggleResult result = service.toggleBookmark(new ToggleBookmarkCommand(USER_ID, GROUP_ID));

		assertThat(result.bookmarked()).isTrue();
		assertThat(repository.reactivations).isEqualTo(1);
	}

	@Test
	void togglingMissingGroupThrowsNotFound() {
		FakeRepository repository = new FakeRepository();
		repository.groupExists = false;
		BookmarkService service = service(repository);

		assertThatThrownBy(() -> service.toggleBookmark(new ToggleBookmarkCommand(USER_ID, GROUP_ID)))
			.isInstanceOf(BookmarkGroupNotFoundException.class);
	}

	private static BookmarkService service(BookmarkRepository repository) {
		AtomicInteger counter = new AtomicInteger();
		return new BookmarkService(repository, CLOCK,
			() -> UUID.fromString("018f0000-0000-7000-8000-0000000000" + String.format("%02d", counter.incrementAndGet())));
	}

	private static final class FakeRepository implements BookmarkRepository {

		private boolean groupExists;
		private Boolean active;
		private int inserts;
		private int softDeletes;
		private int reactivations;

		@Override
		public boolean existsStudyGroup(UUID groupId) {
			return groupExists;
		}

		@Override
		public Optional<Boolean> findBookmarkActive(UUID userId, UUID groupId) {
			return Optional.ofNullable(active);
		}

		@Override
		public void insertBookmark(UUID id, UUID userId, UUID groupId, Instant now) {
			inserts++;
			active = true;
		}

		@Override
		public void reactivateBookmark(UUID userId, UUID groupId, Instant now) {
			reactivations++;
			active = true;
		}

		@Override
		public void softDeleteBookmark(UUID userId, UUID groupId, Instant now) {
			softDeletes++;
			active = false;
		}

		@Override
		public List<BookmarkedGroup> findMyBookmarks(UUID userId) {
			return new ArrayList<>();
		}
	}
}
