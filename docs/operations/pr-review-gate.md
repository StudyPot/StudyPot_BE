# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행한다.
5. merge 전 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 PR 상태를 검증한다.
6. 최종 merge와 cleanup은 `scripts/task/finish-pr.sh <PR_NUMBER>`가 수행한다.

## PR 생성 계약
- target branch는 기본적으로 `develop`이다.
- head branch는 `codex/<slug>`여야 한다.
- PR body에는 다음 증거가 들어가야 한다.
  - Jira issue key/URL
  - 관련 issue closing line
  - `EXEC_PLAN` 경로와 본문
  - 마지막 검증 명령, 상태, 시각
  - review gate checklist
- `STRICT_AUTO_FINISH_PR=0`이 아니면 `create-pr.sh`는 기본적으로 `finish-pr.sh`를 호출한다.

## Merge 전 차단 조건
`verify-pr-ready.sh`는 다음 경우 실패해야 한다.

- PR이 open 상태가 아님
- draft PR
- review decision이 `CHANGES_REQUESTED`
- review/comment activity가 없음
- pending/failing/cancelled checks
- unresolved PR review threads
- merge conflict 또는 blocked merge state

## Subagent Review
- Codex subagent review loop는 사용자가 명시적으로 허용한 경우에만 수행한다.
- 최대 3 round만 허용한다.
- actionable finding은 PR comment로 남기고 코드/테스트/문서에 반영한다.
- 통과 시 PR comment에 아래 마커를 남긴다.

```text
Codex Subagent Review Gate: PASS
Head: <current_pr_head_sha>
Round: <n>/3
```

`finish-pr.sh`는 기본적으로 최신 PR head에 대한 이 pass marker를 요구한다. harness/bootstrap 작업처럼 예외가 필요하면 `STRICT_REQUIRE_CODEX_SUBAGENT_PASS=0`으로 명시한다.

## Cleanup 원칙
`finish-pr.sh`는 다음 조건을 증명하기 전에는 worktree나 branch를 삭제하지 않는다.

- feature worktree가 clean
- feature worktree가 locked 아님
- feature worktree `HEAD`가 검증한 PR head와 동일
- local branch가 remote보다 ahead/diverged 상태가 아님

## Jira Board Sync
- `finish-pr.sh`는 merge, develop sync, branch cleanup, worktree cleanup이 모두 성공한 뒤 Jira Task를 `완료`로 전환한다.
- Jira 전환 실패는 finish 실패로 처리한다.
- 자세한 상태 매핑과 복구 절차는 `docs/operations/jira-board-sync.md`를 따른다.
