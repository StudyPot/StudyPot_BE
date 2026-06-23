# EXEC_PLAN: [feat] 주차 마감 시 AI 주차 리포트 자동 게시

- Task slug: `weekly-report-scheduler`
- Base branch: `develop`
- Feature branch: `codex/weekly-report-scheduler`
- Worktree: `/Users/hyunwoo/Documents/New project 3-worktrees/weekly-report-scheduler`
- Port: `18080`
- Log dir: `/Users/hyunwoo/Documents/New project 3-logs/weekly-report-scheduler`
- Jira issue: `SPT-146`
- Jira URL: https://studypot.atlassian.net/browse/SPT-146
- Jira summary: [feat] 주차 마감 시 멤버 회고 종합 AI 주차 리포트 자동 게시
- Status: `in-progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/ai-contract-v1.md
- [x] docs/specs/notification-contract-v1.md

## Related Feature IDs
- [x] retrospective

## Doc Notes
- ai-contract-v1: LLM 사용은 llm_usage 로 DB 감사한다 — 리포트 생성도 LlmUsageRecorder 로 사용량 기록(purpose=WEEKLY_REPORT). 응답은 OpenAI 구조화(JSON schema) 패턴(ProviderBackedRetrospectiveFeedbackGenerator) 재사용.
- llm_usage.purpose 컬럼은 varchar(80) 이고 CHECK 제약이 없어 enum 값 WEEKLY_REPORT 추가에 마이그레이션 불필요. 리포트 자체는 게시판 글이라 신규 테이블 없음.

## Goal
주차가 마감(ends_at 경과)되면, 그 주차 모든 멤버의 완료 회고를 종합해 AI가 '주차 학습 리포트'를 작성하고 그룹 회고 게시판에 자동으로 글을 올린다. (사용자 결정: 주차 마감 시 자동 생성)

## Approach
- `WeeklyReportScheduler`(@Scheduled, datasource 있을 때만): 마감 후 LOOKBACK(7일) 내 ACTIVE 커리큘럼 주차를 조회 → 주차별로 (1) 동일 제목 리포트 글 존재 시 skip(멱등), (2) 완료 회고 집계(ai_feedback.summary), (3) 회고 없으면 skip, (4) AI 리포트 생성, (5) llm_usage 기록, (6) 그룹 회고 게시판에 그룹장 권한으로 게시.
- AI 생성기 `ProviderBackedWeeklyReportGenerator`(LlmProviderClient, json_schema {title, body}), 설정은 LlmProviderConfiguredCondition 으로 조건부 빈.
- 게시판 작성은 기존 `GroupBoardService.listBoards`(기본 게시판 보장)+`createPost` 재사용 → board 도메인 변경 없음.

## Step Plan
1. LlmUsagePurpose.WEEKLY_REPORT 추가 + OpenAiOutputTokenLimits switch 케이스.
2. report.service: 입력/결과 레코드 + 생성기 인터페이스/예외 + ProviderBacked 구현 + 조건부 설정 빈.
3. report.scheduler: WeeklyReportScheduler(조회/멱등/집계/생성/usage/게시).
4. 생성기 단위 테스트(파싱/오류/provider 실패 래핑).
5. `./gradlew check build` 그린.

## Done Criteria
- 마감된 주차에 완료 회고가 있으면 AI 리포트가 회고 게시판에 1회만(멱등) 자동 게시된다.
- AI 미구성 환경에서는 동작하지 않는다(생성기/recorder 없으면 no-op).
- 신규 테스트 통과 + 기존 테스트 영향 없음.
- `./gradlew check build` 그린.
