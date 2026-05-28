# EXEC_PLAN: [fix] 그룹 상세 조회와 GPT-5 키워드 출력 설정 복구

- Task slug: `spt-119-group-detail-gpt5-keywords`
- Base branch: `develop`
- Feature branch: `codex/spt-119-group-detail-gpt5-keywords`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-119-group-detail-gpt5-keywords`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-119-group-detail-gpt5-keywords`
- Jira issue: `SPT-119`
- Jira URL: https://studypot.atlassian.net/browse/SPT-119
- Jira summary: [fix] 그룹 상세 조회와 GPT-5 키워드 출력 설정 복구
- Status: `implemented`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/auth-permissions-v1.md
- [x] docs/operations/deployment.md

## Related Feature IDs
- [x] study-group-core
- [x] ai-team-leader

## Doc Notes
- Runtime logs show `POST /api/v1/groups` returns 201, then frontend calls `GET /api/v1/groups/{groupId}` and receives 404 because the controller has no read endpoint even though the locked API/OpenAPI contract lists it.
- DB confirms the newly created group and OWNER membership exist, so the group-home failure is not a create rollback or cookie/CSRF issue.
- Runtime `llm_usage` records for `DETAIL_KEYWORD_SUGGEST` show `OPENAI_CHAT_COMPLETION_CONTENT_MISSING` with `gpt-5-nano`, chat-completions mode, and the configured 256 output budget.
- Live GMS probes with the same request show no content when `reasoning_effort` is omitted, even at 1024 completion tokens, because all completion tokens are spent as reasoning tokens. Adding `reasoning_effort=minimal` returns valid JSON content.
- This task implements already locked API/AI behavior rather than changing public API shape.

## Goal
Restore the group-creation golden path by implementing the locked group-detail read endpoint and making GPT-5 chat-completions detail-keyword suggestions emit JSON instead of empty reasoning-only responses.

## Approach
- Add RED tests for `GET /api/v1/groups/{groupId}` returning the current user's group and rejecting non-member access.
- Add RED provider tests proving chat-completions requests for detail-keyword suggestions include `reasoning_effort=minimal` for GPT-5 models and keep the setting visible in audit payload metadata.
- Implement the repository/service/controller read path using the existing `StudyGroupResponse` shape.
- Add a narrow `StudyGroupAccessDeniedException` mapped to 403 so existing/not-owned groups do not look like missing endpoints.
- Keep OpenAPI/API docs unchanged except operational provider-setting docs if a new runtime property is introduced.

## Step Plan
1. Write failing tests for group detail read and GPT-5 reasoning effort request mapping.
2. Implement repository and service read methods plus controller `GET /groups/{groupId}`.
3. Implement detail-keyword reasoning effort support for chat-completions.
4. Run focused controller/provider tests, deployment/doc contract scripts when touched, and `./gradlew check build --no-daemon`.
5. Commit, create PR, run CodeRabbit review, pass review gate, merge, deploy, and verify live `detail-keyword-suggestions` plus group detail.

## Done Criteria
- A created group can be read at `GET /api/v1/groups/{groupId}` by its owner/member.
- A non-member receives a forbidden problem detail for an existing group.
- A missing group receives a not-found problem detail.
- GPT-5 chat-completions detail-keyword requests include minimal reasoning effort and return content in live smoke.
- Standard verification passes before PR creation.

## Implementation Notes
- Added `GET /api/v1/groups/{groupId}` through controller, service, and JDBC repository using the existing `StudyGroupResponse` shape.
- Added `StudyGroupAccessDeniedException` so existing groups hidden from non-members return a 403 problem detail while missing groups still return 404.
- Added GPT-5/o-series chat-completions reasoning effort support for detail keyword suggestions, defaulting `STUDYPOT_AI_OPENAI_REASONING_EFFORT_DETAIL_KEYWORD_SUGGEST` to `minimal`.
- Updated deployment examples, compose files, GitHub Actions runtime upload, and deployment contract tests for the new reasoning-effort override.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest.getGroupReturnsMemberGroup' --no-daemon` failed with 404 before the group detail handler existed.
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest.requestStructuredUsesMinimalReasoningForGpt5DetailKeywordChatCompletions' --no-daemon` failed because `reasoning_effort` was absent.
- GREEN focused: `./gradlew test --tests 'com.studypot.aistudyleader.studygroup.controller.StudyGroupControllerTest' --tests 'com.studypot.aistudyleader.studygroup.service.StudyGroupServiceTest' --tests 'com.studypot.aistudyleader.studygroup.repository.JdbcStudyGroupRepositoryTest' --tests 'com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest' --tests 'com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiCurriculumPropertiesTest' --no-daemon`
- GREEN contracts: `scripts/tests/test_deployment_contracts.sh`
- GREEN contracts: `scripts/tests/test_rumiclean_migration_contracts.sh`
- GREEN full: `./gradlew check build --no-daemon`
- CodeRabbit follow-up: narrowed `supportsReasoningEffort` to GPT-5 model identifiers only and added a negative `gpt-4o-mini` chat-completions test proving `reasoning_effort` and `reasoningEffort` are omitted.
- GREEN follow-up: `./gradlew test --tests 'com.studypot.aistudyleader.curriculum.infrastructure.openai.OpenAiLlmProviderTest' --no-daemon`
- GREEN follow-up full: `./gradlew check build --no-daemon`
