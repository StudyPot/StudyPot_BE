# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행한다.
5. GitHub Actions가 PR 품질 검사와 reviewdog feedback을 실행하고 pass marker를 남긴다.
6. merge 전 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 PR 상태를 검증한다.
7. 최종 merge와 cleanup은 `scripts/task/finish-pr.sh <PR_NUMBER>`가 수행한다.

## PR 생성 계약
- target branch는 기본적으로 `develop`이다.
- head branch는 `codex/<slug>`여야 한다.
- PR body에는 다음 증거가 들어가야 한다.
  - Jira issue key/URL
  - 관련 issue closing line
  - `EXEC_PLAN` 경로와 본문
  - 마지막 검증 명령, 상태, 시각
  - GitHub Actions Review Gate checklist
- `STRICT_AUTO_FINISH_PR=0`이 아니면 `create-pr.sh`는 기본적으로 `finish-pr.sh`를 호출한다.

## Merge 전 차단 조건
`verify-pr-ready.sh`는 다음 경우 실패해야 한다.

- PR이 open 상태가 아님
- draft PR
- review decision이 `CHANGES_REQUESTED`
- review/comment activity가 없음
- pending/failing/cancelled checks
- required GitHub Actions check가 없음
- unresolved PR review threads
- merge conflict 또는 blocked merge state

## GitHub Actions Review Gate
- 기본 review gate는 CodeRabbit이나 Codex subagent가 아니라 GitHub Actions 기반 무료 검사다.
- required check 기본 목록은 `harness-tests`, `shellcheck-reviewdog`, `workflow-lint`, `openapi-parse`, `backend-check`, `codeql-scan`, `review-gate-pass`다.
- `review-gate-pass` job은 선행 job이 모두 success/skipped일 때 최신 PR head에 대해 아래 마커를 남긴다.

```text
GitHub Actions Review Gate: PASS
Head: <current_pr_head_sha>
```

`finish-pr.sh`는 기본적으로 최신 PR head에 대한 이 pass marker를 요구한다. harness/bootstrap 작업처럼 예외가 필요하면 `STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS=0`으로 명시한다.

## Optional Subagent Review
- Codex subagent review loop는 사용자가 명시적으로 허용한 경우에만 보조적으로 수행한다.
- 필요한 경우 `STRICT_REQUIRE_CODEX_SUBAGENT_PASS=1`로 기존 subagent pass marker도 추가 요구할 수 있다.
- 기본 merge gate는 GitHub Actions Review Gate다.

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
