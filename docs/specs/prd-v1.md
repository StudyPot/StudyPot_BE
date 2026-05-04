# AI Study Leader PRD v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Change rule: after lock, product scope changes require a Change Request and ADR as defined in `docs/specs/change-control-v1.md`.

## Objective
Deliver the backend MVP for an AI-assisted study leader that turns group setup and member onboarding into a weekly curriculum, todo tracking, and retrospective feedback loop.

## Problem
Study groups lose momentum because the leader role is manual: collecting member levels, assigning tasks, tracking completion, asking why work was not done, and reflecting that feedback into the next week. The MVP automates that loop without making meetings a hard dependency.

## Goals
- A host can create a study group with topic, detail keywords, max members, and study period.
- A host can share an invite link.
- Every group member, including the host, can submit onboarding.
- A host can start the study even if not every invitee has completed onboarding.
- AI curriculum generation uses onboarding responses submitted at start time.
- Members can complete weekly todo items or provide incomplete reasons.
- AI team leader feedback can be requested through retrospective and chat flows.
- AI team leader feedback can adjust the next week's difficulty, todo shape, and support materials.
- Late joiners can complete onboarding and join from the current week.
- In-app notifications help members notice onboarding, weekly deadlines, incomplete reasons, retrospective readiness, and AI feedback.

## Non-Goals
- Live meeting assistant.
- Voice meeting transcription.
- Full frontend design.
- Calendar scheduling optimization.
- Automatic full curriculum regeneration every time a late member joins.
- Discord integration, bot, channel delivery, and Discord token storage.
- Payment, billing, or enterprise administration.

## Personas
| Persona | Need |
| --- | --- |
| Host | Create the group, invite members, start the curriculum, monitor progress. |
| Member | Submit onboarding, follow weekly todos, complete or explain incomplete work, receive feedback. |
| AI Team Leader | Suggest detail keywords, create curriculum, summarize progress, produce retrospective feedback, and propose next-week adjustments every week. |

## MVP Flow
| Step | Product Behavior | Primary Data |
| --- | --- | --- |
| Group creation | Host enters name, topic, detail keywords, max members, period. | `study_group` |
| Invite | System exposes invite code/link and creates member on join. | `study_group.invite_code`, `group_member` |
| Onboarding | Member submits skill, task preference, availability, note. | `group_onboarding_response`, `member_availability_slot` |
| Host start | Group becomes active and AI curriculum is generated. | `curriculum`, `curriculum_week`, `weekly_task` |
| Weekly execution | Member completes tasks or submits incomplete reason. | `member_week_progress`, `task_completion` |
| Retrospective | AI feedback and next-week adjustment are stored. | `retrospective`, `ai_conversation` |
| Notification | Reminders and status events are delivered in-app and tracked with read state. | `notification` |

## Success Criteria
- A group can reach `ACTIVE` through the full onboarding start path.
- Curriculum generation records the onboarding summary used.
- Weekly todo completion and incomplete reason flows are auditable.
- AI feedback and chat records are linked to member/week context.
- AI feedback can propose audited next-week adjustments after every week.
- In-app notifications surface onboarding, todo deadline, incomplete reason, retrospective, and AI feedback events.
- Jira implementation tasks reference the same source documents and labels.
