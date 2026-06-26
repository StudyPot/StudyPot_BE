# EXEC_PLAN: [feat] 유저 플랜(FREE/PREMIUM)과 호스트 스터디 개수 제한

- Task slug: `user-plan-study-quota`
- Base branch: `develop`
- Feature branch: `codex/user-plan-study-quota`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: `TBD`
- Log dir: `/Users/hyunwoo/Developer/Projects/StudyPot/.codex/logs/user-plan-study-quota`
- Jira issue: ``
- Jira URL:
- Jira summary:
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md
- [x] src/main/java/com/studypot/aistudyleader/studygroup/service/StudyGroupService.java
- [x] src/main/java/com/studypot/aistudyleader/studygroup/repository/JdbcStudyGroupRepository.java

## Related Docs
- [x] docs/specs/db-contract-v1.md (users 테이블 — plan 컬럼 추가는 기존 계약을 깨지 않는 가산적 변경)
- [x] docs/specs/api-contract-v1.md (그룹 생성/사용자 API — 쿼터 조회 엔드포인트·409 에러 추가)
- [x] src/test/java/com/studypot/aistudyleader/studygroup/service/StudyGroupServiceTest.java (CapturingRepository fake)

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- AGENTS.md: DB/API 변경은 change-control 대상이나, 본 변경은 (1) users에 `plan` 컬럼을 default 'FREE'로 **추가**하고 (2) 신규 조회 엔드포인트/409 에러를 **추가**하는 **가산적·하위호환** 변경으로 기존 LOCKED 계약(컬럼/엔드포인트)을 수정/삭제하지 않는다. 정식 CR+ADR 형식화는 리뷰에서 판단.
- ARCHITECTURE.md: 계층 규칙 준수 — 쿼터 한도는 studygroup.service의 `StudyGroupQuotaProperties`(설정)로, plan 조회/그룹 카운트는 studygroup.repository의 JDBC로 처리(자바 차원의 auth 결합 없음, users 테이블만 읽음). ArchUnit은 레이어 규칙만 강제(모듈 격리 없음).

## Goal
무료/유료(FREE/PREMIUM) 유저를 나누고, 무료 유저가 호스트(생성자)로서 "동시에 운영 중(미완료)"인 스터디를 일정 개수(기본 3개, 유료 20개)로 제한한다. 백엔드에서 강제하고, FE가 "왜 안 되는지"를 표시할 수 있도록 쿼터 현황 조회 API와 구조화된 한도초과 에러를 제공한다.

## Approach
- DB: V15 마이그레이션 — `users.plan varchar(40) NOT NULL DEFAULT 'FREE'` + CHECK(FREE/PREMIUM). 기존 유저는 default로 FREE.
- 한도 설정: `StudyGroupQuotaProperties`(@ConfigurationProperties `studypot.studygroup.quota`, free=3/premium=20, env override). `limitForPlan(plan)`.
- 강제: `StudyGroupService.createGroup` 진입부에서 `enforceHostQuota` — `repository.findUserPlan(userId)` + `repository.countActiveHostedGroups(userId)`로 판정, 초과 시 `StudyGroupQuotaExceededException(plan, limit, current)`.
- 카운트 정의: `created_by=? AND deleted_at IS NULL AND status NOT IN ('COMPLETED','ARCHIVED')` (완료/보관/삭제는 슬롯 차지 안 함).
- 조회 API: `GET /api/v1/users/me/study-quota` → `{plan, hostedActiveCount, limit, canCreate}` (FE 사전 안내/버튼 게이팅).
- 에러 매핑: `ApiExceptionHandler` → 409 + `code=STUDY_GROUP_QUOTA_EXCEEDED`, plan/limit/current property.
- 유료 전환(v1): 관리자 수동(직접 SQL `update users set plan='PREMIUM'`). 관리자 HTTP API와 /users/me 플랜 노출은 후속 PR로 분리.

## Step Plan
1. V15 마이그레이션 + `StudyGroupQuotaProperties` + `StudyGroupQuotaExceededException` 추가.
2. 저장소: `countActiveHostedGroups`/`findUserPlan` (인터페이스 + JDBC + SQL) — 모든 구현체/fake에 반영.
3. 서비스: 생성자에 quotaProperties 주입(기본값 호환 생성자 유지), createGroup enforce, getHostStudyQuota 추가.
4. 컨트롤러: `/users/me/study-quota` 엔드포인트 + 응답 DTO.
5. 예외 핸들러 409 매핑(구조화 property).
6. 설정 등록(@EnableConfigurationProperties) + application.yml 한도값.
7. 테스트: 한도 도달 차단 / 프리미엄 초과 허용 / 쿼터 조회 / 프로퍼티 기본값·검증.
8. `./gradlew check build` 통과 후 PR → develop.

## Done Criteria
- `./gradlew check build` 통과.
- 무료 3개째 생성 차단(409, 구조화 에러), 프리미엄은 한도 내 허용, 완료/삭제 스터디는 카운트 제외.
- `GET /users/me/study-quota`가 plan/현재개수/한도/생성가능 여부 반환.
- 기존 계약/엔드포인트 비파괴(가산적).
