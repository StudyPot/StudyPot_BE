package com.studypot.aistudyleader.studygroup.catalog.controller;

import com.studypot.aistudyleader.global.api.ApiPaths;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogCommand;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogEntry;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogPage;
import com.studypot.aistudyleader.studygroup.catalog.StudyGroupCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스터디 그룹 카탈로그", description = "스터디 그룹 핵심 CRUD, 검색, 정렬, 페이지네이션, 상세 조회 API입니다.")
@RestController
@RequestMapping(ApiPaths.V1 + "/groups/catalog")
@RequiredArgsConstructor
class StudyGroupCatalogController {

	private final StudyGroupCatalogService service;

	@Operation(summary = "스터디 그룹 등록", description = "스터디 그룹을 등록하고 201 상태 코드로 상세 응답을 반환합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "스터디 그룹 등록 성공"),
		@ApiResponse(responseCode = "422", description = "입력 검증 실패")
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	StudyGroupCatalogResponse create(@Valid @RequestBody StudyGroupCatalogRequest request) {
		return StudyGroupCatalogResponse.from(service.create(request.toCommand()));
	}

	@Operation(
		summary = "스터디 그룹 목록 검색",
		description = """
			키워드, 상태, 정렬, UUID 커서 페이지네이션으로 스터디 그룹 목록을 조회합니다.
			조회 SQL은 pageSize+1개를 요청해 nextCursor를 산출하고, MySQL utf8mb4_0900_ai_ci collation에서
			study_group_catalog_status_idx, study_group_catalog_name_idx, study_group_catalog_topic_idx,
			study_group_catalog_favorite_start_idx, study_group_catalog_search_cursor_idx 인덱스를 활용하도록
			deleted_at/status 필터, favorite·starts_at·id 정렬, id 커서 조건을 명시합니다.
			"""
	)
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "검색 조건과 페이지네이션이 적용된 목록 반환")
	})
	@GetMapping
	StudyGroupCatalogPageResponse search(
		@Parameter(description = "이름 또는 주제에 적용할 부분 검색어입니다. DB collation 기반으로 대소문자를 구분하지 않습니다.")
		@RequestParam(required = false) String keyword,
		@Parameter(description = "그룹 상태 필터입니다. 비어 있으면 전체 상태를 조회합니다.")
		@RequestParam(required = false) String status,
		@Parameter(description = "정렬 기준입니다. favorite, name, startDate를 지원합니다.")
		@RequestParam(defaultValue = "favorite") String sort,
		@Parameter(description = "페이지 크기입니다. 서비스 계층에서 1~50으로 보정하고 SQL은 pageSize+1개를 조회합니다.")
		@RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize,
		@Parameter(description = "직전 페이지 마지막 항목의 UUID 커서입니다. SQL의 id > cursor 조건으로 다음 페이지를 조회합니다.")
		@RequestParam(required = false) String cursor
	) {
		StudyGroupCatalogPage page = service.search(keyword, status, sort, pageSize, cursor);
		return new StudyGroupCatalogPageResponse(page.items().stream().map(StudyGroupCatalogResponse::from).toList(), page.nextCursor());
	}

	@Operation(summary = "스터디 그룹 상세 조회", description = "연관 멤버 수, 리뷰 평균, 즐겨찾기 상태를 포함해 상세 정보를 조회합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "스터디 그룹 상세 반환"),
		@ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음")
	})
	@GetMapping("/{groupId}")
	StudyGroupCatalogResponse detail(
		@Parameter(description = "상세 조회할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		return StudyGroupCatalogResponse.from(service.detail(groupId));
	}

	@Operation(summary = "스터디 그룹 수정", description = "스터디 그룹 이름, 주제, 상태, 기간, 즐겨찾기 상태를 수정합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "스터디 그룹 수정 성공"),
		@ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음"),
		@ApiResponse(responseCode = "422", description = "입력 검증 실패")
	})
	@PatchMapping("/{groupId}")
	StudyGroupCatalogResponse update(
		@Parameter(description = "수정할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId,
		@Valid @RequestBody StudyGroupCatalogRequest request
	) {
		return StudyGroupCatalogResponse.from(service.update(groupId, request.toCommand()));
	}

	@Operation(summary = "스터디 그룹 삭제", description = "스터디 그룹을 삭제하고 이후 상세 조회에서는 404로 응답합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "204", description = "스터디 그룹 삭제 성공"),
		@ApiResponse(responseCode = "404", description = "스터디 그룹을 찾을 수 없음")
	})
	@DeleteMapping("/{groupId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
		@Parameter(description = "삭제할 스터디 그룹 UUID입니다.", required = true)
		@PathVariable UUID groupId
	) {
		service.delete(groupId);
	}

	@Schema(description = "스터디 그룹 생성·수정 요청입니다.")
	private record StudyGroupCatalogRequest(
		@NotBlank @Size(max = 120) String name,
		@NotBlank @Size(max = 120) String topic,
		@NotBlank @Size(max = 40) String status,
		@NotNull LocalDate startsAt,
		@NotNull LocalDate endsAt,
		boolean favorite
	) {

		private StudyGroupCatalogCommand toCommand() {
			return new StudyGroupCatalogCommand(name, topic, status, startsAt, endsAt, favorite);
		}
	}

	@Schema(description = "스터디 그룹 상세 응답입니다.")
	private record StudyGroupCatalogResponse(
		UUID id,
		String name,
		String topic,
		String status,
		LocalDate startsAt,
		LocalDate endsAt,
		int memberCount,
		double averageRating,
		boolean favorite
	) {

		private static StudyGroupCatalogResponse from(StudyGroupCatalogEntry entry) {
			return new StudyGroupCatalogResponse(
				entry.id(),
				entry.name(),
				entry.topic(),
				entry.status(),
				entry.startsAt(),
				entry.endsAt(),
				entry.memberCount(),
				entry.averageRating(),
				entry.favorite()
			);
		}
	}

	@Schema(description = "스터디 그룹 목록 페이지 응답입니다.")
	private record StudyGroupCatalogPageResponse(List<StudyGroupCatalogResponse> items, String nextCursor) {
	}
}
