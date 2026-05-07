# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행하며, 기본적으로 `gh pr edit <PR> --add-reviewer @copilot`로 GitHub Copilot review를 요청한다.
5. GitHub Actions가 PR 품질 검사와 reviewdog feedback을 실행한다. `review-gate-pass`는 company role gate evidence가 없으면 실패 상태로 남는다.
6. Codex review를 회사 역할 기반 gate로 수행한다. 작성자는 각 role의 actionable feedback을 수정한 뒤 다음 gate를 요청하고 최신 head evidence marker를 남긴다.
7. GitHub Copilot review가 제출되면 actionable Copilot feedback을 코드, 테스트, 또는 문서로 반영하고 Copilot review thread를 모두 resolve한다.
8. role gate marker가 모두 최신 head에 있으면 실패한 `review-gate-pass` job 또는 `PR Quality` workflow를 rerun한다. 통과한 `review-gate-pass`는 GitHub Actions Review Gate pass marker를 남긴다.
9. merge 전 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 PR 상태와 Copilot review/thread 상태를 검증한다.
10. `scripts/task/finish-pr.sh <PR_NUMBER>`는 최신 head와 review gate를 재검증하고 Mattermost incoming webhook으로 manual merge 알림을 보낸다.
11. 사용자가 GitHub에서 직접 merge 버튼을 누른다.
12. GitHub merge 후 Jira Task는 자동으로 완료 처리된다.
13. merge 이후 `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>`가 develop sync, worktree cleanup, branch cleanup, Jira 상태 기록을 수행한다.

## 작업 지속 및 사용자 결정 계약
- 구현이 시작된 feature는 코드, 테스트, review feedback 수정, PR ready notification, user-confirmed merge, post-merge cleanup 경로까지 완료되기 전에는 중간 완료로 멈추지 않는다.
- 멈출 수 있는 경우는 외부 권한/자격 증명, 파괴적 작업 승인, 최신 사용자 결정이 필요한 범위/제품/아키텍처 판단처럼 실제 blocker가 있는 경우뿐이다.
- 문서와 Jira/EXEC_PLAN에 없는 새 기능, 새 범위, 제품 방향, 아키텍처 선택, 의견 의존 tradeoff는 에이전트가 임의로 만들지 않는다.
- 그런 판단이 필요하면 구현 전에 사용자에게 질문하고, 결정 내용을 `EXEC_PLAN`의 `Doc Notes` 또는 `Step Plan`에 기록한다.
- role gate는 승인되지 않은 scope creep 또는 미해결 사용자 결정을 merge-blocking feedback으로 처리한다.
- Mattermost 알림, PR body, PR review comment, role gate 증거 본문처럼 사람이 읽는 communication은 한글로 작성한다. `CTO Architecture Gate: PASS`, `Head: <sha>`처럼 script가 찾는 marker token만 정해진 literal을 유지한다.

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
  - CTO Architecture / QA Verification / Product Value / Final CTO Merge checklist
- `create-pr.sh`는 기본적으로 PR 생성 후 자동 finish를 하지 않는다.
- `create-pr.sh`는 기본적으로 Copilot review를 요청한다. 예외가 필요한 bootstrap/harness 작업만 `STRICT_REQUEST_COPILOT_REVIEW=0`을 명시할 수 있다.
- `STRICT_AUTO_FINISH_PR=1`을 명시한 경우에도 `finish-pr.sh`는 자동 merge하지 않고, ready 검증과 Mattermost manual merge 알림까지만 수행한다.

## Merge 전 차단 조건
`verify-pr-ready.sh`는 다음 경우 실패해야 한다.

- PR이 open 상태가 아님
- draft PR
- review decision이 `CHANGES_REQUESTED`
- review/comment activity가 없음
- GitHub Copilot review activity가 없음
- 미해결 GitHub Copilot review thread가 있음
- pending/failing/cancelled checks
- required GitHub Actions check가 없음
- 최신 PR head에 대한 company role gate pass marker가 없음
- 승인되지 않은 scope creep 또는 미해결 사용자 결정이 role gate evidence에 남아 있음
- unresolved PR review threads
- merge conflict 또는 blocked merge state

## GitHub Actions Review Gate
- 기본 review gate는 CodeRabbit이나 Codex subagent가 아니라 GitHub Actions 기반 무료 검사다.
- required check 기본 목록은 `harness-tests`, `shellcheck-reviewdog`, `workflow-lint`, `openapi-parse`, `db-schema-coverage`, `backend-check`, `codeql-scan`, `review-gate-pass`다.
- `review-gate-pass` job은 선행 job이 모두 success/skipped이고 `scripts/task/verify-role-review-gates.sh`가 최신 PR head의 CTO/QA/Product/Final CTO evidence marker를 모두 확인했을 때 아래 마커를 남긴다.
- `review-gate-pass`는 required branch-protection check이며, 최신 head에 role gate가 모두 존재할 때까지 GitHub merge 버튼을 하드 블록한다.

```text
GitHub Actions Review Gate: PASS
Head: <current_pr_head_sha>
```

`finish-pr.sh`는 기본적으로 최신 PR head에 대한 이 pass marker를 요구한 뒤 Mattermost manual merge 알림을 보낸다. harness/bootstrap 작업처럼 예외가 필요하면 `STRICT_REQUIRE_GITHUB_ACTIONS_REVIEW_PASS=0`으로 명시한다.

## Manual Merge Notification
`finish-pr.sh <PR_NUMBER>`는 merge 가능한 최신 PR head를 확인하면 `scripts/task/notify-pr-ready.sh`를 호출한다.

- 이 수동 merge 알림은 required `review-gate-pass` check와 최신 head role gate가 모두 충족된 뒤에만 전송한다.
- `STUDYPOT_MM_WEBHOOK_URL`: Mattermost incoming webhook URL. 로컬 Keychain 또는 환경변수로만 제공하며 repo에 저장하지 않는다.
- `STUDYPOT_MM_MENTIONS`: 알림에 포함할 Mattermost mention 문자열. 기본 운영 값은 로컬 환경에서 관리한다.
- 알림에는 PR 번호/URL, head SHA, ready status, GitHub에서 사람이 직접 merge 버튼을 눌러야 한다는 안내, merge 후 cleanup 명령을 포함한다.
- 알림에는 GitHub merge 후 Jira Task는 자동으로 완료 처리되며, local cleanup은 별도로 실행해야 한다는 안내를 포함한다.
- 알림 전 `verify-copilot-review.sh`가 `copilot-pull-request-reviewer` activity와 미해결 Copilot review thread 0개를 확인한다. 최신 head re-review를 강제해야 하는 경우 `STRICT_REQUIRE_LATEST_HEAD_COPILOT_REVIEW=1`을 명시한다.
- GitHub branch protection이 사람 리뷰 전 `BLOCKED`를 반환할 수 있으므로 manual merge 알림 모드에서는 checks/pass marker/thread 조건이 통과했고 `CHANGES_REQUESTED`가 없으면 `BLOCKED`를 사람 확인 대기 상태로 허용한다.
- webhook URL이 없으면 secret-safe error로 실패한다. URL 값은 로그/문서/PR body에 출력하지 않는다.

## Company-Style Codex Review Gates
Codex review is part of the default finish gate. It follows a company-style role pipeline instead of numeric review rounds:

- CTO Architecture Gate: CTO architecture and work breakdown. Validate scope, architecture, domain/API/DB boundaries, risk, decomposition, and whether the work is ready for engineering.
- QA Verification Gate: QA browser/API verification. Validate tests, real runtime behavior when applicable, edge cases, regressions, and collision/error flows.
- Product Value Gate: Product/CBO value and retention review. Validate user value, adoption/retention impact, workflow fit, and whether follow-up product ideas should become separate tasks instead of scope creep.
- Final CTO Merge Gate: Final CTO merge approval. Re-check all previous feedback, latest evidence, unresolved threads, contract drift, and merge readiness. Treat actionable issues as merge-blocking.

증거는 모든 role gate에 필수다. 통과한 role gate는 아래 marker 중 하나와 `## 증거` 섹션을 포함한 PR comment를 최신 head에 남겨야 한다. 모든 role 증거 파일은 비어 있지 않은 `사용자 결정` 항목을 포함해야 하며, 추가 사용자 결정이 필요 없었는지 또는 사용자 승인 결정이 `EXEC_PLAN`에 기록됐는지를 한글로 적어야 한다.

```text
CTO Architecture Gate: PASS
QA Verification Gate: PASS
Product Value Gate: PASS
Final CTO Merge Gate: PASS
Head: <current_pr_head_sha>
```

After a Codex role gate passes, use the helper below to post the standard marker:

```bash
scripts/task/post-role-review-pass.sh <PR_NUMBER> <GATE> <evidence_file>
```

Supported gate values are `cto-architecture`, `qa-verification`, `product-value`, and `final-cto-merge`. The evidence file is required. The helper records the current PR head SHA, so run it only after the reviewed fixes have been pushed.

`review-gate-pass`와 `finish-pr.sh`는 기본적으로 PR을 ready로 보기 전에 최신 PR head의 네 가지 role gate marker를 모두 요구한다. 허용되는 marker comment는 `## 증거` 섹션을 포함해야 한다. gate 통과 뒤 새 commit이 push되면 이전 marker가 최신 head와 맞지 않으므로 해당 gate를 반복해야 하며, 새 marker를 게시한 뒤 실패한 `review-gate-pass` job 또는 `PR Quality` workflow를 rerun해야 한다.

Harness/bootstrap exceptions may set `STRICT_REQUIRE_COMPANY_REVIEW_GATES=0`. Partial gates may set `STRICT_REQUIRE_COMPANY_REVIEW_GATES` to a space-separated subset, but feature work should keep the default `cto-architecture qa-verification product-value final-cto-merge`.

### 증거 템플릿
각 증거 파일은 `## 증거`, `사용자 결정`, 그리고 해당 gate가 요구하는 label을 포함해야 한다. 증거 항목은 반드시 채워야 한다. label만 있는 템플릿은 pass marker로 인정하지 않는다.

CTO Architecture Gate:

```markdown
## 증거
- 사용자 결정:
- 아키텍처 검토:
- 작업 분해:
- 위험:
```

QA Verification Gate:

```markdown
## 증거
- 사용자 결정:
- 실행한 명령:
- 검증 시나리오:
- 결과:
```

Product Value Gate:

```markdown
## 증거
- 사용자 결정:
- 사용자 가치:
- 리텐션 영향:
- 범위 결정:
```

Final CTO Merge Gate:

```markdown
## 증거
- 사용자 결정:
- 이전 게이트 확인:
- 미해결 스레드:
- merge 결정:
```

## Cleanup 원칙
`finish-pr.sh cleanup-merged`는 다음 조건을 증명하기 전에는 worktree나 branch를 삭제하지 않는다.

- feature worktree가 clean
- feature worktree가 locked 아님
- feature worktree `HEAD`가 검증한 PR head와 동일
- local branch가 remote보다 ahead/diverged 상태가 아님

## Jira Board Sync
- `finish-pr.sh <PR_NUMBER>`는 자동 merge하지 않는다.
- GitHub merge 후 Jira Task는 자동으로 완료 처리된다.
- `finish-pr.sh cleanup-merged <PR_NUMBER>`는 사용자의 GitHub merge, develop sync, branch cleanup, worktree cleanup이 모두 확인된 뒤 Jira 상태를 idempotent하게 기록한다.
- Jira 전환 실패는 finish 실패로 처리한다.
- 자세한 상태 매핑과 복구 절차는 `docs/operations/jira-board-sync.md`를 따른다.
