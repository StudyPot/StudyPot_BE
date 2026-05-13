# EXEC_PLAN: [retrospective] 미완료 사유 기반 AI 피드백 생성 구현

- Task slug: `spt-44-retrospective-ai`
- Base branch: `develop`
- Feature branch: `codex/spt-44-retrospective-ai`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-44-retrospective-ai`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-44-retrospective-ai`
- Jira issue: `SPT-44`
- Jira URL: https://studypot.atlassian.net/browse/SPT-44
- Jira summary: [retrospective] 미완료 사유 기반 AI 피드백 생성 구현
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md

## Related Feature IDs
- [x] retrospective-feedback
- [x] ai-team-leader

## Doc Notes
- SPT-44 maps to `retrospective-feedback` and uses the already-approved DB-first context boundary for `RETROSPECTIVE_FEEDBACK`.
- No public API path, enum, request/response field, table, or migration change is allowed in this slice.
- Context may include the authenticated member's own progress, task completions, incomplete reasons, relevant group rules/violations, prior retrospective feedback/adjustment, onboarding summary, and retrospective-linked conversation summary.
- `llm_usage.request_payload` must remain redacted metadata, not raw private notes, OAuth data, provider credentials, or tokens.

## Goal
When an active member requests their own retrospective, generate AI feedback from DB-first retrospective context through the Spring Boot LLM provider abstraction, persist the successful or failed LLM usage audit, and store the resulting `ai_feedback` JSON on the existing `retrospective` record without changing the locked REST/DB contracts.

## Approach
- Add a provider-backed retrospective feedback generator that sends `RETROSPECTIVE_FEEDBACK` structured requests and parses the locked AI output shape into `RetrospectiveFeedbackResult`.
- Extend retrospective context collection beyond progress/tasks to include group/rule/violation, prior retrospective, onboarding, and retrospective conversation summary metadata through repository queries.
- Update `RetrospectiveService` so a new or retriable retrospective moves through processing to either COMPLETED with AI feedback or FAILED with a safe error summary and failed `llm_usage`.
- Keep existing idempotent read behavior for already COMPLETED or PROCESSING retrospectives; do not create duplicate retrospective records.
- Cover provider success, provider failure, invalid provider output, context privacy/redaction, and repository SQL additions with focused tests.

## Step Plan
1. Add RED service/generator tests for DB-first context, successful provider response, failed provider response, and invalid response handling.
2. Add repository contract tests for context SQL: own member context, rules/violations limited to the member/group, prior retrospective, onboarding summary, and conversation summary.
3. Implement retrospective feedback generator and wire it conditionally with existing `LlmProviderClient`/`LlmUsageRecorder`.
4. Extend `RetrospectiveRepository`/JDBC SQL with context queries and processing/result update support.
5. Update controller tests only if the response status body changes from PENDING to COMPLETED/FAILED under configured provider.
6. Run targeted retrospective/LLM tests, then `./gradlew check build --no-daemon`.
7. Create PR with `scripts/task/create-pr.sh`, run CodeRabbit review, address valid feedback once, and finish PR readiness.

## Done Criteria
- `POST /api/v1/weeks/{weekId}/retrospectives/me` for an active member can invoke the configured provider and return/store a COMPLETED retrospective with `aiFeedback`.
- Provider success records `llm_usage` with purpose `RETROSPECTIVE_FEEDBACK`, provider/model/token/latency/status/cost metadata, redacted request payload, and response summary.
- Provider call failure or invalid JSON records failed `llm_usage`, updates the same retrospective to FAILED, and does not fabricate successful feedback.
- DB-first context includes own progress/task completion/incomplete reason data plus allowed group/rule/violation/prior/onboarding/conversation summary metadata without exposing cross-member raw private notes.
- Existing GET response returns the stored `aiFeedback`; `nextWeekAdjustment` remains mapped but SPT-46 owns full next-week-adjustment persistence behavior.
- Targeted tests and `./gradlew check build --no-daemon` pass.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.service.RetrospectiveServiceTest' --tests 'com.studypot.aistudyleader.retrospective.service.ProviderBackedRetrospectiveFeedbackGeneratorTest' --tests 'com.studypot.aistudyleader.retrospective.repository.JdbcRetrospectiveRepositoryTest' --no-daemon` failed on missing SPT-44 generator/context types before implementation.
- GREEN: same targeted command passed after adding provider-backed retrospective feedback generation, DB-first context queries, and usage/state persistence.
- Broader: `./gradlew test --tests '*Retrospective*' --tests '*LlmUsage*' --tests 'com.studypot.aistudyleader.ApplicationFeatureWiringTest' --no-daemon` passed.
- Standard: `./gradlew check build --no-daemon` passed.
