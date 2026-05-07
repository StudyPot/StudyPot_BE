# EXEC_PLAN: [harness] PR ready 수동 merge 알림 흐름 구현

- Task slug: `manual-merge-mm-notify`
- Base branch: `develop`
- Feature branch: `codex/manual-merge-mm-notify`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/manual-merge-mm-notify`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/manual-merge-mm-notify`
- Jira issue: `SPT-61`
- Jira URL: https://studypot.atlassian.net/browse/SPT-61
- Jira summary: [harness] PR ready 수동 merge 알림 흐름 구현
- Status: `implemented-local`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/error-ledger.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- User decision: when a PR reaches merge-ready state, do not merge immediately. Send a Mattermost incoming webhook notification so the user can review and click GitHub's merge button directly.
- Mattermost secret handling: use local `STUDYPOT_MM_WEBHOOK_URL` from Keychain-backed shell environment. Do not store webhook URLs in repo docs, task state, PR bodies, or logs.
- Mention targets: use `STUDYPOT_MM_MENTIONS`, currently configured locally as `@hw62459930 @yjhn0410`.
- Existing `create-pr.sh` already defaults `STRICT_AUTO_FINISH_PR` to off. The risky part is `finish-pr.sh`, which currently runs `gh pr merge`, deletes the remote branch, syncs `develop`, removes the worktree, deletes the local branch, and marks Jira Done.
- The new default finish flow should stop after latest-head ready checks and Mattermost notification. Post-human-merge cleanup should be a separate explicit command path.
- Actual failure noted during setup: the first Keychain Jira token storage path truncated long Atlassian tokens to 128 characters when using the `security` password prompt. Fixed locally by storing through shell `read` and verified Jira REST authentication. Do not record token values.
- Actual PR dry-run finding: GitHub reports `mergeStateStatus=BLOCKED` before required human review/approval even when checks pass. Manual merge notification mode must allow that state so the user gets the review/merge prompt.
- User follow-up decision: role gates must not be bypassable through the GitHub UI. The required `review-gate-pass` check should validate latest-head CTO/QA/Product/Final CTO evidence markers so GitHub's merge button stays blocked until those markers exist.
- User follow-up decision: 모든 Mattermost 알림, PR body, PR review comment, role gate evidence의 사람이 읽는 본문은 한글로 작성한다. Script가 찾는 marker token(`CTO Architecture Gate: PASS`, `Head: <sha>` 등)만 literal을 유지한다.

## Goal
Change the PR finish harness so merge-ready PRs notify the user through Mattermost and wait for manual GitHub merge instead of automatically merging and cleaning up. Also enforce the company role gates through the required GitHub Actions `review-gate-pass` check, not only through local finish scripts.

## Approach
Use TDD against the shell harness contracts. First update static/script tests to require manual merge notification behavior and to reject automatic `gh pr merge` in the default finish path. Then update `finish-pr.sh` to keep all readiness checks, send a Mattermost webhook payload with PR URL/head/verification context, and exit without merging. Add an explicit post-merge cleanup path that only runs after GitHub reports the PR is merged, then performs the existing safe worktree cleanup and Jira Done transition. For hard enforcement, add a shared role-gate validator and run it inside the required GitHub Actions `review-gate-pass` job before the Actions PASS marker is posted. For the communication-language contract, make the human-facing MM/PR/review/evidence text Korean while preserving machine-readable marker tokens.

## Step Plan
- [x] Add failing tests in `scripts/tests/test_pr_scripts_static.sh` for Mattermost env names, notification helper presence, default no-auto-merge behavior, and post-merge cleanup command coverage.
- [x] Run the focused static test and confirm it fails for missing notification/manual merge behavior.
- [x] Refactor `scripts/task/finish-pr.sh` into a default `notify-ready` mode and an explicit `cleanup-merged` mode while preserving readiness gates and safe cleanup checks.
- [x] Ensure the webhook payload uses `STUDYPOT_MM_WEBHOOK_URL` and `STUDYPOT_MM_MENTIONS`, never prints the webhook URL, and includes PR number/URL/head SHA/manual merge instruction.
- [x] Update PR review/Jira/harness docs and AGENTS wording so the executable contract matches manual merge.
- [x] Run focused shell syntax/static tests.
- [x] Run full harness tests with `bash scripts/tests/run.sh`.
- [x] Run standard verification with `./gradlew check build --no-daemon`.
- [x] Update this `EXEC_PLAN` with verification results and any real blocker.
- [x] Add a failing role-gate validation test that proves missing/stale/incomplete latest-head role evidence fails.
- [x] Add `scripts/task/verify-role-review-gates.sh` and call it from both `finish-pr.sh` and `.github/workflows/pr-quality.yml`.
- [x] Document that `review-gate-pass` hard-blocks the GitHub merge button until CTO/QA/Product/Final CTO evidence markers exist.
- [x] Add failing tests requiring Korean MM notification text, PR checklist text, and role gate evidence labels.
- [x] Update `notify-pr-ready.sh`, `create-pr.sh`, `post-role-review-pass.sh`, and `verify-role-review-gates.sh` so human-facing text is Korean by default.
- [x] Rerun focused role-gate/static tests after hard enforcement changes.
- [x] Rerun full harness tests and Gradle verification after hard enforcement changes.
- [ ] Push the amended PR, post latest-head role gate markers, rerun `review-gate-pass`, and send the manual merge notification only after the required check is green.

## Verification
- `bash scripts/tests/test_pr_scripts_static.sh && bash scripts/tests/test_manual_merge_notification.sh` - passed.
- `bash scripts/tests/run.sh` - passed.
- `./gradlew check build --no-daemon` - passed at 2026-05-06T06:51:53Z.
- PR #26 CI checks - passed.
- `bash scripts/tests/test_pr_scripts_static.sh && bash scripts/tests/test_role_review_gate_validation.sh && bash scripts/tests/test_role_review_evidence.sh` - passed.
- `bash scripts/tests/run.sh` - passed after hard enforcement changes.
- `./gradlew check build --no-daemon` - passed after hard enforcement changes.
- `bash scripts/tests/test_manual_merge_notification.sh && bash scripts/tests/test_role_review_evidence.sh` - passed after Korean communication contract changes.
- `bash scripts/tests/test_role_review_gate_validation.sh && bash scripts/tests/test_pr_scripts_static.sh` - passed after Korean communication contract changes.
- `bash scripts/tests/run.sh` - passed after Korean communication contract changes.
- `./gradlew check build --no-daemon` - passed after Korean communication contract changes.
- `bash scripts/tests/test_role_review_evidence.sh && bash scripts/tests/test_pr_scripts_static.sh` - passed after reviewdog source hint fix.
- `bash scripts/tests/run.sh` - passed after reviewdog source hint fix.
- `./gradlew check build --no-daemon` - passed after reviewdog source hint fix.

## Done Criteria
- `finish-pr.sh <PR_NUMBER>` verifies latest-head merge readiness and sends a Mattermost notification instead of invoking `gh pr merge`.
- `finish-pr.sh cleanup-merged <PR_NUMBER>` or an equivalent explicit mode only cleans local/remote state and marks Jira Done after the PR is already merged by the user.
- Mattermost webhook URL is read only from `STUDYPOT_MM_WEBHOOK_URL`; missing webhook configuration produces a clear secret-safe error.
- Notification body mentions `STUDYPOT_MM_MENTIONS` and tells the user to review and press the GitHub merge button manually.
- Static harness tests cover the new default and cleanup contracts.
- Required GitHub Actions `review-gate-pass` fails until latest-head CTO/QA/Product/Final CTO evidence markers are present, so the GitHub merge button cannot bypass the role gates.
- `finish-pr.sh` and GitHub Actions share the same role-gate validation script.
- Mattermost PR-ready notifications, generated PR bodies/checklists, and role review evidence comments use Korean human-facing text.
- `bash scripts/tests/run.sh` passes.
- `./gradlew check build --no-daemon` passes.
