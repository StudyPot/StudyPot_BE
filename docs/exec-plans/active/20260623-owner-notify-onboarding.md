# EXEC_PLAN: [fix] 온보딩 단계 그룹장 멤버 가입 알림 미발행 수정

- Task slug: `owner-notify-onboarding`
- Base branch: `develop`
- Feature branch: `codex/owner-notify-onboarding`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/owner-notify-onboarding`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/owner-notify-onboarding`
- Jira issue: `SPT-142`
- Jira URL: https://studypot.atlassian.net/browse/SPT-142
- Jira summary: [fix] 온보딩 단계 그룹장에게 멤버 가입 알림 미발행 수정
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
- ADR-20260601-notification-sse-stream / notification-contract-v1: 멤버 가입 시 그룹장에게 MEMBER_JOINED IN_APP 알림을 보내 실시간(SSE)로 온보딩 현황을 갱신한다. 본 수정은 알림 계약/스키마를 바꾸지 않고, 알림 대상(그룹장) 조회가 누락되던 버그만 고친다.

## Goal
온보딩 단계에서 새 멤버가 가입해도 그룹장의 온보딩 현황이 실시간으로 갱신되지 않던 문제를 고친다. 그룹장(OWNER)도 온보딩 단계엔 본인 status 가 PENDING_ONBOARDING 인데, `SELECT_OWNER_USER_ID` 가 `gm.status = 'ACTIVE'` 를 요구해 `findOwnerUserId` 가 빈 값을 반환 → `publishMemberJoined` 미호출 → MEMBER_JOINED 알림이 생성/전송되지 않았다.

## Approach
- `SELECT_OWNER_USER_ID` 의 `and gm.status = 'ACTIVE'` 를 `and gm.status <> 'LEFT'` 로 완화한다. owner 는 permission='OWNER' 로 식별되며 온보딩 완료 여부와 무관하게(탈퇴 제외) 알림을 받아야 한다. 이 쿼리는 memberJoined 발행 경로(StudyGroupService)에서만 사용되므로 영향 범위가 좁다.

## Step Plan
1. `StudyGroupJdbcSql.SELECT_OWNER_USER_ID` 의 status 조건 완화.
2. `JdbcStudyGroupRepositoryTest` 에 owner 조회가 온보딩 상태와 무관하게 owner 를 찾고 ACTIVE 를 강제하지 않음을 검증하는 테스트 추가.
3. `./gradlew check build` 그린 확인.

## Done Criteria
- 온보딩 단계(그룹장 PENDING_ONBOARDING)에 새 멤버가 가입하면 `findOwnerUserId` 가 그룹장을 반환해 MEMBER_JOINED 알림이 발행된다.
- 신규 쿼리 테스트가 통과하고 기존 테스트가 모두 통과한다.
- `./gradlew check build` 그린.
