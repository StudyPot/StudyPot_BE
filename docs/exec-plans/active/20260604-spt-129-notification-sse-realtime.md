# EXEC_PLAN: [notification] SSE 실시간 알림 수신 실패 복구 및 화면 토스트 검증

- Task slug: `spt-129-notification-sse-realtime`
- Base branch: `develop`
- Feature branch: `codex/spt-129-notification-sse-realtime`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-129-notification-sse-realtime`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-129-notification-sse-realtime`
- Jira issue: `SPT-129`
- Jira URL: https://studypot.atlassian.net/browse/SPT-129
- Jira summary: [notification] SSE 실시간 알림 수신 실패 복구 및 화면 토스트 검증
- Frontend worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/StudyPot_FE-spt-129-notification-sse-realtime`
- Frontend branch: `codex/spt-129-notification-sse-realtime`
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/adr/ADR-20260601-notification-sse-stream.md
- [x] docs/specs/change-requests/CR-20260601-notification-sse-stream.md
- [x] docs/operations/local-development.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md
- [x] docs/operations/obsidian-error-ledger.md

## Related Feature IDs
- [x] notification

## Doc Notes
- Notification v1 keeps MySQL as the durable source of truth and treats SSE as a best-effort app-open transport.
- `notification-created` must be emitted after a newly delivered `IN_APP` row is created for the authenticated recipient.
- Idempotency replays must not emit a duplicate realtime event.
- Clients must reconcile missed events with `GET /api/v1/users/me/notifications` after reconnect.
- RabbitMQ dispatch remains compatible because the final `NotificationService.createNotification` row-creation path owns SSE publication.
- Current production triage showed RabbitMQ enabled, queue consumer attached, notification rows created, and repeated `/users/me/notifications/stream` async timeouts.
- Jira `SPT-129` was created for this recovery work and converted to the harness-allowed `작업` issue type before `init-task.sh` moved it to `진행 중`.
- Backend implementation already had a concrete `NotificationStreamService` bean implementing `NotificationStreamPublisher`; a configuration regression test now proves `NotificationService` receives it instead of the noop fallback when the bean exists.
- Frontend root cause was a recovery gap: after EventSource error/timeout the store closed the stream and started polling fallback but did not retry SSE, and reconciliation did not toast new notifications when the first fetch returned an empty list.

## Goal
Make realtime in-app notifications work end-to-end across backend and frontend: when a new notification is created for the logged-in recipient, the browser receives `notification-created` over SSE and shows the notification toast without requiring a manual refresh.

## Approach
- Keep the locked notification contract intact: no DB/API enum/product contract expansion.
- Start with reproduction tests for the observed failure class: SSE connection lifecycle and client reconnection/reconciliation.
- Add backend observability and lifecycle safeguards only where they prove or preserve the existing contract: stream registration/removal, publish active-connection count, and send outcome.
- Update the frontend notification store so EventSource errors/timeouts do not permanently disable realtime delivery; use polling only as recovery/reconciliation and retry SSE with bounded backoff.
- Verify with targeted backend and frontend tests, then run the full backend verification command and frontend build/test commands.
- Finish with a browser-level smoke that triggers or simulates a new notification and confirms a visible toast.

## Step Plan
1. [x] Inspect current backend notification SSE classes, frontend notification store, and existing tests.
2. [x] Create failing backend tests for stream lifecycle/diagnostic behavior if backend changes are required.
3. [x] Create failing frontend tests for EventSource reconnect/reconciliation and toast insertion on `notification-created`.
4. [x] Implement the smallest backend and frontend changes needed to pass those tests.
5. [x] Run targeted backend tests for notification service/controller/rabbit worker and targeted frontend unit tests.
6. [x] Run `./gradlew check build --no-daemon` in this worktree.
7. [x] Run frontend build/test verification in the frontend branch/worktree.
8. [x] Start local frontend smoke path to confirm the browser receives a realtime notification toast from a `notification-created` EventSource event.
9. [ ] Create PRs or otherwise prepare merge/deploy evidence according to each repo's workflow.

## Done Criteria
- Backend tests prove new notification rows publish `notification-created` to the recipient stream and idempotency replays do not duplicate events.
- Backend logs or diagnostics can distinguish stream subscribe/remove, publish active connection count, and send failures without exposing secrets.
- Frontend tests prove `notification-created` updates the store and shows a toast.
- Frontend tests prove SSE error/timeout recovery attempts realtime reconnect and reconciles missed notifications.
- Manual or automated browser smoke confirms a new notification appears as a toast in the UI.
- Backend `./gradlew check build --no-daemon` passes.
- Frontend build and relevant tests pass, or pre-existing unrelated failures are documented with evidence.

## Verification
- Backend targeted notification tests: `./gradlew test --tests 'com.studypot.aistudyleader.notification.service.NotificationApplicationConfigurationTest' --tests 'com.studypot.aistudyleader.notification.controller.NotificationStreamServiceTest' --tests 'com.studypot.aistudyleader.notification.controller.NotificationControllerTest' --tests 'com.studypot.aistudyleader.notification.service.NotificationServiceTest' --tests 'com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationConfigurationTest' --tests 'com.studypot.aistudyleader.notification.infrastructure.rabbitmq.RabbitNotificationWorkerTest' --no-daemon` passed.
- Backend full verification: `./gradlew check build --no-daemon` passed.
- Frontend notification RED check: the new notification store tests failed before the frontend fix for empty-initial reconciliation toast and SSE error reconnect.
- Frontend unit tests: `npm run test:unit -- --run` passed, 16 files and 64 tests.
- Frontend build: `npm run build` passed with `vue-tsc --build` and Vite production build.
- Browser smoke: `npx playwright test e2e/notification-toast.spec.ts --project=chromium` passed and confirmed a visible toast for `notification-created`.
