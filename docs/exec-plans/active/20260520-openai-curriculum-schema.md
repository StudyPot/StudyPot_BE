# EXEC_PLAN: [fix] OpenAI 커리큘럼 Structured Outputs 스키마 수정

- Task slug: `openai-curriculum-schema`
- Base branch: `develop`
- Feature branch: `codex/openai-curriculum-schema`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/openai-curriculum-schema`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/openai-curriculum-schema`
- Jira issue: `SPT-97`
- Jira URL: https://studypot.atlassian.net/browse/SPT-97
- Jira summary: OpenAI 커리큘럼 Structured Outputs 스키마 수정
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/confluence/06-ai-team-leader.md

## Related Feature IDs
- [x] curriculum-core
- [x] ai-team-leader

## Doc Notes
- 로컬 실제 플로우에서 `POST /api/v1/groups/{groupId}/start`가 503을 반환했고, 동일한 OpenAI Responses API 요청 형태를 직접 호출했을 때 `invalid_json_schema` 400이 확인됐다.
- OpenAI Structured Outputs는 object schema마다 `additionalProperties: false`와 모든 property를 포함한 `required` 배열을 요구한다.
- 이번 변경은 공개 API/DB/제품 출력 필드를 바꾸지 않고, 이미 정의된 커리큘럼 출력 형태를 provider가 수락하는 JSON Schema 표현으로 맞추는 버그 수정이다.

## Goal
Spring Boot 스터디 시작 시 커리큘럼 생성용 OpenAI Structured Outputs 요청이 provider schema validation을 통과하게 수정하고, 로컬 실제 플로우에서 커리큘럼 생성까지 확인한다.

## Approach
- `ProviderBackedCurriculumGenerator`의 `schemaFormat()`만 좁게 수정한다.
- 최상위, 주차, 리소스, 과제 object schema에 `additionalProperties: false`를 추가한다.
- provider 요구사항에 맞게 object schema의 `required`가 모든 property를 포함하도록 맞춘다.
- 기존 parser/domain은 같은 커리큘럼 출력 구조를 읽도록 유지한다.
- 단위 테스트에서 OpenAI 호환 schema contract를 검증한다.

## Step Plan
1. 실패 원인과 관련 문서를 기록한다.
2. 커리큘럼 생성 JSON Schema를 OpenAI Structured Outputs 요구사항에 맞게 수정한다.
3. provider-backed generator 단위 테스트에 schema contract 검증을 추가한다.
4. `./gradlew test --tests ...`와 `./gradlew check build --no-daemon`을 실행한다.
5. PR 생성, CodeRabbit review, review gate, 자동 merge/cleanup을 완료한다.
6. develop에서 로컬 실제 사용자/스터디/멤버/온보딩/start 플로우를 재검증한다.

## Done Criteria
- 실제 원인: OpenAI `invalid_json_schema`가 재현 및 기록됨.
- 커리큘럼 schema가 provider validation 요구사항을 만족함.
- 관련 단위 테스트와 전체 Gradle 검증이 통과함.
- PR review gate와 CodeRabbit marker가 통과하고 PR이 자동 merge됨.
- 새 실제 로컬 Spring Boot 스터디에서 멤버 3명 온보딩 후 `POST /start`가 성공하고 커리큘럼/현재 주차/과제가 조회됨.
