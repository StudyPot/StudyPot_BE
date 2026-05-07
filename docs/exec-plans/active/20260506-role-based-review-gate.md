# EXEC_PLAN: Convert subagent review rounds to role gates

- Task slug: `role-based-review-gate`
- Base branch: `origin/develop`
- Feature branch: `codex/role-based-review-gate`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/subagent-review-gate`
- Port: `n/a`
- Log dir: `n/a`
- Jira issue: `n/a-harness`
- Jira URL: n/a
- Jira summary: Harness exception; Jira lookup not required
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/operations/pr-review-gate.md
- [x] docs/testing/codex-harness.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md` currently describes a three-round Codex subagent review gate.
- `docs/operations/pr-review-gate.md` currently documents Round 1/2/3 strictness.
- The requested direction is closer to a company-style role pipeline: CTO architecture, QA verification, Product/CBO value review, and final CTO merge approval.
- `finish-pr.sh` is the merge blocker that should enforce latest-head role PASS markers.
- Verification passed with `git diff --check`, `bash scripts/tests/run.sh`, and `./gradlew check build --no-daemon` on 2026-05-06.

## Goal
Replace numeric Codex subagent review rounds with role-based company gates that mirror how a real product team reviews work before merge.

## Approach
Keep the latest-head marker model from the previous harness change, but rename and reshape it around roles. Update PR body checklist, helper script, finish gate enforcement, docs, and static harness tests together.

## Step Plan
1. Change static harness tests to expect role gate markers and reject the old round helper contract.
2. Update PR creation checklist to ask for CTO Architecture, QA Verification, Product Value, and Final CTO Merge gates.
3. Update finish gate enforcement to require all role markers for the latest PR head by default.
4. Replace the pass marker helper with a role-aware helper.
5. Update docs and `AGENTS.md` to describe the company-style role pipeline.
6. Run harness tests and standard Gradle verification.

## Done Criteria
- PR checklist includes CTO Architecture, QA Verification, Product Value, and Final CTO Merge gates.
- `finish-pr.sh` requires all default role PASS markers for the current PR head.
- Helper script can post a standard marker for each supported role gate.
- Docs explain the role responsibilities and marker format.
- `bash scripts/tests/run.sh` passes.
- `./gradlew check build --no-daemon` passes.
