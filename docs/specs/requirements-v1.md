# AI Study Leader Requirements v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Retrospective/chat context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).
- Onboarding simplification and auto-merge harness behavior are authorized by [CR-20260520-onboarding-simplification-auto-merge](./change-requests/CR-20260520-onboarding-simplification-auto-merge.md) and [ADR-20260520-onboarding-simplification-auto-merge](./adr/ADR-20260520-onboarding-simplification-auto-merge.md).
- Fixed one-week sprint windows are authorized by [CR-20260601-fixed-weekly-sprint-windows](./change-requests/CR-20260601-fixed-weekly-sprint-windows.md) and [ADR-20260601-fixed-weekly-sprint-windows](./adr/ADR-20260601-fixed-weekly-sprint-windows.md).

## Priority Definitions
| Priority | Meaning |
| --- | --- |
| P0 | Required for MVP implementation. |
| P1 | Important but can follow after the first backend MVP path works. |
| P2 | Explicitly deferred from MVP. |

## P0 Functional Requirements
| Feature ID | Req ID | Requirement | Acceptance |
| --- | --- | --- | --- |
| `identity-core` | `REQ-ID-001` | Users authenticate and own application profile data. | Google OAuth code exchange creates or updates `users`, `oauth_account`, and application token records. |
| `identity-core` | `REQ-ID-002` | Users can refresh and revoke application sessions. | `refresh_token` stores hashed refresh tokens, expiry, revocation state, and supports rotation plus logout/logout-all revocation. |
| `study-group-core` | `REQ-GRP-001` | Host can create a study group. | Required inputs are name, topic, detail keywords, max members, starts/ends dates. |
| `study-group-core` | `REQ-GRP-002` | Created group enters onboarding flow. | `study_group.status = ONBOARDING`, owner member is `PENDING_ONBOARDING`. |
| `study-group-core` | `REQ-INV-001` | Host can share invite link/code. | Invite code is unique and creates pending member records. |
| `group-onboarding` | `REQ-ONB-001` | Host and members submit group-specific onboarding. | Public response stores overall skill level, note, availability slots, and submitted timestamp. The backend may project the skill level into internal keyword scores for curriculum context. |
| `group-onboarding` | `REQ-ONB-002` | Onboarding stores recurring availability slots. | Slots include day of week, start time, end time, and timezone. |
| `group-onboarding` | `REQ-ONB-003` | Onboarding skill level uses a 1 to 5 scale. | Invalid values are rejected. |
| `curriculum-core` | `REQ-CUR-001` | Host can start study after onboarding begins. | Start does not require all members to submit onboarding. |
| `curriculum-core` | `REQ-CUR-002` | AI curriculum uses submitted onboarding responses. | `curriculum.onboarding_summary` stores generation context, and generated weeks must match the fixed one-week sprint windows derived from the group period. |
| `weekly-todo` | `REQ-TODO-001` | Curriculum weeks contain weekly tasks. | Tasks have type, order, title, required flag, and due timestamp equal to the planned sprint window end. |
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

## Retrospective and AI Context Boundary
- `retrospective-feedback` owns the final retrospective result and status. It is not replaced by chat history.
- `ai-team-leader` chat is an input/support interface. A `RETROSPECTIVE` conversation can contribute context to a retrospective, but the retrospective remains the canonical output.
- MVP retrieval for retrospective/chat is DB-first: collect current week, weekly tasks, member week progress, task completions, incomplete reasons, group rules/violations, prior retrospectives, prior next-week adjustments, onboarding summary, and conversation summary through backend repositories before calling an LLM provider.
- Vector or graph retrieval for unstructured learning materials is deferred until official docs, lecture links, group files, or reusable curriculum examples become product data.

## P2 / Deferred
| Area | Reason |
| --- | --- |
| Live meeting assistant | Out of MVP; requires real-time UX and latency contract. |
| Voice transcription | Out of MVP; requires separate privacy and storage policy. |
| Heavy synchronous meeting automation | MVP is asynchronous onboarding/todo/feedback. |
| Automatic full curriculum regeneration for late joiners | Late joiner context is applied to future adjustment only. |
| Discord integration and bot delivery | Out of MVP; in-app notification is the first notification surface. |
| Vector/graph retrieval infrastructure | Deferred until non-structured study material exists and SPT-82 or a later task approves the service boundary. |
| Configurable sprint duration | Current MVP sprint unit is fixed to one week; changing the unit requires a later API/product contract. |
