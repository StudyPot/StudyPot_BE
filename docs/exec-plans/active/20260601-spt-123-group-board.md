# EXEC_PLAN: [study-group-board] 그룹별 게시판 기본 API 추가

- Task slug: `spt-123-group-board`
- Base branch: `develop`
- Feature branch: `codex/spt-123-group-board`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-123-group-board`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-123-group-board`
- Jira issue: `SPT-123`
- Jira URL: https://studypot.atlassian.net/browse/SPT-123
- Jira summary: [study-group-board] 그룹별 게시판 기본 API 추가
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/04-erd-data-model.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/07-permissions-state.md
- [x] docs/confluence/09-qa-acceptance.md
- [x] docs/confluence/10-jira-mapping.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] study-group-board
- [x] study-group-core

## Doc Notes
- SPT-123 adds a new `study-group-board` feature ID because the locked MVP did not have board/post/comment APIs or tables.
- The board module will live under `com.studypot.aistudyleader.studygroup.board`, matching the existing `studygroup.rules` module boundary.
- Default group boards are `NOTICE`, `QUESTION`, `RESOURCE`, and `RETROSPECTIVE`; `GET /api/v1/groups/{groupId}/boards` ensures these default board rows exist for the group before returning them. This keeps the public API aligned with the Jira candidate list and avoids adding unrequested board-management endpoints.
- Active group membership is required for board/post/comment read and write. `PENDING_ONBOARDING`, `LEFT`, deleted membership, non-member, and cross-group access are rejected. Missing groups/resources return not found unless the resource exists in another group, in which case cross-group access is forbidden.
- Authors can edit/delete their own posts/comments. Owners can delete posts/comments and change post `pinned` state. Owners who are also authors may edit content. Non-author owners cannot rewrite another member's title/content.
- File upload, reactions/likes, mentions, search, tags, realtime updates, board notifications, and AI board summaries are excluded.
- API/DB/permission/QA changes require CR/ADR and updates to source specs plus Confluence drafts.

## Goal
Add durable group-scoped board/post/comment APIs so active study members can read boards, create/read/update/delete posts, and create/read/update/delete comments while preserving group, author, and owner permission boundaries.

## Approach
Follow TDD with focused controller/service/repository tests first. Add domain records/enums for board, post, comment, membership, and cursors. Add `GroupBoardService`, JDBC repository, configuration beans, and a Flyway V3 migration. Use cursor pagination for post/comment lists, RFC 9457 validation errors for invalid input/cursors, and soft delete for posts/comments. Update OpenAPI, API/DB/auth/QA/coverage specs, CR/ADR, and Confluence drafts.

## Step Plan
1. Add RED controller/service/repository tests for board list initialization, post list/create/read/update/delete, comment list/create/update/delete, invalid input, invalid cursor, non-member/LEFT access, cross-group access, author-only edits, and owner delete/pin behavior.
2. Add board domain records/enums, commands/queries, service exceptions, repository interface, JDBC SQL/repository, persistence/application configuration, and Flyway migration.
3. Add controller endpoints and response/request DTOs with Korean Swagger descriptions.
4. Add CR/ADR and update API, OpenAPI, DB contract/schema, auth/permissions, QA, feature coverage, and Confluence drafts.
5. Run focused tests, migration/static docs checks, OpenAPI parse, `scripts/tests/test_swagger_docs_contracts.sh`, `git diff --check`, and `./gradlew check build --no-daemon`.
6. Commit, create PR with `[feat] 그룹별 게시판 기본 API 추가`, run CodeRabbit, satisfy review gate, auto-merge, cleanup, and confirm Jira done.

## Done Criteria
- `GET /api/v1/groups/{groupId}/boards` returns default group boards for active members.
- Post list/create/read/update/delete APIs persist to MySQL and enforce board/group boundaries.
- Comment list/create/update/delete APIs persist to MySQL and enforce post/group boundaries.
- Authors can edit/delete their own content; owners can delete and pin posts; non-author owners cannot rewrite another member's title/content.
- `PENDING_ONBOARDING`, `LEFT`, deleted membership, non-member, and cross-group access are rejected.
- Invalid title/content/cursor/page size/deleted resources are tested and mapped to Problem Detail responses.
- CR/ADR, API/OpenAPI, DB contract/schema, auth/permission, QA, coverage, and Confluence docs match implementation.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR review gate, CodeRabbit marker, auto-merge, local cleanup, and Jira completion finish successfully.

## Verification Log
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.board.service.GroupBoardServiceTest' --tests 'com.studypot.aistudyleader.studygroup.board.controller.GroupBoardControllerTest' --tests 'com.studypot.aistudyleader.studygroup.board.repository.JdbcGroupBoardRepositoryTest' --no-daemon` failed because production `studygroup.board` classes did not exist yet.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.board.service.GroupBoardServiceTest' --tests 'com.studypot.aistudyleader.studygroup.board.controller.GroupBoardControllerTest' --tests 'com.studypot.aistudyleader.studygroup.board.repository.JdbcGroupBoardRepositoryTest' --no-daemon` passed after adding board domain, service, controller, repository, and migration implementation.
- Docs: `ruby -ryaml -e 'd=YAML.load_file("docs/specs/openapi.yaml"); abort("missing paths") unless d["paths"]; abort("missing schemas") unless d.dig("components","schemas"); puts "paths=#{d["paths"].length} schemas=#{d.dig("components","schemas").length}"'` passed with `paths=36 schemas=59`.
- Docs: `scripts/tests/test_swagger_docs_contracts.sh` passed.
- Whitespace: `git diff --check` passed.
- Full: `./gradlew check build --no-daemon` passed.
