# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행한다.
5. GitHub Actions가 PR 품질 검사와 reviewdog feedback을 실행하고 pass marker를 남긴다.
6. Codex subagent review를 3회 수행한다. 작성자는 각 round의 actionable feedback을 수정한 뒤 다음 round를 요청한다.
7. merge 전 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 PR 상태를 검증한다.
8. 최종 merge와 cleanup은 `scripts/task/finish-pr.sh <PR_NUMBER>`가 수행한다.

## PR 생성 계약
- target branch는 기본적으로 `develop`이다.
- head branch는 `codex/<slug>`여야 한다.
- PR body에는 다음 증거가 들어가야 한다.
  - Jira issue key/URL
  - 관련 issue closing line
  - `EXEC_PLAN` 경로와 본문
  - 마지막 검증 명령, 상태, 시각
  - GitHub Actions Review Gate checklist
  - Codex Subagent Review Round 1/2/3 checklist
- `create-pr.sh`는 기본적으로 PR 생성 후 자동 finish를 하지 않는다.
- `STRICT_AUTO_FINISH_PR=1`을 명시한 경우에만 PR 생성 직후 `finish-pr.sh`를 호출한다.

## Merge 전 차단 조건
`verify-pr-ready.sh`는 다음 경우 실패해야 한다.

- PR이 open 상태가 아님
- draft PR
- review decision이 `CHANGES_REQUESTED`
- review/comment activity가 없음
- pending/failing/cancelled checks
- required GitHub Actions check가 없음
- 최신 PR head에 대한 Codex Subagent Review Round 1/2/3 pass marker가 없음
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

## Progressive Codex Subagent Review
Codex subagent review is part of the default finish gate. The review gets stricter after each correction cycle:

- Round 1: flexible architecture and direction review. Focus on scope, approach, missing tests, high-risk design issues, and obviously dangerous behavior. Avoid blocking on small style choices.
- Round 2: focused fix verification review. Re-check the author's fixes, test coverage, edge cases, contracts, and any newly introduced risk.
- Round 3: strict final merge-readiness review. Treat every actionable bug, missing verification, unresolved thread, contract drift, unsafe cleanup, or stale evidence as merge-blocking.

Each passing review round must leave a PR comment on the latest head using this format:

```text
Codex Subagent Review Round <1|2|3>: PASS
Head: <current_pr_head_sha>
```

After a Codex subagent review round passes, use the helper below to post the standard marker:

```bash
scripts/task/post-subagent-review-pass.sh <PR_NUMBER> <ROUND>
```

An optional notes file may be passed as the third argument. The helper records the current PR head SHA, so run it only after the reviewed fixes have been pushed.

`finish-pr.sh` requires all three round markers for the latest PR head by default. If a new commit is pushed after a round passes, that round must be repeated because the previous marker no longer matches the current head.

Harness/bootstrap exceptions may set `STRICT_REQUIRE_CODEX_SUBAGENT_ROUNDS=0`. Partial gates may set the value to `1` or `2`, but feature work should keep the default `3`.

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
