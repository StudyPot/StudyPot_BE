# EXEC_PLAN: [docs] 남은 문서 source of truth 정리

- Task slug: `docs-source-of-truth-cleanup`
- Base branch: `develop`
- Feature branch: `codex/docs-source-of-truth-cleanup`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/docs-source-of-truth-cleanup`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/docs-source-of-truth-cleanup`
- Jira issue: `SPT-56`
- Jira URL: https://studypot.atlassian.net/browse/SPT-56
- Jira summary: [docs] 남은 문서 source of truth 정리
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/specs/change-control-v1.md
- [x] docs/architecture/backend-map.md
- [x] docs/confluence/README.md
- [x] docs/confluence/00-doc-hub.md
- [x] docs/confluence/10-jira-mapping.md
- [x] scripts/hooks/pre-commit.sh
- [x] scripts/hooks/pre-push.sh
- [x] scripts/tests/run.sh
- [x] scripts/tests/test_hook_enforcement.sh
- [x] scripts/tests/test_quality_gate_contracts.sh
- [x] scripts/tests/test_pr_scripts_static.sh
- [x] scripts/tests/test_docs_source_of_truth.sh

## Related Feature IDs
- [x] documentation-source-of-truth
- [x] n/a-harness

## Doc Notes
- User decision: SPT-4, SPT-6, and SPT-7 have been completed; clean up the remaining documents before implementation work continues.
- SPT-56 was created as the Jira Task for this documentation cleanup because the remaining documentation issues are mostly Story/Epic records and the repo harness requires an implementation Task issue.
- This work is limited to documentation metadata, links, Jira mapping, and validation guardrails. It must not change locked product/API/DB/AI/notification/permission/QA semantics.
- `docs/specs/change-control-v1.md` allows link/path corrections and non-semantic clarification without a new ADR.
- While creating the PR, installed hook symlinks failed to resolve `scripts/task/common.sh`; the hook scripts now resolve their real path before sourcing shared helpers, and `test_hook_enforcement.sh` covers the installed-symlink path.

## Goal
Make the remaining documentation set consistent after SPT-4 requirements, SPT-6 ERD, and SPT-7 API publication, so developers can start follow-up implementation tasks from a current source-of-truth map.

## Approach
Update documentation hubs and mapping pages to reflect completed source tasks, current workspace paths, published Confluence pages, and SPT-56 cleanup scope. Add a lightweight docs-source test that validates local markdown links, prevents stale workspace paths in current docs, and checks the Jira mapping/documentation task references. Keep the required git hook path healthy so the documented harness can actually create and push the PR.

## Step Plan
1. Update top-level docs hub and architecture metadata.
2. Update Confluence draft hub and Jira mapping pages.
3. Add docs source-of-truth validation to the local harness.
4. Fix installed hook symlink path resolution discovered during PR creation.
5. Run targeted docs/hook tests, full harness tests, `git diff --check`, and `./gradlew check build --no-daemon`.

## Done Criteria
- Documentation hubs identify Requirements v0.3, ERD v0.8 MySQL8, OpenAPI/API v1, no-Discord, and IN_APP notification as the current baseline.
- SPT-4, SPT-6, SPT-7 are marked as published/completed source tasks in the mapping docs.
- Remaining documentation cleanup is traceable to SPT-56.
- Current source docs no longer point at the old `/Users/hyunwoo/Documents/New project 3` repo path.
- Local markdown links in `docs/` resolve.
- Installed hook symlinks can run `pre-commit` and `pre-push` without losing the shared helper path.
- `bash scripts/tests/run.sh`, `git diff --check`, and `./gradlew check build --no-daemon` pass.

## Verification
- [x] `bash scripts/tests/test_docs_source_of_truth.sh`
- [x] `bash scripts/tests/test_hook_enforcement.sh`
- [x] `bash scripts/tests/test_pr_scripts_static.sh`
- [x] `git diff --check`
- [x] `bash scripts/tests/run.sh`
- [x] `./gradlew check build --no-daemon`
