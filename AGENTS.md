# AI Study Leader Strict Workflow

이 저장소의 작업 규칙은 권고가 아니라 기본 운영 계약이다.

## 절대 규칙
- clone 이후 한 번은 `scripts/hooks/install-hooks.sh`를 실행해 훅을 활성화한다.
- 계획 없이 코드를 작성하지 않는다.
- 구현은 항상 별도 `codex/<slug>` worktree에서 진행한다.
- 구현 작업은 Jira `SPT`의 구현 Task 이슈에서 시작한다. Obsidian을 작업 큐로 사용하지 않는다.
- 시작 명령은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123` 형식이다.
- `main` 또는 `develop` checkout에서 `src/`를 직접 수정하지 않는다.
- 구현 전에는 `AGENTS.md -> ARCHITECTURE.md -> docs/index.md -> 작업 관련 docs` 순서로 읽는다.
- test 작성과 검증 실행은 건너뛰지 않는다.
- commit subject는 `[feat] 설명` 형식을 따른다.
- push와 PR 생성은 `scripts/task/create-pr.sh`를 기본 경로로 사용한다.
- merge는 PR review gate를 통과한 뒤 `scripts/task/finish-pr.sh`로만 수행한다.
- green CI만으로 merge하지 않는다. GitHub Actions Review Gate 마커와 unresolved thread 상태를 확인한다.
- v1 기획/API/DB/AI/알림/권한/QA 명세는 `LOCKED_FOR_IMPLEMENTATION` 상태다. 변경은 `docs/specs/change-control-v1.md`의 Change Request + ADR 절차 없이는 금지한다.

## 1단계: EXEC_PLAN 생성
- 시작 명령: `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`
- 이 단계는 `codex/<slug>` branch, 외부 worktree, `EXEC_PLAN`, reserved port, log directory, task state를 만든다.
- `EXEC_PLAN`은 생성된 feature worktree 안에 있어야 하며 base checkout을 dirty하게 만들면 안 된다.
- 생성된 `EXEC_PLAN`에는 아래가 비어 있으면 안 된다.
  - Required Reads
  - Related Docs
  - Related Feature IDs
  - Doc Notes
  - Goal
  - Approach
  - Step Plan
  - Done Criteria

## 2단계: Worktree에서 구현
- 생성된 worktree 디렉터리로 이동해서 작업한다.
- 읽은 문서는 반드시 `EXEC_PLAN`의 문서 섹션에 남긴다.
- feature 작업은 실제 product `feature_id`를 `Related Feature IDs`에 연결한다.
- harness 또는 infra 작업은 `n/a-harness`를 사용한다.
- `main` 또는 `develop` checkout에서는 `src/`를 직접 수정하지 않는다.

## 3단계: Test 작성
- 새 기능은 정상 동작, 엣지 케이스, 입력 검증 테스트를 포함한다.
- 버그 수정은 재현 테스트를 먼저 작성한다.
- 리팩터링은 기존 동작 확인 테스트를 포함한다.

## 4단계: 검증 실행
- 표준 검증 명령은 `./gradlew check build --no-daemon` 이다.
- 하나라도 실패하면 commit 하지 않는다.
- hook은 마지막 성공 검증 명령, 상태, 시각을 task state에 기록해야 한다.

## 5단계: PR Review Gate
- `scripts/task/create-pr.sh`는 feature branch를 push하고 PR 본문에 `EXEC_PLAN`, 검증 상태, review gate checklist를 포함한다.
- PR 본문에는 Jira issue key/URL을 포함한다.
- PR target은 `develop`을 기본으로 한다.
- GitHub Actions Review Gate가 최신 PR head에 대해 PASS comment를 남기기 전에는 merge 준비 완료로 보지 않는다.
- reviewdog/actionlint feedback과 모든 actionable review comment는 코드, 테스트, 문서 중 하나로 처리하고 review thread를 resolve한다.
- subagent review는 사용자가 명시적으로 허용한 경우에만 보조적으로 수행한다.
- 기본 review gate comment에는 아래 마커가 있어야 한다.
  - `GitHub Actions Review Gate: PASS`
  - `Head: <current_pr_head_sha>`
- `scripts/task/finish-pr.sh`는 PR head가 검증 중 바뀌지 않았는지 확인하고, clean/unlocked feature worktree만 정리한다.
- `scripts/task/finish-pr.sh`는 PR merge와 cleanup이 끝난 뒤 Jira Task를 `완료`로 전환한다.

## 관련 문서
- 구조 요약: `ARCHITECTURE.md`
- 상세 문서 허브: `docs/index.md`
- v1 변경 통제: `docs/specs/change-control-v1.md`
- Jira Board Sync: `docs/operations/jira-board-sync.md`
- PR review gate: `docs/operations/pr-review-gate.md`
- Obsidian error ledger: `docs/operations/obsidian-error-ledger.md`
