# EXEC_PLAN: Add progressive subagent PR review gate

- Task slug: `subagent-review-gate`
- Base branch: `origin/develop`
- Feature branch: `codex/subagent-review-gate`
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
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/testing/codex-harness.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- `AGENTS.md` requires PR merge only after the review gate, not green CI alone.
- `docs/operations/pr-review-gate.md` currently treats subagent review as optional and only supports a single legacy pass marker.
- `create-pr.sh` currently calls `finish-pr.sh` by default, which conflicts with a multi-round review loop.
- `finish-pr.sh` is the right merge blocker because it already verifies latest PR head, required checks, review threads, and pass markers.
- Verification passed with `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon` on 2026-05-06.

## Goal
Require a three-round Codex subagent review loop before feature PRs can be finished, with each round becoming stricter after the author addresses feedback.

## Approach
Keep GitHub Actions as the baseline quality gate, but make Codex subagent review a first-class finish gate. Add round-specific PASS markers tied to the latest PR head, update the PR checklist, stop automatic finishing by default, and cover the contract with harness static tests.

## Step Plan
1. Add failing static harness tests that expect three subagent review markers, default no-auto-finish PR creation, and strict finish gate checks.
2. Update `create-pr.sh` to include the three review rounds in the PR body and default `STRICT_AUTO_FINISH_PR` to `0`.
3. Update `finish-pr.sh` to require Round 1, Round 2, and Round 3 latest-head PASS markers by default, with an explicit environment override for harness/bootstrap exceptions.
4. Update PR review gate docs to describe the progressive review strictness and marker format.
5. Run `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon`.

## Done Criteria
- PR body includes Round 1, Round 2, and Round 3 subagent review checklist items.
- `finish-pr.sh` blocks finish unless all required subagent review round markers exist for the latest PR head.
- Review strictness is documented as flexible first pass, focused second pass, strict final pass.
- Harness tests pass.
- Standard Gradle verification passes.
