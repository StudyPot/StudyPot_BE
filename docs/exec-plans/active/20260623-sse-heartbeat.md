# EXEC_PLAN: [fix] 알림 SSE keepalive 하트비트 + 타임아웃 graceful 처리

- Task slug: `sse-heartbeat`
- Base branch: `develop`
- Feature branch: `codex/sse-heartbeat`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/sse-heartbeat`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/sse-heartbeat`
- Jira issue: `SPT-140`
- Jira URL: https://studypot.atlassian.net/browse/SPT-140
- Jira summary: [fix] 알림 SSE keepalive 하트비트 + 타임아웃 graceful 처리
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
- ADR-20260601-notification-sse-stream: SSE는 IN_APP 알림의 실시간 전송 수단이며, 연결은 `recipient_user_id`로 키잉하고 completion/timeout/send 실패/error 시 등록을 해제하도록 결정됨. 본 작업은 이 결정을 깨지 않고, 누락돼 있던 keepalive 하트비트를 추가하고 timeout 시 emitter를 명시적으로 complete 하는 견고성 보강이다. 알림 채널 추가가 아니며 계약/enum/DB 변경 없음.
- notification-contract-v1: IN_APP 알림 응답 계약은 변경하지 않는다. 하트비트는 SSE 코멘트(`: heartbeat`)로 전송되어 EventSource 가 무시하므로 클라이언트 이벤트 계약에 영향이 없다.
- ARCHITECTURE/docs/index: 단일 Spring Boot API 프로세스 전제 유지. 멀티 인스턴스 팬아웃은 ADR대로 본 작업 범위 밖이다.

## Goal
프로덕션(rumiclean) 알림 SSE 스트림을 운영 환경에서 안정적으로 유지한다. 현재 keepalive 하트비트가 없어 (1) 30분 emitter 타임아웃마다 `AsyncRequestTimeoutException` WARN 이 쌓이고, (2) 리버스 프록시/NAT 유휴 종료로 끊긴 죽은 연결을 다음 알림 발행(최대 30분)까지 감지하지 못한다. 이를 제거한다.

## Approach
- `NotificationStreamService` 에 `@Scheduled` 하트비트를 추가해 설정 가능한 주기(기본 20초)로 모든 활성 SSE 연결에 SSE 코멘트 핑을 보낸다. 전송 실패 시 즉시 해당 연결을 제거·정리한다(끊긴 연결 조기 감지).
- `onTimeout` 콜백에서 `emitter.complete()` 를 먼저 호출해 MVC 비동기 계층의 `AsyncRequestTimeoutException` WARN 소음을 제거한 뒤 정리한다.
- `NotificationStreamConnection` 인터페이스에 `sendHeartbeat()` 와 `complete()` 를 추가하고 SSE 구현체에 위임한다.
- 주기는 `studypot.notification.stream.heartbeat-ms`(env `STUDYPOT_NOTIFICATION_STREAM_HEARTBEAT_MS`)로 노출한다.
- 기존 SSE 동작/계약(이벤트 이름, 응답 스키마, 인증, Caddy gzip 제외)은 변경하지 않는다.

## Step Plan
1. `NotificationStreamConnection` 에 `sendHeartbeat()`/`complete()` 추가, SSE 구현체에 위임 메서드 구현.
2. `onTimeout` 핸들러를 `complete()` 후 cleanup 으로 변경.
3. `@Scheduled` `sendHeartbeats()` 추가 — 활성 연결 전체에 코멘트 핑, 실패 연결 제거.
4. `application.yml` 에 `studypot.notification.stream.heartbeat-ms` 노출.
5. `NotificationStreamServiceTest` 에 하트비트 핑/죽은 연결 제거, 타임아웃 complete 테스트 추가 및 fake 구현 갱신.
6. `./gradlew check build` 로 검증.

## Done Criteria
- 활성 SSE 연결에 주기적 하트비트가 전송되고, 전송 실패 연결은 즉시 등록 해제된다.
- emitter 타임아웃 시 `AsyncRequestTimeoutException` WARN 이 더 이상 발생하지 않는다(graceful complete).
- 하트비트 주기가 설정으로 조정 가능하다.
- 기존 알림/SSE 계약과 테스트가 그대로 통과하고, 신규 테스트가 통과한다.
- `./gradlew check build` 그린.
