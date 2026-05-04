# EXEC_PLAN: [docs] 온보딩 MySQL8 기준 문서/Jira 정리

- Task slug: `docs-onboarding-mysql8-sync`
- Base branch: `develop`
- Feature branch: `codex/docs-onboarding-mysql8-sync`
- Worktree: `/tmp/studypot-docs-onboarding-mysql8-sync`
- Port: `n/a`
- Log dir: `n/a`
- Jira issue: `SPT-50`
- Jira URL: https://studypot.atlassian.net/browse/SPT-50
- Jira summary: [qa] ERD v0.8 MySQL8 스키마/수용 기준 검증 추가
- Status: `completed`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/pr-review-gate.md
- [x] /Users/hyunwoo/Downloads/요구사항_정의서_v0.3.docx
- [x] /Users/hyunwoo/Desktop/Project/CodingTestTeam/outputs/erd-mysql8/ERD_설계문서_v0.8_MySQL8.docx
- [x] /Users/hyunwoo/Desktop/Project/CodingTestTeam/outputs/erd-mysql8/ERD_MySQL8_v0.8.mmd

## Related Feature IDs
- [x] identity-core
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core
- [x] weekly-todo
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] notification
- [x] n/a-harness

## Doc Notes
- Current repo v1 docs are locked around a meeting/session centered PostgreSQL baseline.
- User-approved source of truth is Requirements v0.3, ERD v0.8, MySQL8, and onboarding MVP.
- Confluence access returned 403 because the Atlassian app is not installed for Confluence; fallback is local Markdown page drafts.
- Shell Jira REST credentials are not present, so actual Jira writes use the Atlassian MCP connector.
- Local worktree creation from `/Users/hyunwoo/Documents/New project 3` failed with Git mmap timeouts; this task uses a clean GitHub clone at `/tmp/studypot-docs-onboarding-mysql8-sync`.

## Goal
Bring repo specs, Jira, and Confluence-ready documentation to one implementation source of truth: group creation, invite, member onboarding, host start, AI curriculum, weekly todo, retrospective feedback, and AI team leader chat on MySQL8.

## Approach
1. Record Change Request and ADR for replacing the locked meeting-centered v1 baseline.
2. Rewrite locked spec docs to the onboarding MVP contract.
3. Keep API, DB, AI, notification, permission, QA, and feature coverage docs aligned with ERD v0.8 entities and statuses.
4. Generate Confluence-ready Markdown pages if Confluence access remains blocked.
5. Update Jira issue descriptions and labels to point at the same source of truth.

## Step Plan
1. Create Change Request and ADR.
2. Update `ARCHITECTURE.md`, `docs/index.md`, and `docs/specs/*`.
3. Add Confluence Markdown draft pages under `docs/confluence/`.
4. Verify OpenAPI YAML parses and the ERD entity/status terms exist in docs.
5. Update Jira documentation issues and validate stale labels/search results.
6. Run `./gradlew check build --no-daemon`.

## Done Criteria
- Repo docs mention MySQL8 and onboarding MVP consistently.
- Meeting/session-centric P0 language is removed or explicitly deferred.
- OpenAPI and DB schema include onboarding, curriculum, weekly todo, retrospective, AI conversation, notification, and LLM usage resources.
- Jira has no `erd-v06`, `erd-v07`, or `meeting` labels.
- Empty-label Jira documentation issues are populated.
- Confluence pages are created, or Markdown drafts exist with the intended page tree.
- Standard Gradle verification has been attempted and results are recorded.

## Verification Results
- DOCX render: requirements v0.3 rendered to 10 pages at `/tmp/requirements_v03_render_check`.
- DOCX render: ERD v0.8 rendered to 25 pages at `/tmp/erd_v08_render_check`.
- OpenAPI parse: `ruby -ryaml` parsed 19 paths successfully.
- DB schema coverage: all ERD v0.8 onboarding MVP tables found in `docs/specs/db-schema-v1.sql`; notification scope later superseded by `SPT-55`.
- Stale repo text check: no `P0.*meeting`, `meeting.*P0`, `study_sessions`, or stale PostgreSQL baseline matches in active specs.
- Jira stale labels: JQL for `erd-v06`, `erd-v07`, `meeting` returned 0 issues.
- Jira empty labels: JQL for empty labels returned 0 issues.
- Implementation task labels: `SPT-19` to `SPT-50` all have `requirements-v03`, `erd-v08`, and `onboarding`.
- Gradle: `./gradlew check build --no-daemon` succeeded.
