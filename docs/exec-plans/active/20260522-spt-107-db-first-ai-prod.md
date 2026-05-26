# EXEC_PLAN: [qa/ops] DB-first AI 운영 실호출 검증

- Task slug: `spt-107-db-first-ai-prod`
- Base branch: `develop`
- Feature branch: `codex/spt-107-db-first-ai-prod`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-107-db-first-ai-prod`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-107-db-first-ai-prod`
- Jira issue: `SPT-107`
- Jira URL: https://studypot.atlassian.net/browse/SPT-107
- Jira summary: [qa/ops] DB-first AI 운영 실호출 검증
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/adr/ADR-20260512-retrospective-rag-boundary.md
- [x] docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md
- [x] scripts/task/verify-ai-golden-path.sh

## Related Feature IDs
- [x] n/a-harness
- [x] retrospective-feedback
- [x] ai-team-leader

## Doc Notes
- `AGENTS.md`: implementation must start from Jira, use a `codex/<slug>` worktree, keep an `EXEC_PLAN`, run tests, create a PR, pass CodeRabbit/review gates, and finish/cleanup through the harness.
- `ARCHITECTURE.md`: MVP golden path includes group creation, onboarding, host start, AI curriculum, weekly todo, and AI retrospective feedback; AI state is persisted through curriculum, retrospective, conversation, notification, and `llm_usage`.
- `docs/index.md`: harness/infrastructure work uses `n/a-harness`; locked v1 specs cannot change without CR/ADR.
- `docs/specs/ai-contract-v1.md`: `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT` use a backend DB-first context builder before the provider call; vector store, GraphRAG, FastAPI split, MCP, and agent orchestration are deferred.
- `ADR-20260512-retrospective-rag-boundary.md`: RAG means deterministic, permission-filtered DB-first context for MVP, not a separate vector service.
- `ADR-20260519-ai-llm-rag-architecture.md`: keep `ContextBuilder -> LlmProviderClient -> ProviderAdapter` inside Spring Boot until a later approved task proves that vector/graph/service-split retrieval is needed.
- `scripts/task/verify-ai-golden-path.sh`: the local backend-only golden path already covers retrospective and AI conversation endpoints, but it starts a local app and cannot verify the deployed `studypot.rumiclean.com` runtime directly.

## Goal
Add and run a repeatable production DB-first AI verification harness that proves the deployed StudyPot backend can build DB-first retrospective/chat context, call the configured GMS-backed provider, persist successful AI outputs, and record `llm_usage` evidence for `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT` without claiming vector/embedding RAG completion.

## Approach
Create a separate production smoke script instead of changing product API/DB contracts. The script will SSH to the configured `rumiclean` compose host, safely load runtime env without printing secrets, seed temporary auth users directly in MySQL, drive public StudyPot REST APIs through `studypot.rumiclean.com`, query sanitized DB evidence for context/source coverage and usage records, then cleanup all test data. Static harness tests will lock the script contract, required endpoints, secret-redaction posture, and DB-first evidence checks.

## Step Plan
1. [x] Add a static test for the production DB-first AI verification script contract.
2. [x] Add `scripts/task/verify-ai-db-first-prod.sh` with configurable SSH host, compose dir, base URL, run id, log dir, cleanup, and sanitized evidence summary.
3. [x] Run targeted harness tests and shell syntax checks.
4. [x] Run the production verification against `studypot.rumiclean.com` and record only sanitized evidence.
5. [x] Run `./gradlew check build --no-daemon`.
6. [ ] Commit, create PR, request CodeRabbit review, pass review gate, merge through `finish-pr.sh`, and confirm cleanup.

## Evidence Notes
- PASS: `scripts/tests/test_ai_db_first_prod_verification_contracts.sh`
- PASS: `scripts/tests/test_deployment_contracts.sh`
- PASS: `scripts/tests/test_rumiclean_migration_contracts.sh`
- PASS: `scripts/tests/run.sh`
- PASS: `./gradlew check build --no-daemon`
- PASS: `scripts/hooks/pre-commit.sh`
- Production discovery: initial DB-first production verification reached group creation/join/onboarding, then `POST /api/v1/groups/{groupId}/start` returned HTTP 503. The failed `llm_usage` record for `CURRICULUM_GENERATE` showed `OPENAI_REQUEST_FAILED`, `latency_ms` around 30031, and zero input/output tokens while production `STUDYPOT_AI_OPENAI_READ_TIMEOUT` was `30s`. This identifies a provider read-timeout configuration blocker before retrospective/chat verification.
- Fix scope added: Deploy workflow now uploads `STUDYPOT_AI_OPENAI_CONNECT_TIMEOUT` and `STUDYPOT_AI_OPENAI_READ_TIMEOUT` to `.runtime.env`; GitHub Secrets were registered with `5s` and `120s`; compose/docs/tests now keep the production AI read timeout at `120s`.
- Cleanup: the debug production group/user/usage rows left for timeout diagnosis were manually removed; leftover debug users/groups/usage were all `0`.

## Done Criteria
- Production script proves `RETROSPECTIVE_FEEDBACK` and `TEAM_LEAD_CHAT` real provider calls through the deployed backend with HTTP success and MySQL evidence.
- `llm_usage` evidence includes purpose, `SUCCESS`, model, token counts, and latency for both purposes.
- Retrospective evidence includes DB-first `input_summary` keys and non-empty task/progress/onboarding context.
- AI conversation evidence includes assistant metadata `retrievalContextVersion=db-first-v1` and `TEAM_LEAD_CHAT` request payload task/message counts.
- Test users/groups are cleaned up and leftover counts are reported.
- No provider key, JWT secret, bearer token, OAuth token, cookie, or credential value is printed or committed.
- `./gradlew check build --no-daemon` passes, PR review gate passes, and the PR is merged/cleaned up.
