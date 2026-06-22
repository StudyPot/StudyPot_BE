package com.studypot.aistudyleader.bookmark.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.bookmark.domain.BookmarkedGroup;
import com.studypot.aistudyleader.bookmark.service.BookmarkService;
import com.studypot.aistudyleader.bookmark.service.BookmarkServiceUnavailableException;
import com.studypot.aistudyleader.bookmark.service.BookmarkToggleResult;
import com.studypot.aistudyleader.bookmark.service.ListMyBookmarksQuery;
import com.studypot.aistudyleader.bookmark.service.ToggleBookmarkCommand;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.domain.StudyGroup;
import com.studypot.aistudyleader.studygroup.domain.StudyGroupStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "북마크", description = "사용자가 스터디 그룹을 찜(북마크)하고 목록을 조회하는 API입니다.")
@RestController
@RequiredArgsConstructor
class BookmarkController {

	private final ObjectProvider<BookmarkService> bookmarkService;

	@Operation(summary = "그룹 북마크 토글", description = "스터디 그룹 북마크(찜) 상태를 토글하고 변경된 상태를 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "북마크 상태 토글 결과 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "북마크 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/bookmark")
	BookmarkToggleResponse toggleBookmark(
		Authentication authentication,
		@Parameter(description = "북마크를 토글할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		BookmarkToggleResult result = service().toggleBookmark(
			new ToggleBookmarkCommand(authenticatedUserId(authentication), groupId)
		);
		return new BookmarkToggleResponse(result.groupId(), result.bookmarked());
	}

	@Operation(summary = "내 북마크 목록 조회", description = "인증된 사용자가 북마크한 스터디 그룹 목록을 최신순으로 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "북마크 그룹 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "503", description = "북마크 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/bookmarks")
	List<BookmarkResponse> listMyBookmarks(Authentication authentication) {
		return service().listMyBookmarks(new ListMyBookmarksQuery(authenticatedUserId(authentication))).stream()
			.map(BookmarkResponse::from)
			.toList();
	}

	private BookmarkService service() {
		return bookmarkService.getIfAvailable(() -> {
			throw new BookmarkServiceUnavailableException("bookmark service is not configured.");
		});
	}

	private static UUID authenticatedUserId(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		String subject = authenticatedSubject(authentication);
		if (subject == null || subject.isBlank()) {
			throw new AuthSessionRejectedException("authenticated user is required.");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new AuthSessionRejectedException("authenticated user is invalid.");
		}
	}

	private static String authenticatedSubject(Authentication authentication) {
		Object principal = authentication.getPrincipal();
		if (principal instanceof Jwt jwt) {
			return jwt.getSubject();
		}
		return authentication.getName();
	}

	@Schema(description = "북마크 토글 결과입니다.")
	private record BookmarkToggleResponse(
		@Schema(description = "대상 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "토글 후 북마크 여부입니다.", example = "true")
		boolean bookmarked
	) {
	}

	@Schema(description = "북마크한 스터디 그룹 항목입니다.")
	private record BookmarkResponse(
		@Schema(description = "북마크한 스터디 그룹 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
		UUID groupId,
		@Schema(description = "북마크한 스터디 그룹 정보입니다.")
		BookmarkGroupResponse group,
		@Schema(description = "북마크한 시각입니다.", example = "2026-06-22T10:00:00Z")
		Instant bookmarkedAt
	) {

		private static BookmarkResponse from(BookmarkedGroup bookmarked) {
			StudyGroup group = bookmarked.group();
			return new BookmarkResponse(group.id(), BookmarkGroupResponse.from(group), bookmarked.bookmarkedAt());
		}
	}

	@Schema(description = "북마크 응답에 포함되는 스터디 그룹 요약입니다.")
	private record BookmarkGroupResponse(
		UUID id,
		UUID createdBy,
		String name,
		String topic,
		List<String> detailKeywords,
		StudyGroupStatus status,
		int maxMembers,
		String inviteCode,
		LocalDate startsAt,
		LocalDate endsAt
	) {

		private static BookmarkGroupResponse from(StudyGroup group) {
			return new BookmarkGroupResponse(
				group.id(),
				group.createdBy(),
				group.name(),
				group.topic(),
				group.detailKeywords(),
				group.status(),
				group.maxMembers(),
				group.inviteCode(),
				group.startsAt(),
				group.endsAt()
			);
		}
	}
}
