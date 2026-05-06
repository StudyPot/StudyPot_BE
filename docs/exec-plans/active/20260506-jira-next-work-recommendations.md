# EXEC_PLAN: Jira Next Work Recommendations

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/pr-review-gate.md

## Related Docs
- AGENTS.md
- docs/operations/jira-board-sync.md
- scripts/task/jira-board.sh
- scripts/tests/test_jira_board_sync.sh
- scripts/tests/testlib.sh

## Related Feature IDs
- n/a-harness

## Doc Notes
- Harness-related work is allowed to skip Jira task selection for this repository-maintenance request.
- The existing Jira harness can validate and transition a single task, but cannot scan the project and recommend the next tasks.
- Jira Cloud search should use `/rest/api/3/search/jql` instead of the removed legacy `/rest/api/3/search` endpoint.

## Goal
Add a front-of-workflow command for "recommend my next tasks" that reads Jira project work, compares done and not-done items, and recommends about three next implementation tasks.

## Approach
- Extend `scripts/task/jira-board.sh` with a `recommend-next` command.
- Search the Jira project with JQL, paginate results, and summarize status context.
- Rank not-done implementation tasks by in-progress first, then todo, then other open statuses.
- Document the command in AGENTS.md and Jira board sync docs.
- Add fake Jira search support and tests for the recommendation output.

## Step Plan
- [x] Add this EXEC_PLAN before implementation.
- [x] Add the `recommend-next` command.
- [x] Update docs and AGENTS workflow guidance.
- [x] Add tests for Jira search and recommendation ranking.
- [x] Run focused harness tests and full Gradle verification.
- [ ] Commit and push directly to develop as requested for harness work.

## Done Criteria
- `scripts/task/jira-board.sh recommend-next` reads Jira search results and prints three recommended next tasks by default.
- Recommendation output includes done/in-progress/todo context.
- The command supports an optional `--limit <N>` argument.
- Tests prove in-progress work is recommended before todo work and done work is not recommended.
- Verification passes before direct develop push.

## Verification
- `bash -n scripts/task/jira-board.sh && bash -n scripts/tests/test_jira_next_recommendations.sh && bash scripts/tests/test_jira_next_recommendations.sh` - passed.
- `bash scripts/tests/test_pr_scripts_static.sh` - passed.
- `git diff --check` - passed.
- `bash scripts/tests/run.sh` - passed.
- `./gradlew check build --no-daemon` - passed.
