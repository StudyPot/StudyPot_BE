# EXEC_PLAN: [qa/ops] AI golden path 실사용 검증 및 운영 설정 점검

- Task slug: `spt-102-ai-golden-path`
- Base branch: `develop`
- Feature branch: `codex/spt-102-ai-golden-path`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-102-ai-golden-path`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-102-ai-golden-path`
- Jira issue: `SPT-102`
- Jira URL: https://studypot.atlassian.net/browse/SPT-102
- Jira summary: [qa/ops] AI golden path 실사용 검증 및 운영 설정 점검
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/user-journeys-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/operations/local-development.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] n/a-harness
- [x] identity-core
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core
- [x] weekly-todo
- [x] retrospective-feedback
- [x] ai-team-leader
- [x] notification

## Doc Notes
- `ARCHITECTURE.md`: MVP golden path is group creation -> invite link -> member onboarding -> host start -> AI curriculum -> weekly todo -> retrospective feedback. AI state must remain stored through curriculum, retrospective, conversation, notification, and `llm_usage`.
- `docs/index.md`: AI team leader is a weekly operator, not just initial curriculum generation. External channels are out of MVP; `IN_APP` notification remains first.
- `docs/specs/user-journeys-v1.md`: golden path acceptance includes host/member onboarding, host start with submitted onboarding only, weekly todo execution, retrospective/chat context, and in-app notification read state.
- `docs/specs/requirements-v1.md`: P0 includes study group, onboarding, curriculum, weekly todo, retrospective feedback, AI chat, in-app notification, and LLM usage. P2 excludes Discord, live meeting assistant, voice transcription, and vector/graph retrieval.
- `docs/specs/api-contract-v1.md`: runtime verification should use `/api/v1/groups`, `/groups/{groupId}/join`, `/groups/{groupId}/onboarding/me`, `/groups/{groupId}/start`, `/groups/{groupId}/curriculum`, `/groups/{groupId}/weeks/current`, `/weeks/{weekId}/tasks`, `/weeks/{weekId}/progress/me`, `/tasks/{taskId}/completion/me`, `/weeks/{weekId}/retrospectives/me`, `/groups/{groupId}/ai-conversations`, `/ai-conversations/{conversationId}/messages`, notifications, and group `llm-usage`.
- `docs/specs/ai-contract-v1.md`: every AI call creates `llm_usage`; MySQL remains durable source of truth; Redis/RabbitMQ must not own final AI results or durable audit.
- `docs/specs/qa-acceptance-v1.md`: required scenarios include create group -> onboarding -> host start, current week task completion, retrospective AI feedback, AI conversation message persistence, LLM usage logging, and in-app notification idempotency/read-state update.
- `docs/specs/notification-contract-v1.md`: MVP notification is `IN_APP`; external channels are post-MVP and excluded from this task.
- `docs/operations/local-development.md`: local smoke already verifies MySQL, app startup, Flyway, health, OpenAPI, Swagger, and auth refresh wiring, but it does not execute the full AI golden path.
- `docs/testing/codex-harness.md`: standard verification is `./gradlew check build --no-daemon`; external services should be isolated when possible, but this task intentionally adds a local-only manual/live verification script for real OpenAI path checks.
- `docs/operations/pr-review-gate.md`: PR completion requires a latest-head CodeRabbit PASS/ADDRESSED marker, GitHub Actions Review Gate PASS, no unresolved threads, and Korean human-facing evidence.
- `docs/operations/jira-board-sync.md`: implementation work must start from Jira Task state; PR bodies carry `Jira-Key`, and merge/cleanup records Jira completion idempotently.
- `docs/operations/obsidian-error-ledger.md`: real local verification failures should be recorded in the repo error ledger first; Obsidian mirror is optional and must not block the PR path.

## Goal
Create a repeatable local QA/ops verification path that proves, without frontend work, that the StudyPot backend can run the MVP AI golden path through real API calls and that the required local/AI operational settings are present or reported clearly.

## Approach
- Add a dedicated `scripts/task/verify-ai-golden-path.sh` manual verification script instead of expanding product scope or changing locked API/DB/AI contracts.
- Keep the script local-only and secret-safe: it must not print OpenAI API keys, JWT secrets, OAuth secrets, access tokens, refresh tokens, or raw provider credentials.
- Reuse the existing local app startup style from `scripts/task/verify-local-dev.sh`: MySQL connectivity, `SPRING_PROFILES_ACTIVE=local`, additional local config, generated logs under the task log dir, public Swagger only for local verification, and cleanup trap.
- Seed only local test users needed for bearer-token API verification. Use short-lived HS256 JWTs generated from the configured local JWT secret; do not depend on real Google OAuth during this API-path verification.
- Exercise the API path with curl and validate responses with Python JSON parsing: detail keyword suggestion, group creation, member join, host/member onboarding, host start curriculum generation, curriculum/current week/tasks/progress/task completion, retrospective request/read, AI conversation open/message, notifications, and group LLM usage.
- Capture evidence under `build/ai-golden-path/` and the task log dir. Store response bodies and a redacted summary, not secrets.
- If real OpenAI configuration is absent, fail early with a clear operational setup message. Do not silently fall back to mocks for the live golden-path script.
- If OpenAI configuration is present but invalid, run a secret-safe provider preflight before booting the app and redact any provider-returned key material.
- Add a static harness test for the new script contract so future edits preserve key safety and coverage expectations.

## Step Plan
1. Add a RED static test `scripts/tests/test_ai_golden_path_verification_contracts.sh` and register it in `scripts/tests/run.sh`. The test should fail while the script is missing.
2. Implement the minimal `scripts/task/verify-ai-golden-path.sh` to satisfy the static contract: executable, local-config required, OpenAI key presence check without echoing the value, app startup/cleanup trap, JWT generation, API calls, DB assertions, redacted evidence summary, and explicit endpoint coverage markers.
3. Run the static test and `scripts/tests/run.sh` until they pass.
4. Run `./gradlew check build --no-daemon` to verify the repository.
5. Run `scripts/task/verify-local-dev.sh` to confirm baseline local DB/app/Swagger readiness if MySQL/local config are available.
6. Run `scripts/task/verify-ai-golden-path.sh` with `STUDYPOT_LOCAL_CONFIG`, local DB variables, and real `STUDYPOT_AI_OPENAI_API_KEY` configured. If missing or blocked, record the exact blocker and keep code verification complete.
7. Inspect generated evidence and DB counts for `curriculum`, `curriculum_week`, `weekly_task`, `retrospective`, `ai_conversation`, `ai_conversation_message`, `notification`, and `llm_usage`.
8. If the live verification exposes a narrow harness/config defect, fix it in this PR with a failing test first. If it exposes product/API behavior or scope changes, create follow-up Jira Task candidates instead of changing locked contracts here.
9. Commit, create PR with `scripts/task/create-pr.sh`, run CodeRabbit agent review, address one feedback loop if needed, and finish through the review gate/auto-merge cleanup path.

## Progress Notes
- Added `scripts/tests/test_ai_golden_path_verification_contracts.sh`, registered it in `scripts/tests/run.sh`, and confirmed the RED state before adding the script.
- Added `scripts/task/verify-ai-golden-path.sh` for backend-only live verification with generated local JWTs, local user seed/cleanup, AI golden path API calls, DB assertions, and redacted evidence output.
- Fixed `scripts/task/verify-local-dev.sh` to include the local CSRF header/cookie pair when asserting invalid refresh-token behavior; the current security filter correctly returns 403 without CSRF before auth can return 401.
- `bash scripts/tests/run.sh` passed after the script and CSRF smoke updates.
- `scripts/task/verify-local-dev.sh` passed against the local MySQL/Spring profile using the local config outside the worktree.
- First live `scripts/task/verify-ai-golden-path.sh` run reached `/api/v1/users/me`, then failed at `/api/v1/groups/detail-keyword-suggestions` with HTTP 503. Direct provider preflight identified the external blocker as an invalid OpenAI API key, so the script was updated to detect that before app startup without printing key material.
- Second live run failed early at the new OpenAI API key preflight with HTTP 401 and a redacted provider message, confirming the remaining blocker is external credential setup rather than backend boot/API wiring.
- `./gradlew check build --no-daemon` passed in the SPT-102 worktree after all script, test, and documentation updates.

## Done Criteria
- `scripts/task/verify-ai-golden-path.sh` exists, is executable, and documents/runs the backend-only AI golden path without frontend dependency.
- `scripts/tests/test_ai_golden_path_verification_contracts.sh` protects the script's endpoint coverage, secret-safety checks, local config requirement, app cleanup trap, and evidence output contract.
- The new static test is included in `scripts/tests/run.sh`.
- `./gradlew check build --no-daemon` passes in the SPT-102 worktree.
- A local live run either passes and leaves redacted evidence under `build/ai-golden-path/`, or fails with a concrete external blocker such as missing local MySQL, missing real OpenAI API key, provider billing/auth failure, or unavailable local config.
- Any discovered product/API/contract gap is written as a follow-up Jira candidate rather than being fixed through unapproved scope expansion.
