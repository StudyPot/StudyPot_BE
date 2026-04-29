# Auth And Permissions v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- Login: Google OAuth.
- Discord: account/channel link for notification routing.
- Changes require Change Request and ADR.

## Identity Model
- `users` is the core application identity.
- `user_oauth_accounts` stores Google OAuth identity.
- `user_discord_accounts` stores linked Discord account identity.
- Discord IDs are not stored on core study tables.

## Authentication Rules
- Anonymous users can only start and complete OAuth flows.
- All `/api/v1` endpoints except OAuth require authenticated user context.
- Session/cookie versus bearer token storage is selected during Spring security implementation, but endpoint behavior and permission checks are locked here.
- Current user identity is always available through `GET /api/v1/users/me`.

## Group Roles
| Role | Meaning |
| --- | --- |
| `owner` | Full group administration and archive authority. |
| `manager` | Operational authority for sessions, invitations, goals, resources, AI generation, and Discord channel settings. |
| `member` | Participation authority for attendance, notes, own action items, progress logs, and visible resources. |

## Permission Matrix
| Capability | Owner | Manager | Member |
| --- | --- | --- | --- |
| Read group | yes | yes | yes |
| Update group name/description/timezone | yes | yes | no |
| Update group rules/schedule defaults | yes | yes | no |
| Archive group | yes | no | no |
| Invite member | yes | yes | no |
| Update member role | yes | no | no |
| Remove member | yes | yes | no |
| Create/update session | yes | yes | no |
| Submit own attendance | yes | yes | yes |
| Update another member attendance | yes | yes | no |
| Submit own notes | yes | yes | yes |
| Read group notes | yes | yes | yes |
| Create action item for self | yes | yes | yes |
| Assign action item to others | yes | yes | no |
| Update own assigned action item | yes | yes | yes |
| Manage goals | yes | yes | own member goal only |
| Add resources | yes | yes | yes |
| Generate AI preparation brief | yes | yes | no |
| Generate AI feedback report | yes | yes | no |
| Read group AI feedback | yes | yes | yes |
| Read individual AI feedback for others | yes | yes | no |
| Connect Discord channel | yes | yes | no |
| Read notification logs | yes | yes | no |

## Access Failure Rules
- No authentication: `401`.
- Not a group member: `403`.
- Soft-deleted resource: `404`.
- Active member required but member is `left`, `removed`, or `paused`: `403`.
- Private individual AI report requested by another member: `403`.

## Privacy Rules
- Individual feedback is visible to:
  - target member
  - owner
  - manager
- Group feedback is visible to all active group members.
- Discord channel messages never include individual feedback body.
- Redacted AI snapshots are visible only through internal admin/debug tooling, not MVP user APIs.
