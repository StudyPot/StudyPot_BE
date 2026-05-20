# ADR-20260520-onboarding-simplification-auto-merge

## Status
- Approved

## Context
- Locked v1 onboarding exposed draft save plus submit, and required users to provide keyword-level skill maps and task preference maps.
- The current product flow needs a Swagger-testable onboarding form that asks only for overall skill, additional notes, and available days/times.
- The locked harness contract stopped after review gates and asked the user to manually click merge on GitHub.
- The user has now approved Codex-managed automatic merge after the review gates pass.

## Decision
- Public onboarding submission uses `POST /api/v1/groups/{groupId}/onboarding/me` with `skillLevel`, `additionalNote`, and `availabilitySlots`.
- The response exposes `skillLevel`, `additionalNote`, `availabilitySlots`, `status`, and `submittedAt`; it does not expose internal `keywordSkillLevels` or `taskPreferences`.
- The backend keeps the existing DB schema for this slice. It maps the public skill level to every group detail keyword internally and stores task preferences as `{}`.
- A member that already submitted onboarding receives a conflict instead of mutating the submitted response.
- `scripts/task/finish-pr.sh <PR_NUMBER>` becomes the auto-merge finish path: verify latest-head gates, merge the PR, then perform safe local cleanup and Jira recording.
- `scripts/task/finish-pr.sh cleanup-merged <PR_NUMBER>` remains as a recovery path for externally merged or interrupted PRs.

## Consequences
- Positive: the onboarding flow matches the intended user input and Swagger can exercise it in one call.
- Positive: curriculum and retrospective code can continue reading the existing onboarding JSON columns without a migration.
- Positive: PR finish work is fully agent-owned after CodeRabbit and GitHub Actions review gates pass.
- Negative: clients using the old draft endpoints must move to the simplified submit endpoint.
- Negative: internal keyword-level skill data becomes a projection of the overall skill level until a later product decision reintroduces finer-grained input.
- Migration or compatibility notes: no DB migration is required; existing rows with detailed maps still read successfully, and API reads derive `skillLevel` from stored keyword scores.

## Affected Feature IDs
- `group-onboarding`
- `n/a-harness`

## Affected Documents
- `docs/specs/change-control-v1.md`
- `docs/specs/requirements-v1.md`
- `docs/specs/api-contract-v1.md`
- `docs/specs/openapi.yaml`
- `docs/specs/qa-acceptance-v1.md`
- `docs/specs/feature-coverage-matrix.md`
- `docs/confluence/02-requirements.md`
- `docs/confluence/05-api-spec.md`
- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/index.md`
- `docs/testing/codex-harness.md`
- `docs/operations/pr-review-gate.md`
- `docs/operations/jira-board-sync.md`
- `docs/operations/github-actions-review-gate.md`
- `docs/quality/scorecard.md`

## Linked Change Request
- [CR-20260520-onboarding-simplification-auto-merge](../change-requests/CR-20260520-onboarding-simplification-auto-merge.md)
