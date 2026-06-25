package com.studypot.aistudyleader.studygroup.board.controller;

import com.studypot.aistudyleader.auth.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.global.api.CursorPageResponse;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoard;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardComment;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPost;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardPostSummary;
import com.studypot.aistudyleader.studygroup.board.domain.GroupBoardType;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardCommentCommand;
import com.studypot.aistudyleader.studygroup.board.service.CreateGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.DeleteGroupBoardCommentCommand;
import com.studypot.aistudyleader.studygroup.board.service.DeleteGroupBoardPostCommand;
import com.studypot.aistudyleader.studygroup.board.service.GetGroupBoardPostQuery;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardService;
import com.studypot.aistudyleader.studygroup.board.service.GroupBoardServiceUnavailableException;
import com.studypot.aistudyleader.studygroup.board.service.ListGroupBoardCommentsQuery;
import com.studypot.aistudyleader.studygroup.board.service.ListAllGroupBoardPostsQuery;
import com.studypot.aistudyleader.studygroup.board.service.ListGroupBoardPostsQuery;
import com.studypot.aistudyleader.studygroup.board.service.ListGroupBoardsQuery;
import com.studypot.aistudyleader.studygroup.board.service.UpdateGroupBoardCommentCommand;
import com.studypot.aistudyleader.studygroup.board.service.UpdateGroupBoardPostCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "그룹 게시판", description = "스터디 그룹별 기본 게시판, 게시글, 댓글을 관리하는 API입니다.")
@RestController
@RequiredArgsConstructor
class GroupBoardController {

	private final ObjectProvider<GroupBoardService> groupBoardService;

	@Operation(
		summary = "그룹 게시판 목록 조회",
		description = "활성 그룹 멤버가 그룹의 기본 게시판 목록을 조회합니다. 게시판이 없으면 기본 게시판을 생성한 뒤 반환합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 게시판 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/boards")
	List<GroupBoardResponse> listBoards(
		Authentication authentication,
		@Parameter(description = "게시판 목록을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return service().listBoards(new ListGroupBoardsQuery(authenticatedUserId(authentication), groupId))
			.stream()
			.map(GroupBoardResponse::from)
			.toList();
	}

	@Operation(
		summary = "그룹 게시글 목록 조회",
		description = "활성 그룹 멤버가 특정 게시판의 게시글을 고정글 우선, 최신순 커서 페이지로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "게시글 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹 또는 게시판 접근 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시판을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "커서 또는 페이지 크기가 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/boards/{boardId}/posts")
	CursorPageResponse<GroupBoardPostSummaryResponse> listPosts(
		Authentication authentication,
		@Parameter(description = "게시판이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "게시글을 조회할 게시판 UUID입니다.", required = true)
		@PathVariable UUID boardId,
		@Parameter(description = "다음 페이지 조회용 커서입니다.")
		@RequestParam(required = false) String cursor,
		@Parameter(description = "정렬 기준입니다. (createdAt, commentCount) 기본 createdAt.")
		@RequestParam(required = false) String sort,
		@Parameter(description = "정렬 방향입니다. (asc, desc) 기본 desc.")
		@RequestParam(required = false) String order,
		@Parameter(description = "조회할 게시글 수입니다. 1부터 100까지 허용됩니다.")
		@RequestParam(defaultValue = "20") int pageSize
	) {
		CursorPageResponse<GroupBoardPostSummary> page = service().listPosts(
			new ListGroupBoardPostsQuery(authenticatedUserId(authentication), groupId, boardId, cursor, sort, order, pageSize)
		);
		return new CursorPageResponse<>(
			page.items().stream().map(GroupBoardPostSummaryResponse::from).toList(),
			page.pageInfo()
		);
	}

	@Operation(
		summary = "그룹 게시판 전체 글 조회",
		description = "활성 그룹 멤버가 그룹의 모든 게시판 글을 고정글 우선, 최신순 커서 페이지로 한 번에 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "그룹 전체 게시글 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아님"),
		@ApiResponse(responseCode = "404", description = "대상 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "커서 또는 페이지 크기가 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/posts")
	CursorPageResponse<GroupBoardPostSummaryResponse> listAllPosts(
		Authentication authentication,
		@Parameter(description = "게시글을 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "다음 페이지 조회용 커서입니다.")
		@RequestParam(required = false) String cursor,
		@Parameter(description = "정렬 기준입니다. (createdAt, commentCount) 기본 createdAt.")
		@RequestParam(required = false) String sort,
		@Parameter(description = "정렬 방향입니다. (asc, desc) 기본 desc.")
		@RequestParam(required = false) String order,
		@Parameter(description = "조회할 게시글 수입니다. 1부터 100까지 허용됩니다.")
		@RequestParam(defaultValue = "20") int pageSize
	) {
		CursorPageResponse<GroupBoardPostSummary> page = service().listAllPosts(
			new ListAllGroupBoardPostsQuery(authenticatedUserId(authentication), groupId, cursor, sort, order, pageSize)
		);
		return new CursorPageResponse<>(
			page.items().stream().map(GroupBoardPostSummaryResponse::from).toList(),
			page.pageInfo()
		);
	}

	@Operation(
		summary = "그룹 게시글 생성",
		description = "활성 그룹 멤버가 특정 게시판에 게시글을 생성합니다. 고정글 생성은 그룹 OWNER만 가능합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "게시글 생성 후 상세 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹 또는 게시판에 작성할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시판을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "제목 또는 본문이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/boards/{boardId}/posts")
	@ResponseStatus(HttpStatus.CREATED)
	GroupBoardPostResponse createPost(
		Authentication authentication,
		@Parameter(description = "게시판이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "게시글을 생성할 게시판 UUID입니다.", required = true)
		@PathVariable UUID boardId,
		@Valid @RequestBody CreatePostRequest request
	) {
		GroupBoardPost post = service().createPost(request.toCommand(authenticatedUserId(authentication), groupId, boardId));
		return GroupBoardPostResponse.from(post);
	}

	@Operation(
		summary = "그룹 게시글 상세 조회",
		description = "활성 그룹 멤버가 게시글 상세를 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "게시글 상세 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹의 활성 멤버가 아니거나 게시글이 다른 그룹에 속함"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시글을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/posts/{postId}")
	GroupBoardPostResponse getPost(
		Authentication authentication,
		@Parameter(description = "게시글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "조회할 게시글 UUID입니다.", required = true)
		@PathVariable UUID postId
	) {
		return GroupBoardPostResponse.from(service().getPost(new GetGroupBoardPostQuery(authenticatedUserId(authentication), groupId, postId)));
	}

	@Operation(
		summary = "그룹 게시글 수정",
		description = "작성자는 제목과 본문을 수정할 수 있고, 그룹 OWNER는 고정 여부만 변경할 수 있습니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "게시글 수정 후 상세 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "게시글 수정 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시글을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "수정 요청이 비어 있거나 값이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/posts/{postId}")
	GroupBoardPostResponse updatePost(
		Authentication authentication,
		@Parameter(description = "게시글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "수정할 게시글 UUID입니다.", required = true)
		@PathVariable UUID postId,
		@Valid @RequestBody UpdatePostRequest request
	) {
		GroupBoardPost post = service().updatePost(request.toCommand(authenticatedUserId(authentication), groupId, postId));
		return GroupBoardPostResponse.from(post);
	}

	@Operation(
		summary = "그룹 게시글 삭제",
		description = "게시글 작성자 또는 그룹 OWNER가 게시글을 soft delete 처리합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "게시글 삭제 완료"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "게시글 삭제 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시글을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@DeleteMapping(ApiPaths.V1 + "/groups/{groupId}/posts/{postId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deletePost(
		Authentication authentication,
		@Parameter(description = "게시글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "삭제할 게시글 UUID입니다.", required = true)
		@PathVariable UUID postId
	) {
		service().deletePost(new DeleteGroupBoardPostCommand(authenticatedUserId(authentication), groupId, postId));
	}

	@Operation(
		summary = "그룹 게시글 댓글 목록 조회",
		description = "활성 그룹 멤버가 게시글 댓글을 작성순 커서 페이지로 조회합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "댓글 목록 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹 또는 게시글 접근 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시글을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "커서 또는 페이지 크기가 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/groups/{groupId}/posts/{postId}/comments")
	CursorPageResponse<GroupBoardCommentResponse> listComments(
		Authentication authentication,
		@Parameter(description = "게시글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "댓글을 조회할 게시글 UUID입니다.", required = true)
		@PathVariable UUID postId,
		@Parameter(description = "다음 페이지 조회용 커서입니다.")
		@RequestParam(required = false) String cursor,
		@Parameter(description = "조회할 댓글 수입니다. 1부터 100까지 허용됩니다.")
		@RequestParam(defaultValue = "20") int pageSize
	) {
		CursorPageResponse<GroupBoardComment> page = service().listComments(
			new ListGroupBoardCommentsQuery(authenticatedUserId(authentication), groupId, postId, cursor, pageSize)
		);
		return new CursorPageResponse<>(
			page.items().stream().map(GroupBoardCommentResponse::from).toList(),
			page.pageInfo()
		);
	}

	@Operation(
		summary = "그룹 게시글 댓글 생성",
		description = "활성 그룹 멤버가 게시글에 댓글을 작성합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "댓글 생성 후 상세 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "대상 그룹 또는 게시글에 댓글을 작성할 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 게시글을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "댓글 내용이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@PostMapping(ApiPaths.V1 + "/groups/{groupId}/posts/{postId}/comments")
	@ResponseStatus(HttpStatus.CREATED)
	GroupBoardCommentResponse createComment(
		Authentication authentication,
		@Parameter(description = "게시글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "댓글을 작성할 게시글 UUID입니다.", required = true)
		@PathVariable UUID postId,
		@Valid @RequestBody CreateCommentRequest request
	) {
		GroupBoardComment comment = service().createComment(request.toCommand(authenticatedUserId(authentication), groupId, postId));
		return GroupBoardCommentResponse.from(comment);
	}

	@Operation(
		summary = "그룹 게시글 댓글 수정",
		description = "댓글 작성자가 자신의 댓글 내용을 수정합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "댓글 수정 후 상세 반환"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "댓글 수정 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 댓글을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "댓글 내용이 올바르지 않음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@PatchMapping(ApiPaths.V1 + "/groups/{groupId}/comments/{commentId}")
	GroupBoardCommentResponse updateComment(
		Authentication authentication,
		@Parameter(description = "댓글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "수정할 댓글 UUID입니다.", required = true)
		@PathVariable UUID commentId,
		@Valid @RequestBody UpdateCommentRequest request
	) {
		GroupBoardComment comment = service().updateComment(request.toCommand(authenticatedUserId(authentication), groupId, commentId));
		return GroupBoardCommentResponse.from(comment);
	}

	@Operation(
		summary = "그룹 게시글 댓글 삭제",
		description = "댓글 작성자 또는 그룹 OWNER가 댓글을 soft delete 처리합니다."
	)
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "댓글 삭제 완료"),
		@ApiResponse(responseCode = "401", description = "인증된 사용자 정보를 확인할 수 없음"),
		@ApiResponse(responseCode = "403", description = "댓글 삭제 권한이 없음"),
		@ApiResponse(responseCode = "404", description = "대상 그룹 또는 댓글을 찾을 수 없음"),
		@ApiResponse(responseCode = "503", description = "그룹 게시판 서비스가 아직 구성되지 않음")
	})
	@DeleteMapping(ApiPaths.V1 + "/groups/{groupId}/comments/{commentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteComment(
		Authentication authentication,
		@Parameter(description = "댓글이 속한 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Parameter(description = "삭제할 댓글 UUID입니다.", required = true)
		@PathVariable UUID commentId
	) {
		service().deleteComment(new DeleteGroupBoardCommentCommand(authenticatedUserId(authentication), groupId, commentId));
	}

	private GroupBoardService service() {
		return groupBoardService.getIfAvailable(() -> {
			throw new GroupBoardServiceUnavailableException("group board service is not configured.");
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

	@Schema(description = "그룹 게시글 생성 요청입니다.")
	private record CreatePostRequest(
		@Schema(description = "게시글 제목입니다.", example = "이번 주 질문입니다.")
		@NotBlank
		@Size(max = 200)
		String title,
		@Schema(description = "게시글 본문입니다.", example = "JPA 연관관계 매핑에서 헷갈린 부분을 공유합니다.")
		@NotBlank
		@Size(max = 10000)
		String content,
		@Schema(description = "게시글 고정 여부입니다. true는 그룹 OWNER만 사용할 수 있습니다.", example = "false")
		Boolean pinned
	) {

		CreateGroupBoardPostCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID boardId) {
			return new CreateGroupBoardPostCommand(authenticatedUserId, groupId, boardId, title, content, Boolean.TRUE.equals(pinned));
		}
	}

	@Schema(description = "그룹 게시글 수정 요청입니다.")
	private record UpdatePostRequest(
		@Schema(description = "변경할 게시글 제목입니다.", example = "수정된 질문입니다.")
		@Size(max = 200)
		String title,
		@Schema(description = "변경할 게시글 본문입니다.", example = "수정된 본문입니다.")
		@Size(max = 10000)
		String content,
		@Schema(description = "변경할 게시글 고정 여부입니다.", example = "true")
		Boolean pinned
	) {

		UpdateGroupBoardPostCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID postId) {
			return new UpdateGroupBoardPostCommand(authenticatedUserId, groupId, postId, title, content, pinned);
		}
	}

	@Schema(description = "그룹 게시글 댓글 생성 요청입니다.")
	private record CreateCommentRequest(
		@Schema(description = "댓글 내용입니다.", example = "저도 같은 부분이 궁금합니다.")
		@NotBlank
		@Size(max = 3000)
		String content
	) {

		CreateGroupBoardCommentCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID postId) {
			return new CreateGroupBoardCommentCommand(authenticatedUserId, groupId, postId, content);
		}
	}

	@Schema(description = "그룹 게시글 댓글 수정 요청입니다.")
	private record UpdateCommentRequest(
		@Schema(description = "변경할 댓글 내용입니다.", example = "수정된 댓글입니다.")
		@NotBlank
		@Size(max = 3000)
		String content
	) {

		UpdateGroupBoardCommentCommand toCommand(UUID authenticatedUserId, UUID groupId, UUID commentId) {
			return new UpdateGroupBoardCommentCommand(authenticatedUserId, groupId, commentId, content);
		}
	}

	@Schema(description = "그룹 게시판 응답입니다.")
	private record GroupBoardResponse(
		@Schema(description = "게시판 UUID입니다.")
		UUID id,
		@Schema(description = "게시판이 속한 그룹 UUID입니다.")
		UUID groupId,
		@Schema(description = "게시판 유형입니다.")
		GroupBoardType boardType,
		@Schema(description = "게시판 이름입니다.")
		String name,
		@Schema(description = "게시판 설명입니다.")
		String description,
		@Schema(description = "게시판 노출 순서입니다.")
		int displayOrder,
		@Schema(description = "기본 게시판 여부입니다.")
		boolean defaultBoard,
		@Schema(description = "생성 시각입니다.")
		Instant createdAt,
		@Schema(description = "수정 시각입니다.")
		Instant updatedAt
	) {

		static GroupBoardResponse from(GroupBoard board) {
			return new GroupBoardResponse(
				board.id(),
				board.groupId(),
				board.boardType(),
				board.name(),
				board.description(),
				board.displayOrder(),
				board.defaultBoard(),
				board.createdAt(),
				board.updatedAt()
			);
		}
	}

	@Schema(description = "게시판 작성자 응답입니다.")
	private record GroupBoardAuthorResponse(
		@Schema(description = "작성자 그룹 멤버 UUID입니다.")
		UUID memberId,
		@Schema(description = "작성자 사용자 UUID입니다.")
		UUID userId,
		@Schema(description = "작성자 표시 이름입니다.")
		String displayName
	) {

		static GroupBoardAuthorResponse from(UUID memberId, UUID userId, String displayName) {
			return new GroupBoardAuthorResponse(memberId, userId, displayName);
		}
	}

	@Schema(description = "그룹 게시글 목록 항목 응답입니다.")
	private record GroupBoardPostSummaryResponse(
		@Schema(description = "게시글 UUID입니다.")
		UUID id,
		@Schema(description = "게시글이 속한 그룹 UUID입니다.")
		UUID groupId,
		@Schema(description = "게시글이 속한 게시판 UUID입니다.")
		UUID boardId,
		@Schema(description = "작성자 정보입니다.")
		GroupBoardAuthorResponse author,
		@Schema(description = "게시글 제목입니다.")
		String title,
		@Schema(description = "게시글 본문 미리보기입니다.")
		String contentPreview,
		@Schema(description = "게시글 고정 여부입니다.")
		boolean pinned,
		@Schema(description = "댓글 수입니다.")
		int commentCount,
		@Schema(description = "생성 시각입니다.")
		Instant createdAt,
		@Schema(description = "수정 시각입니다.")
		Instant updatedAt
	) {

		static GroupBoardPostSummaryResponse from(GroupBoardPostSummary post) {
			return new GroupBoardPostSummaryResponse(
				post.id(),
				post.groupId(),
				post.boardId(),
				GroupBoardAuthorResponse.from(post.authorMemberId(), post.authorUserId(), post.authorDisplayName()),
				post.title(),
				post.contentPreview(),
				post.pinned(),
				post.commentCount(),
				post.createdAt(),
				post.updatedAt()
			);
		}
	}

	@Schema(description = "그룹 게시글 상세 응답입니다.")
	private record GroupBoardPostResponse(
		@Schema(description = "게시글 UUID입니다.")
		UUID id,
		@Schema(description = "게시글이 속한 그룹 UUID입니다.")
		UUID groupId,
		@Schema(description = "게시글이 속한 게시판 UUID입니다.")
		UUID boardId,
		@Schema(description = "작성자 정보입니다.")
		GroupBoardAuthorResponse author,
		@Schema(description = "게시글 제목입니다.")
		String title,
		@Schema(description = "게시글 본문입니다.")
		String content,
		@Schema(description = "게시글 고정 여부입니다.")
		boolean pinned,
		@Schema(description = "생성 시각입니다.")
		Instant createdAt,
		@Schema(description = "수정 시각입니다.")
		Instant updatedAt
	) {

		static GroupBoardPostResponse from(GroupBoardPost post) {
			return new GroupBoardPostResponse(
				post.id(),
				post.groupId(),
				post.boardId(),
				GroupBoardAuthorResponse.from(post.authorMemberId(), post.authorUserId(), post.authorDisplayName()),
				post.title(),
				post.content(),
				post.pinned(),
				post.createdAt(),
				post.updatedAt()
			);
		}
	}

	@Schema(description = "그룹 게시글 댓글 응답입니다.")
	private record GroupBoardCommentResponse(
		@Schema(description = "댓글 UUID입니다.")
		UUID id,
		@Schema(description = "댓글이 속한 그룹 UUID입니다.")
		UUID groupId,
		@Schema(description = "댓글이 속한 게시글 UUID입니다.")
		UUID postId,
		@Schema(description = "작성자 정보입니다.")
		GroupBoardAuthorResponse author,
		@Schema(description = "댓글 내용입니다.")
		String content,
		@Schema(description = "생성 시각입니다.")
		Instant createdAt,
		@Schema(description = "수정 시각입니다.")
		Instant updatedAt
	) {

		static GroupBoardCommentResponse from(GroupBoardComment comment) {
			return new GroupBoardCommentResponse(
				comment.id(),
				comment.groupId(),
				comment.postId(),
				GroupBoardAuthorResponse.from(comment.authorMemberId(), comment.authorUserId(), comment.authorDisplayName()),
				comment.content(),
				comment.createdAt(),
				comment.updatedAt()
			);
		}
	}
}
