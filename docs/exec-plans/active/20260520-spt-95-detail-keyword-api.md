# EXEC_PLAN: [feat] AI 세부 키워드 추천 API 노출

- Task slug: `spt-95-detail-keyword-api`
- Base branch: `develop`
- Feature branch: `codex/spt-95-detail-keyword-api`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-95-detail-keyword-api`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-95-detail-keyword-api`
- Jira issue: `SPT-95`
- Jira URL: https://studypot.atlassian.net/browse/SPT-95
- Jira summary: [feat] AI 세부 키워드 추천 API 노출
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] ai-team-leader
- [x] study-group-core

## Doc Notes
- Locked v1 docs already include `REQ-AI-001`: AI can suggest detail keywords, and candidates are not persisted unless selected by the user.
- Existing implementation has internal `DetailKeywordSuggestionService`, but no public Swagger/OpenAPI endpoint for the group-creation helper flow.
- `docs/specs/change-control-v1.md` requires CR/ADR for a new endpoint and AI JSON schema change.
- Product owner decision in this Codex session: expose a Swagger-callable suggestion API and make the AI result use a stable `keywords` parameter, not suggestion objects with `reason`.
- The suggestion endpoint must not create a group and must not persist candidate keywords; selected keywords are still submitted later through `POST /api/v1/groups`.

## Goal
Expose a public authenticated API for the group-creation helper flow so a Swagger user can submit a broad study topic such as `Spring Boot` and receive several candidate detail keyword strings such as `JPA` and `Spring Security`.

## Approach
- Record the locked-doc change with `CR-20260520-detail-keyword-suggestion-api` and `ADR-20260520-detail-keyword-suggestion-api`.
- Change the detail keyword AI structured-output contract to a deterministic object with one response field: `keywords: string[]`.
- Add `POST /api/v1/groups/detail-keyword-suggestions` to the study group controller, using the existing authentication extraction and validation style.
- Keep `topic` required; allow optional `hintKeywords`; default `maxCandidates` to 5 when omitted.
- Preserve no-persistence behavior and `llm_usage` audit for every provider success/failure.

## Step Plan
1. [x] Add CR/ADR and update locked docs/OpenAPI for the public detail keyword suggestion API.
2. [x] Add failing service/controller tests for keyword-only schema, auth, validation, and Swagger-facing response shape.
3. [x] Update `DetailKeywordSuggestionService` to request/parse `keywords` only.
4. [x] Add `StudyGroupController` endpoint and request/response DTOs.
5. [x] Run focused tests and OpenAPI parsing checks.
6. [x] Run `./gradlew check build --no-daemon`.
7. [ ] Commit, create PR, run CodeRabbit, and finish the PR gate.

## Done Criteria
- `POST /api/v1/groups/detail-keyword-suggestions` is documented in OpenAPI and visible in Swagger.
- Request with only `{ "topic": "Spring Boot" }` can return `{"keywords":["JPA","Spring Security",...]}` when the AI provider is configured.
- AI structured output for this purpose uses a stable `keywords` parameter.
- Suggestions are not persisted as candidates; selected keywords remain part of the later create-group request.
- Authentication, validation, success, and unavailable-provider behavior are covered by tests.
- `./gradlew check build --no-daemon` passes.

## Verification
- RED: `./gradlew test --tests com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionServiceTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --no-daemon` failed because `DetailKeywordSuggestions#keywords()` and the new endpoint contract did not exist.
- PASS: `./gradlew test --tests com.studypot.aistudyleader.studygroup.service.DetailKeywordSuggestionServiceTest --tests com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest --no-daemon`
- PASS: `bash scripts/tests/test_swagger_docs_contracts.sh`
- PASS: `ruby -ryaml -e 'doc=YAML.load_file("docs/specs/openapi.yaml"); abort("openapi must be 3.1.x") unless doc.fetch("openapi").to_s.start_with?("3.1."); abort("info.title is required") unless doc.dig("info", "title"); abort("paths is required") unless doc["paths"].is_a?(Hash); puts "OpenAPI parsed paths=#{doc.fetch("paths").length} schemas=#{doc.fetch("components").fetch("schemas").length}"'`
- PASS: `./gradlew check build --no-daemon`
