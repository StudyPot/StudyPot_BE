package com.studypot.aistudyleader.studygroup.board.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record GroupBoardPostCursor(boolean pinned, Instant createdAt, UUID id) {

	public GroupBoardPostCursor {
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		Objects.requireNonNull(id, "id must not be null");
	}
}
