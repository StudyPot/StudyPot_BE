# EXEC_PLAN: [ops] Repo error ledger 정리

- Task slug: `error-ledger`
- Base branch: `develop`
- Feature branch: `codex/error-ledger`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/error-ledger`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/error-ledger`
- Jira issue: `SPT-57`
- Jira URL: https://studypot.atlassian.net/browse/SPT-57
- Jira summary: [ops] Repo error ledger 정리
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/operations/error-ledger.md
- [x] docs/exec-plans/active/20260506-docs-source-of-truth-cleanup.md
- [x] scripts/tests/run.sh
- [x] scripts/tests/test_pr_scripts_static.sh
- [x] scripts/tests/test_docs_source_of_truth.sh
- [x] scripts/tests/test_error_ledger_contracts.sh

## Related Feature IDs
- [x] n/a-harness
- [x] error-ledger

## Doc Notes
- User decision: create a repo-tracked Error Ledger first, then optionally mirror to Obsidian.
- Keep repo docs as the executable/source-of-truth layer; Obsidian remains session continuity and mirror.
- Record only actual failures encountered during recent SPT-20/SPT-56 work; do not invent placeholder incidents.
- Do not store secrets, API tokens, or full sensitive values in the ledger.

## Goal
Create a repo-owned Error Ledger that records recent real failures, their causes, fixes, and next-time checkpoints, so future work can learn from the failures without relying on iCloud/Obsidian availability.

## Approach
Add `docs/operations/error-ledger.md`, link it from the docs hub, update the Obsidian ledger operation doc to make repo ledger primary, and add a small harness test that verifies required incident sections and links.

## Step Plan
1. Create the repo Error Ledger with recent actual incidents.
2. Update `docs/index.md` and `docs/operations/obsidian-error-ledger.md`.
3. Add and wire a harness test for the error ledger contract.
4. Run targeted tests, full harness, diff check, and Gradle verification.

## Done Criteria
- `docs/operations/error-ledger.md` exists and includes the required item format.
- Recent token exposure, PR `BEHIND`, reviewdog `SC1091`, hook symlink, verification-state, and iCloud/Obsidian read-hang incidents are recorded without secrets.
- The new shell test executable-bit failure discovered by the full harness is recorded.
- `docs/index.md` links to the repo Error Ledger.
- `docs/operations/obsidian-error-ledger.md` states that Obsidian mirrors the repo ledger.
- Harness tests cover the Error Ledger contract.
- `bash scripts/tests/run.sh`, `git diff --check`, and `./gradlew check build --no-daemon` pass.

## Verification
- [x] `bash scripts/tests/test_error_ledger_contracts.sh`
- [x] `bash scripts/tests/test_pr_scripts_static.sh`
- [x] `git diff --check`
- [x] `bash scripts/tests/run.sh`
- [x] `./gradlew check build --no-daemon`
