# EXEC_PLAN: [study-group-member] 스터디별 내 마이페이지 조회/수정 API 추가

- Task slug: `spt-125-group-member-profile`
- Base branch: `develop`
- Feature branch: `codex/spt-125-group-member-profile`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-125-group-member-profile`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-125-group-member-profile`
- Jira issue: `SPT-125`
- Jira URL: https://studypot.atlassian.net/browse/SPT-125
- Jira summary: [study-group-member] 스터디별 내 마이페이지 조회/수정 API 추가
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] study-group-core
- [x] group-onboarding
- [x] weekly-todo
- [x] retrospective-feedback

## Doc Notes
- SPT-125 adds two existing-user scoped endpoints under a group:
  - `GET /api/v1/groups/{groupId}/members/me/profile`
  - `PATCH /api/v1/groups/{groupId}/members/me/profile`
- The current DB already has `group_member.display_name varchar(80)`, so the PATCH scope is limited to `displayName`. New profile fields such as introduction, goal, image, or memo are deferred because they require DB/API/product scope changes beyond the task's necessary path.
- Profile lookup is limited to current `PENDING_ONBOARDING` or `ACTIVE` memberships. `LEFT`, deleted, other-group, and non-member access is forbidden when the group exists.
- The profile response will expose current group member identity/permission/status, onboarding submission summary, current week summary, task completion counts for the current week, and latest completed retrospective availability.
- This task changes API paths and response/request schemas, so it requires a focused Change Request + ADR and updates to API/OpenAPI/Auth/QA/coverage/confluence docs.

## Goal
Add a group-scoped "my profile" API that lets the authenticated user read their own study-specific member profile and update their group display name without mixing it with the global `/users/me` account profile.

## Approach
Use TDD around `StudyGroupController` and `StudyGroupService`. Add a repository-backed profile projection to the study-group module so one API call can summarize the current member row plus existing onboarding/current-week/progress/retrospective data. Keep the write path narrow: validate and update `group_member.display_name` only, then return the refreshed profile.

## Step Plan
1. [x] Add RED controller/service tests for profile read, profile update, missing auth, invalid display name, missing group, and non-member/LEFT access.
2. [x] Add domain projection records, service commands/queries, repository methods, and JDBC SQL for profile read/update.
3. [x] Add the two controller endpoints and Swagger schemas.
4. [x] Add CR/ADR and update API, OpenAPI, auth/permissions, QA, feature coverage, and Confluence drafts.
5. [x] Run focused tests, OpenAPI parse/static docs checks, then `./gradlew check build --no-daemon`.
6. [ ] Commit, create PR, run CodeRabbit, satisfy review gate, auto-merge, cleanup, and confirm Jira done.

## Verification Log
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest' --tests 'com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest' --no-daemon` failed at compile because `StudyGroupMemberProfile` and the profile service contract did not exist yet.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest' --tests 'com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest' --tests 'com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest' --no-daemon` passed.
- Docs: `scripts/tests/test_swagger_docs_contracts.sh` passed after adding Korean `@Schema` descriptions for the new profile response records.
- Docs: `ruby -ryaml -e 'doc = YAML.load_file("docs/specs/openapi.yaml"); puts "paths=#{doc.fetch("paths").size} schemas=#{doc.fetch("components").fetch("schemas").size}"'` returned `paths=31 schemas=47`.
- Static: `git diff --check` passed.
- Full: `./gradlew check build --no-daemon` passed.

## Done Criteria
- `GET /api/v1/groups/{groupId}/members/me/profile` returns the authenticated current member's group profile and summaries.
- `PATCH /api/v1/groups/{groupId}/members/me/profile` validates and updates only `displayName`, then returns the refreshed profile.
- Missing groups return 404; existing groups with no current membership, LEFT membership, or other-user access return 403.
- API/OpenAPI/Auth/QA/coverage/Confluence docs and CR/ADR match the implementation.
- Focused tests and `./gradlew check build --no-daemon` pass.
- PR review gate, CodeRabbit marker, auto-merge, local cleanup, and Jira completion finish successfully.
