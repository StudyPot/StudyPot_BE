# AI Study Leader 개발 문서

이 디렉터리는 Confluence 문서의 repo mirror이자 publication source입니다. Confluence에 이미 발행된 페이지와 아직 Markdown draft로 남은 페이지를 같은 tree 안에서 관리합니다.

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
- Published source tasks: `SPT-4`, `SPT-6`, `SPT-7`
- Remaining docs cleanup task: `SPT-56`

## Confluence Publication Note
Confluence publication is available through authenticated Confluence REST API access to space `D`. Keep the Markdown source and published page list synchronized when a page is created or refreshed.

Published root:
- `AI Study Leader 개발 문서`: https://studypot.atlassian.net/wiki/spaces/D/pages/1835009/AI+Study+Leader

Published pages:
- `02 요구사항`: https://studypot.atlassian.net/wiki/spaces/D/pages/1867777/02
- `04 ERD / 데이터 모델`: https://studypot.atlassian.net/wiki/spaces/D/pages/1933313/04+ERD
- `05 API 명세`: https://studypot.atlassian.net/wiki/spaces/D/pages/1736706/05+API

Draft pages not yet confirmed as published:
- `00 문서 허브`
- `01 MVP 골든패스`
- `03 유저 플로우`
- `06 AI 팀장 명세`
- `07 권한 / 상태 전이`
- `08 알림`
- `09 QA / Acceptance Criteria`
- `10 Jira 매핑`
