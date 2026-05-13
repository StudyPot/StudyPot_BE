# EXEC_PLAN: [ai-curriculum] 세부 키워드 추천/커리큘럼 생성 호출 구현

- Task slug: `spt-43-ai-curriculum`
- Base branch: `develop`
- Feature branch: `codex/spt-43-ai-curriculum`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-43-ai-curriculum`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-43-ai-curriculum`
- Jira issue: `SPT-43`
- Jira URL: https://studypot.atlassian.net/browse/SPT-43
- Jira summary: [ai-curriculum] 세부 키워드 추천/커리큘럼 생성 호출 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/product-brief.md
- [x] docs/specs/prd-v1.md
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] curriculum-core
- [x] ai-team-leader

## Doc Notes
- Jira SPT-43 goal: 그룹 생성 시 세부 키워드 후보 추천과 호스트 시작 시 커리큘럼 생성 LLM 호출을 구현한다.
- Jira SPT-43 acceptance: 세부 키워드 후보는 응답으로만 제공하고 최종 선택값만 저장한다. 호스트 시작 시 제출된 온보딩 응답 요약을 prompt 입력으로 사용한다. 생성된 주차와 todo를 검증된 shape으로 저장한다.
- Linked SPT-16 MVP scope: AI detail keyword candidates are transient and not persisted; selected/direct detail keywords and submitted onboarding responses drive the initial curriculum.
- `docs/specs/ai-contract-v1.md` defines `DETAIL_KEYWORD_SUGGEST` and `CURRICULUM_GENERATE`, and requires every AI call to create `llm_usage`.
- `docs/specs/api-contract-v1.md` and `docs/specs/openapi.yaml` expose `POST /api/v1/groups/{groupId}/start` for curriculum generation, but no public detail-keyword suggestion endpoint is locked.
- Because v1 API docs are `LOCKED_FOR_IMPLEMENTATION`, this PR must not invent a new REST path for detail keyword suggestions. SPT-43 will add an internal provider port/use case for suggestions and keep candidates non-persistent; exposing it publicly requires a Change Request + ADR.
- `docs/specs/db-schema-v1.sql` already has `curriculum.llm_usage_id`, curriculum/week/task tables, and `llm_usage`; no DB migration is planned.
- `docs/specs/auth-permissions-v1.md` requires only owner to start study and active members to read curriculum/current week.
- SPT-42 introduced common `llm` domain and owner-only usage log reads; SPT-43 should reuse that domain instead of adding another usage shape.
- Existing OpenAI code is curriculum-specific. SPT-43 should move the provider boundary toward a reusable Spring Boot LLM provider abstraction so later SPT-44/45/46 can reuse the same port.

## Goal
Build a reusable Spring Boot LLM provider abstraction for curriculum/detail-keyword AI calls, connect host-start curriculum generation through that abstraction, and preserve the existing curriculum persistence flow with successful and failed LLM usage audit records. Keep detail keyword suggestions transient and non-persistent; do not add a public suggestion API without CR/ADR approval.

## Approach
1. Introduce a reusable `llm` provider port that accepts purpose, instructions, JSON input, and expected structured-output schema, and returns provider/model/token/latency/status/text metadata without exposing provider secrets.
2. Refactor the OpenAI Responses adapter to implement the generic provider port, while preserving existing OpenAI configuration via environment-bound properties only.
3. Refactor curriculum generation into a provider-backed generator that builds the locked curriculum schema prompt, parses/validates output, and returns `CurriculumGeneration`.
4. Add an internal detail keyword suggestion generator/use case through the same provider port; candidates are returned by the use case and are not persisted.
5. Ensure curriculum generation failure records failed `llm_usage` without activating the group or creating fake curriculum/week/task records.
6. Keep public OpenAPI unchanged in this PR. If a product-visible keyword suggestion endpoint is needed, create CR/ADR first.
7. Follow TDD: write failing provider/curriculum/detail-keyword/failure tests first, verify RED, then implement.

## Step Plan
1. [x] Add RED tests for the generic LLM provider request/response contract and OpenAI adapter mapping.
2. [x] Add RED service tests proving curriculum start calls the provider-backed generator with submitted onboarding summary and persists successful `llm_usage` + curriculum/week/task records.
3. [x] Add RED failure tests proving provider failure records failed `llm_usage`, keeps group `ONBOARDING`, and does not persist fake curriculum/week/task records.
4. [x] Add RED tests for transient detail keyword suggestion generation: request uses topic/hints, response returns candidates/rationale, records `llm_usage`, and persists no candidate rows.
5. [x] Implement the `llm` provider port, OpenAI adapter, curriculum generator refactor, and failed-usage persistence boundary.
6. [x] Update wiring so no OpenAI provider is registered without API key and fake provider can be injected in tests.
7. [x] Run targeted tests, then `./gradlew check build --no-daemon`.
8. [ ] Create PR, run CodeRabbit review once, address valid feedback once, finish gate, wait for manual merge, and cleanup.

## Implementation Notes
- RED observed: targeted new tests failed at `compileTestJava` because `LlmProviderClient`, `LlmStructuredRequest`, `LlmStructuredResponse`, `LlmCallFailure`, and `LlmProviderCallException` did not exist yet.
- Added reusable `llm.service` provider port and audit failure model, then wired OpenAI Responses through `OpenAiLlmProvider`.
- Removed the old curriculum-only `OpenAiCurriculumGenerator`; curriculum generation now uses `ProviderBackedCurriculumGenerator` through the generic provider port.
- Added internal `DetailKeywordSuggestionService` with no controller and no candidate persistence, while still recording `DETAIL_KEYWORD_SUGGEST` in `llm_usage`.
- Added failed `llm_usage` persistence for provider-backed curriculum failures through `CurriculumRepository.saveFailedLlmUsage`.

## Verification
- `./gradlew test --tests 'com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest.startStudyRecordsFailedLlmUsageWhenGeneratorFailsAfterProviderCall' --tests 'com.studypot.aistudyleader.curriculum.service.ProviderBackedCurriculumGeneratorTest' --tests 'com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionServiceTest' --tests 'com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest' --no-daemon` passed.
- `./gradlew test --tests '*Curriculum*' --tests '*OpenAi*' --tests '*StudyGroup*' --tests '*LlmUsage*' --tests 'com.studypot.aistudyleader.ApplicationFeatureWiringTest' --no-daemon` passed.
- `./gradlew test --tests 'com.studypot.aistudyleader.architecture.*' --no-daemon` passed.
- `./gradlew check build --no-daemon` passed.

## Done Criteria
- `CURRICULUM_GENERATE` uses a reusable Spring Boot LLM provider abstraction instead of a curriculum-only OpenAI implementation.
- OpenAI provider configuration still reads API key/model/base URL/timeouts from configuration or environment only; no secret appears in code/tests/docs.
- Host start sends submitted onboarding summary and group topic/detail keywords to the provider-backed generator.
- Valid provider output creates `curriculum`, `curriculum_week`, `weekly_task`, and successful `llm_usage` with provider/model/token/latency/status/cost/summary/redacted payload.
- Invalid provider output is rejected or mapped to generation failure; no fake curriculum is created.
- Provider call failure records failed `llm_usage`, leaves the study group `ONBOARDING`, and does not create curriculum/week/task rows.
- Detail keyword suggestion has an internal provider-backed use case that returns suggestions/rationale and does not persist candidate keywords.
- No public detail keyword suggestion endpoint or OpenAPI change is added without CR/ADR.
- Related controller/service/repository/provider tests pass, including success, validation, permission, and failure boundaries relevant to this slice.
- `./gradlew check build --no-daemon` passes.
- PR includes Jira `SPT-43`, EXEC_PLAN, verification evidence, CodeRabbit marker, and review gate evidence.
