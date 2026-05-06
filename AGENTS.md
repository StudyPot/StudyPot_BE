# AI Study Leader Strict Workflow

The working rules in this repository are not recommendations; they are the default operating contract.

## Absolute Rules
- After cloning, run `scripts/hooks/install-hooks.sh` once to enable the hooks.
- Do not write code without a plan.
- Always implement in a separate `codex/<slug>` worktree.
- Start implementation work from a Jira `SPT` implementation Task issue. Do not use Obsidian as the work queue.
- Use this start command format: `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`.
- Do not edit `src/` directly from a `main` or `develop` checkout.
- Before implementation, read documents in this order: `AGENTS.md -> ARCHITECTURE.md -> docs/index.md -> task-related docs`.
- Do not skip writing tests or running verification.
- Commit subjects must follow the `[feat] description` format.
- Use `scripts/task/create-pr.sh` as the default path for push and PR creation.
- Merge only through `scripts/task/finish-pr.sh` after passing the PR review gate.
- Do not merge based on green CI alone. Check the GitHub Actions Review Gate marker and unresolved thread status.
- The v1 planning/API/DB/AI/notification/permission/QA specs are `LOCKED_FOR_IMPLEMENTATION`. Changes are forbidden without the Change Request + ADR process in `docs/specs/change-control-v1.md`.

## Step 1: Create EXEC_PLAN
- Start command: `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`
- This step creates the `codex/<slug>` branch, external worktree, `EXEC_PLAN`, reserved port, log directory, and task state.
- `EXEC_PLAN` must live inside the generated feature worktree and must not dirty the base checkout.
- The generated `EXEC_PLAN` must not leave any of the following sections empty:
  - Required Reads
  - Related Docs
  - Related Feature IDs
  - Doc Notes
  - Goal
  - Approach
  - Step Plan
  - Done Criteria

## Step 2: Implement In The Worktree
- Move into the generated worktree directory before working.
- Record every document you read in the document sections of `EXEC_PLAN`.
- For feature work, link the real product `feature_id` in `Related Feature IDs`.
- For harness or infrastructure work, use `n/a-harness`.
- Do not edit `src/` directly from a `main` or `develop` checkout.

## Step 3: Write Tests
- New features must include happy-path, edge-case, and input validation tests.
- Bug fixes must start with a reproduction test.
- Refactors must include tests that confirm existing behavior is preserved.

## Step 4: Run Verification
- The standard verification command is `./gradlew check build --no-daemon`.
- Do not commit if anything fails.
- Hooks must record the last successful verification command, status, and timestamp in task state.

## Step 5: PR Review Gate
- `scripts/task/create-pr.sh` pushes the feature branch and includes `EXEC_PLAN`, verification status, and the review gate checklist in the PR body.
- The PR body must include the Jira issue key/URL.
- The default PR target is `develop`.
- Do not consider a PR ready to merge until the GitHub Actions Review Gate has posted a PASS comment for the latest PR head.
- Address reviewdog/actionlint feedback and every actionable review comment through code, tests, or docs, then resolve the review threads.
- Codex subagent review runs in three rounds by default: flexible architecture/direction review, focused fix verification review, and strict final merge-readiness review.
- After each round, address the actionable feedback before requesting the next round.
- The default review gate comment must contain these markers:
  - `GitHub Actions Review Gate: PASS`
  - `Head: <current_pr_head_sha>`
- The final merge gate also requires latest-head `Codex Subagent Review Round 1/2/3: PASS` markers unless explicitly disabled for harness/bootstrap exceptions.
- `scripts/task/finish-pr.sh` verifies that the PR head did not change during verification and only cleans up a clean/unlocked feature worktree.
- `scripts/task/finish-pr.sh` transitions the Jira Task to the done status after PR merge and cleanup are complete.

## Related Documents
- Architecture summary: `ARCHITECTURE.md`
- Detailed docs hub: `docs/index.md`
- v1 change control: `docs/specs/change-control-v1.md`
- Jira Board Sync: `docs/operations/jira-board-sync.md`
- PR review gate: `docs/operations/pr-review-gate.md`
- Obsidian error ledger: `docs/operations/obsidian-error-ledger.md`
