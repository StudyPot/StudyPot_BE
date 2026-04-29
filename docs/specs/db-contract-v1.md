# DB Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- DDL draft: `docs/specs/db-schema-v1.sql`
- ERD source: `docs/specs/domain-erd.md`
- Changes require Change Request and ADR.

## Platform Contract
- Database: PostgreSQL.
- Extensions: `citext`.
- Application supplies UUIDv7 IDs before insert.
- Every table in the MVP ERD has:
  - `id uuid primary key`
  - `created_at timestamptz not null default now()`
  - `updated_at timestamptz not null default now()`
  - `deleted_at timestamptz null`
- Flexible values use `jsonb not null default '{}'::jsonb` unless the ERD explicitly uses an array-like payload.
- Soft-delete uniqueness uses partial unique indexes with `where deleted_at is null`.

## Migration Ownership
- Spring Boot owns migrations.
- Migration tool is part of backend scaffold work; v1 documents assume a Flyway-style ordered migration but do not create application scaffold here.
- `db-schema-v1.sql` is migration-ready draft SQL, not an executed migration.

## Required Tables
The DDL draft must create exactly these MVP tables:

1. `users`
2. `user_oauth_accounts`
3. `user_discord_accounts`
4. `study_groups`
5. `study_group_members`
6. `study_group_invitations`
7. `study_sessions`
8. `study_session_attendance`
9. `study_session_notes`
10. `study_session_action_items`
11. `study_goals`
12. `member_progress_logs`
13. `study_resources`
14. `ai_prompt_runs`
15. `ai_preparation_briefs`
16. `ai_feedback_reports`
17. `discord_notification_channels`
18. `discord_notification_logs`

## Referential Policy
- Foreign keys use `on delete restrict` by default because business deletion is soft delete.
- Historical rows are preserved after user, group, or Discord unlink.
- Service logic must prevent cross-group references where a member/session/goal/resource belongs to a different group.

## Index Policy
- Every FK used for joins has an index.
- Every active unique business identity has a partial unique index.
- JSONB query surfaces have GIN indexes:
  - `study_groups.rules`
  - `study_groups.schedule_defaults`
  - `study_session_notes.structured_payload`
  - `member_progress_logs.progress_payload`
  - `discord_notification_channels.notification_settings`

## Retention Contract
- AI prompt input snapshots: 180 days.
- AI derived reports: 365 days.
- Discord notification logs: 180 days.
- Retention cleanup is post-scaffold operational work but must respect these v1 defaults.

## Privacy Contract
- Provider profiles, AI input snapshots, AI outputs, and Discord payloads must be redacted before persistence.
- Bot tokens, OAuth access tokens, refresh tokens, and raw secrets are not persisted in these MVP tables.
- Individual AI feedback access:
  - owner/manager: all reports in group
  - member: group reports and own individual reports

## Implementation Notes For Spring
- Entity packages should keep domain and persistence concerns separated according to `ARCHITECTURE.md`.
- Prefer application-level enum validation plus DB check constraints.
- Use optimistic locking only if introduced through an ADR; it is not part of v1 DB contract.
