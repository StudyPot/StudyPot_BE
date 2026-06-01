# EXEC_PLAN: [feat] 인앱 알림 SSE 실시간 수신 API 추가

- Task slug: `spt-121-notification-sse-stream`
- Base branch: `develop`
- Feature branch: `codex/spt-121-notification-sse-stream`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-121-notification-sse-stream`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-121-notification-sse-stream`
- Jira issue: `SPT-121`
- Jira URL: https://studypot.atlassian.net/browse/SPT-121
- Jira summary: [notification] SSE 기반 인앱 알림 실시간 수신 API 추가
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/change-control-v1.md
- [x] docs/specs/change-request-template.md
- [x] docs/specs/adr-template.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/api-contract-v1.md
- [x] docs/specs/openapi.yaml
- [x] docs/specs/qa-acceptance-v1.md
- [x] docs/specs/feature-coverage-matrix.md
- [x] docs/operations/local-development.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] `notification`

## Doc Notes
- `notification-contract-v1.md` keeps MySQL `notification` as the durable source of truth and external channels out of MVP.
- `change-control-v1.md` requires Change Request + ADR for a new endpoint and notification delivery behavior change.
- `SPT-121` explicitly limits scope to existing `IN_APP` notification semantics, recipient-based SSE delivery, heartbeat/timeout cleanup, RabbitMQ compatibility after final row creation, and no FCM/Web Push/email/Discord/Kakao.

## Goal
Add an authenticated SSE stream at `GET /api/v1/users/me/notifications/stream` so a browser session can receive newly delivered `IN_APP` notifications for the authenticated recipient in real time, including notifications triggered by other groups. Keep the existing list/read APIs as the durable recovery path after reconnect.

## Approach
1. Record the locked-spec change with a small CR/ADR pair because the API shape and delivery behavior change.
2. Add focused failing tests first:
   - authenticated stream endpoint returns an SSE async response;
   - unauthenticated stream requests are rejected;
   - creating a delivered notification publishes exactly to the recipient stream;
   - publish failure does not fail row creation;
   - stream registry removes closed connections and does not leak resources.
3. Implement a Spring MVC `SseEmitter`-based in-process stream service keyed by `recipient_user_id`.
4. Publish `notification-created` only after `NotificationService.createNotification` successfully persists or resolves the delivered notification row.
5. Keep RabbitMQ compatibility by publishing from the final row-creation service path, not from enqueue code.
6. Update API/notification/OpenAPI/QA/feature-coverage docs and local-development notes.

## Step Plan
1. [x] Add CR/ADR documentation for the SSE notification stream.
2. [x] Add RED tests for controller, stream service, and create-notification publish behavior.
3. [x] Implement the stream publisher/service and controller endpoint.
4. [x] Update API contracts and OpenAPI examples.
5. [x] Run focused tests, then `./gradlew check build --no-daemon`.
6. [ ] Create PR, run CodeRabbit review, address one review round if needed, wait for review gate, finish auto-merge/cleanup.

## Verification
- RED: `./gradlew test --tests 'com.studypot.aistudyleader.notification.controller.NotificationControllerTest' --tests 'com.studypot.aistudyleader.notification.controller.NotificationStreamServiceTest' --tests 'com.studypot.aistudyleader.notification.service.NotificationServiceTest' --no-daemon` failed before implementation because `NotificationStreamConnection` and `NotificationStreamPublisher` did not exist.
- GREEN focused: same focused notification test command passed after implementation.
- GREEN full: `./gradlew check build --no-daemon` passed.

## Done Criteria
- `GET /api/v1/users/me/notifications/stream` is authenticated and returns `text/event-stream`.
- New delivered `IN_APP` notifications are sent as `notification-created` events to active streams for the notification recipient only.
- Notifications from any group reach the recipient's user-level stream.
- SSE send failures and disconnected clients do not roll back notification row creation.
- Stream completion, timeout, and send errors remove server-side registrations.
- Existing notification list/read APIs remain compatible and can recover missed events after reconnect.
- Required docs and OpenAPI are updated under the change-control process.
- Focused tests and `./gradlew check build --no-daemon` pass.
