# Error Ledger

This ledger records real failures that affected local verification, CI, PR gates, Jira transitions, or session continuity. It is the repo-tracked source of truth for failure memory. Obsidian may mirror these notes, but executable fixes and guardrails must live in the repo.

## Rules
- Record actual failures only.
- Do not store secrets, API tokens, passwords, cookies, or full sensitive values.
- Prefer a fix that is executable by scripts, tests, docs, or CI.
- Add a prevention checkpoint that can be reused before future PRs.
- Keep entries short enough to scan during task planning.

## Entry Format
Each entry must include:

- Date
- Work / feature id
- Symptom
- Cause
- Fix
- Prevent next time
- Next checkpoint

## Entries

### 2026-05-06 - Jira API Token Exposed In Chat
- Work / feature id: `SPT-20`, `SPT-56`, `n/a-harness`
- Symptom: A Jira API token was pasted directly into the chat and then used for Jira-backed task scripts.
- Cause: The local Jira harness requires `JIRA_EMAIL` and `JIRA_API_TOKEN`, but there was no safer local secret handoff in place during the session.
- Fix: Use the token only as an environment variable for the required commands, avoid printing it, and remind the user to revoke and rotate it after the work.
- Prevent next time: Prefer local shell exports, a local untracked env file, or a credential manager flow instead of pasting tokens into chat.
- Next checkpoint: Before using Jira scripts, confirm whether `JIRA_EMAIL` and `JIRA_API_TOKEN` are already available locally; never write token values into repo docs, logs, commits, or PR comments.

### 2026-05-06 - `finish-pr.sh` Blocked By `BEHIND`
- Work / feature id: `SPT-20`, `n/a-harness`
- Symptom: `scripts/task/finish-pr.sh 8` failed with `PR merge state is blocked: BEHIND`.
- Cause: `develop` advanced after the PR was created, and the repository uses strict required checks against the latest base branch.
- Fix: Rebase the feature branch onto `origin/develop`, rerun local verification, force-push with lease, rerun CI, repost latest-head role gate markers, and rerun `verify-pr-ready.sh`.
- Prevent next time: Check `gh pr view <number> --json mergeStateStatus,headRefOid,baseRefOid` immediately before finish.
- Next checkpoint: If merge state is `BEHIND`, update the feature branch before posting final role gates because role gate markers are head-specific.

### 2026-05-06 - reviewdog ShellCheck `SC1091` Source Hint
- Work / feature id: `SPT-20`, `SPT-56`, `n/a-harness`
- Symptom: PR reviewdog opened unresolved ShellCheck comments for `SC1091` because test scripts used source hints that ShellCheck could not follow.
- Cause: Some scripts used `# shellcheck source=./testlib.sh`, which works for a human reading the file but not for the CI ShellCheck path.
- Fix: Change hints to repo-root relative paths such as `# shellcheck source=scripts/tests/testlib.sh`.
- Prevent next time: Match new shell script source hints to existing CI-friendly patterns before PR creation.
- Next checkpoint: Run `gh pr checks <number>` and query review threads after reviewdog completes; resolve only after the fix is pushed and the latest head has no replacement thread.

### 2026-05-06 - Installed Hook Symlinks Could Not Source `common.sh`
- Work / feature id: `SPT-56`, `n/a-harness`
- Symptom: Running `.githooks/pre-commit` failed with `../task/common.sh: No such file or directory`.
- Cause: The installed hook is a symlink under `.githooks`, and the hook scripts resolved `SCRIPT_DIR` from the symlink location rather than the real script path under `scripts/hooks`.
- Fix: Resolve symlinks in `pre-commit.sh` and `pre-push.sh` before sourcing `scripts/task/common.sh`; update `test_hook_enforcement.sh` to cover installed-symlink execution.
- Prevent next time: Any hook script that sources repo helpers must resolve its real path, not only `BASH_SOURCE[0]`'s symlink directory.
- Next checkpoint: After changing hook install behavior, run both direct hook tests and installed-symlink hook tests.

### 2026-05-06 - PR Creation Blocked By Missing Verification State
- Work / feature id: `SPT-56`, `n/a-harness`
- Symptom: `scripts/task/create-pr.sh` failed with `last verification status is not passed`.
- Cause: The first commit was created before the worktree hook path had successfully recorded `LAST_VERIFY_STATUS=passed` in task state.
- Fix: Install/fix hooks, rerun the verification path through the hook, confirm task state contains `LAST_VERIFY_STATUS=passed`, then rerun `create-pr.sh`.
- Prevent next time: Check `.codex/task-state/<slug>.env` for `LAST_VERIFY_COMMAND`, `LAST_VERIFY_STATUS`, and `LAST_VERIFY_AT` before PR creation.
- Next checkpoint: If `create-pr.sh` blocks on verification state, do not bypass it; rerun the configured verification command through the hook or intentionally update task state only with fresh evidence.

### 2026-05-06 - Obsidian/iCloud Error Ledger Read Hung
- Work / feature id: `SPT-57`, `n/a-harness`
- Symptom: Reading the Obsidian error ledger through the iCloud path hung and had to be interrupted by killing the read processes.
- Cause: The Obsidian vault lives under iCloud Drive, and file provider synchronization can block direct shell reads.
- Fix: Stop relying on Obsidian as the primary ledger for executable work; create this repo-tracked ledger and treat Obsidian as an optional mirror.
- Prevent next time: Read and update repo docs first; mirror to Obsidian only after repo changes are committed or when iCloud files are responsive.
- Next checkpoint: If an iCloud-backed read stalls, stop it and continue from repo-tracked state instead of blocking task progress.

### 2026-05-06 - New Harness Test Missing Executable Bit
- Work / feature id: `SPT-57`, `n/a-harness`
- Symptom: `bash scripts/tests/run.sh` failed with `Permission denied` when invoking `test_error_ledger_contracts.sh`.
- Cause: The new shell test was added without executable mode, while `run.sh` invokes test files directly.
- Fix: Set the test file executable and rerun the full harness successfully.
- Prevent next time: After adding a shell test that is called directly from `run.sh`, check `git status --short` or `git ls-files -s` for executable mode.
- Next checkpoint: Run the full `bash scripts/tests/run.sh`, not only the new test directly, before committing harness changes.
