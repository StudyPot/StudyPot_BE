# Jira Board Sync

이 문서는 구현 작업의 source of truth를 Jira Board로 고정하는 하네스 계약이다.

## 기본 원칙
- 구현 작업은 Obsidian에서 고르지 않는다.
- 구현 작업은 Jira `SPT` 프로젝트의 구현 단위 Task 이슈에서 고른다.
- 사용자가 "다음에 할 것"을 추천해 달라고 하면 먼저 Jira 전체 작업 맥락을 읽고 약 세 개의 다음 후보를 추천한다.
- 작업 시작은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로만 수행한다.
- 작업 시작 시 Jira 이슈는 `해야 할 일`에서 `진행 중`으로 이동한다.
- 사용자가 GitHub에서 직접 PR을 merge하고 `finish-pr.sh cleanup-merged` local cleanup이 끝난 뒤 Jira 이슈는 `완료`로 이동한다.
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
| Human merge + `finish-pr.sh cleanup-merged` cleanup succeeds | `완료` |

The script discovers transition IDs from each issue's available transitions. It does not hardcode transition IDs.

## 다음 작업 추천
다음 작업 추천은 작업 시작 전 후보 선정 단계다.

```bash
scripts/task/jira-board.sh recommend-next --limit 3
```

이 명령은 Jira Cloud JQL search endpoint인 `/rest/api/3/search/jql`로 프로젝트 이슈를 읽고, 완료/진행 중/해야 할 일/기타 미완료 상태를 요약한 뒤 다음 작업 후보를 출력한다.

추천 순서는 다음 원칙을 따른다.

1. 이미 `진행 중`인 구현 Task를 먼저 마무리한다.
2. 그다음 `해야 할 일` 상태의 구현 Task를 추천한다.
3. 그 외 미완료 상태는 상태 확인이 필요한 후보로 뒤에 둔다.
4. `완료` 상태의 이슈는 추천 후보에서 제외하고, 최근 완료 맥락으로만 보여준다.

추천은 작업 시작이 아니다. 선택한 후보는 반드시 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.

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
- `finish-pr.sh <PR_NUMBER>` verifies PR readiness and sends the Korean Mattermost manual merge notification without changing Jira to `완료`.
- `finish-pr.sh cleanup-merged <PR_NUMBER>` transitions Jira to `완료` only after the human PR merge, develop sync, branch cleanup, and worktree cleanup succeed.

## Failure And Recovery
- If Jira is already `완료`, `init-task.sh --jira` fails.
- If Jira cannot transition to `진행 중`, task start fails.
- If Jira cannot transition to `완료`, `finish-pr.sh cleanup-merged` fails after local cleanup and reports the Jira error.
- Manual recovery:
  1. Inspect Jira issue status in the board.
  2. Fix missing env or Jira permissions.
  3. Re-run the failing harness command.
  4. Do not manually mark Done unless PR merge actually succeeded.

## Test Mode
Tests use `STRICT_JIRA_API_STUB` to route Jira calls to a fake API script.

Production usage must not set `STRICT_JIRA_API_STUB`.
