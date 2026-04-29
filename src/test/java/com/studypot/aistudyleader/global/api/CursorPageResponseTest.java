package com.studypot.aistudyleader.global.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorPageResponseTest {

	@Test
	void firstPageContainsItemsAndPageInfo() {
		var response = CursorPageResponse.firstPage(List.of("group-1"), "next-cursor");

		assertThat(response.items()).containsExactly("group-1");
		assertThat(response.pageInfo().nextCursor()).isEqualTo("next-cursor");
		assertThat(response.pageInfo().hasNext()).isTrue();
	}

	@Test
	void firstPageWithoutCursorMarksNoNextPage() {
		var response = CursorPageResponse.firstPage(List.of(), null);

		assertThat(response.items()).isEmpty();
		assertThat(response.pageInfo().nextCursor()).isNull();
		assertThat(response.pageInfo().hasNext()).isFalse();
	}

	@Test
	void responseDefensivelyCopiesItems() {
		var items = new ArrayList<>(List.of("group-1"));
		var response = CursorPageResponse.firstPage(items, null);

		items.add("group-2");

		assertThat(response.items()).containsExactly("group-1");
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> response.items().add("group-3"));
	}

	@Test
	void itemsAreRequired() {
		assertThatNullPointerException()
			.isThrownBy(() -> CursorPageResponse.firstPage(null, null))
			.withMessage("items must not be null");
	}
}
