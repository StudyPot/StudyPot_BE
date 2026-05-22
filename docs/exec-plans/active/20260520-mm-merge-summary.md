# EXEC_PLAN: [harness] Mattermost merge 알림 내용 요약 개선

- Task slug: `mm-merge-summary`
- Base branch: `develop`
- Feature branch: `codex/mm-merge-summary`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/mm-merge-summary`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/mm-merge-summary`
- Jira issue: `SPT-101`
- Jira URL: https://studypot.atlassian.net/browse/SPT-101
- Jira summary: [harness] Mattermost merge 알림 내용 요약 개선
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- 2026-05-20: User decision: Mattermost merge-complete notifications should prioritize what changed or what feature was added, not head SHA, Jira 처리, or `finish-pr.sh` implementation details.
- 2026-05-20: Scope is harness communication only. Auto merge, review gate, Jira transition, and cleanup behavior stay unchanged.
- 2026-05-20: Apply TDD: update `test_auto_merge_notification.sh` first so the current metadata-heavy message fails, then update notification generation.
- 2026-05-20: Keep Mattermost-facing summary Korean when possible; if an extracted PR body/EXEC_PLAN summary has no Korean text, fall back to the Korean PR title description.

## Goal
Make StudyPot Mattermost merge-complete notifications useful at a glance by showing the PR title and a concise change summary before the PR link, while removing noisy operational metadata from the message body.

## Approach
Keep `notify-pr-ready.sh` as the single Mattermost payload sender, but allow callers to provide a PR title and summary through environment variables. Update `finish-pr.sh` to derive a concise summary from the PR body/EXEC_PLAN and pass it to the sender. Update tests and docs to lock the new notification contract.

## Step Plan
- [x] Read required harness and review-gate documents.
- [x] Create and start Jira task `SPT-101`.
- [x] RED: change Mattermost payload test to require change summary and reject noisy metadata.
- [x] GREEN: update `notify-pr-ready.sh` message format.
- [x] Wire `finish-pr.sh` to pass PR title and derived summary.
- [x] Update PR review gate docs.
- [x] Run `bash scripts/tests/run.sh`.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Commit, create PR, run CodeRabbit, pass review gate, merge, and cleanup.

## Done Criteria
- Mattermost merge notification includes PR title/change content and optional concise summary.
- Mattermost merge notification no longer foregrounds head SHA, Jira handling, or `finish-pr.sh` internal processing details.
- Direct sender test and static harness tests cover the new contract.
- `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon` pass.
- PR is merged through the existing StudyPot review gate and cleanup completes.
