# AI Study Leader Strict Workflow

The working rules in this repository are not recommendations; they are the default operating contract.

## Absolute Rules
- After cloning, run `scripts/hooks/install-hooks.sh` once to enable the hooks.
- When the user asks what to do next, run `scripts/task/jira-board.sh recommend-next --limit 3` first, read Jira done/not-done context, and recommend about three next tasks before starting implementation.
- Do not write code without a plan.
- Always implement in a separate `codex/<slug>` worktree.
- Once implementation starts, continue through code, tests, review feedback, PR ready notification, user-confirmed merge, and post-merge cleanup until the feature is complete or a real blocker requires user input.
- Do not invent unrequested scope, product direction, architecture choices, or opinion-sensitive tradeoffs. Ask the user first, then record the decision in `EXEC_PLAN`.
- Start implementation work from a Jira `SPT` implementation Task issue. Do not use Obsidian as the work queue.
- Use this start command format: `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`.
- Do not edit `src/` directly from a `main` or `develop` checkout.
- Before implementation, read documents in this order: `AGENTS.md -> ARCHITECTURE.md -> docs/index.md -> task-related docs`.
- Do not skip writing tests or running verification.
- Human-authored commit subjects and PR titles must follow the `[type] 한글 내용` format. `type` must be lowercase English, such as `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `build`, `perf`, `style`, or `revert`.
- Human-facing Mattermost notifications, PR bodies, PR review comments, and review evidence must be written in Korean. Keep only machine-readable marker tokens such as `CodeRabbit Subagent Review: PASS` and `Head: <sha>` in their required literal form.
- Use `scripts/task/create-pr.sh` as the default path for push and PR creation.
- After PR creation, run `scripts/task/run-coderabbit-review.sh <PR_NUMBER>` to request one CodeRabbit agent-mode review. If CodeRabbit reports issues, fix that feedback once and post addressed evidence with `scripts/task/post-coderabbit-addressed.sh <PR_NUMBER> <evidence_file>`.
- Do not auto-merge PRs. After passing the PR review gate, use `scripts/task/finish-pr.sh` to verify readiness and send the Mattermost manual-merge notification; the human user clicks the GitHub merge button.
- After the human merge, GitHub Actions moves the linked Jira Task to Done; then run `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>` for develop sync, worktree cleanup, branch cleanup, and idempotent Jira state recording.
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
- Do not send the Mattermost manual-merge notification until the latest PR head has a `CodeRabbit Subagent Review: PASS` marker or a `CodeRabbit Subagent Review: ADDRESSED` marker with evidence.
- Address reviewdog/actionlint feedback and every actionable review comment through code, tests, or docs, then resolve the review threads.
- The default review loop is one CodeRabbit agent-mode review. If CodeRabbit returns zero issues, post `CodeRabbit Subagent Review: PASS`. If it returns issues, fix those issues once, verify, and post `CodeRabbit Subagent Review: ADDRESSED`.
- Evidence-free CodeRabbit ADDRESSED markers are not accepted. The evidence must include `## 증거` plus non-empty `리뷰 결과`, `수정 범위`, and `검증` entries.
- The GitHub Actions `review-gate-pass` required check must verify the latest-head CodeRabbit review marker and fail until it exists.
- The default review gate comment must contain these markers:
  - `GitHub Actions Review Gate: PASS`
  - `Head: <current_pr_head_sha>`
- `scripts/task/finish-pr.sh` verifies that the PR head did not change during verification and sends a Mattermost notification instead of merging.
- GitHub Actions transitions the linked Jira Task to the done status after the human merge; `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>` remains required for local cleanup and idempotent Jira state recording.

## Related Documents
- Architecture summary: `ARCHITECTURE.md`
- Detailed docs hub: `docs/index.md`
- v1 change control: `docs/specs/change-control-v1.md`
- Jira Board Sync: `docs/operations/jira-board-sync.md`
- PR review gate: `docs/operations/pr-review-gate.md`
- Obsidian error ledger: `docs/operations/obsidian-error-ledger.md`
