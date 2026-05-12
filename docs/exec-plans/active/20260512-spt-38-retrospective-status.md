# EXEC_PLAN: [retrospective] retrospective 생성/상태 처리 구현

- Task slug: `spt-38-retrospective-status`
- Base branch: `develop`
- Feature branch: `codex/spt-38-retrospective-status`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-38-retrospective-status`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-38-retrospective-status`
- Jira issue: `SPT-38`
- Jira URL: https://studypot.atlassian.net/browse/SPT-38
- Jira summary: [retrospective] retrospective 생성/상태 처리 구현
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/confluence/06-ai-team-leader.md

## Related Feature IDs
- [x] retrospective-feedback

## Doc Notes
- Jira SPT-38 acceptance: `retrospective` and `member_week_progress` scope, trigger types `WEEK_ENDED`, `INCOMPLETE_MODAL`, `USER_CHAT`, `MANUAL`, JSON `input_summary`, processing status, and `completed_at` persistence.
- SPT-81 boundary comments and CR/ADR require this task to implement retrospective creation/read/status persistence without LLM provider calls, FastAPI service split, vector/GraphRAG, MCP, or agentic workflow.
- Locked OpenAPI exposes `POST /api/v1/weeks/{weekId}/retrospectives/me` with no request body. The public endpoint will therefore create/request a manual retrospective (`MANUAL`) and keep the other trigger values as internal service/domain enum support without changing API shape.
- `RetrospectiveResponse` contains `id`, `status`, `aiFeedback`, and `nextWeekAdjustment`. SPT-38 returns persisted feedback/adjustment JSON when present, but does not generate or map AI feedback/next-week adjustment; that remains for SPT-41/44/46.
- `input_summary` is the DB-first context boundary. For this slice it will summarize the authenticated member's progress, task completion counts/details, incomplete reasons, and conversation summary placeholder without reading or exposing another member's private notes.

## Goal
Implement SPT-38 retrospective foundation: an active authenticated group member can request and read their own week retrospective through the locked REST endpoints, the backend creates or reuses a week/member/progress-scoped `retrospective` row, stores deterministic JSON `input_summary`, records status timestamps, and preserves the existing API/DB contracts.

## Approach
- Add a `retrospective` bounded context with domain records/enums, service commands/queries, repository port/JDBC implementation, and REST controller.
- Reuse existing membership/read context patterns from curriculum weekly todo: resolve the authenticated member through `curriculum_week -> curriculum -> study_group -> group_member`, require active membership, reject missing week/non-member/pending/left members through existing problem-detail mappings.
- Create a retrospective only when `member_week_progress` exists for the current member and week; otherwise return not found because SPT-38 acceptance says the retrospective is created from weekly progress.
- Make `requestMyRetrospective` idempotent for the same progress/week/member: return an existing retrospective if found, otherwise insert a new `PENDING` retrospective with `requested_at/created_at/updated_at = now` and `completed_at = null`.
- Build `input_summary` from the progress row and the member's task completion rows for the week. Include completed/incomplete/skipped/todo counts, completion notes, incomplete reasons, and a `conversationSummary` placeholder so later SPT-39/40 conversation persistence can attach real summaries without changing this table contract.
- Keep status enum support for `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` and trigger enum support for all Jira-accepted values. No LLM usage, AI provider invocation, notification delivery, or next-week adjustment mutation in this task.

## Step Plan
1. [x] Initialize `codex/spt-38-retrospective-status` worktree from Jira SPT-38.
2. [x] Read required docs, SPT-81 CR/ADR, and Jira description/comments.
3. [x] Add RED unit/controller/repository tests for retrospective trigger/status/domain, service happy path/idempotency/permission failures, REST 202/200 responses, and JDBC SQL/argument behavior.
4. [x] Implement retrospective domain/service/repository/controller and application wiring.
5. [x] Extend global exception mapping and wiring tests for the new feature.
6. [x] Run targeted retrospective tests, then `./gradlew check build --no-daemon`.
7. [ ] Commit with `[feat] 회고 생성 상태 처리 구현`, create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address review feedback once if needed, and finish PR readiness notification.

## Verification
- [x] RED: `./gradlew test --tests '*Retrospective*' --no-daemon` failed before implementation because the retrospective classes were missing.
- [x] Focused: `./gradlew test --tests '*Retrospective*' --no-daemon` passed after implementation.
- [x] Full: `./gradlew check build --no-daemon` passed on 2026-05-12.

## Done Criteria
- `POST /api/v1/weeks/{weekId}/retrospectives/me` returns HTTP 202 with the created or existing retrospective for the authenticated active member.
- `GET /api/v1/weeks/{weekId}/retrospectives/me` returns HTTP 200 for the authenticated member's existing retrospective and rejects missing/cross-member/non-active access through problem details.
- New retrospective rows persist `progress_id`, `curriculum_week_id`, `member_id`, `trigger_type`, JSON `input_summary`, `status`, `requested_at`, and nullable `completed_at`.
- Trigger/status enum coverage includes all SPT-38 accepted trigger types and locked status values.
- Tests cover happy path, idempotent existing retrospective, missing progress/retrospective, inactive membership, response shape, JSON serialization, and JDBC argument/SQL contract.
- No locked spec shape change, no LLM/provider/vector/FastAPI/notification implementation.
- `./gradlew check build --no-daemon` passes and task state records successful verification.
