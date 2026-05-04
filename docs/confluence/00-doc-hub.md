# 00 문서 허브

## 한눈에 보기
- 최신 기준: Requirements v0.3, ERD v0.8, MySQL8.
- MVP: 그룹 생성 -> 초대 링크 -> 온보딩 -> 호스트 시작 -> AI 커리큘럼 -> 주차 todo -> 회고/AI 피드백.
- 알림: MVP는 서비스 내부 `IN_APP` 알림이며 Discord 연동은 제외.
- AI 팀장: 매주 회고와 다음 주 조정까지 담당.
- Repo source: `docs/specs/*`.
- Jira source: `SPT-10` Epic and implementation tasks `SPT-19` to `SPT-55`.

## 읽기 순서
1. MVP 골든패스
2. 요구사항
3. ERD / 데이터 모델
4. API 명세
5. 권한 / 상태 전이
6. AI 팀장 명세
7. QA / Acceptance Criteria
8. Jira 매핑

## 변경 통제
- v1 문서는 `LOCKED_FOR_IMPLEMENTATION`.
- 변경은 Change Request + ADR 필요.
- 현재 변경: `CR-20260504-no-discord-inapp-notification`, `ADR-20260504-no-discord-inapp-notification`.
