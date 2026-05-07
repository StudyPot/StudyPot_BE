# EXEC_PLAN: [chore] 커밋 컨벤션 하네스 정리

- Task slug: `commit-convention-harness`
- Base branch: `develop`
- Feature branch: `codex/commit-convention-harness`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/commit-convention-harness`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/commit-convention-harness`
- Jira issue: `SPT-60`
- Jira URL: https://studypot.atlassian.net/browse/SPT-60
- Jira summary: [chore] 커밋 컨벤션 하네스 정리
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [ ] docs/operations/jira-board-sync.md
- [ ] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- User decision: Merge commits are acceptable and must not be changed in this task.
- The convention applies to human-authored commit subjects and PR titles: `[type] 한글 내용`.
- Allowed `type` values should be lowercase English tokens such as `feat`, `fix`, and `chore`.
- Existing public `develop` history is not rewritten in this task because that would require a destructive force-push workflow.

## Goal
Update the harness so future direct commit subjects and PR titles follow `[type] 한글 내용`, while leaving the merge strategy unchanged.

## Approach
1. Add a shared commit subject validator for task scripts.
2. Use the validator from PR creation and PR finish checks.
3. Update `commit-msg` to allow multiple lowercase English types and require Korean description text.
4. Update docs and tests so the convention is executable and visible.

## Step Plan
- [x] Create Jira Task `SPT-60` and start `codex/commit-convention-harness` worktree.
- [x] Read required workflow, architecture, docs index, and related harness docs.
- [x] Record user decision that merge commits are acceptable and merge strategy must remain unchanged.
- [x] Update commit/PR title convention validation.
- [x] Update docs and tests.
- [x] Run targeted tests and `./gradlew check build --no-daemon`.
- [ ] Commit with the new `[chore] 한글 내용` convention and create PR.

## Verification
- `bash scripts/tests/test_commit_convention.sh` - passed.
- `bash scripts/tests/test_pr_scripts_static.sh` - passed.
- `bash -n scripts/task/common.sh scripts/hooks/commit-msg.sh scripts/task/create-pr.sh scripts/task/finish-pr.sh scripts/tests/test_commit_convention.sh` - passed.
- `git diff --check` - passed.
- `./gradlew check build --no-daemon` - passed.
- `bash scripts/tests/run.sh` - passed after setting the new test script executable bit.
- CI `harness-tests` initially failed because Bash locale handling did not match Korean ranges; replaced Korean detection with Python Unicode codepoint validation.
- `LC_ALL=C bash scripts/tests/test_commit_convention.sh` behavior is covered through the test helper and passed locally.
- `bash scripts/tests/run.sh` - passed after locale-safe Korean validation.
- `./gradlew check build --no-daemon` - passed after locale-safe Korean validation.

## Done Criteria
- `[feat] 한글 내용`, `[chore] 한글 내용`, and `[fix] 한글 내용` are accepted by the commit message hook.
- `[FEAT] : 한글 내용`, `[feat] English only`, and `feat: 한글 내용` are rejected.
- `create-pr.sh` and `finish-pr.sh` validate PR titles with the same convention.
- `finish-pr.sh` still uses the existing merge strategy.
- Related harness tests pass.
