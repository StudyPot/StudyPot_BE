# 09 QA / Acceptance Criteria

## Required Scenarios
- Create group -> host onboarding -> invite member -> member onboarding -> host start.
- Host start with partial onboarding completion.
- Current week task completion.
- Overdue incomplete reason capture.
- AI retrospective and next-week adjustment.
- AI conversation and LLM usage logging.
- In-app notification idempotency and read-state update.

## Verification
- OpenAPI YAML parses.
- DB schema includes all ERD v0.8 entities.
- Jira labels do not include `erd-v06`, `erd-v07`, or `meeting`.
- `./gradlew check build --no-daemon` passes before merge.

## Source
- `docs/specs/qa-acceptance-v1.md`
