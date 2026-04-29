package com.studypot.aistudyleader.global.api;

import java.util.List;
import java.util.Objects;

public record CursorPageResponse<T>(List<T> items, PageInfoResponse pageInfo) {

	public CursorPageResponse {
		items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
		Objects.requireNonNull(pageInfo, "pageInfo must not be null");
	}

	public static <T> CursorPageResponse<T> firstPage(List<T> items, String nextCursor) {
		return new CursorPageResponse<>(items, new PageInfoResponse(nextCursor, nextCursor != null));
	}
}
