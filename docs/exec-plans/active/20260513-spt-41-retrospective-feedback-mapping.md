# EXEC_PLAN: [retrospective] AI 팀장 피드백 결과 매핑 구현

- Task slug: `spt-41-retrospective-feedback-mapping`
- Base branch: `develop`
- Feature branch: `codex/spt-41-retrospective-feedback-mapping`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-41-retrospective-feedback-mapping`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-41-retrospective-feedback-mapping`
- Jira issue: `SPT-41`
- Jira URL: https://studypot.atlassian.net/browse/SPT-41
- Jira summary: [retrospective] AI 팀장 피드백 결과 매핑 구현
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] retrospective-feedback
- [x] ai-team-leader

## Doc Notes
- SPT-41 Jira goal: AI 팀장 회고 결과를 `retrospective.ai_feedback`과 사용자 응답 모델로 매핑한다.
- SPT-41 Jira acceptance: 완료/미완료 사유와 대화 요약 기반 피드백, `next_week_adjustment` JSON shape 검증, 실패 시 상태와 오류 안전 기록.
- `docs/specs/ai-contract-v1.md`는 `RETROSPECTIVE_FEEDBACK` 출력으로 feedback과 next-week adjustment를 정의하고, 실패 시 `retrospective.status = FAILED`로 재시도 가능하게 남긴다.
- SPT-42는 `llm_usage`, SPT-44는 미완료 사유 기반 실제 AI 피드백 생성, SPT-46은 next-week adjustment 세부 매핑이다. 따라서 SPT-41은 provider 호출을 만들지 않고 AI 결과를 받아 retrospective에 반영하는 매핑/상태 전이 경계를 만든다.
- v1 OpenAPI `RetrospectiveResponse`는 `id`, `status`, `aiFeedback`, `nextWeekAdjustment`만 노출한다. 새 public API field나 endpoint는 추가하지 않는다.

## Goal
후속 AI/provider 단계가 산출한 회고 결과를 검증된 `ai_feedback`/`next_week_adjustment` JSON으로 매핑하고, 성공/실패 상태를 `retrospective`에 안전하게 저장할 수 있는 도메인, 서비스, 저장소 경계를 구현한다.

## Approach
1. `RetrospectiveFeedbackResult` 도메인 값을 추가해 summary, strengths, risks, actionItems, next-week adjustment shape을 검증한다.
2. `Retrospective`에 feedback 완료/실패 상태 전이 메서드를 추가한다.
3. `RetrospectiveService`에 내부 use case를 추가해 retrospective를 조회한 뒤 성공 결과 또는 실패 요약을 저장한다.
4. `RetrospectiveRepository`에 retrospective 단건 조회와 결과 업데이트 메서드를 추가한다.
5. Controller 응답은 기존 OpenAPI shape을 유지하되, completed retrospective의 구조화 JSON이 그대로 노출되는지 테스트한다.
6. 실제 provider 호출, LLM usage 생성, 미완료 사유 기반 prompt 생성, curriculum_week/weekly_task 적용은 후속 SPT-42/44/46 범위로 남긴다.

## Step Plan
1. 기존 retrospective 생성/조회/저장 테스트 구조를 확인한다.
2. 도메인/서비스/저장소 테스트를 먼저 추가하고 실패를 확인한다.
3. 피드백 결과 값 객체, retrospective 상태 전이, repository update SQL, service use case를 구현한다.
4. 컨트롤러 응답 회귀 테스트를 보강한다.
5. targeted test와 `./gradlew check build --no-daemon`을 통과시킨다.
6. PR 생성, CodeRabbit review, review gate, Mattermost merge-ready 알림까지 진행한다.

## Done Criteria
- AI 회고 결과 payload가 `summary`, `strengths`, `risks`, `actionItems`를 포함한 `ai_feedback` JSON으로 매핑된다.
- `next_week_adjustment`는 허용된 top-level shape만 저장하고 잘못된 값 타입은 거부된다.
- 성공 매핑 시 retrospective 상태가 `COMPLETED`가 되고 `completedAt`, `updatedAt`, `llmUsageId`가 반영된다. SPT-41은 `llmUsageId`를 받아 저장하는 경계만 구현하며, 실제 `llm_usage` 생성과 값 제공은 SPT-42 범위다.
- 실패 매핑 시 retrospective 상태가 `FAILED`가 되고 사용자/운영자가 볼 수 있는 안전한 오류 요약이 저장되며 재시도 가능 상태를 유지한다.
- `GET /api/v1/weeks/{weekId}/retrospectives/me`는 기존 OpenAPI 응답 shape 안에서 저장된 `aiFeedback`/`nextWeekAdjustment`를 반환한다.
- 실제 AI provider 호출, `llm_usage` 생성, 미완료 사유 기반 prompt 생성, 주차/todo 조정 적용은 이 PR에서 구현하지 않는다.
- 관련 테스트와 `./gradlew check build --no-daemon`이 통과한다.
- PR에는 Jira `SPT-41` 링크, 검증 결과, review gate 증거가 포함된다.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.*' --no-daemon` failed at `compileTestJava` because `RetrospectiveFeedbackResult` did not exist yet.
- GREEN: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.*' --no-daemon` passed.
- Full: `./gradlew check build --no-daemon` passed.
- CodeRabbit addressed: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.*' --no-daemon` passed.
- CodeRabbit addressed full: `./gradlew check build --no-daemon` passed.
