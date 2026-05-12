# AI Study Leader Auth and Permissions v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.
- Retrospective/chat context boundary is authorized by [CR-20260512-retrospective-rag-boundary](./change-requests/CR-20260512-retrospective-rag-boundary.md) and [ADR-20260512-retrospective-rag-boundary](./adr/ADR-20260512-retrospective-rag-boundary.md).

## Roles and Statuses
| Concept | Values |
| --- | --- |
| Group permission | `OWNER`, `MEMBER` |
| Group member status | `PENDING_ONBOARDING`, `ACTIVE`, `LEFT` |
| Group status | `DRAFT`, `ONBOARDING`, `ACTIVE`, `COMPLETED`, `ARCHIVED` |

## Permission Matrix
| Action | Anonymous | Authenticated Non-Member | Pending Member | Active Member | Owner |
| --- | --- | --- | --- | --- | --- |
| Start Google OAuth redirect login | yes | yes | yes | yes | yes |
| Complete Google OAuth callback | yes | yes | yes | yes | yes |
| Exchange Google authorization code | yes | yes | yes | yes | yes |
| Refresh application token | yes | yes | yes | yes | yes |
| Logout current session | no | yes | yes | yes | yes |
| Logout all sessions | no | yes | yes | yes | yes |
| Read own user profile | no | yes | yes | yes | yes |
| Create group | no | yes | yes | yes | yes |
| Join group by invite | no | yes | yes | yes | yes |
| Read group summary | no | no | yes | yes | yes |
| Update group | no | no | no | no | yes |
| Submit own onboarding | no | no | yes | yes | yes |
| List member onboarding status | no | no | limited | yes | yes |
| Start study | no | no | no | no | yes |
| Read curriculum/current week | no | no | no | yes | yes |
| Read own week progress | no | no | no | yes | yes |
| Complete own task | no | no | no | yes | yes |
| Submit own incomplete reason | no | no | no | yes | yes |
| Request own retrospective | no | no | no | yes | yes |
| Chat with AI team leader | no | no | no | yes | yes |
| Read own notifications | no | yes | yes | yes | yes |
| Mark own notifications read | no | yes | yes | yes | yes |
| Read group notification logs | no | no | no | no | yes |
| Read LLM usage logs | no | no | no | no | yes |

`Read own week progress` is approved by [CR-20260512-week-progress-read-endpoint](./change-requests/CR-20260512-week-progress-read-endpoint.md) and [ADR-20260512-week-progress-read-endpoint](./adr/ADR-20260512-week-progress-read-endpoint.md).

## Data Visibility
- Members can read their own onboarding response.
- Owners can see onboarding completion status and aggregate summaries needed to start the study.
- Owners should not receive raw private notes beyond what is needed for group operation unless the product explicitly exposes them.
- Members can read their own retrospective and conversation records.
- AI context building for a member's retrospective/chat can use that member's own weekly progress, task completion records, incomplete reasons, and conversation summary. A conversation summary is an internally stored digest of that member's conversation, not the raw `ai_conversation_message` content.
- AI context building may use anonymized group-level aggregates that the product contract already allows for operation, such as completion rate, incomplete task count, and participation count.
- AI context building must not expose another member's identifiable private onboarding note, task completion note, incomplete reason, raw conversation message, or unaggregated conversation summary to a member unless a later permission change explicitly allows it.
- Members can read and acknowledge their own in-app notifications.
- Owners can read group-level operational records such as notification logs and LLM usage logs, but cannot mark another member's notifications read.

## State Rules
- A `PENDING_ONBOARDING` member can submit onboarding but cannot complete weekly tasks.
- An `ACTIVE` member can read and participate in current/future weeks.
- A `LEFT` member cannot create new progress, retrospective, conversation, or completion records.
- `ARCHIVED` groups are read-only except for owner/admin audit access.

## Security Requirements
- Cookie authentication changes in this locked document are approved by `CR-20260508-oauth2-cookie-login` and `ADR-20260508-oauth2-cookie-login`; future changes still require the Change Request + ADR flow in `docs/specs/change-control-v1.md`.
- Bearer token authentication or `studypot_access_token` HttpOnly cookie authentication is required for all `/api/v1` endpoints except explicit public auth endpoints and public invite metadata endpoints if added later.
- `GET /api/oauth2/authorization/google`, `GET /api/login/oauth2/code/google`, `POST /api/v1/auth/oauth/google`, and `POST /api/v1/auth/refresh` are explicit public auth endpoints.
- `POST /api/v1/auth/logout` and `POST /api/v1/auth/logout-all` require authenticated access via bearer token or access-token cookie.
- Browser login token cookies must be HttpOnly. Production cookies must also be Secure and must use a SameSite policy compatible with the deployed frontend/API domains.
- OAuth callback state mismatch must not issue application token cookies under any condition.
- Refresh tokens must be stored as hashes and rotated on refresh.
- A refresh token used after rotation or revocation must be rejected.
- Cross-group access must be rejected even if the resource ID exists.
- Service logic must verify that member, week, task, retrospective, and conversation belong to the same group.
- Member week progress read permission is authorized by `CR-20260512-week-progress-read-endpoint` and `ADR-20260512-week-progress-read-endpoint`.
