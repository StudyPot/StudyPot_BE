# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/run-coderabbit-review.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행한다. Copilot review는 기본 요청하지 않는다.
5. GitHub Actions가 PR 품질 검사와 reviewdog feedback을 실행한다.
6. 작성자는 `scripts/task/run-coderabbit-review.sh <PR_NUMBER>`로 CodeRabbit agent-mode review를 한 번 실행한다.
7. CodeRabbit이 0 issues를 보고하면 helper가 최신 head에 `CodeRabbit Subagent Review: PASS` marker를 남긴다.
8. CodeRabbit이 issues를 보고하면 helper가 `CodeRabbit Subagent Review: NEEDS_FIX` marker를 남기고 실패한다. 작성자는 그 feedback을 한 번 수정하고 검증한 뒤 `scripts/task/post-coderabbit-addressed.sh <PR_NUMBER> <evidence_file>`로 `CodeRabbit Subagent Review: ADDRESSED` marker를 남긴다.
9. CodeRabbit marker가 최신 head에 있으면 실패한 `review-gate-pass` job 또는 `PR Quality` workflow를 rerun한다. 통과한 `review-gate-pass`는 GitHub Actions Review Gate pass marker를 남긴다.
10. merge 전 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 PR 상태, CodeRabbit marker, checks, unresolved threads를 검증한다.
11. `scripts/task/finish-pr.sh <PR_NUMBER>`는 최신 head와 review gate를 재검증하고 GitHub PR을 자동 merge한다.
12. GitHub merge 후 Jira Task는 자동으로 완료 처리된다.
13. `finish-pr.sh`는 merge 직후 develop sync, worktree cleanup, branch cleanup, Jira 상태 기록을 수행한다.
14. 외부에서 이미 merge됐거나 finish가 중간에 끊긴 경우 `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>`로 같은 cleanup을 재시도한다.

## 작업 지속 및 사용자 결정 계약
- 구현이 시작된 feature는 코드, 테스트, review feedback 수정, PR 생성, review gate, 자동 merge, post-merge cleanup 경로까지 완료되기 전에는 중간 완료로 멈추지 않는다.
- 멈출 수 있는 경우는 외부 권한/자격 증명, 파괴적 작업 승인, 최신 사용자 결정이 필요한 범위/제품/아키텍처 판단처럼 실제 blocker가 있는 경우뿐이다.
- 문서와 Jira/EXEC_PLAN에 없는 새 기능, 새 범위, 제품 방향, 아키텍처 선택, 의견 의존 tradeoff는 에이전트가 임의로 만들지 않는다.
- 그런 판단이 필요하면 구현 전에 사용자에게 질문하고, 결정 내용을 `EXEC_PLAN`의 `Doc Notes` 또는 `Step Plan`에 기록한다.
- Mattermost 알림, PR body, PR review comment, CodeRabbit review 증거 본문처럼 사람이 읽는 communication은 한글로 작성한다. `CodeRabbit Subagent Review: PASS`, `CodeRabbit Subagent Review: ADDRESSED`, `Head: <sha>`처럼 script가 찾는 marker token만 정해진 literal을 유지한다.

## PR 생성 계약
- target branch는 기본적으로 `develop`이다.
- head branch는 `codex/<slug>`여야 한다.
- 사람이 작성하는 commit subject와 PR title은 `[type] 한글 내용` 형식을 따른다.
- `type`은 `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `build`, `perf`, `style`, `revert` 중 하나여야 한다.
- 제목의 내용 부분은 한글 설명을 포함해야 한다.
- 기존 public `develop` 히스토리는 legacy로 두며, 히스토리 rewrite/force push는 명시적인 사용자 승인 없이는 수행하지 않는다.
- Merge commit 제목은 GitHub merge 흐름의 일부로 허용한다.
- PR body에는 다음 증거가 들어가야 한다.
  - Jira issue key/URL
  - 관련 issue closing line
  - `EXEC_PLAN` 경로와 본문
  - 마지막 검증 명령, 상태, 시각
  - GitHub Actions Review Gate checklist
  - CodeRabbit Subagent Review checklist
- `create-pr.sh`는 기본적으로 PR 생성 후 자동 finish를 하지 않는다.
- `create-pr.sh`는 기본적으로 Copilot review를 요청하지 않는다. legacy 비교가 꼭 필요한 경우에만 `STRICT_REQUEST_COPILOT_REVIEW=1`을 명시할 수 있다.
- `STRICT_AUTO_FINISH_PR=1`을 명시하면 PR 생성 후 `finish-pr.sh`까지 이어서 실행되며, review gate가 충족된 경우 자동 merge와 cleanup까지 수행한다.

## CodeRabbit Subagent Review
`scripts/task/run-coderabbit-review.sh <PR_NUMBER>`는 아래 순서로 동작한다.

- `coderabbit --version`과 `coderabbit auth status --agent`를 확인한다.
- PR target branch를 읽어 `coderabbit review --agent -t all --base <base_ref>`를 실행한다.
- repo root에 `AGENTS.md`가 있으면 `-c AGENTS.md`를 함께 넘긴다.
- NDJSON output은 task log directory 또는 `STRICT_CODERABBIT_OUTPUT` 경로에 저장한다.
- `finding` event가 없으면 PR에 `CodeRabbit Subagent Review: PASS` marker를 게시한다.
- `finding` event가 있으면 PR에 `CodeRabbit Subagent Review: NEEDS_FIX` marker를 게시하고 실패한다.
- `error` event, CLI 실패, 인증 실패, 네트워크 실패는 수동 리뷰로 대체하지 않고 실패한다.

PASS marker는 최신 head와 최소 증거를 포함해야 한다.

```text
CodeRabbit Subagent Review: PASS
Head: <current_pr_head_sha>

## 증거
- 리뷰 결과: CodeRabbit raised 0 issues.
- 검증: coderabbit review --agent -t all --base develop -c AGENTS.md
```

NEEDS_FIX는 ready gate를 만족하지 않는다. 작성자는 CodeRabbit 지적사항을 한 번 수정하고 검증한 뒤 아래 evidence file을 준비한다.

```markdown
## 증거
- 리뷰 결과:
- 수정 범위:
- 검증:
```

그 뒤 helper로 ADDRESSED marker를 남긴다.

```bash
scripts/task/post-coderabbit-addressed.sh <PR_NUMBER> <evidence_file>
```

ADDRESSED marker는 최신 head와 `## 증거`, `리뷰 결과`, `수정 범위`, `검증`을 포함해야 한다. gate 통과 뒤 새 commit이 push되면 이전 marker가 최신 head와 맞지 않으므로 CodeRabbit review 또는 ADDRESSED evidence를 최신 head 기준으로 다시 게시해야 한다.

## Merge 전 차단 조건
`verify-pr-ready.sh`는 다음 경우 실패해야 한다.

- PR이 open 상태가 아님
- draft PR
- review decision이 `CHANGES_REQUESTED`
- review/comment activity가 없음
- 최신 PR head에 대한 CodeRabbit PASS 또는 ADDRESSED marker가 없음
- pending/failing/cancelled checks
- required GitHub Actions check가 없음
- unresolved PR review threads
- merge conflict 또는 blocked merge state

## GitHub Actions Review Gate
- GitHub Actions는 CodeRabbit이나 Codex subagent가 아니라 최신 head review marker를 검증하는 verifier다.
- required check 기본 목록은 `harness-tests`, `shellcheck-reviewdog`, `workflow-lint`, `openapi-parse`, `db-schema-coverage`, `backend-check`, `codeql-scan`, `review-gate-pass`다.
- `review-gate-pass` job은 선행 job이 모두 success/skipped이고 `scripts/task/verify-coderabbit-review.sh`가 최신 PR head의 CodeRabbit PASS 또는 ADDRESSED marker를 확인했을 때 아래 마커를 남긴다.
- `review-gate-pass`는 required branch-protection check이며, 최신 head에 CodeRabbit review marker가 존재할 때까지 GitHub merge 버튼을 하드 블록한다.

```text
GitHub Actions Review Gate: PASS
Head: <current_pr_head_sha>
```

`finish-pr.sh`는 기본적으로 최신 PR head에 대한 이 pass marker를 요구한 뒤 자동 merge를 실행한다. harness/bootstrap 작업처럼 예외가 필요하면 `STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS=0`으로 명시한다.

## Auto Merge Notification
`finish-pr.sh <PR_NUMBER>`는 merge 가능한 최신 PR head를 확인하면 `gh pr merge <PR_NUMBER> --merge`를 실행하고, Mattermost webhook이 설정된 경우 `scripts/task/notify-pr-ready.sh`로 자동 merge 완료 알림을 보낸다.

- 자동 merge는 required `review-gate-pass` check와 최신 head CodeRabbit review marker가 모두 충족된 뒤에만 실행한다.
- `STUDYPOT_MM_WEBHOOK_URL`: Mattermost incoming webhook URL. 로컬 Keychain 또는 환경변수로만 제공하며 repo에 저장하지 않는다.
- `STUDYPOT_MM_MENTIONS`: 알림에 포함할 Mattermost mention 문자열. 기본 운영 값은 로컬 환경에서 관리한다.
- 알림에는 PR 번호/URL, head SHA, 자동 merge 완료 상태, Jira/cleanup 처리 안내를 포함한다.
- GitHub branch protection이 `BLOCKED`를 반환할 수 있으므로 자동 merge 모드에서는 checks/pass marker/thread 조건이 통과했고 `CHANGES_REQUESTED`가 없으면 `BLOCKED`를 merge CLI의 최종 판단 대상으로 허용한다.
- webhook URL이 없으면 알림만 건너뛰고 merge/cleanup은 계속한다. URL 값은 로그/문서/PR body에 출력하지 않는다.

## Legacy Role Gate Scripts
`scripts/task/verify-role-review-gates.sh`와 `scripts/task/post-role-review-pass.sh`는 과거 CTO/QA/Product/Final CTO marker 계약을 보존하는 legacy helper다. 현재 기본 PR 완료 경로에서는 호출하지 않는다. 새 기본 흐름에서 실제 merge-blocking reviewer는 `coderabbit review --agent`이고, GitHub Actions는 그 결과 marker를 검증한다.

## Cleanup 원칙
`finish-pr.sh cleanup-merged`는 다음 조건을 증명하기 전에는 worktree나 branch를 삭제하지 않는다.

- feature worktree가 clean
- feature worktree가 locked 아님
- feature worktree `HEAD`가 검증한 PR head와 동일
- local branch가 remote보다 ahead/diverged 상태가 아님

## Jira Board Sync
- `finish-pr.sh <PR_NUMBER>`는 review gate 통과 후 자동 merge하고 cleanup까지 수행한다.
- GitHub merge 후 Jira Task는 자동으로 완료 처리된다.
- `finish-pr.sh cleanup-merged <PR_NUMBER>`는 외부 merge 또는 중단된 finish 이후 develop sync, branch cleanup, worktree cleanup이 모두 확인된 뒤 Jira 상태를 idempotent하게 기록한다.
- Jira 전환 실패는 finish 실패로 처리한다.
- 자세한 상태 매핑과 복구 절차는 `docs/operations/jira-board-sync.md`를 따른다.
