# EXEC_PLAN: [bug] OpenAI provider 활성화 시 ObjectMapper bean 누락 수정

- Task slug: `spt-94-openai-objectmapper`
- Base branch: `develop`
- Feature branch: `codex/spt-94-openai-objectmapper`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-94-openai-objectmapper`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-94-openai-objectmapper`
- Jira issue: `SPT-94`
- Jira URL: https://studypot.atlassian.net/browse/SPT-94
- Jira summary: [bug] OpenAI provider 활성화 시 ObjectMapper bean 누락 수정
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/operations/local-development.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] ai-team-leader
- [x] curriculum-core
- [x] retrospective-feedback

## Doc Notes
- `docs/specs/ai-contract-v1.md` requires AI provider calls to back curriculum generation, retrospective feedback, and team-leader chat while recording `llm_usage`.
- `docs/operations/local-development.md` keeps real secrets in ignored `config/application-local.yml`; enabling `studypot.ai.openai.api-key` locally must not break application startup.
- The bug is a wiring issue only: no API, DB schema, provider contract, or product behavior change is planned.

## Goal
Fix the local/runtime startup failure that appears when `studypot.ai.openai.api-key` enables the OpenAI provider: the Spring context must provide a `com.fasterxml.jackson.databind.ObjectMapper` for provider-backed curriculum, retrospective, and AI chat beans.

## Approach
- Start with a reproduction wiring test that enables a fake OpenAI API key and asserts the provider-backed beans are present.
- Add one global fallback `com.fasterxml.jackson.databind.ObjectMapper` bean instead of scattering per-configuration fallbacks.
- Keep the existing OpenAI provider activation rule unchanged: no API key means provider-backed generators stay disabled; API key means the shared `LlmProviderClient` and dependent generators are available.
- Do not call the real OpenAI network in tests; this is a Spring bean wiring regression.

## Step Plan
1. [x] Add a RED Spring wiring test with `studypot.ai.openai.api-key=test-key`, disabled Flyway, and a mock datasource.
2. [x] Verify the RED failure reproduces the missing `ObjectMapper` bean problem.
3. [x] Add a global Jackson configuration that registers `com.fasterxml.jackson.databind.ObjectMapper` when one is missing.
4. [x] Extend assertions so OpenAI enabled mode registers `LlmProviderClient`, `CurriculumGenerator`, AI assistant generator, and retrospective feedback generator.
5. [x] Run focused wiring tests, then `./gradlew check build --no-daemon`.
6. [ ] Commit, create PR, run CodeRabbit, finish the PR gate, and wait for manual merge.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.ApplicationOpenAiWiringTest --no-daemon` failed with missing `com.fasterxml.jackson.databind.ObjectMapper` for `openAiLlmProvider`.
- PASS: `./gradlew test --tests com.studypot.aistudyleader.ApplicationOpenAiWiringTest --no-daemon`
- PASS: `./gradlew test --tests com.studypot.aistudyleader.ApplicationFeatureWiringTest --no-daemon`
- PASS: `./gradlew check build --no-daemon`

## Done Criteria
- Application context starts when `studypot.ai.openai.api-key` is non-blank.
- A `com.fasterxml.jackson.databind.ObjectMapper` bean is available without requiring per-feature fallback wiring.
- OpenAI enabled wiring registers the shared `LlmProviderClient` and provider-backed curriculum/retrospective/chat generators.
- Existing no-key local behavior remains verified.
- `./gradlew check build --no-daemon` passes.
