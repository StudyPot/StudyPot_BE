# EXEC_PLAN: [weekly-todo] member_week_progress 조회 API 추가

- Task slug: `spt-79-member-week-progress-read`
- Base branch: `develop`
- Feature branch: `codex/spt-79-member-week-progress-read`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-79-member-week-progress-read`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-79-member-week-progress-read`
- Jira issue: `SPT-79`
- Jira URL: https://studypot.atlassian.net/browse/SPT-79
- Jira summary: [weekly-todo] member_week_progress 조회 API 추가
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/exec-plans/active/20260512-spt-36-member-week-progress.md
- [x] docs/exec-plans/active/20260512-spt-37-task-completion.md

## Related Feature IDs
- [x] `weekly-todo`

## Doc Notes
- The v1 planning/API/permission/QA docs are `LOCKED_FOR_IMPLEMENTATION`; a new `GET /api/v1/weeks/{weekId}/progress/me` endpoint requires Change Request + ADR.
- User decision recorded in this task: use option 2, adding a separate read-only progress endpoint instead of treating the existing `PUT` response as the only 조회 surface.
- Existing locked API only exposes `PUT /api/v1/weeks/{weekId}/progress/me`; `GET /api/v1/groups/{groupId}/weeks/current` returns week fields and does not include member progress.
- The new GET endpoint is read-only: it returns an existing current user's `member_week_progress`; it does not create progress. Missing progress returns not found.
- Existing SPT-36 creation/update behavior remains owned by `PUT /api/v1/weeks/{weekId}/progress/me`.

## Goal
Add the SPT-79 weekly-todo slice: an authenticated ACTIVE group member can call `GET /api/v1/weeks/{weekId}/progress/me` to read their own existing `member_week_progress` for that curriculum week, while preserving the existing PUT create/update behavior.

## Approach
1. Add `CR-20260512-week-progress-read-endpoint` and `ADR-20260512-week-progress-read-endpoint`.
2. Refresh affected locked docs: change control index, API contract, OpenAPI, Confluence API draft, auth/permission matrix, QA acceptance, and feature coverage notes if needed.
3. Add service/repository/controller read path using the existing weekly-todo membership context and `MemberWeekProgressResponse`.
4. Keep GET side-effect free: do not insert rows when progress is missing.
5. Cover happy path, missing progress, non-member or inactive member rejection, and controller authentication/validation behavior.

## Step Plan
1. Write CR/ADR and update locked documentation references.
2. Add failing tests for `GET /weeks/{weekId}/progress/me` at controller/service/repository levels.
3. Implement query object, repository/service method, and controller mapping.
4. Run targeted tests for curriculum controller/service/repository.
5. Run `./gradlew check build --no-daemon`.
6. Commit with `[feat] 주차 진행 조회 API 추가`.
7. Create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address valid feedback once, verify PR readiness, and send manual-merge notification.

## Done Criteria
- Change Request and ADR for the new endpoint are present and linked.
- API/OpenAPI/permission/QA docs include `GET /api/v1/weeks/{weekId}/progress/me`.
- `GET /api/v1/weeks/{weekId}/progress/me` returns `MemberWeekProgressResponse` for the authenticated ACTIVE member's existing progress.
- Missing progress returns not found and does not create a row.
- Non-members, pending members, and left members cannot read progress.
- Existing `PUT /api/v1/weeks/{weekId}/progress/me` behavior is unchanged.
- Controller, service, and repository tests cover the new read path.
- `./gradlew check build --no-daemon` passes.
- PR is created through the harness and passes the review gate.

## Verification Log
- `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon`: PASS.
- `./gradlew test --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --no-daemon`: PASS.
- `ruby -e 'require "yaml"; doc = YAML.load_file("docs/specs/openapi.yaml"); abort("openapi must be 3.1.x") unless doc.fetch("openapi").to_s.start_with?("3.1."); abort("info.title is required") unless doc.dig("info", "title"); abort("paths is required") unless doc["paths"].is_a?(Hash); puts "OpenAPI parsed paths=#{doc.fetch("paths").length} schemas=#{doc.fetch("components").fetch("schemas").length}"'`: PASS (`paths=27`, `schemas=31`).
- `git diff --check`: PASS.
- `./gradlew check build --no-daemon`: PASS.
