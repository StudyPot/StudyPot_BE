# EXEC_PLAN: [llm-usage] LLM 호출/비용 추적 구현

- Task slug: `spt-42-llm-usage-llm`
- Base branch: `develop`
- Feature branch: `codex/spt-42-llm-usage-llm`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-42-llm-usage-llm`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-42-llm-usage-llm`
- Jira issue: `SPT-42`
- Jira URL: https://studypot.atlassian.net/browse/SPT-42
- Jira summary: [llm-usage] LLM 호출/비용 추적 구현
- Status: `review-fixes`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] ai-team-leader
- [x] curriculum-core
- [x] retrospective-feedback

## Doc Notes
- Jira SPT-42 goal: 세부 키워드 추천, 커리큘럼 생성, AI 팀장 대화, 회고 분석에 대한 LLM 호출 기록을 `llm_usage`에 저장한다.
- Jira SPT-42 acceptance: `DETAIL_KEYWORD_SUGGEST`, `CURRICULUM_GENERATE`, `CURRICULUM_REGENERATE_WEEK`, `TEAM_LEAD_CHAT`, `RETROSPECTIVE_ANALYZE`, `NEXT_WEEK_ADJUST` purpose 지원, token/latency/cost/status/request-response summary 저장, 사용자/그룹 단위 조회 가능.
- `docs/specs/ai-contract-v1.md`는 `RETROSPECTIVE_FEEDBACK` purpose도 잠긴 계약으로 사용한다. SPT-42에서는 Jira acceptance와 locked AI contract를 모두 수용하도록 purpose enum에 포함한다.
- `docs/specs/db-schema-v1.sql`의 `llm_usage`는 이미 `provider`, `model`, token, cost, latency, status, error, `request_payload`, `response_summary`, UTC date index를 포함한다. DB 컬럼/enum/마이그레이션 변경은 하지 않는다.
- `docs/specs/openapi.yaml`에는 `GET /api/v1/groups/{groupId}/llm-usage`와 제한된 `LlmUsageResponse` shape가 이미 있다. public response field를 새로 늘리지 않는다.
- `docs/specs/auth-permissions-v1.md`는 LLM usage log 조회를 owner 전용으로 정의한다. SPT-42는 이 endpoint에 owner-only read boundary를 둔다.
- provider abstraction, 실제 chat/retrospective provider 호출, curriculum provider 재설계는 SPT-43/44 범위로 남긴다. SPT-42는 저장/조회/감사 데이터 경계를 만든다.

## Goal
`llm_usage`를 커리큘럼/AI 대화/회고 provider 흐름이 공통으로 사용할 수 있는 감사 로그 도메인으로 만들고, 안전하게 redacted request payload를 저장하며, 그룹 owner가 잠긴 OpenAPI shape로 그룹 LLM usage 기록을 조회할 수 있게 한다.

## Approach
1. 기존 `curriculum.domain.LlmUsage`, `LlmProvider`, `LlmUsageStatus`를 공용 `llm.domain`으로 이동하고 `LlmUsagePurpose`를 추가한다.
2. `LlmUsage` 생성 시 token/cost/latency/status를 검증하고, `requestPayload`는 민감 키/긴 문자열을 redaction/truncation 처리한다.
3. `llm_usage` 전용 repository/service/controller를 추가해 owner-only 그룹 조회를 구현한다.
4. 기존 curriculum start 저장 경로는 새 공용 `LlmUsage` 도메인을 사용하도록 업데이트해 회귀 없이 유지한다.
5. repository/service/controller/domain 테스트를 먼저 추가해 실패를 확인한 뒤 구현한다.

## Step Plan
1. [x] LLM usage 도메인 테스트를 추가한다: purpose enum, negative token/cost/latency rejection, secret/token/OAuth key redaction, long private note truncation.
2. [x] LLM usage service/controller 테스트를 추가한다: 인증 필요, owner 조회 성공, non-owner/LEFT/cross-group 거부, group not found 처리.
3. [x] LLM usage repository 테스트를 추가한다: insert SQL 매핑, group/user date query, row mapping, JSON serialization/deserialization.
4. [x] failing test를 확인한 뒤 공용 `llm` domain/repository/service/controller/configuration을 구현한다.
5. [x] 기존 curriculum import와 테스트를 새 공용 도메인으로 정리한다.
6. [x] targeted test와 `./gradlew check build --no-daemon`을 통과시킨다.
7. [ ] PR 생성 완료, CodeRabbit feedback 수정, review gate, Mattermost manual merge 알림까지 진행한다.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.llm.*' --no-daemon` failed at `compileTestJava` with missing `llm` domain/repository/service/controller classes before implementation.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.llm.*' --no-daemon` passed after implementation and payload null-handling hardening.
- Regression: `./gradlew test --tests 'com.studypot.aistudyleader.ApplicationFeatureWiringTest' --tests 'com.studypot.aistudyleader.llm.controller.LlmUsageControllerTest' --no-daemon` passed after aligning persistence configuration with existing datasource-gated repository configuration.
- Full: `./gradlew check build --no-daemon` passed on 2026-05-13.

## Review Notes
- CodeRabbit status/step-plan mismatch: valid. Status updated to `review-fixes` while PR review gate remains in progress.
- CodeRabbit negative output token test gap: valid. Added explicit output token validation assertion.
- CodeRabbit repository sort contract: valid. Documented fixed newest-first ordering in `LlmUsageRepository`; SQL already orders by `created_at desc, id desc`.
- CodeRabbit `@Service` suggestion: not applied. This codebase wires services through `*ApplicationConfiguration` beans rather than service component scanning, and `LlmUsageApplicationConfiguration` already registers `LlmUsageService`.
- CodeRabbit migration concern: not applied. Existing curriculum path already wrote `CURRICULUM_GENERATE` as a string, so `usage.purpose().name()` preserves the stored value without a data migration.
- CodeRabbit non-JWT authentication fallback: not applied in this slice. The fallback matches existing controller pattern and supports current Spring Security MVC tests that pass the UUID through `authentication.getName()`.

## Done Criteria
- SPT-42/Jira 목적값과 locked AI contract 목적값이 `LlmUsagePurpose`로 표현된다.
- `llm_usage`에 provider, model, token, latency, status, cost/error, redacted request payload, response summary, UTC created date가 저장된다.
- request payload는 token/secret/OAuth/API key/authorization/cookie/password/credential 계열 민감 키를 저장하지 않는다.
- 긴 private note류 문자열은 감사 가능 범위로 잘려 저장된다.
- owner는 `GET /api/v1/groups/{groupId}/llm-usage`로 자기 그룹 usage 기록을 조회할 수 있고, non-owner/LEFT/cross-group 사용자는 거부된다.
- repository는 group/user/date 기준 조회를 제공해 사용자/그룹 단위 usage 확인이 가능하다.
- 기존 curriculum generation 저장 경로는 새 공용 LLM usage 도메인으로 동일하게 동작한다.
- 실제 provider 추상화 변경, chat/retrospective provider 호출 연결, next-week adjustment 적용은 이 PR에서 구현하지 않는다.
- 관련 테스트와 `./gradlew check build --no-daemon`이 통과한다.
- PR에는 Jira `SPT-42` 링크, 검증 결과, review gate 증거가 포함된다.
