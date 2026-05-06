# Codex Harness

## 표준 검증 계약
- Codex의 기본 검증 명령은 `./gradlew check build --no-daemon` 입니다.
- commit-msg hook은 사람이 작성하는 제목을 `[type] 한글 내용` 형식으로 검증합니다.
- 허용되는 `type`은 lowercase English token인 `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `build`, `perf`, `style`, `revert` 입니다.
- stale build output 또는 파일 시스템 race가 의심되면 `./gradlew --stop` 후 `./gradlew clean check build --no-daemon`을 사용합니다.
- 외부 의존성은 가능한 한 test profile, mock, fixture, 또는 local-only 설정으로 격리합니다.

## 포함 범위
- 핵심 사용자 여정 테스트
- domain/service regression 테스트
- 구조 또는 layering guardrail 테스트
- OpenAPI parse와 ERD v0.8 MySQL8 `db-schema-coverage` 계약 테스트

## 실패 시 확인 순서
1. 현재 worktree와 branch가 `codex/<slug>`인지 확인합니다.
2. `EXEC_PLAN`의 필수 섹션과 feature id가 채워졌는지 확인합니다.
3. task state에 `JIRA_ISSUE_KEY`와 `JIRA_ISSUE_URL`이 있는지 확인합니다.
4. Jira issue가 `SPT`의 구현 Task이고 상태가 작업 단계와 맞는지 확인합니다.
5. 검증 명령이 로컬과 CI에서 같은 실패 지점을 가리키는지 비교합니다.
6. 외부 서비스, secret, local-only dependency가 테스트를 막고 있지 않은지 확인합니다.
7. PR 단계라면 `scripts/task/verify-pr-ready.sh <PR_NUMBER>`가 checks/reviews/threads를 막고 있는지 확인합니다.

## GitHub Actions Review Gate 테스트
- `PR Quality` workflow는 PR마다 하네스 테스트, ShellCheck/reviewdog, actionlint, OpenAPI parse, `db-schema-coverage`, backend check를 실행합니다.
- `CodeQL` workflow는 Java/Kotlin source가 생기면 분석을 실행하고, 구현 전에는 skip/pass 상태를 유지합니다.
- `finish-pr.sh`는 최신 PR head에 대한 `GitHub Actions Review Gate: PASS` marker가 없으면 merge하지 않습니다.
- static test는 workflow 파일 존재, required check 이름, PASS marker 문자열을 확인해야 합니다.

## 금지 사항
- CI에서 테스트를 임의로 제외하기
- 실서비스 secret을 테스트에 요구하기
- 실패한 검증을 통과한 것으로 기록하기
- review activity 없이 green CI만 보고 merge하기
- GitHub Actions Review Gate marker 없이 merge하기
- dirty 또는 locked worktree를 cleanup 대상으로 삼기
- Jira Board 전환 실패를 무시하고 작업 시작/완료 처리하기

## Jira 동기화 테스트
- `scripts/task/jira-board.sh`는 실제 Jira 대신 `STRICT_JIRA_API_STUB` 기반 fake API로 테스트합니다.
- 테스트는 다음을 확인해야 합니다.
  - `해야 할 일` 이슈가 작업 시작 시 `진행 중`으로 이동
  - 이미 `진행 중`인 이슈는 재시작 허용
  - `완료` 이슈는 작업 시작 차단
  - 전환 대상이 없으면 실패
  - 완료 처리 시 `JIRA_DONE_AT` 기록
