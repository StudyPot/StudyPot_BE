# EXEC_PLAN: [curriculum] 호스트 시작 시 curriculum 생성 구현

- Task slug: `spt-32-curriculum-start`
- Base branch: `develop`
- Feature branch: `codex/spt-32-curriculum-start`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-32-curriculum-start`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-32-curriculum-start`
- Jira issue: `SPT-32`
- Jira URL: https://studypot.atlassian.net/browse/SPT-32
- Jira summary: [curriculum] 호스트 시작 시 curriculum 생성 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] Jira SPT-32 issue detail
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/operations/local-development.md
- [x] OpenAI Responses API reference and Structured Outputs guide

## Related Feature IDs
- [x] curriculum-core
- [x] ai-team-leader: llm_usage persistence required by CURRICULUM_GENERATE

## Doc Notes
- Locked v1 docs map SPT-32 to `curriculum-core`, especially `REQ-CUR-001`, `REQ-CUR-002`, `QA-CUR-001`, `QA-CUR-002`, and `QA-CUR-003`.
- Jira SPT-32 acceptance requires owner-only start, `SUBMITTED` onboarding responses only in `curriculum.onboarding_summary`, group status transition to `ACTIVE`, and no automatic regeneration from late-joiner onboarding.
- The OpenAPI contract already exposes `POST /api/v1/groups/{groupId}/start` and `GET /api/v1/groups/{groupId}/curriculum`; `CurriculumResponse` contains curriculum metadata and onboarding summary, not nested weeks/tasks.
- The AI contract requires `CURRICULUM_GENERATE` to persist `curriculum`, `curriculum_week`, `weekly_task`, and `llm_usage`. External provider details are not in the locked product docs, so the implementation will use a service port and a conditional OpenAI adapter rather than hard-coding provider calls into domain/service code.
- Late-joiner onboarding, retrospective feedback, AI team leader chat, weekly progress/task completion, notification delivery, and schedule optimization remain outside this task.

## Goal
호스트가 `/api/v1/groups/{groupId}/start`를 호출하면, 현재 제출된 온보딩 응답을 기반으로 스터디 그룹을 `ACTIVE` 상태로 전환하고, 최초 `curriculum`, `curriculum_week`, `weekly_task`, `llm_usage`를 저장한 뒤 `/api/v1/groups/{groupId}/curriculum`에서 활성 커리큘럼을 조회할 수 있게 만든다.

## Approach
Add a focused `curriculum` feature slice following the current controller/service/repository/domain package style.

1. Model the curriculum aggregate, weeks, weekly tasks, submitted onboarding summary entries, and LLM usage metadata in `com.studypot.aistudyleader.curriculum.domain`.
2. Add `CurriculumService` with `startStudy` and `getCurriculum` use cases. It will validate owner-only start, reject non-ONBOARDING starts, use only `SUBMITTED` onboarding rows, delegate generation through a `CurriculumGenerator` port, and persist the start result atomically.
3. Add JDBC persistence for start context lookup, submitted onboarding response lookup, `llm_usage` insert, group `ACTIVE` transition, curriculum/week/task inserts, and active curriculum readback.
4. Add a REST controller for `POST /api/v1/groups/{groupId}/start` and `GET /api/v1/groups/{groupId}/curriculum`, preserving the locked `CurriculumResponse` shape.
5. Add a conditional OpenAI Responses API adapter behind the `CurriculumGenerator` port. Tests will inject a fake generator so unit and MVC verification do not require network access or secrets.
6. Extend global exception mapping for curriculum not-found, forbidden, conflict, generation failure, and service-unavailable cases.

## Step Plan
1. Write failing domain/service tests for successful host start from submitted onboarding, owner-only rejection, duplicate/non-ONBOARDING start rejection, and read access to an active curriculum.
2. Implement the minimal domain records/enums and service contracts needed to satisfy the service tests.
3. Write failing controller tests for authentication, `201` start response, `GET` curriculum response, and forbidden/conflict problem details.
4. Implement `CurriculumController` and exception mapping.
5. Write failing JDBC repository tests for SQL argument mapping: submitted-only onboarding lookup, `llm_usage` insert, group active transition, curriculum/week/task inserts, and active curriculum query.
6. Implement `CurriculumJdbcSql`, `JdbcCurriculumRepository`, and persistence configuration.
7. Write focused tests for the OpenAI adapter request/response parsing using a local mock HTTP surface or mocked exchange boundary; do not call the real network in tests.
8. Implement the conditional OpenAI adapter and properties.
9. Run targeted tests for curriculum/controller/repository and then `./gradlew check build --no-daemon`.
10. Create the PR through `scripts/task/create-pr.sh`, run CodeRabbit review, address one review pass if needed, rerun verification, and prepare the manual merge notification through `scripts/task/finish-pr.sh`.

## Done Criteria
- `POST /api/v1/groups/{groupId}/start` requires authentication and owner permission.
- Starting an ONBOARDING group with submitted onboarding responses creates one active curriculum, weeks, weekly tasks, and one `CURRICULUM_GENERATE` `llm_usage` record.
- `curriculum.onboarding_summary` contains only onboarding responses whose status was `SUBMITTED` at start time.
- Starting transitions `study_group.status` to `ACTIVE` and sets `started_at`.
- Re-starting an already active/started group is rejected without regenerating curriculum.
- `GET /api/v1/groups/{groupId}/curriculum` returns the active curriculum using the locked OpenAPI response shape.
- Late-joiner onboarding does not regenerate or mutate the existing curriculum.
- The OpenAI adapter is isolated behind a port and is not required for tests.
- Targeted curriculum tests and `./gradlew check build --no-daemon` pass.
- PR body and review evidence are Korean, include SPT-32, and satisfy the CodeRabbit/GitHub Actions review gate.

## Implementation Notes
- Added a new `curriculum` feature slice with domain records/enums, `CurriculumService`, REST controller, JDBC repository, and conditional OpenAI Responses API adapter.
- `POST /api/v1/groups/{groupId}/start` now delegates to `CurriculumService`, validates owner-only start, uses only submitted onboarding rows from persistence, inserts `llm_usage`, transitions the group to `ACTIVE`, and stores curriculum weeks/tasks.
- `GET /api/v1/groups/{groupId}/curriculum` returns the active curriculum using the locked `CurriculumResponse` shape. Weeks/tasks are persisted for downstream weekly-todo endpoints but are not embedded in this response because OpenAPI does not define nested week/task fields there.
- `config/application-local.example.yml` documents optional `studypot.ai.openai` settings. If no API key is configured, the OpenAI generator bean is not created and tests continue to use fake generators.

## Verification
- [x] `./gradlew test --tests com.studypot.aistudyleader.curriculum.service.CurriculumServiceTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.curriculum.controller.CurriculumControllerTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.curriculum.repository.JdbcCurriculumRepositoryTest --no-daemon`
- [x] `./gradlew test --tests com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiCurriculumGeneratorTest --no-daemon`
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.curriculum.*' --no-daemon`
- [x] `./gradlew check build --no-daemon`
