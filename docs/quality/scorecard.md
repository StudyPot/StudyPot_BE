# Quality Scorecard

## Gates
- Planning: `EXEC_PLAN` is complete before implementation.
- Worktree: feature work happens only in a generated `codex/<slug>` worktree.
- Tests: behavior changes include focused tests.
- Validation: `./gradlew check build --no-daemon` passes before commit.
- Docs: repo docs are updated before Obsidian mirrors.
- PR Review: review/comment activity exists, checks pass, actionable feedback is addressed, review threads are resolved, and the required `review-gate-pass` check has verified latest-head CTO/QA/Product/Final CTO evidence before merge.
- Finish: `finish-pr.sh` owns ready verification and Korean Mattermost manual merge notification; `finish-pr.sh cleanup-merged` owns post-human-merge cleanup and never deletes dirty, locked, ahead, or diverged worktrees.
- Handoff: Obsidian current state and error ledger are updated after each feature.

## Current Baseline
- Stack: `Java 21 + Gradle + Spring Boot`
- Verification: `./gradlew check build --no-daemon`
- Generated at: `2026-04-29 09:08:43`
