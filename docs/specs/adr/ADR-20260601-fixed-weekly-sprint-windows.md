# ADR-20260601 Fixed Weekly Sprint Windows

## Status
- Approved

## Context
- Study groups already have date-only `starts_at` and `ends_at` fields, and curriculum weeks already persist `week_number`, `starts_at`, and `ends_at`.
- The implementation previously let the AI response decide how many weeks existed, and generated 7-day week windows from the host-start timestamp.
- The product decision for SPT-124 is to use one-week sprint units now and defer configurable sprint duration.
- The v1 package is locked, so changing the AI generation behavior requires CR/ADR approval even though no API or DB schema changes are needed.

## Decision
- At host start, compute deterministic fixed one-week sprint windows from `study_group.starts_at` through `study_group.ends_at`.
- Treat `study_group.ends_at` as an inclusive date and cap the final sprint at the exclusive boundary of `ends_at + 1 day`.
- Store sprint boundaries at UTC day boundaries in `curriculum_week.starts_at` and `curriculum_week.ends_at`.
- Set weekly task `due_at` to the planned sprint window `ends_at`.
- Pass the sprint plan to the curriculum-generation provider request.
- Reject provider output unless `totalWeeks` and the week array count match the planned sprint count and week numbers are sequential.
- Keep configurable sprint duration, sprint-calendar preferences, timezone customization, and manual sprint editing out of this task.

## Consequences
- Positive: Week numbers and due dates become deterministic from the study period.
- Positive: AI content can no longer silently shrink or expand the study schedule.
- Positive: The change reuses existing `curriculum`, `curriculum_week`, and `weekly_task` tables.
- Negative: The final sprint can be shorter than one week when the study period is not divisible by seven days.
- Negative: Users cannot choose two-week or custom sprint durations yet.
- Migration or compatibility notes: No migration is required. Existing endpoints keep their current shapes.

## Affected Feature IDs
- `curriculum-core`
- `weekly-todo`
- `ai-team-leader`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/user-journeys-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/db-contract-v1.md`
- `docs/specs/ai-contract-v1.md`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/02-requirements.md`
- `docs/confluence/04-erd-data-model.md`
- `docs/confluence/05-api-spec.md`
- `docs/confluence/09-qa-acceptance.md`

## Linked Change Request
- [CR-20260601-fixed-weekly-sprint-windows](../change-requests/CR-20260601-fixed-weekly-sprint-windows.md)
