# 09 QA / Acceptance Criteria

## Required Scenarios
- Create group -> host onboarding -> invite member -> member onboarding -> host start.
- Host start with partial onboarding completion.
- Current week task completion.
- Overdue incomplete reason capture.
- Task completion frontend response fields for done, incomplete, skipped, repeated done, pending-member rejection, and cross-group rejection.
- AI retrospective and next-week adjustment.
- AI conversation and LLM usage logging.
- AI conversation message-list recovery and SSE lifecycle events.
- In-app notification idempotency and read-state update.
- In-app notification SSE subscribe, recipient-only delivery, cross-group recipient delivery, send-failure isolation, and disconnect cleanup.

## Verification
- OpenAPI YAML parses.
- DB schema includes all ERD v0.8 entities.
- Jira labels do not include `erd-v06`, `erd-v07`, or `meeting`.
- `./gradlew check build --no-daemon` passes before merge.

## Source
- `docs/specs/qa-acceptance-v1.md`
