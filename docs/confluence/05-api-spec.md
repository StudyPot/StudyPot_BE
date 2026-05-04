# 05 API 명세

## Core Resources
- `/api/v1/groups`
- `/api/v1/groups/{groupId}/join`
- `/api/v1/groups/{groupId}/onboarding/me`
- `/api/v1/groups/{groupId}/start`
- `/api/v1/groups/{groupId}/curriculum`
- `/api/v1/groups/{groupId}/weeks/current`
- `/api/v1/tasks/{taskId}/completion/me`
- `/api/v1/weeks/{weekId}/retrospectives/me`
- `/api/v1/groups/{groupId}/ai-conversations`
- `/api/v1/ai-conversations/{conversationId}/messages`
- `/api/v1/users/me/notifications`
- `/api/v1/notifications/{notificationId}/read`
- `/api/v1/users/me/notifications/read-all`
- `/api/v1/groups/{groupId}/notifications`
- `/api/v1/groups/{groupId}/llm-usage`

## Source
- Human contract: `docs/specs/api-contract-v1.md`
- Machine contract: `docs/specs/openapi.yaml`
