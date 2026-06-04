# EXEC_PLAN: AI팀장 메시지 전송 회고 컨텍스트 SQL 오류 수정

- Task slug: `spt-137-ai-retrospective-sql`
- Base branch: `develop`
- Feature branch: `codex/spt-137-ai-retrospective-sql`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/spt-137-ai-retrospective-sql`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/spt-137-ai-retrospective-sql`
- Jira issue: `SPT-137`
- Jira URL: https://studypot.atlassian.net/browse/SPT-137
- Jira summary: AI팀장 메시지 전송 회고 컨텍스트 SQL 오류 수정
- Status: `verified`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/db-contract-v1.md
- [x] docs/specs/db-schema-v1.sql
- [x] src/main/resources/db/migration/V1__erd_v0_8_mysql8_schema.sql
- [x] docs/exec-plans/active/20260604-ai-chat-retrospective-context.md
- [x] docs/testing/codex-harness.md
- [x] docs/operations/pr-review-gate.md

## Related Feature IDs
- [x] ai-team-leader
- [x] retrospective-feedback

## Doc Notes
- 운영 `ssh rumiclean` 로그에서 `POST /api/v1/ai-conversations/{conversationId}/messages` 직후 `BadSqlGrammarException`이 발생했고, 원인은 `retrospective.deleted_at` 컬럼 참조였다.
- v1 DB 스키마와 `V1__erd_v0_8_mysql8_schema.sql`의 `retrospective` 테이블에는 `deleted_at`이 없다. 이 작업은 DB 스키마 변경 없이 코드 쿼리를 locked schema에 맞춘다.
- AI 계약은 `TEAM_LEAD_CHAT`이 DB-first context builder로 회고 요약을 읽어야 한다고 요구한다. 없는 회고 데이터는 `NOT_AVAILABLE`로 남겨야 한다.
- SPT-136은 일반 AI팀장 채팅에 현재 주차 회고 컨텍스트를 보강했지만, 주차 기반 회고 조회 SQL이 운영 스키마와 불일치했다.

## Goal
AI팀장 일반 채팅 메시지 전송 중 현재 주차 회고 컨텍스트 조회가 운영 DB 스키마와 맞지 않아 실패하는 문제를 수정한다. 사용자가 메시지를 보내면 회고 데이터가 있으면 DB-first context에 포함하고, 회고가 없으면 `NOT_AVAILABLE`로 안전하게 이어져야 한다.

## Approach
`SELECT_RETROSPECTIVE_PROMPT_CONTEXT_BY_WEEK`에서 `retrospective.deleted_at` 조건을 제거한다. 해당 테이블은 soft delete 대상이 아니므로 별도 마이그레이션을 만들지 않는다. 회귀 방지를 위해 repository SQL 문자열 테스트에 `deleted_at` 미참조 assertion을 추가하고, 관련 AI repository/service 테스트와 전체 Gradle 검증을 실행한다.

## Step Plan
1. 운영 로그의 재현 원인을 코드와 v1 스키마로 대조한다.
2. 주차 기반 회고 prompt context SQL을 locked DB 스키마에 맞게 수정한다.
3. repository SQL 회귀 테스트가 `retrospective.deleted_at` 재참조를 차단하도록 보강한다.
4. targeted AI repository/service tests를 실행한다.
5. `./gradlew check build --no-daemon`을 실행하고 PR/review gate/merge/deploy까지 완료한다.

## Done Criteria
- `SELECT_RETROSPECTIVE_PROMPT_CONTEXT_BY_WEEK`가 `retrospective`의 실제 컬럼만 참조한다.
- 일반 `TEAM_LEAD_CHAT` 메시지 전송 시 현재 주차 회고 컨텍스트 조회가 SQL 오류 없이 동작한다.
- 회고가 없는 경우 기존처럼 `NOT_AVAILABLE`로 남는다.
- 회귀 테스트와 `./gradlew check build --no-daemon`이 통과한다.
- PR 생성, CodeRabbit marker, GitHub Actions Review Gate, merge, cleanup, 운영 배포 확인이 끝난다.

## Verification
- [x] `./gradlew test --tests 'com.studypot.aistudyleader.ai.repository.JdbcAiConversationRepositoryTest' --tests 'com.studypot.aistudyleader.ai.service.AiConversationServiceTest' --no-daemon` - PASS
- [x] `./gradlew check build --no-daemon` - PASS
