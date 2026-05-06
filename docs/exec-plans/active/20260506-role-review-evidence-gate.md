# EXEC_PLAN: Require evidence for role review gates

- Task slug: `role-review-evidence-gate`
- Base branch: `origin/develop`
- Feature branch: `codex/role-review-evidence-gate`
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
- `AGENTS.md` requires the company-style role pipeline before merge.
- `docs/operations/pr-review-gate.md` defines CTO Architecture, QA Verification, Product Value, and Final CTO Merge gates.
- `post-role-review-pass.sh` currently allows marker posting without a notes/evidence file.
- `finish-pr.sh` currently accepts a marker comment if it contains only marker and head SHA.
- Verification passed with `git diff --check`, `bash scripts/tests/run.sh`, and `./gradlew check build --no-daemon` on 2026-05-06. The first full harness run exposed a missing executable bit on the new test script; `chmod +x scripts/tests/test_role_review_evidence.sh` fixed it and the full harness suite then passed. After tightening `finish-pr.sh` to require role-specific evidence labels, the focused evidence tests, full harness suite, and Gradle verification passed again.

## Goal
Prevent every role gate from posting or accepting a PASS marker unless role-specific review evidence is attached.

## Approach
Make the role review helper require a non-empty evidence file with a common `## Evidence` section and role-specific checklist labels. Make `finish-pr.sh` accept only latest-head role marker comments that also contain evidence. Document the evidence contract and cover it with harness tests.

## Step Plan
1. Add failing static harness checks for required evidence file behavior and evidence-aware finish gate matching.
2. Update `post-role-review-pass.sh` to require an evidence file and validate role-specific evidence labels.
3. Update `finish-pr.sh` to reject marker comments that do not contain a `## Evidence` section.
4. Update PR checklist and docs to describe evidence requirements for all roles.
5. Run harness tests and standard Gradle verification.

## Done Criteria
- CTO, QA, Product/CBO, and Final CTO role marker helper calls fail without evidence.
- Evidence files must include common and role-specific evidence labels.
- `finish-pr.sh` requires both marker/head SHA and evidence in the accepted PR comment.
- Docs define the evidence templates.
- `bash scripts/tests/run.sh` passes.
- `./gradlew check build --no-daemon` passes.
