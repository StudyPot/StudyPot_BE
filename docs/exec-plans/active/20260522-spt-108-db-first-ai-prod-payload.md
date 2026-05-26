# EXEC_PLAN: [fix] DB-first AI 운영 검증 진행률 payload 보정

- Task slug: `spt-108-db-first-ai-prod-payload`
- Base branch: `develop`
- Feature branch: `codex/spt-108-db-first-ai-prod-payload`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-108-db-first-ai-prod-payload`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-108-db-first-ai-prod-payload`
- Jira issue: `SPT-108`
- Jira URL: https://studypot.atlassian.net/browse/SPT-108
- Jira summary: [fix] DB-first AI 운영 검증 진행률 payload 보정
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/deployment.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- SPT-107 production verification reached `/api/v1/groups/{groupId}/start` successfully after the 120s AI read timeout deploy.
- The remaining failure is harness payload drift: `PUT /api/v1/weeks/{weekId}/progress/me` returned 422 because `INCOMPLETE` progress included `completionNote`.
- Domain validation forbids `completionNote` for `INCOMPLETE` week progress and task completion. The API/spec shape remains unchanged; the verification fixture must send only `incompleteReason` for incomplete states.
- This task does not implement vector/embedding RAG. It only repairs DB-first AI production verification coverage.

## Goal
Make the DB-first AI production verification harness use API-valid incomplete progress/task payloads, then re-run the strict PR/deploy path and production verification.

## Approach
- Remove `completionNote` from `INCOMPLETE` payloads in `scripts/task/verify-ai-db-first-prod.sh`.
- Add static harness assertions that the DB-first production script never sends `completionNote` in the `progress.json` or `task-completion.json` incomplete fixtures.
- Keep verification evidence focused on the production flow: curriculum generation success, retrospective feedback success, team-lead chat success, DB-first context metadata, and cleanup.

## Step Plan
1. Patch the production verification script payloads.
2. Strengthen script contract tests for incomplete payload validation.
3. Run focused script tests and `./gradlew check build --no-daemon`.
4. Create PR through `scripts/task/create-pr.sh`, run CodeRabbit, address review gate feedback if any, and finish via `scripts/task/finish-pr.sh`.
5. Watch the deploy workflow and rerun `scripts/task/verify-ai-db-first-prod.sh` against `https://studypot.rumiclean.com`.

## Done Criteria
- `scripts/task/verify-ai-db-first-prod.sh` no longer sends `completionNote` for `INCOMPLETE` week progress or task completion.
- Static tests fail if those invalid fields are reintroduced.
- Local verification and PR required checks pass.
- PR is merged through the repository finish script.
- Production verification returns `result=PASS` for DB-first retrospective/team-lead-chat evidence, without claiming vector/embedding RAG.
