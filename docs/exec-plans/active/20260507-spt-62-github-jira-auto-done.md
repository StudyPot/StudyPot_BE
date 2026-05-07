# EXEC_PLAN: [harness] GitHub merge 후 Jira 자동 완료 연동

- Task slug: `spt-62-github-jira-auto-done`
- Base branch: `develop`
- Feature branch: `codex/spt-62-github-jira-auto-done`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-62-github-jira-auto-done`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-62-github-jira-auto-done`
- Jira issue: `SPT-62`
- Jira URL: https://studypot.atlassian.net/browse/SPT-62
- Jira summary: [harness] GitHub merge 후 Jira 자동 완료 연동
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] .github/workflows/pr-quality.yml
- [x] scripts/task/create-pr.sh
- [x] scripts/task/finish-pr.sh
- [x] scripts/task/jira-board.sh
- [x] scripts/tests/test_pr_scripts_static.sh
- [x] scripts/tests/test_jira_board_sync.sh
- [x] scripts/tests/testlib.sh

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- User decision: when a human presses GitHub merge, Jira should move to Done automatically instead of waiting for local cleanup.
- Existing contract already keeps human-controlled GitHub merge and local `finish-pr.sh cleanup-merged` cleanup; this task only automates the Jira transition after merge.
- Jira transition remains source-of-truth guarded through `scripts/task/jira-board.sh`, using the same transition discovery and fake API test harness.
- `cleanup-merged` remains required for develop sync and local branch/worktree cleanup, and is safe when Jira is already Done.
- GitHub Action must not leak Jira credentials; it uses repository secrets `JIRA_EMAIL` and `JIRA_API_TOKEN`.

## Goal
Make harness-created PRs close their linked Jira Task automatically when a human merges the PR into `develop`, while preserving manual GitHub merge, review gates, and local cleanup.

## Approach
- Add a small merge-event script that reads the GitHub pull request event payload, verifies `merged == true`, base `develop`, and `codex/*` head branch, extracts an `SPT-*` Jira key, and calls `jira-board.sh done-task`.
- Add a GitHub Actions workflow for `pull_request.closed` on `develop` that runs the script with Jira secrets.
- Add a machine-readable `Jira-Key: SPT-123` line to `create-pr.sh` PR bodies so the merge event can reliably map PRs back to Jira.
- Update docs and harness static tests to describe automatic Jira Done plus local cleanup.
- Use fake Jira API tests for happy path, skip cases, and missing-key validation.

## Step Plan
1. Add a failing test for merged `codex/*` PR events transitioning the linked Jira key to Done.
2. Add failing static assertions for the new workflow/script and PR body Jira key line.
3. Implement `scripts/task/mark-jira-done-from-pr.sh` with event parsing and Jira transition.
4. Add `.github/workflows/jira-auto-done.yml`.
5. Update `create-pr.sh`, docs, and test runner references.
6. Run focused shell tests, then `./gradlew check build --no-daemon`.
7. Commit, create PR, pass review gates, and run finish notification. Jira auto-done for this new feature will apply after this workflow is merged and secrets are available.

## Done Criteria
- Merged `develop` PRs from `codex/*` with a linked `SPT-*` key transition Jira to `완료`.
- Non-merged PR close events and non-`develop` base branches do not transition Jira.
- Merged `codex/*` PRs without a Jira key fail loudly.
- `create-pr.sh` includes a stable `Jira-Key: SPT-123` line in PR bodies.
- Documentation states that Jira Done is automatic after human GitHub merge, while `cleanup-merged` remains for local cleanup and is idempotent for Jira.
- Harness tests and standard Gradle verification pass.

## Verification Evidence
- RED: `bash scripts/tests/test_jira_auto_done_on_merge.sh` failed because `scripts/task/mark-jira-done-from-pr.sh` did not exist.
- GREEN focused: `bash scripts/tests/test_jira_auto_done_on_merge.sh` passed.
- Static contract: `bash scripts/tests/test_pr_scripts_static.sh` passed.
- Full harness: `bash scripts/tests/run.sh` passed.
- Standard verification: `./gradlew check build --no-daemon` passed.
- Workflow parse: `ruby -e 'require "yaml"; YAML.load_file(".github/workflows/jira-auto-done.yml"); puts "jira-auto-done workflow parsed"'` passed.
