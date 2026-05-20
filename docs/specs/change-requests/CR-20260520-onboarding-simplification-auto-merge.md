# CR-20260520-onboarding-simplification-auto-merge

## Status
- Approved

## Request
- Replace the public onboarding draft/save-then-submit flow with one submit request that accepts only overall `skillLevel`, `additionalNote`, and `availabilitySlots`.
- Remove public onboarding request/response fields `keywordSkillLevels` and `taskPreferences`.
- Change the PR finish harness so `scripts/task/finish-pr.sh <PR_NUMBER>` automatically merges a review-gate-passed PR and then runs safe cleanup.

## Reason
- Swagger-based onboarding testing is too granular when a user only needs to state overall skill, extra notes, and available time.
- The existing public API exposes internal curriculum context details before they are useful to users.
- The current manual merge stop is no longer the desired StudyPot workflow; the user approved Codex-managed merge after review gates.

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

## Impact
- Product: onboarding becomes a one-step submission with a simpler user mental model.
- API: `POST /api/v1/groups/{groupId}/onboarding/me` accepts a simplified body and returns submitted onboarding; draft save and submit subresource are removed from the public contract.
- DB: no schema change; the backend maps `skillLevel` to existing keyword score JSON and stores an empty task preference JSON object.
- AI: initial curriculum and retrospective context continue to receive submitted onboarding summaries through existing DB-backed paths.
- Notification: no notification type or delivery behavior changes.
- Permissions: group member access remains unchanged; pending members can submit onboarding.
- QA: onboarding acceptance focuses on one-step submit, 1-to-5 skill validation, availability validation, and duplicate-submission rejection.

## Compatibility
- Backward compatible: no for the public onboarding API shape.
- Migration required: no DB migration; existing stored rows remain readable.

## Decision
- Approved by: Product owner request in Codex session
- Date: 2026-05-20
- Linked ADR: [ADR-20260520-onboarding-simplification-auto-merge](../adr/ADR-20260520-onboarding-simplification-auto-merge.md)
