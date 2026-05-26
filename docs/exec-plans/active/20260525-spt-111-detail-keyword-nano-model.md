# EXEC_PLAN: [feat] 세부 키워드 추천 nano 모델 분리

- Task slug: `spt-111-detail-keyword-nano-model`
- Base branch: `develop`
- Feature branch: `codex/spt-111-detail-keyword-nano-model`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-111-detail-keyword-nano-model`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-111-detail-keyword-nano-model`
- Jira issue: `SPT-111`
- Jira URL: https://studypot.atlassian.net/browse/SPT-111
- Jira summary: [feat] 세부 키워드 추천 nano 모델 분리
- Status: `local_verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/change-requests/CR-20260520-detail-keyword-suggestion-api.md
- [x] docs/specs/adr/ADR-20260520-detail-keyword-suggestion-api.md
- [x] docs/operations/deployment.md
- [x] src/main/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiCurriculumProperties.java
- [x] src/main/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiLlmProvider.java
- [x] src/test/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiCurriculumPropertiesTest.java
- [x] src/test/java/com/studypot/aistudyleader/curriculum/infrastructure/openai/OpenAiLlmProviderTest.java

## Related Feature IDs
- [x] ai-team-leader
- [x] study-group-core

## Doc Notes
- `DETAIL_KEYWORD_SUGGEST` is approved as the group creation helper and returns only transient keyword candidates.
- `llm_usage` must keep model, provider, purpose, token counts, redacted request payload, and summary for every AI call.
- Deployment runtime values are supplied by `.runtime.env` from GitHub Secrets, so a new per-purpose model override needs app config, compose passthrough, workflow passthrough, and deployment docs.
- User decision for this task: only the category/detail keyword recommendation path may use nano. Curriculum generation, retrospective feedback, and team-lead chat keep the existing global model unless explicitly changed later.

## Goal
- Allow `DETAIL_KEYWORD_SUGGEST` to use a dedicated low-cost nano model through runtime configuration while preserving the current global model for all other AI purposes by default.
- Keep the default behavior backward compatible: if no detail keyword override is configured, every purpose continues to use `STUDYPOT_AI_OPENAI_MODEL`.

## Approach
- Add a small per-purpose model settings value under `studypot.ai.openai.models.detail-keyword-suggest`.
- Resolve the effective model per request inside `OpenAiLlmProvider` so request body, success response audit, and failure audit all record the actual model used for that purpose.
- Add `STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST` passthrough to local examples, application config, compose files, GitHub Actions runtime upload, and deployment documentation.
- Do not change prompts, input context, output schema, or output token budgets.

## Step Plan
1. Write failing provider/properties tests for `DETAIL_KEYWORD_SUGGEST` using the configured nano model while `CURRICULUM_GENERATE` still uses the global model.
2. Implement the per-purpose model settings object and effective model resolver.
3. Update app config, local examples, deploy compose files, GitHub Actions `.runtime.env` upload, and deployment docs.
4. Add static deployment contract coverage for the new env passthrough.
5. Run focused Gradle tests, deployment contract tests, shellcheck if touched shell logic, and `./gradlew check build --no-daemon`.
6. Create PR, run CodeRabbit, wait for GitHub Actions review gate, merge, and clean up through repo scripts.
7. Configure production secret/env for detail keyword nano and verify `/api/v1/groups/detail-keyword-suggestions` records nano as the model while other golden-path AI calls stay on the global model.

## Done Criteria
- `OpenAiLlmProviderTest` proves detail keyword requests use the dedicated nano model and curriculum requests keep the global model.
- `OpenAiCurriculumPropertiesTest` proves the per-purpose model setting binds from kebab-case config and defaults to the global model.
- Deployment contract tests cover `STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST`.
- `./gradlew check build --no-daemon` succeeds.
- PR review gate and CodeRabbit marker pass, and the PR is merged to `develop`.
- Production runtime has `STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST` set to the agreed nano model, and a live detail keyword suggestion request records that model in `llm_usage`.

## Verification
- RED: `./gradlew test --tests OpenAiLlmProviderTest --tests OpenAiCurriculumPropertiesTest --no-daemon` failed because `OpenAiPurposeModels` and `OpenAiCurriculumProperties.models()` did not exist yet.
- GREEN focused: `./gradlew test --tests OpenAiLlmProviderTest --tests OpenAiCurriculumPropertiesTest --no-daemon`
- Deployment contracts: `bash scripts/tests/test_deployment_contracts.sh && bash scripts/tests/test_rumiclean_migration_contracts.sh`
- Shell: `shellcheck --external-sources scripts/tests/test_deployment_contracts.sh scripts/tests/test_rumiclean_migration_contracts.sh`
- Static harness: `bash scripts/tests/run.sh`
- Full backend: `./gradlew check build --no-daemon`
