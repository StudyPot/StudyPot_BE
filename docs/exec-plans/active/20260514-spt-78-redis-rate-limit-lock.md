# EXEC_PLAN: [infra] Redis 기반 LLM/API rate limit 및 중복 생성 lock 도입

- Task slug: `spt-78-redis-rate-limit-lock`
- Base branch: `develop`
- Feature branch: `codex/spt-78-redis-rate-limit-lock`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-78-redis-rate-limit-lock`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-78-redis-rate-limit-lock`
- Jira issue: `SPT-78`
- Jira URL: https://studypot.atlassian.net/browse/SPT-78
- Jira summary: [infra] Redis 기반 LLM/API rate limit 및 중복 생성 lock 도입
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/notification-contract-v1.md
- [x] docs/specs/requirements-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/change-control-v1.md
- [x] docs/operations/local-development.md
- [x] docs/operations/pr-review-gate.md
- [x] docs/operations/jira-board-sync.md

## Related Feature IDs
- [x] n/a-harness
- [x] ai-team-leader
- [x] notification

## Doc Notes
- `docs/specs/ai-contract-v1.md`: AI 호출은 `llm_usage` 감사 기록과 연결되어야 하며, Redis 도입이 LLM 사용 기록의 source of truth를 대체하면 안 된다.
- `docs/specs/notification-contract-v1.md`: v1 알림은 `IN_APP` 중심이며, 알림 실패는 핵심 트랜잭션을 롤백하지 않는 정책이다. Redis는 알림 레코드 자체가 아니라 전송 제한, 중복 방지, 이벤트 보조 저장소 후보로만 다룬다.
- `docs/specs/requirements-v1.md`: P0/P1 범위에 AI 생성, 알림, LLM 사용 집계가 포함된다. 이번 작업은 API/DB 계약 변경 없이 Redis를 인프라 보호 계층으로 추가하는 방향을 우선한다.
- `docs/specs/db-contract-v1.md`: MySQL은 `llm_usage`, `notification`, `curriculum`, `retrospective` 등 영속 데이터의 기준 저장소다. Redis 키는 TTL 기반 카운터/락처럼 재생성 가능하고 짧게 살아야 한다.
- `docs/specs/change-control-v1.md`: locked v1 계약의 endpoint, schema, enum, 응답 형식 변경은 Change Request + ADR 없이 진행하지 않는다.
- `docs/operations/local-development.md`: 로컬/test 기본값은 rate limit disabled/no-op이며, Redis 경로를 직접 확인할 때만 Redis 실행과 `STUDYPOT_RATE_LIMIT_ENABLED=true`가 필요하다.
- Source context: 이번 PR에서 `spring-boot-starter-data-redis`, `spring.data.redis.*`, `studypot.rate-limit.*` 설정을 추가한다.

## Goal
사용자가 직접 Redis를 StudyPot 백엔드에 붙이면서, Redis를 왜 쓰는지와 어떤 한계가 있는지를 설명할 수 있게 만든다.

구현 목표는 Redis 기반 보호 계층의 1차 기반을 추가하는 것이다.
- 기존 `GET /api/v1/users/me` 현재 사용자 조회를 대상으로 Redis fixed-window rate limit을 먼저 적용한다.
- LLM/API 호출과 AI 생성 lock은 같은 보호 계층에서 확장할 수 있도록 정책/키/guard 구조를 둔다.
- MySQL이 가진 영속 데이터의 책임을 Redis로 옮기지 않는다.
- locked v1 API/DB/AI/notification 계약은 변경하지 않는다.
- 사용자가 Redis 핵심 로직을 이해한 뒤 Codex가 테스트, 연결, 검증, PR/CodeRabbit 게이트를 담당한다.

## Approach
학습 순서와 구현 순서를 일치시킨다.

1. Redis 개념 정리
   - Rate limit: `INCR`, `EXPIRE`, `TTL`을 이용한 fixed-window 카운터를 먼저 이해한다.
   - Duplicate lock: `SET key value NX EX seconds`로 획득하고, 소유자 토큰이 맞을 때만 해제하는 이유를 이해한다.
   - Redis는 빠른 임시 상태 저장소이고, 장애나 만료 이후에도 DB 기준 상태가 깨지지 않아야 한다.

2. Spring Boot 연결 지점 파악
   - 의존성 후보: Spring Data Redis 또는 Lettuce 기반 RedisTemplate.
   - 설정 후보: `spring.data.redis.*`, `studypot.redis.*`, feature toggle.
   - 테스트 후보: 포트 인터페이스 단위 테스트, fake/in-memory adapter 테스트, 필요 시 로컬 Redis smoke test.

3. 애플리케이션 경계 설계
   - Redis 코드는 컨트롤러나 도메인 서비스에 직접 흩뿌리지 않는다.
   - 후보 port: `RateLimiter`, `GenerationLock`, `RedisKeyFactory`.
   - AI 호출 지점은 `LlmProviderClient` 또는 AI service 주변에서 제한한다.
   - 알림은 전송 제한/중복 방지 후보로만 다루고, `notification` row의 소유권은 DB에 남긴다.

4. 결정이 필요한 정책
   - 2026-05-14 사용자 결정: 운영에서는 LLM 보호 목적의 Redis 장애를 fail-closed로 다루고, local/test에서는 disabled 또는 no-op 모드를 허용한다.
   - 2026-05-14 사용자 결정: 모든 API에 전역 필터로 걸지 않고, 비용 있는 LLM 경로부터 좁게 적용한다.
   - 2026-05-14 사용자 결정: 첫 학습 적용 대상은 새 API가 아니라 기존 `GET /api/v1/users/me` 현재 사용자 조회에 좁은 rate limit을 붙이는 방식으로 시작한다.
   - 2026-05-14 사용자 결정: 1차 limit은 현재 사용자 조회 60회/분, 사용자별 AI 대화 5회/분, 그룹별 커리큘럼 생성 3회/10분, 회고 피드백 2회/일로 시작한다.
   - 2026-05-14 key 기준: `users/me`는 `userId`, AI 대화는 `userId` 또는 `conversationId`, 커리큘럼 생성은 `groupId`, 회고 피드백은 `userId + weekId`를 우선 검토한다.

## Step Plan
1. 멘토링 킥오프
   - Codex가 현재 코드의 Redis 삽입 후보 위치를 설명한다.
   - 사용자가 Redis 명령과 키 설계를 말로 설명할 수 있는지 확인한다.

2. 최소 인프라 추가
   - 사용자가 Redis 의존성과 설정 클래스를 추가한다.
   - Codex는 변경 전후 diff를 리뷰하고 설정 누락, profile 영향, 테스트 격리를 확인한다.

3. Rate limit 구현
   - 사용자가 작은 port/interface를 먼저 만든다.
   - 사용자가 fixed-window 또는 token-bucket 중 하나를 선택해 구현한다.
   - Codex는 원자성, TTL 설정 순서, key cardinality, 예외 처리, 테스트 커버리지를 리뷰한다.

4. Duplicate generation lock 후속 범위
   - `SET NX EX` 기반 lock은 이번 1차 PR의 직접 구현 범위에서 제외한다.
   - 후속 작업에서 AI 생성 경로에 붙일 때 TTL, lock key 범위, owner-token release를 검증한다.

5. 통합 지점 연결
   - 사용자가 기존 `GET /api/v1/users/me`에 좁은 rate limit을 먼저 연결해 Redis end-to-end 흐름을 검증한다.
   - AI 대화, 커리큘럼 생성, 회고 피드백 정책 값은 설정에 둔다.
   - 해당 AI 경로 연결과 duplicate lock은 후속 PR에서 진행한다.
   - Codex는 locked API 계약 변경 여부와 `llm_usage` 기록 보존 여부를 확인한다.

6. 문서와 검증
   - 사용자가 로컬 Redis 실행 방법과 장애 정책을 문서화한다.
   - Codex가 `./gradlew check build --no-daemon`을 실행한다.
   - Codex가 커밋, PR 생성, CodeRabbit review, finish-pr까지 진행한다.

## Done Criteria
- 사용자가 Redis key, TTL, rate limit window, lock owner token, 장애 정책을 직접 설명할 수 있다.
- Redis 도입 후에도 MySQL의 `llm_usage`, `notification`, AI 결과 저장 책임이 유지된다.
- rate limit 테스트가 허용, 제한 초과, window 만료 또는 TTL 동작, Redis 장애 정책을 검증한다.
- duplicate lock은 이번 1차 PR에서 직접 구현하지 않고 후속 작업 범위로 남긴다.
- `GET /api/v1/users/me` 현재 사용자 조회에 좁은 Redis rate limit이 적용되어 학습용 end-to-end 흐름을 검증할 수 있다.
- `RateLimiter`, `RateLimitGuard`, `RateLimitProperties` 테스트가 허용, 제한 초과, TTL 복구, 장애 시 fail-open/fail-closed 정책을 검증한다.
- locked v1 API/DB 계약 변경이 없거나, 변경이 필요하면 별도 Change Request/ADR이 있다.
- 로컬 개발 문서에 Redis 실행/비활성화/테스트 방식이 반영되어 있다.
- `./gradlew check build --no-daemon`이 통과한다.
- CodeRabbit review gate가 최신 head 기준 PASS 또는 증거 포함 ADDRESSED 상태다.
