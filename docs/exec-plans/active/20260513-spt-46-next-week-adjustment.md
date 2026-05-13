# EXEC_PLAN: [retrospective] 다음 주 조정 매핑 구현

- Task slug: `spt-46-next-week-adjustment`
- Base branch: `develop`
- Feature branch: `codex/spt-46-next-week-adjustment`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-46-next-week-adjustment`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-46-next-week-adjustment`
- Jira issue: `SPT-46`
- Jira URL: https://studypot.atlassian.net/browse/SPT-46
- Jira summary: [retrospective] next_week_adjustment 매핑 구현
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-requests/CR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md

## Related Feature IDs
- [x] retrospective-feedback
- [x] ai-team-leader

## Doc Notes
- Locked v1 AI contract requires `RETROSPECTIVE_FEEDBACK` to output both feedback and `next_week_adjustment`, persisted through `retrospective` and `llm_usage`.
- API contract already exposes `RetrospectiveResponse.nextWeekAdjustment`; SPT-46 must not add or rename REST fields.
- DB contract already contains `retrospective.next_week_adjustment` JSON; no migration or schema change is expected.
- QA requires AI retrospective feedback with next-week adjustment and JSON storage using DB-first context.
- CR/ADR 20260512 keeps this inside Spring Boot DB-first context and explicitly defers Vector DB, GraphRAG, MCP, FastAPI, and agent orchestration.

## Goal
Ensure the AI retrospective result's `nextWeekAdjustment` is treated as a required structured provider output, mapped into `RetrospectiveFeedbackResult`, persisted to `retrospective.next_week_adjustment`, and returned by the existing retrospective APIs without API or DB contract changes.

## Approach
Add focused regression coverage around the existing retrospective feedback path:
- Domain: validate accepted `nextWeekAdjustment` keys and reject unsupported, blank, or malformed nested values.
- Provider adapter: reject provider JSON that omits or malforms `nextWeekAdjustment` so the service records failed generation instead of completing with an empty adjustment.
- Service/controller/repository: keep the existing mapping path from provider result -> completed retrospective -> `next_week_adjustment` SQL argument -> response body, and add missing assertions where coverage is weak.
- Keep permissions and provider abstraction behavior from earlier SPT slices unchanged.

## Step Plan
1. Add/adjust tests that first expose missing strictness in provider output parsing for `nextWeekAdjustment`.
2. Implement the smallest production change needed to reject missing or invalid adjustment output.
3. Strengthen repository/controller/service assertions only where SPT-46 behavior is not already locked.
4. Run targeted retrospective tests.
5. Run `./gradlew check build --no-daemon`.
6. Commit, create PR, run CodeRabbit review, address one review loop if needed, and run PR readiness gate.

## Done Criteria
- Provider-backed retrospective feedback rejects missing or malformed `nextWeekAdjustment` and surfaces `RETROSPECTIVE_RESPONSE_INVALID`.
- Successful retrospective generation persists a non-empty `nextWeekAdjustment` in the completed retrospective.
- Repository test proves `next_week_adjustment` is written on result update and read SQL includes the column.
- Controller test proves completed retrospective responses include `nextWeekAdjustment`.
- No API path/field or DB schema changes are introduced.
- `./gradlew check build --no-daemon` passes.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.service.ProviderBackedRetrospectiveFeedbackGeneratorTest' --no-daemon` failed before production change because missing `nextWeekAdjustment` was accepted.
- GREEN targeted: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.service.ProviderBackedRetrospectiveFeedbackGeneratorTest' --no-daemon`
- GREEN package: `./gradlew test --tests 'com.studypot.aistudyleader.retrospective.*' --no-daemon`
- GREEN standard: `./gradlew check build --no-daemon`
