# EXEC_PLAN: [chore] package-info.java 제거

- Task slug: `remove-package-info`
- Base branch: `develop`
- Feature branch: `codex/remove-package-info`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/remove-package-info`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/remove-package-info`
- Jira issue: `SPT-59`
- Jira URL: https://studypot.atlassian.net/browse/SPT-59
- Jira summary: [chore] package-info.java 제거
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [ ] n/a-harness
- [x] n/a-harness

## Doc Notes
- `package-info.java` files currently contain only package declarations; no package Javadoc or package-level annotations are present.
- Removing them does not change runtime behavior, public API contracts, DB schema, AI behavior, notification behavior, or permission rules.
- `docs/architecture/backend-map.md` still defines the intended package boundaries; empty package placeholder files are not needed once the user has decided they are visual clutter.

## Goal
Remove all empty `package-info.java` placeholder files under `src/main/java` so the source tree is less noisy before real implementation starts.

## Approach
1. Confirm each `package-info.java` is package-declaration-only.
2. Delete all confirmed placeholder files.
3. Verify no `package-info.java` remains.
4. Run the standard Gradle verification and record the result.

## Step Plan
- [x] Create Jira Task `SPT-59` and start `codex/remove-package-info` worktree.
- [x] Read required workflow, architecture, docs index, and task-related docs.
- [x] Inspect all `package-info.java` files and confirm they contain no annotations/Javadoc.
- [x] Delete all package declaration-only `package-info.java` files.
- [x] Run `rg --files | rg 'package-info\.java$'` and confirm no output.
- [x] Add a structure regression test that prevents empty `package-info.java` placeholders from returning.
- [x] Run `./gradlew check build --no-daemon`.
- [ ] Commit with the required subject format and create PR through the task script.

## Verification
- `rg --files | rg 'package-info\.java$' || true` - passed, no output.
- `git diff --check` - passed.
- `./gradlew check build --no-daemon` - passed after deleting placeholders.
- `./gradlew check build --no-daemon` - passed after adding `PackageInfoPlaceholderTest`.

## Done Criteria
- No `src/main/java/**/package-info.java` files remain.
- No production classes, API specs, DB specs, or behavior are changed.
- `./gradlew check build --no-daemon` passes.
- PR references `SPT-59` and includes this `EXEC_PLAN`.
