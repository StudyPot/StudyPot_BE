# EXEC_PLAN: [fix] in-process 알림 발행 SSE 푸시 유실 수정

- Task slug: `inproc-sse-fix`
- Base branch: `develop`
- Feature branch: `codex/inproc-sse-fix`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/inproc-sse-fix`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/inproc-sse-fix`
- Jira issue: `SPT-141`
- Jira URL: https://studypot.atlassian.net/browse/SPT-141
- Jira summary: [fix] in-process 알림 발행 SSE 푸시 유실 수정 (실시간 알림 복구)
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/adr/ADR-20260601-notification-sse-stream.md
- [x] docs/specs/notification-contract-v1.md
- [ ] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] notification
- [ ] <feature-id>

## Doc Notes
- ADR-20260601-notification-sse-stream: SSE 는 IN_APP 알림의 실시간 전송 수단이고, `notification-created` 는 `NotificationService.createNotification` 이 새 알림을 영속화한 뒤 발행한다고 결정돼 있다. 본 수정은 그 계약을 깨지 않고, in-process 발행 경로에서 그 발행이 유실되던 버그를 고친다. 멱등 재생(replay)은 신규 이벤트로 발행하지 않는다는 규칙도 유지(newlyCreated 일 때만 푸시).
- notification-contract-v1: 실패는 핵심 트랜잭션을 롤백하지 않는다 — createNotificationSafely 는 예외를 잡아 삼킨다(유지). 응답/스키마 변경 없음.

## Goal
RabbitMQ 비활성(in-process) 경로에서 실시간 SSE 알림이 가지 않고 30초 폴링으로만 도착하던 문제를 고친다. 프로덕션(rumiclean)은 현재 알림 RabbitMQ 를 끈 상태라 이 경로가 활성이며, 사용자가 "알림이 10초 넘게 늦는다"고 보고했다.

## Approach
- 근본 원인: `NotificationService.createNotificationSafely` 가 `@Transactional` 인 `createNotification` 을 **같은 빈 내부 호출**(프록시 우회)로 부른다. 그래서 createNotification 이 자체 트랜잭션 없이 "업무 트랜잭션의 afterCommit 단계"에서 실행되고, 그 안에서 SSE 푸시를 다시 `afterCommit` 으로 등록하면 진행 중인 콜백 순회에 잡히지 않아 유실된다. (RabbitMQ 워커 경로는 createNotification 을 프록시로 호출 → 자체 트랜잭션 → afterCommit 정상 발화하므로 영향 없음.)
- 수정: 저장 로직을 `persistNotification(command)` 로 추출(저장 + 신규여부 반환). 워커 경로의 `createNotification` 은 그대로 afterCommit 푸시. in-process 경로의 `createNotificationSafely` 는 이미 커밋 후이므로 **저장 직후 즉시** `streamPublisher.publishNotificationCreated(...)` 를 호출한다(afterCommit 재등록 안 함). 신규 생성일 때만 푸시(멱등 재생 제외).

## Step Plan
1. `NotificationService` 에 `persistNotification` + `PersistedNotification` 레코드 추출.
2. `createNotification`(워커 경로): persist 후 newlyCreated 면 afterCommit 푸시(기존 동작 유지).
3. `createNotificationSafely`(in-process 경로): persist 후 newlyCreated 면 즉시 푸시.
4. 회귀 테스트 추가: afterCommit 중 콜백 순회(캡처 목록) 상황을 모사해, in-process 발행이 SSE 이벤트를 유실 없이 내보내는지 검증(수정 전 실패, 수정 후 통과 확인됨).
5. `./gradlew check build` 그린 확인.

## Done Criteria
- in-process 경로에서 새 알림 생성 시 SSE `notification-created` 가 즉시 발행된다(폴링 의존 제거).
- 멱등 재생은 SSE 이벤트를 발행하지 않는다.
- RabbitMQ 워커 경로의 기존 동작/테스트가 그대로 통과한다.
- 신규 회귀 테스트가 통과하고, 수정 전에는 실패함을 확인.
- `./gradlew check build` 그린.
