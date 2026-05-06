# 10 Jira 매핑

## Key Issues
- `SPT-4`: Requirements source, completed and published as Confluence `02 요구사항`.
- `SPT-6`: ERD/data-model source, completed and published as Confluence `04 ERD / 데이터 모델`.
- `SPT-7`: API source, completed and published as Confluence `05 API 명세`.
- `SPT-9`: Architecture source Story; keep as planning/reference context, not a harness start target.
- `SPT-10`: Backend MVP Epic.
- `SPT-19` to `SPT-50`: implementation and verification Task queue.
- `SPT-51`: ERD document consolidation Story; use as reference for ERD follow-up, not a harness start target.
- `SPT-53`: AI prompt/team leader document consolidation Story; use as reference for AI follow-up, not a harness start target.
- `SPT-55`: Discord removal and in-app notification source update, completed.
- `SPT-56`: remaining documentation source-of-truth cleanup Task.
- `SPT-58`: Auth login/token API source update Task.

## Harness Start Rule
- Start implementation only from Jira `SPT` issues of type `작업`/`Task`.
- Do not start worktrees from Epic, Feature, Story, or Obsidian records.
- Use `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`.

## Required Labels
- `requirements-v03`
- `erd-v08`
- `onboarding`
- Feature-specific labels such as `weekly-todo`, `retrospective`, `ai-chat`, `notification`, `in-app-notification`, `no-discord`, `qa`

## Stale Labels
- `erd-v06`
- `erd-v07`
- `meeting`
