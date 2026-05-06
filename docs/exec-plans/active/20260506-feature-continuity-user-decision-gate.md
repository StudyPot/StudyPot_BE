# EXEC_PLAN: Feature Continuity And User Decision Gate

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md
- [x] docs/operations/pr-review-gate.md

## Related Docs
- AGENTS.md
- docs/operations/pr-review-gate.md
- scripts/task/create-pr.sh
- scripts/tests/test_pr_scripts_static.sh

## Related Feature IDs
- n/a-harness

## Doc Notes
- Harness work does not require Jira for this repository-maintenance request.
- Current workflow already requires plan, worktree, verification, PR gates, and develop merge.
- Feature continuity and user-decision escalation are not yet explicit enough in the repository operating contract.

## Goal
Make the harness contract explicit that agents must keep working until the feature is complete, and must ask the user before adding unrequested scope or making a product/architecture decision that needs owner input.

## Approach
- Add absolute rules to AGENTS.md.
- Document the same contract in the PR review gate flow.
- Add checklist and static-test coverage so future harness edits do not drop the rule accidentally.

## Step Plan
- [x] Add this EXEC_PLAN before implementation.
- [x] Update AGENTS.md with feature continuity and user decision escalation rules.
- [x] Update docs/operations/pr-review-gate.md with the operational contract and review-gate evidence expectations.
- [x] Update create-pr checklist and static tests.
- [x] Run focused harness tests and full Gradle verification.
- [ ] Commit and push directly to develop as requested for harness work.

## Done Criteria
- AGENTS.md states the feature-continuity rule.
- AGENTS.md states that new scope or opinion-required decisions must be escalated to the user before implementation.
- PR gate docs and checklist make reviewers verify those expectations.
- Static tests cover the new checklist and documentation markers.
- Verification passes before commit and direct develop push.

## Verification
- `bash scripts/tests/test_pr_scripts_static.sh` - passed.
- `bash scripts/tests/test_role_review_evidence.sh` - passed.
- `git diff --check` - passed.
- `bash scripts/tests/run.sh` - passed.
- `./gradlew check build --no-daemon` - passed.
