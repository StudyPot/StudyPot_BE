# EXEC_PLAN: [dev-env] Swagger 설명 한글화 및 문서 가드 보강

- Task slug: `spt-80-swagger-docs-ko`
- Base branch: `develop`
- Feature branch: `codex/spt-80-swagger-docs-ko`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-80-swagger-docs-ko`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-80-swagger-docs-ko`
- Jira issue: `SPT-80`
- Jira URL: https://studypot.atlassian.net/browse/SPT-80
- Jira summary: [dev-env] Swagger 설명 한글화 및 문서 가드 보강
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml

## Related Feature IDs
- [x] n/a-harness
- [x] identity-core
- [x] study-group-core
- [x] group-onboarding
- [x] curriculum-core
- [x] weekly-todo
- [x] study-group-rules

## Doc Notes
- SPT-80 is a developer-experience and harness quality task. It must not change locked v1 API paths, request/response fields, enum values, or auth behavior.
- Runtime SpringDoc annotations can add Korean Swagger UI descriptions without changing product semantics.
- `docs/specs/openapi.yaml` remains the locked machine contract; this task does not edit it.
- Future API additions should fail harness tests when controller endpoints are missing class-level `@Tag` or method-level `@Operation` details.

## Goal
현재 구현되어 Swagger UI에 노출되는 컨트롤러 API의 설명을 한글로 세세하게 보강하고, 이후 새 API를 추가할 때 Swagger Docs 누락을 하네스가 잡도록 만든다.

## Approach
1. Add a RED harness test that scans controller source files and fails when an HTTP mapping method lacks detailed Swagger annotations.
2. Document the Swagger Docs requirement in `docs/testing/codex-harness.md` and include the new static test in `scripts/tests/run.sh`.
3. Add Korean `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, and `@Schema` annotations to the currently implemented controllers.
4. Keep runtime behavior unchanged by limiting edits to documentation annotations, harness docs, and tests.
5. Run the focused harness test first, then `bash scripts/tests/run.sh`, targeted Gradle tests if needed, and finally `./gradlew check build --no-daemon`.

## Step Plan
- [x] RED: create `scripts/tests/test_swagger_docs_contracts.sh`, add it to `scripts/tests/run.sh`, and run it to confirm current controllers fail because Swagger details are missing.
- [x] GREEN: add Korean Swagger annotations to `AuthController`, `StudyGroupController`, `OnboardingController`, `CurriculumController`, and `GroupRuleController`.
- [x] GREEN: update `docs/testing/codex-harness.md` with the future-API Swagger Docs contract.
- [x] VERIFY: run `bash scripts/tests/test_swagger_docs_contracts.sh`.
- [x] VERIFY: run `bash scripts/tests/run.sh`.
- [x] VERIFY: run `./gradlew check build --no-daemon`.
- [ ] PR: create the PR through `scripts/task/create-pr.sh`, run CodeRabbit review, address actionable feedback once, and finish PR readiness through the harness.

## Done Criteria
- Current runtime controller APIs have Korean Swagger summaries, descriptions, response descriptions, parameter descriptions, and request/response schema descriptions where relevant.
- New controller HTTP mapping methods cannot be added without detailed Swagger operation annotations because the harness test fails.
- `docs/testing/codex-harness.md` states the Swagger Docs requirement for future API work.
- The locked `docs/specs/openapi.yaml` contract is unchanged.
- `bash scripts/tests/run.sh` and `./gradlew check build --no-daemon` pass.

## Verification
- RED 1: `bash scripts/tests/test_swagger_docs_contracts.sh` -> FAIL because `docs/testing/codex-harness.md` did not yet contain the Swagger Docs contract.
- RED 2: `bash scripts/tests/test_swagger_docs_contracts.sh` -> FAIL with current controller violations for missing Korean `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter`, and `@Schema` annotations.
- GREEN: `bash scripts/tests/test_swagger_docs_contracts.sh` -> PASS after adding the harness docs contract and controller Swagger annotations.
- `./gradlew compileJava --no-daemon` -> PASS.
- `./gradlew test --tests 'com.studypot.aistudyleader.global.api.SwaggerDocumentationContractTest' --no-daemon` -> PASS.
- `bash scripts/tests/run.sh` -> PASS.
- `./gradlew check build --no-daemon` -> PASS.
