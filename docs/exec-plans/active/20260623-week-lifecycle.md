# EXEC_PLAN: [feat] 주차 상태 시간 기반 자동 전이 스케줄러

- Task slug: `week-lifecycle`
- Base branch: `develop`
- Feature branch: `codex/week-lifecycle`
- Jira issue: `SPT-149`
- Jira URL: https://studypot.atlassian.net/browse/SPT-149
- Jira summary: [feat] 주차 상태 시간 기반 자동 전이 스케줄러
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/db-schema-v1.sql

## Related Feature IDs
- [x] curriculum

## Doc Notes
- db-schema-v1.sql: 신규 마이그레이션/컬럼 없음. 기존 curriculum_week.status(PENDING/IN_PROGRESS/COMPLETED) CHECK 제약과 starts_at/ends_at 만 사용. db-schema-coverage 게이트 영향 없음.

## Goal
커리큘럼 주차 상태가 생성 후 영영 바뀌지 않던(1주차만 IN_PROGRESS) 문제를 고친다. starts_at/ends_at 기준으로 주차를 자동 전이시켜 '현재 주차' 조회와 회고 리마인더(IN_PROGRESS 조건)가 2주차 이후에도 동작하게 한다.

## Approach
- `curriculum/scheduler/WeekLifecycleScheduler`(@Component, @ConditionalOnProperty spring.datasource.url, @Scheduled fixedDelay 기본 5분, 키 studypot.week-lifecycle.interval-ms).
- 매 틱:
  1. COMPLETE_ENDED_WEEKS: status<>COMPLETED & ends_at<=now & ACTIVE 커리큘럼 → COMPLETED (IN_PROGRESS 마감 + 다운타임으로 통째로 지난 PENDING도 정리).
  2. SELECT_WEEKS_TO_ACTIVATE: PENDING & starts_at<=now & ends_at>now & ACTIVE → 활성화 대상 조회.
  3. ACTIVATE_WEEK: id별 PENDING→IN_PROGRESS(동시성 대비 status=PENDING 조건), 성공 시 NotificationEventPublisher.publishWeekStarted(WEEK_STARTED, 멱등키로 중복 방지) 발송.
- 1주차는 생성 시 이미 IN_PROGRESS+WEEK_STARTED 발송됨 → PENDING 조건으로 재선택/재알림 안 됨.
- RetrospectiveReminderScheduler/RetrospectiveJdbc 패턴 그대로(JdbcTemplate 직접, UuidBinary, Timestamp). 저장소/스키마 변경 없음.

## Step Plan
1. WeekLifecycleScheduler 작성.
2. 단위 테스트(완료 전이/활성화+알림/경합 시 미발송) — JdbcTemplate mock 패턴.
3. `./gradlew check build` 그린.

## Done Criteria
- 마감 지난 주차가 COMPLETED 로, 시작 도래한 다음 주차가 IN_PROGRESS 로 자동 전이되고 WEEK_STARTED 알림이 1회 발송된다.
- 1주차 중복 알림 없음, 신규 마이그레이션 없음, 전체 테스트 통과.
