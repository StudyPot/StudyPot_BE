# EXEC_PLAN: [qa/ops] LLM 비용 정확도 가드레일 적용

- Task slug: `spt-109-llm-cost-guardrails`
- Base branch: `develop`
- Feature branch: `codex/spt-109-llm-cost-guardrails`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-109-llm-cost-guardrails`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-109-llm-cost-guardrails`
- Jira issue: `SPT-109`
- Jira URL: https://studypot.atlassian.net/browse/SPT-109
- Jira summary: [qa/ops] LLM 비용 정확도 가드레일 적용
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/adr/ADR-20260519-ai-llm-rag-architecture.md
- [x] docs/operations/deployment.md
- [x] scripts/task/verify-ai-db-first-prod.sh

## Related Feature IDs
- [x] n/a-harness
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] curriculum-core

## Doc Notes
- v1 AI contract is locked. This task must not change public API, DB schema, stored AI output shape, notification behavior, or RAG boundary.
- DB-first context remains the MVP retrieval boundary. Vector RAG, FastAPI split, agentic workflows, and model/tool orchestration remain deferred.
- User decision for this task: preserve answer quality. Do not introduce model downgrade or aggressive context truncation in this PR.
- Runtime guardrails should be observable through `llm_usage.request_payload` and deployment/runtime env, without logging secrets.

## Goal
Add accuracy-preserving LLM cost guardrails so production AI calls have purpose-specific output budgets and verification evidence for token usage, while keeping the current DB-first prompt context and model selection unchanged by default.

## Approach
1. Extend OpenAI provider configuration with purpose-specific max output token budgets.
2. Send `max_output_tokens` for Responses mode and `max_completion_tokens` for Chat Completions mode.
3. Add the selected budget and API mode into redacted `llm_usage.request_payload` for audit.
4. Pass the new runtime env values through application config, compose files, GitHub Actions deploy env upload, and deployment docs.
5. Extend tests and production verification assertions around token-budget evidence.
6. Avoid runtime AI JSON shape changes unless a separate Change Request/ADR is approved.

## Step Plan
1. Update OpenAI properties and provider payload construction.
2. Add unit tests for default and bound token budgets in Responses and Chat Completions modes.
3. Add deployment/runtime env pass-through for the purpose-specific budgets.
4. Update static deployment and production verification contract tests.
5. Run focused tests, then `./gradlew check build --no-daemon`.
6. Create PR, run CodeRabbit, satisfy review gate, merge, and clean up.

## Done Criteria
- OpenAI request payloads include purpose-specific output budgets.
- Successful and failed `llm_usage` records include `apiMode` and `maxOutputTokens` in request payload metadata.
- Runtime env supports separate budgets for detail keyword, curriculum, retrospective, and team-lead chat calls.
- Production DB-first AI verification checks that deployed usage payloads expose positive token evidence and max output token budget metadata.
- No model downgrade, no vector RAG addition, and no aggressive DB-first context truncation is introduced.
- `./gradlew check build --no-daemon` passes.
- PR review gate and CodeRabbit workflow complete before merge.

## Verification
- [x] `./gradlew test --tests OpenAiLlmProviderTest --tests OpenAiCurriculumPropertiesTest --no-daemon`
- [x] `bash scripts/tests/test_deployment_contracts.sh && bash scripts/tests/test_rumiclean_migration_contracts.sh && bash scripts/tests/test_ai_db_first_prod_verification_contracts.sh`
- [x] `bash scripts/tests/run.sh`
- [x] `./gradlew check build --no-daemon`
