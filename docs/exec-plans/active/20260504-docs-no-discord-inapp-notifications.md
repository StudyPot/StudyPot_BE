# EXEC_PLAN: [docs] Discord 제거와 in-app 알림/AI 주차 조정 반영

- Task slug: `docs-no-discord-inapp-notifications`
- Base branch: `develop`
- Feature branch: `codex/docs-no-discord-inapp-notifications`
- Worktree: `/tmp/studypot-no-discord-harness/worktrees/docs-no-discord-inapp-notifications`
- Port: `18080`
- Log dir: `/tmp/studypot-no-discord-harness/logs/docs-no-discord-inapp-notifications`
- Jira issue: `SPT-55`
- Jira URL: https://studypot.atlassian.net/browse/SPT-55
- Jira summary: [docs] Discord 제거와 in-app 알림/AI 주차 조정 반영
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/product-brief.md
- [x] docs/specs/prd-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/domain-erd.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/confluence/README.md
- [x] docs/architecture/backend-map.md

## Related Feature IDs
- [x] notification
- [x] ai-team-leader
- [x] retrospective-feedback
- [x] identity-core
- [x] n/a-harness

## Doc Notes
- Product decision: Discord integration is removed from MVP. Notification remains required as in-app product functionality.
- AI team leader is a weekly operator: it generates the initial curriculum, then uses weekly todo completion, incomplete reasons, and chat context to produce retrospective feedback and next-week adjustments.
- Locked docs require a new Change Request and ADR because this removes a table, endpoints, feature scope, and enum values.

## Goal
Update locked repo docs, API/DB contracts, Confluence drafts, and Jira references so the implementation baseline removes Discord and keeps in-app notification plus weekly AI adjustment behavior.

## Approach
Add a new CR/ADR, remove Discord-specific table/API/contract references from active v1 specs, rename the notification contract/draft pages, revise notification DB/API shapes for `IN_APP`, and update Jira tasks that previously assumed Discord.

## Step Plan
- [x] Add CR/ADR for Discord removal and in-app notification.
- [x] Update product, PRD, requirements, AI, permission, QA, and coverage docs.
- [x] Update ERD, DB contract, SQL schema, and OpenAPI.
- [x] Rename Discord contract/draft pages to notification-focused documents.
- [x] Update Jira affected tasks.
- [x] Verify with text scans, OpenAPI parse, DB schema coverage, and Gradle build.

## Done Criteria
- Active v1 docs contain no P0 Discord integration, Discord bot, `discord_integration`, `discord_guild_id`, or `discord_channel_id` requirement.
- Notification MVP is documented as in-app notification with recipient/read state.
- AI weekly next-week adjustment role is explicitly documented.
- `./gradlew check build --no-daemon` passes.
- OpenAPI YAML parses.
- Jira issues reflect no-Discord/in-app notification scope.

## Verification
- [x] `ruby -ryaml -e 'doc=YAML.load_file("docs/specs/openapi.yaml"); ...'`: PASS, OpenAPI 3.1.0 with 21 paths.
- [x] DB schema table coverage: PASS, 19 MVP tables and no Discord-specific schema objects.
- [x] `git diff --check`: PASS.
- [x] `./gradlew check build --no-daemon`: PASS on 2026-05-04, 7 tasks up-to-date.
- [x] Jira stale label scan: PASS, `erd-v06`, `erd-v07`, `meeting`, `discord-notifications` returned 0 issues.
- [x] Jira stale schema text scan: PASS, `discord_integration` remains only on `no-discord` cleanup/context issues.
