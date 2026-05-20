# EXEC_PLAN: [infra] Dependabot 설정 제거

- Task slug: `remove-dependabot`
- Base branch: `develop`
- Feature branch: `codex/remove-dependabot`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/remove-dependabot`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/remove-dependabot`
- Jira issue: `SPT-100`
- Jira URL: https://studypot.atlassian.net/browse/SPT-100
- Jira summary: [infra] Dependabot 설정 제거
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/github-actions-review-gate.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- 2026-05-20: Open Dependabot PRs targeted `main` and had no reported checks, so they did not exercise the `develop` PR quality/review gate flow.
- 2026-05-20: Dependabot labels in the config did not exist in GitHub, producing Dependabot warning comments on generated PRs.
- 2026-05-20: This is a harness/infrastructure cleanup only; product, API, DB, AI, notification, and permission specs remain untouched.

## Goal
Remove Dependabot automation from this repository so GitHub no longer opens automatic dependency update PRs that bypass the StudyPot `develop` review gate path.

## Approach
Delete `.github/dependabot.yml` and update the GitHub Actions review gate document so the documented workflow no longer claims Dependabot is configured.

## Step Plan
- [x] Read required workflow and review-gate documents.
- [x] Create and start Jira task `SPT-100`.
- [x] Remove `.github/dependabot.yml`.
- [x] Remove the stale Dependabot note from `docs/operations/github-actions-review-gate.md`.
- [x] Run `./gradlew check build --no-daemon`.
- [x] Commit with a `[type] 한글 내용` subject.
- [ ] Create PR to `develop` through `scripts/task/create-pr.sh`.
- [ ] Run CodeRabbit review and finish the PR through the review gate.

## Done Criteria
- `.github/dependabot.yml` no longer exists in the feature branch.
- No repo documentation still claims Dependabot is configured.
- `./gradlew check build --no-daemon` passes and task state records the verification.
- PR is created against `develop` and passes the StudyPot review gate.
- PR is merged and local cleanup completes.
