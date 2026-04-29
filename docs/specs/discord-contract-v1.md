# Discord Contract v1

## Lock Status
- Status: `LOCKED_FOR_IMPLEMENTATION`
- MVP scope: notification-only.
- Slash commands and Discord-first UX are post-MVP.
- Changes require Change Request and ADR.

## Supported Notification Types
| Type | Trigger | Recipient | Required Payload |
| --- | --- | --- | --- |
| `session_reminder` | Session reminder offset arrives. | Connected channel. | group, session, scheduled time, action link. |
| `prep_brief` | Preparation brief is generated. | Connected channel. | session, brief summary, link. |
| `feedback_ready` | Feedback report is generated. | Connected channel. | session, report type, link. |
| `action_item_due` | Action item due window arrives. | Connected channel. | action item, assignee display name, due date. |

## Channel Connection
- Owners and managers can connect Discord channels.
- Backend stores:
  - `discord_guild_id`
  - `discord_channel_id`
  - `channel_name`
  - `status`
  - `notification_settings`
- Discord bot access validation happens before channel status becomes `active`.

## `notification_settings` Shape
```json
{
  "enabledTypes": ["session_reminder", "prep_brief", "feedback_ready", "action_item_due"],
  "reminderOffsetsMinutes": [1440, 60, 10],
  "mentionPolicy": {
    "mentionEveryone": false,
    "mentionAssignees": true
  }
}
```

Validation rules:
- `enabledTypes` values are limited to supported notification types.
- `mentionEveryone` defaults to false.
- Reminder offsets must be positive integers.

## Notification Log Contract
Every attempted notification creates or updates `discord_notification_logs`.

| Status | Meaning |
| --- | --- |
| `pending` | Scheduled but not sent. |
| `sent` | Discord accepted the message. |
| `failed` | Send attempt failed. |
| `skipped` | Notification was intentionally skipped by settings or business rule. |

`payload` must be redacted and must not include:
- Discord bot token
- OAuth token
- private AI input snapshot
- raw user secret

## Retry Rules
- Retry only `failed` rows caused by transient Discord/API errors.
- Do not retry `skipped` rows.
- If bot/channel access is revoked, mark channel `revoked` and log failure.
- Discord delivery failure must not roll back the business event that triggered it.

## Message Content Rules
- Messages are concise and action-oriented.
- Messages include a web client link when the action requires UI.
- Individual AI feedback is never posted directly into channel text.
- Channel messages may announce that feedback is ready, but members must open the web client to view private individual feedback.

## Post-MVP
- Slash commands.
- Direct messages.
- Discord-native note submission.
- Interactive Discord buttons.
