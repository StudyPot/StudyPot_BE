# AI Study Leader PRD v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Locked date: `2026-04-29`
- Change rule: after lock, product scope changes require a Change Request and ADR as defined in `docs/specs/change-control-v1.md`.
- Source package:
  - `docs/specs/product-brief.md`
  - `docs/specs/requirements-v1.md`
  - `docs/specs/domain-erd.md`
  - `docs/specs/api-contract-v1.md`
  - `docs/specs/openapi.yaml`
  - `docs/specs/ai-contract-v1.md`
  - `docs/specs/discord-contract-v1.md`
  - `docs/specs/auth-permissions-v1.md`
  - `docs/specs/qa-acceptance-v1.md`

## Problem
Small study groups often fail because the leader role is repetitive and fragile. Someone must remind members, prepare the agenda, collect progress, notice blockers, and summarize follow-up items. When that person gets busy, the group loses rhythm.

AI Study Leader provides a backend system that makes the leader role repeatable. It stores group rules, sessions, structured notes, Discord notification settings, and AI preparation/feedback outputs so study teams can keep moving without relying on one human coordinator.

## Target Users
- Bootcamp and SSAFY-style learners in small algorithm, CS, or project study groups.
- Side-study groups with 2 to 6 members, optimized for 2 to 3 active members.
- Members who want accountability and practical feedback without running live voice automation.

## Product Goals
- Help a group create a stable study operating rhythm.
- Make every session have clear preparation prompts and follow-up actions.
- Reduce manual reminder work through Discord notifications.
- Preserve structured meeting history for AI feedback and progress tracking.
- Keep MVP backend implementation bounded and testable.

## Success Criteria
- A user can sign in with Google and link a Discord account.
- A group owner can create a study group, invite members, set rules, and connect a Discord notification channel.
- A group can schedule sessions and record attendance, notes, action items, goals, and resources.
- The system can persist AI preparation briefs and AI feedback reports using provider-neutral schemas.
- Discord notification logs record delivery attempts and failures.
- Every MVP feature maps to `feature_id`, API endpoints, DB tables, and QA scenarios.

## MVP Scope
| Area | Included |
| --- | --- |
| Identity | Google OAuth login, current user profile, Discord account link/unlink. |
| Groups | Create/update/archive groups, member roles, invitations, JSONB rules, schedule defaults. |
| Sessions | Schedule/update sessions, attendance, structured notes, action items. |
| Learning | Goals, member progress logs, shared resources. |
| AI | Preparation brief and feedback report contracts with persisted prompt runs. |
| Discord | Channel connection, notification settings, notification logs, retryable failure state. |
| QA | Feature-level acceptance scenarios and release checklist. |

## Non-Scope
| Area | Reason |
| --- | --- |
| Real-time STT and voice transcription | MVP focuses on structured asynchronous inputs. |
| Live meeting assistant | Requires a separate latency and UX model. |
| Discord slash commands | Notification-only MVP keeps Discord integration small. |
| Billing | No monetization requirement for v1. |
| Uploaded files | URL-based study resources are sufficient for v1. |
| Group rule version history | Rules live in `study_groups.rules JSONB`; version history is post-MVP. |
| Frontend implementation | Backend contracts are defined here; UI is a separate client. |

## Platform Decisions
- Backend: Java 21, Gradle, Spring Boot.
- API: REST `/api/v1`, OpenAPI 3.1, JSON.
- Error format: `application/problem+json`.
- Database: PostgreSQL, UUIDv7 supplied by application code, JSONB for flexible structured values.
- Auth: Google OAuth for app login; Discord account link for notifications.
- AI: provider-neutral contract, fixed JSON schemas, configurable provider/model.
- Discord: notification-only MVP.

## User Roles
- `owner`: full group administration, member management, Discord channel management, destructive group actions.
- `manager`: session, invitation, goal, resource, AI generation, and notification configuration within a group.
- `member`: session participation, notes, attendance, own action items, own progress logs, and visible group resources.

## Release Readiness
V1 is ready for backend implementation when:
- OpenAPI parses successfully.
- DDL draft has all 18 ERD tables.
- Feature matrix maps every feature to PRD, requirements, API, DB, and QA.
- No unlocked scope questions remain in v1 docs.
- Obsidian `Current State` points to the locked source documents.
