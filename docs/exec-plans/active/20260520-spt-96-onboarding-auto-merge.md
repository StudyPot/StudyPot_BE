# EXEC_PLAN: [feat] 온보딩 단순화 및 자동 merge 하네스 전환

- Task slug: `spt-96-onboarding-auto-merge`
- Base branch: `develop`
- Feature branch: `codex/spt-96-onboarding-auto-merge`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-96-onboarding-auto-merge`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-96-onboarding-auto-merge`
- Jira issue: `SPT-96`
- Jira URL: https://studypot.atlassian.net/browse/SPT-96
- Jira summary: [feat] 온보딩 단순화 및 자동 merge 하네스 전환
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/02-requirements.md
- [x] docs/confluence/05-api-spec.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/github-actions-review-gate.md
- [x] docs/quality/scorecard.md

## Related Feature IDs
- [x] group-onboarding
- [x] n/a-harness

## Doc Notes
- User decision on 2026-05-20: onboarding should collect only overall skill level, additional notes, and available days/times. Public `keywordSkillLevels`, `taskPreferences`, and draft/save-then-submit UX are too granular for the Swagger-driven flow.
- User decision on 2026-05-20: the harness should automatically merge PRs after the required review gates pass instead of stopping for a manual GitHub merge click.
- Because v1 specs are locked, this task records a Change Request and ADR before changing public onboarding API shape and the PR finish contract.
- DB migration is intentionally avoided for this slice. The simplified public `skillLevel` is mapped internally to the existing `group_onboarding_response.keyword_skill_levels` JSON for every group detail keyword, and `task_preferences` is stored as `{}`.

## Goal
Simplify group onboarding so a member submits one payload with `skillLevel`, `additionalNote`, and `availabilitySlots`, then receives a submitted response immediately. Update the PR finish harness so `scripts/task/finish-pr.sh <PR_NUMBER>` verifies the latest head review gates, performs the GitHub merge, then runs the existing safe cleanup path.

## Approach
Use a compatibility-preserving implementation:
- Replace the public draft/save endpoint pair with `POST /api/v1/groups/{groupId}/onboarding/me` using the simplified request.
- Keep `GET /api/v1/groups/{groupId}/onboarding/me` for reading the current response, but expose `skillLevel` rather than the internal score maps.
- Store the public skill level by assigning it to each group detail keyword internally, and store task preferences as an empty map.
- Reject a second submission for an already submitted onboarding response.
- Update locked specs, Confluence drafts, OpenAPI, and focused controller/service tests.
- Change `finish-pr.sh` default mode from manual-merge notification to auto-merge plus cleanup, while keeping `cleanup-merged` as a recovery/idempotency command.
- Update shell harness tests and Korean Mattermost notification text to match the new auto-merge contract.

## Step Plan
1. Add CR/ADR and update `change-control-v1.md`.
2. Update onboarding tests to assert the simplified submit API and response shape.
3. Implement simplified command/controller/service behavior and conflict handling.
4. Update API, QA, Confluence, and harness docs.
5. Update `finish-pr.sh`, `notify-pr-ready.sh`, and shell contract tests for auto merge.
6. Run focused tests, then `./gradlew check build --no-daemon`.
7. Commit, create PR, run CodeRabbit review, satisfy review gate, and use the updated finish flow.

## Done Criteria
- `POST /api/v1/groups/{groupId}/onboarding/me` submits onboarding in one call with `skillLevel`, `additionalNote`, and `availabilitySlots`.
- Swagger/OpenAPI no longer documents public `keywordSkillLevels`, `taskPreferences`, `PUT /onboarding/me`, or `/onboarding/me/submit`.
- Service tests prove public `skillLevel` is mapped to internal keyword scores, `taskPreferences` is empty, invalid slots are rejected, pending members activate, and duplicate submission conflicts.
- `finish-pr.sh <PR_NUMBER>` auto-merges after latest-head GitHub Actions Review Gate and CodeRabbit markers pass, then performs safe cleanup/Jira recording.
- `cleanup-merged` remains available for externally merged or interrupted PR cleanup.
- Harness/docs tests and `./gradlew check build --no-daemon` pass.

## Verification
- `./gradlew test --tests 'com.studypot.aistudyleader.onboarding.*' --no-daemon` - passed on 2026-05-20.
- `bash scripts/tests/test_pr_scripts_static.sh` - passed on 2026-05-20.
- `bash scripts/tests/test_auto_merge_notification.sh` - passed on 2026-05-20.
- `bash scripts/tests/test_docs_source_of_truth.sh` - passed on 2026-05-20.
- `bash scripts/tests/test_quality_gate_contracts.sh` - passed on 2026-05-20.
- `bash scripts/tests/test_swagger_docs_contracts.sh` - passed on 2026-05-20.
- `bash scripts/tests/run.sh` - passed on 2026-05-20 after preserving executable permission on the renamed auto-merge notification test.
- `./gradlew check build --no-daemon` - passed on 2026-05-20.
