# AI Study Leader 개발 문서

Confluence 앱 권한이 열리면 이 디렉터리의 Markdown을 같은 제목의 Confluence 페이지로 생성한다.

## Page Tree
- [00 문서 허브](./00-doc-hub.md)
- [01 MVP 골든패스](./01-mvp-golden-path.md)
- [02 요구사항](./02-requirements.md)
- [03 유저 플로우](./03-user-flow.md)
- [04 ERD / 데이터 모델](./04-erd-data-model.md)
- [05 API 명세](./05-api-spec.md)
- [06 AI 팀장 명세](./06-ai-team-leader.md)
- [07 권한 / 상태 전이](./07-permissions-state.md)
- [08 알림](./08-notification.md)
- [09 QA / Acceptance Criteria](./09-qa-acceptance.md)
- [10 Jira 매핑](./10-jira-mapping.md)

## Source of Truth
- Requirements: `/Users/hyunwoo/Downloads/요구사항_정의서_v0.3.docx`
- ERD: `/Users/hyunwoo/Desktop/Project/CodingTestTeam/outputs/erd-mysql8/ERD_설계문서_v0.8_MySQL8.docx`
- Mermaid: `/Users/hyunwoo/Desktop/Project/CodingTestTeam/outputs/erd-mysql8/ERD_MySQL8_v0.8.mmd`
- Repo specs: `docs/specs/*`
- Jira Epic: `SPT-10`

## Confluence Publication Note
Atlassian MCP/Rovo search can access Jira but still returns 403 for Confluence: `The app is not installed on this instance`.
Confluence publication is available through authenticated Confluence REST API access to space `D`.

Published root:
- `AI Study Leader 개발 문서`: https://studypot.atlassian.net/wiki/spaces/D/pages/1835009/AI+Study+Leader

Published pages:
- `02 요구사항`: https://studypot.atlassian.net/wiki/spaces/D/pages/1867777/02
- `04 ERD / 데이터 모델`: https://studypot.atlassian.net/wiki/spaces/D/pages/1933313/04+ERD
