# PR Review Gate

이 문서는 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 공유하는 PR 완료 계약이다.

## 기본 흐름
1. feature 작업은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`로 시작한다.
2. 구현과 검증은 생성된 `codex/<slug>` worktree에서만 수행한다.
3. commit 전 hook이 `EXEC_PLAN`, related docs, feature id, tests, verification을 확인한다.
4. PR 생성은 `scripts/task/create-pr.sh`로 수행한다.
5. GitHub Actions가 PR 품질 검사와 reviewdog feedback을 실행하고 pass marker를 남긴다.
6. Codex review를 회사 역할 기반 gate로 수행한다. 작성자는 각 role의 actionable feedback을 수정한 뒤 다음 gate를 요청한다.
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
  - CTO Architecture / QA Verification / Product Value / Final CTO Merge checklist
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
- 최신 PR head에 대한 company role gate pass marker가 없음
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

## Company-Style Codex Review Gates
Codex review is part of the default finish gate. It follows a company-style role pipeline instead of numeric review rounds:

- CTO Architecture Gate: CTO architecture and work breakdown. Validate scope, architecture, domain/API/DB boundaries, risk, decomposition, and whether the work is ready for engineering.
- QA Verification Gate: QA browser/API verification. Validate tests, real runtime behavior when applicable, edge cases, regressions, and collision/error flows.
- Product Value Gate: Product/CBO value and retention review. Validate user value, adoption/retention impact, workflow fit, and whether follow-up product ideas should become separate tasks instead of scope creep.
- Final CTO Merge Gate: Final CTO merge approval. Re-check all previous feedback, latest evidence, unresolved threads, contract drift, and merge readiness. Treat actionable issues as merge-blocking.

Evidence is mandatory for every role gate. Each passing role gate must leave a PR comment on the latest head using one of these markers and a `## Evidence` section:

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

`finish-pr.sh` requires all four role gate markers for the latest PR head by default, and each accepted marker comment must include a `## Evidence` section. If a new commit is pushed after a gate passes, that gate must be repeated because the previous marker no longer matches the current head.

Harness/bootstrap exceptions may set `STRICT_REQUIRE_COMPANY_REVIEW_GATES=0`. Partial gates may set `STRICT_REQUIRE_COMPANY_REVIEW_GATES` to a space-separated subset, but feature work should keep the default `cto-architecture qa-verification product-value final-cto-merge`.

### Evidence Templates
Each evidence file must include `## Evidence` and the labels required for that gate.

CTO Architecture Gate:

```markdown
## Evidence
- Architecture Reviewed:
- Work Breakdown:
- Risks:
```

QA Verification Gate:

```markdown
## Evidence
- Commands Run:
- Scenarios Tested:
- Results:
```

Product Value Gate:

```markdown
## Evidence
- User Value:
- Retention Impact:
- Scope Decision:
```

Final CTO Merge Gate:

```markdown
## Evidence
- Prior Gates Checked:
- Unresolved Threads:
- Merge Decision:
```

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
