# Jira Board Sync

이 문서는 구현 작업의 source of truth를 Jira Board로 고정하는 하네스 계약이다.

## 기본 원칙
- 구현 작업은 Obsidian에서 고르지 않는다.
- 구현 작업은 Jira `SPT` 프로젝트의 구현 단위 Task 이슈에서 고른다.
- 작업 시작은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로만 수행한다.
- 작업 시작 시 Jira 이슈는 `해야 할 일`에서 `진행 중`으로 이동한다.
- PR merge와 local cleanup이 끝난 뒤 Jira 이슈는 `완료`로 이동한다.
- Jira 전환 실패는 하네스 실패다.

## 인증 환경변수
Jira REST API 호출은 repo에 secret을 저장하지 않고 로컬 환경변수로만 인증한다.

```bash
export JIRA_EMAIL="your-email@example.com"
export JIRA_API_TOKEN="your-atlassian-api-token"
export STRICT_JIRA_BASE_URL="https://studypot.atlassian.net"
export STRICT_JIRA_PROJECT_KEY="SPT"
```

## 상태 매핑
| Harness event | Jira status |
| --- | --- |
| Task selected but not started | `해야 할 일` |
| `init-task.sh --jira` succeeds | `진행 중` |
| `finish-pr.sh` completes merge and cleanup | `완료` |

The script discovers transition IDs from each issue's available transitions. It does not hardcode transition IDs.

## 허용 이슈
- Project key: `SPT`
- Issue type: `작업` or `Task`
- `Feature`, `Epic`, `Story`, `Bug` are not implementation start targets.
- Feature/Epic tracking should be done through Jira links and labels, not by starting the harness from them.

## Task State
`init-task.sh --jira` stores these fields under `.codex/task-state/<slug>.env`:

- `JIRA_ISSUE_KEY`
- `JIRA_ISSUE_URL`
- `JIRA_ISSUE_SUMMARY`
- `JIRA_STARTED_AT`
- `JIRA_DONE_AT`

`new-exec-plan.sh` mirrors the Jira key and URL into the `EXEC_PLAN` header.

## PR Flow
- `create-pr.sh` requires Jira task state.
- PR body includes:
  - `Jira: [SPT-123](https://studypot.atlassian.net/browse/SPT-123)`
  - `EXEC_PLAN`
  - verification evidence
  - review gate checklist
- `finish-pr.sh` transitions Jira to `완료` only after PR merge, develop sync, branch cleanup, and worktree cleanup succeed.

## Failure And Recovery
- If Jira is already `완료`, `init-task.sh --jira` fails.
- If Jira cannot transition to `진행 중`, task start fails.
- If Jira cannot transition to `완료`, `finish-pr.sh` fails after local cleanup and reports the Jira error.
- Manual recovery:
  1. Inspect Jira issue status in the board.
  2. Fix missing env or Jira permissions.
  3. Re-run the failing harness command.
  4. Do not manually mark Done unless PR merge actually succeeded.

## Test Mode
Tests use `STRICT_JIRA_API_STUB` to route Jira calls to a fake API script.

Production usage must not set `STRICT_JIRA_API_STUB`.
