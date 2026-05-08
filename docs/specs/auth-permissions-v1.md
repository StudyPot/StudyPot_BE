# AI Study Leader Auth and Permissions v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Source: Requirements v0.3, ERD v0.8 MySQL8.
- Changes require Change Request and ADR.

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
| Complete own task | no | no | no | yes | yes |
| Submit own incomplete reason | no | no | no | yes | yes |
| Request own retrospective | no | no | no | yes | yes |
| Chat with AI team leader | no | no | no | yes | yes |
| Read own notifications | no | yes | yes | yes | yes |
| Mark own notifications read | no | yes | yes | yes | yes |
| Read group notification logs | no | no | no | no | yes |
| Read LLM usage logs | no | no | no | no | yes |

## Data Visibility
- Members can read their own onboarding response.
- Owners can see onboarding completion status and aggregate summaries needed to start the study.
- Owners should not receive raw private notes beyond what is needed for group operation unless the product explicitly exposes them.
- Members can read their own retrospective and conversation records.
- Members can read and acknowledge their own in-app notifications.
- Owners can read group-level operational records such as notification logs and LLM usage logs, but cannot mark another member's notifications read.

## State Rules
- A `PENDING_ONBOARDING` member can submit onboarding but cannot complete weekly tasks.
- An `ACTIVE` member can participate in current/future weeks.
- A `LEFT` member cannot create new progress, retrospective, conversation, or completion records.
- `ARCHIVED` groups are read-only except for owner/admin audit access.

## Security Requirements
- Bearer token authentication or `studypot_access_token` HttpOnly cookie authentication is required for all `/api/v1` endpoints except explicit public auth endpoints and public invite metadata endpoints if added later.
- `GET /api/oauth2/authorization/google`, `GET /api/login/oauth2/code/google`, `POST /api/v1/auth/oauth/google`, and `POST /api/v1/auth/refresh` are explicit public auth endpoints.
- `POST /api/v1/auth/logout` and `POST /api/v1/auth/logout-all` require authenticated access via bearer token or access-token cookie.
- Browser login token cookies must be HttpOnly. Production cookies should also be Secure and use a SameSite policy compatible with the deployed frontend/API domains.
- OAuth callback state mismatch must not issue application token cookies.
- Refresh tokens must be stored as hashes and rotated on refresh.
- A refresh token used after rotation or revocation must be rejected.
- Cross-group access must be rejected even if the resource ID exists.
- Service logic must verify that member, week, task, retrospective, and conversation belong to the same group.
