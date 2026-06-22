package com.studypot.aistudyleader.bookmark.repository;

import com.studypot.aistudyleader.bookmark.domain.BookmarkedGroup;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository {

	boolean existsStudyGroup(UUID groupId);

	/**
	 * 북마크 상태를 조회합니다. 존재하지 않으면 비어 있고, 활성(찜됨)이면 true, 소프트삭제 상태면 false 입니다.
	 */
	Optional<Boolean> findBookmarkActive(UUID userId, UUID groupId);

	void insertBookmark(UUID id, UUID userId, UUID groupId, Instant now);

	void reactivateBookmark(UUID userId, UUID groupId, Instant now);

	void softDeleteBookmark(UUID userId, UUID groupId, Instant now);

	List<BookmarkedGroup> findMyBookmarks(UUID userId);
}
