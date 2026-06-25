package com.studypot.aistudyleader.bookmark.domain;

import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import java.time.Instant;
import java.util.Objects;

/**
 * 사용자가 북마크(찜)한 스터디 그룹과 북마크 시각입니다.
 */
public record BookmarkedGroup(StudyGroup group, Instant bookmarkedAt) {

	public BookmarkedGroup {
		Objects.requireNonNull(group, "group must not be null");
		Objects.requireNonNull(bookmarkedAt, "bookmarkedAt must not be null");
	}
}
