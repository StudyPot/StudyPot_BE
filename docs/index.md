# AI Study Leader Harness Docs

이 문서는 Codex와 사람이 같은 검증 계약을 공유하기 위한 필수 문서 허브입니다.

## 읽기 순서
- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/index.md`
- 아래 문서 중 작업 관련 문서

## 문서 맵
- [제품 명세](./specs/product-brief.md)
- [PRD v1](./specs/prd-v1.md)
- [사용자 여정 v1](./specs/user-journeys-v1.md)
- [요구사항 v1](./specs/requirements-v1.md)
- [API 계약 v1](./specs/api-contract-v1.md)
- [OpenAPI v1](./specs/openapi.yaml)
- [도메인 ERD 노트](./specs/domain-erd.md)
- [DB 계약 v1](./specs/db-contract-v1.md)
- [DB DDL 초안 v1](./specs/db-schema-v1.sql)
- [AI 계약 v1](./specs/ai-contract-v1.md)
- [Discord 계약 v1](./specs/discord-contract-v1.md)
- [Auth/Permission v1](./specs/auth-permissions-v1.md)
- [QA 수용 기준 v1](./specs/qa-acceptance-v1.md)
- [변경 통제 v1](./specs/change-control-v1.md)
- [기능 커버리지 매트릭스](./specs/feature-coverage-matrix.md)
- [아키텍처 맵](./architecture/backend-map.md)
- [Codex 테스트 하네스](./testing/codex-harness.md)
- [PR 리뷰 게이트](./operations/pr-review-gate.md)
- [Jira Board Sync](./operations/jira-board-sync.md)
- [Obsidian 에러 레저 운영](./operations/obsidian-error-ledger.md)
- [품질 스코어카드](./quality/scorecard.md)
- [현재 실행 계획](./exec-plans/active/harness-rollout.md)

## 기본 원칙
- 기본 검증 명령은 `TODO: set verification command` 입니다.
- 작업에 사용한 관련 문서는 `EXEC_PLAN`의 `Related Docs`와 `Doc Notes`에 남겨야 합니다.
- 기능 작업은 `EXEC_PLAN`의 `Related Feature IDs`와 연결합니다.
- 제품/ERD 변경은 `docs/specs/`를 먼저 갱신한 뒤 Obsidian mirror를 갱신합니다.
- v1 명세는 `LOCKED_FOR_IMPLEMENTATION` 상태이며, 기획/API/DB/AI/Discord/권한/QA 변경은 Change Request와 ADR 없이는 금지합니다.
- 구현 작업의 source of truth는 Jira Board입니다. Obsidian은 mirror/회고/세션 연속성 용도로만 사용합니다.
- harness/infrastructure 작업은 `Related Feature IDs`에 `n/a-harness`를 사용합니다.
- PR은 `develop` 대상으로 만들고, merge 전에는 review activity, unresolved thread, checks, subagent pass marker를 확인합니다.
- 긴 설명보다 실행 가능한 계약을 문서에 남깁니다.
