# EXEC_PLAN: [feat] 게시판 전체조회 API + 활동 개수에 게시글 포함

- Task slug: `board-all-posts-activity`
- Base branch: `develop`
- Feature branch: `codex/board-all-posts-activity`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/board-all-posts-activity`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/board-all-posts-activity`
- Jira issue: `SPT-144`
- Jira URL: https://studypot.atlassian.net/browse/SPT-144
- Jira summary: [feat] 게시판 전체 글 조회 API + 활동 잔디 개수에 게시글 작성 포함
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/notification-contract-v1.md
- [ ] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] study-group
- [ ] <feature-id>

## Doc Notes
- api-contract-v1: 신규 조회 엔드포인트는 기존 커서 페이지네이션 규약(CursorPageResponse)을 그대로 따른다. 게시판 전체 조회는 기존 보드별 조회와 동일 응답 스키마(GroupBoardPostSummaryResponse)를 재사용해 계약 호환성을 유지한다.
- 활동 잔디 응답 스키마(GroupActivityHeatmapResponse)는 변경하지 않는다. 집계 소스만 "완료 todo"에서 "완료 todo + 작성 게시글"로 확장한다(개수 의미만 넓힘).

## Goal
백엔드 신규 기능 2종:
- 게시판 전체 조회: 그룹의 모든 게시판 글을 한 번에(보드 구분 없이) 최신순 커서 페이지로 조회하는 API.
- 활동 잔디 개수에 게시글 포함: 활동 히트맵 개수에 완료한 todo 뿐 아니라 작성한 게시글도 합산.

## Approach
- 게시판 전체 조회: `GET /groups/{groupId}/posts` 추가. `SELECT_ALL_POSTS`(SELECT_POSTS 에서 board_id 조건 제거, 정렬/커서 동일) + `findAllPosts` repo + `ListAllGroupBoardPostsQuery` + `GroupBoardService.listAllPosts`. 응답/커서는 보드별 조회와 동일.
- 활동 개수: `SELECT_GROUP_DONE_ACTIVITY_COUNTS` 를 task_completion(DONE) 과 group_board_post(작성) 의 UNION ALL 서브쿼리로 LEFT JOIN 집계하도록 변경. 파라미터는 (from,to,from,to,groupId). 응답 매핑/스키마는 그대로.

## Step Plan
1. GroupBoardJdbcSql.SELECT_ALL_POSTS 추가 + repo(findAllPosts) + Jdbc 구현.
2. ListAllGroupBoardPostsQuery + GroupBoardService.listAllPosts + 컨트롤러 GET /groups/{groupId}/posts.
3. 활동 집계 SQL UNION 확장 + Jdbc 파라미터 조정.
4. 테스트: 보드 서비스/컨트롤러 fake 에 findAllPosts, listAllPosts 서비스 테스트, 활동 SQL 내용 테스트.
5. `./gradlew check build` 그린.

## Done Criteria
- `GET /groups/{groupId}/posts` 가 활성 멤버에게 그룹 전체 게시글을 커서 페이지로 반환한다.
- 활동 히트맵 개수에 작성한 게시글이 합산된다.
- 기존 보드/커리큘럼 테스트가 모두 통과하고 신규 테스트가 통과한다.
- `./gradlew check build` 그린.
