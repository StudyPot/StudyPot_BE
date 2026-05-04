# AI Study Leader Requirements v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

## Priority Definitions
| Priority | Meaning |
| --- | --- |
| P0 | Required for MVP implementation. |
| P1 | Important but can follow after the first backend MVP path works. |
| P2 | Explicitly deferred from MVP. |

## P0 Functional Requirements
| Feature ID | Req ID | Requirement | Acceptance |
| --- | --- | --- | --- |
| `identity-core` | `REQ-ID-001` | Users authenticate and own application profile data. | `users`, `oauth_account`, and `refresh_token` support login/session lifecycle. |
| `identity-core` | `REQ-ID-002` | Users can refresh and revoke application sessions. | `refresh_token` stores hashed refresh tokens, expiry, and revocation state. |
| `study-group-core` | `REQ-GRP-001` | Host can create a study group. | Required inputs are name, topic, detail keywords, max members, starts/ends dates. |
| `study-group-core` | `REQ-GRP-002` | Created group enters onboarding flow. | `study_group.status = ONBOARDING`, owner member is `PENDING_ONBOARDING`. |
| `study-group-core` | `REQ-INV-001` | Host can share invite link/code. | Invite code is unique and creates pending member records. |
| `group-onboarding` | `REQ-ONB-001` | Host and members submit group-specific onboarding. | Response stores keyword skills, task preferences, note, and submitted timestamp. |
| `group-onboarding` | `REQ-ONB-002` | Onboarding stores recurring availability slots. | Slots include day of week, start time, end time, and timezone. |
| `group-onboarding` | `REQ-ONB-003` | Skill and task preference scores use 1 to 5 scale. | Invalid values are rejected. |
| `curriculum-core` | `REQ-CUR-001` | Host can start study after onboarding begins. | Start does not require all members to submit onboarding. |
| `curriculum-core` | `REQ-CUR-002` | AI curriculum uses submitted onboarding responses. | `curriculum.onboarding_summary` stores generation context. |
| `weekly-todo` | `REQ-TODO-001` | Curriculum weeks contain weekly tasks. | Tasks have type, order, title, required flag, and due timestamp. |
| `weekly-todo` | `REQ-TODO-002` | Members can complete todos before deadline. | Completion timestamp and note are stored. |
| `weekly-todo` | `REQ-TODO-003` | Members must submit incomplete reason after deadline. | Incomplete reason and submission timestamp are stored. |
| `retrospective-feedback` | `REQ-RETRO-001` | Retrospective is created from weekly progress. | Trigger can be week end, incomplete modal, user chat, or manual request. |
| `retrospective-feedback` | `REQ-RETRO-002` | AI feedback can propose next-week adjustment. | Feedback and adjustment are stored as JSON. |
| `ai-team-leader` | `REQ-AI-001` | AI can suggest detail keywords. | Suggestions are not persisted as candidates unless selected by user. |
| `ai-team-leader` | `REQ-AI-002` | AI chat stores messages and summary. | Messages link to conversation and LLM usage. |
| `notification` | `REQ-NOTI-001` | System creates in-app notifications for onboarding, weekly todo, incomplete reason, retrospective, and AI feedback events. | Notification stores recipient, title/body, payload, related resource IDs, idempotency key, status, delivered timestamp, and read timestamp. |
| `notification` | `REQ-NOTI-002` | Members can list and mark their own notifications as read. | Unread and read states are queryable by recipient user. |

## P1 Requirements
| Feature ID | Req ID | Requirement |
| --- | --- | --- |
| `study-group-rules` | `REQ-RULE-001` | Group rules can define task deadline and retrospective policies. |
| `notification` | `REQ-NOTI-003` | External delivery channels can be added after in-app notification is stable. |
| `ai-team-leader` | `REQ-AI-003` | LLM usage can be aggregated by group, user, purpose, and UTC date. |

## P2 / Deferred
| Area | Reason |
| --- | --- |
| Live meeting assistant | Out of MVP; requires real-time UX and latency contract. |
| Voice transcription | Out of MVP; requires separate privacy and storage policy. |
| Heavy synchronous meeting automation | MVP is asynchronous onboarding/todo/feedback. |
| Automatic full curriculum regeneration for late joiners | Late joiner context is applied to future adjustment only. |
| Discord integration and bot delivery | Out of MVP; in-app notification is the first notification surface. |
