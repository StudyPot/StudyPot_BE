# AI Study Leader Architecture

이 문서는 작업 시작 전에 읽는 아키텍처 요약 문서다.

## 현재 구조
- Stack adapter: `Spring Boot`
- Repository: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Verification command: `./gradlew check build --no-daemon`
- Remote: `https://github.com/StudyPot/StudyPot_BE.git`
- Intended PR target: `develop`
- Locked backend baseline: Java 21 + Gradle + Spring Boot
- Locked API baseline: REST `/api/v1` + OpenAPI 3.1
- Locked DB baseline: MySQL 8 + UUIDv7 stored as `BINARY(16)` + `JSON`
- Work tracking source of truth: Jira `SPT` Board

## 제품 기준
- Source of truth: Requirements v0.3, ERD v0.8 MySQL8.
- MVP golden path: group creation -> invite link -> member onboarding -> host start -> AI curriculum -> weekly todo -> retrospective feedback.
- Meeting-centered flows are deferred out of P0.
- AI team leader behavior is asynchronous and stored through curriculum, retrospective, conversation, in-app notification, and LLM usage records.
- Discord integration is not part of MVP; notification is in-app first and external channels are deferred.

## 계층 규칙
- 기존 프로젝트의 계층과 모듈 경계를 우선한다.
- public API, domain/service, repository, infrastructure, configuration 책임을 섞지 않는다.
- shared code 변경은 테스트와 문서 갱신을 함께 수행한다.
- DB/API/AI/알림/권한 변경은 `docs/specs/change-control-v1.md`의 Change Request + ADR 기록을 먼저 확인한다.

## 작업 규칙
- 기능 구현 전에는 `AGENTS.md -> ARCHITECTURE.md -> docs/index.md -> 작업 관련 docs` 순서로 읽는다.
- 읽은 문서는 `EXEC_PLAN`의 문서 섹션에 남긴다.
- 구조 규칙은 문서와 테스트 둘 다로 유지한다.
- feature 구현은 `scripts/task/init-task.sh <slug> "[title]" --jira SPT-123`가 만든 `codex/<slug>` worktree에서만 진행한다.
- PR 생성, review gate 확인, merge/cleanup은 `scripts/task/create-pr.sh`, `scripts/task/verify-pr-ready.sh`, `scripts/task/finish-pr.sh`가 소유한다.

## 상세 문서
- 문서 허브: [docs/index.md](./docs/index.md)
- 제품 명세: [docs/specs/product-brief.md](./docs/specs/product-brief.md)
- PRD v1: [docs/specs/prd-v1.md](./docs/specs/prd-v1.md)
- 사용자 여정 v1: [docs/specs/user-journeys-v1.md](./docs/specs/user-journeys-v1.md)
- 요구사항 v1: [docs/specs/requirements-v1.md](./docs/specs/requirements-v1.md)
- API 계약 v1: [docs/specs/api-contract-v1.md](./docs/specs/api-contract-v1.md)
- OpenAPI v1: [docs/specs/openapi.yaml](./docs/specs/openapi.yaml)
- 도메인 ERD 노트: [docs/specs/domain-erd.md](./docs/specs/domain-erd.md)
- DB 계약 v1: [docs/specs/db-contract-v1.md](./docs/specs/db-contract-v1.md)
- DB DDL 초안 v1: [docs/specs/db-schema-v1.sql](./docs/specs/db-schema-v1.sql)
- AI 계약 v1: [docs/specs/ai-contract-v1.md](./docs/specs/ai-contract-v1.md)
- Notification 계약 v1: [docs/specs/notification-contract-v1.md](./docs/specs/notification-contract-v1.md)
- Auth/Permission v1: [docs/specs/auth-permissions-v1.md](./docs/specs/auth-permissions-v1.md)
- QA 수용 기준 v1: [docs/specs/qa-acceptance-v1.md](./docs/specs/qa-acceptance-v1.md)
- 변경 통제 v1: [docs/specs/change-control-v1.md](./docs/specs/change-control-v1.md)
- 기능 커버리지 매트릭스: [docs/specs/feature-coverage-matrix.md](./docs/specs/feature-coverage-matrix.md)
- Confluence draft pages: [docs/confluence/README.md](./docs/confluence/README.md)
- 구조 상세: [docs/architecture/backend-map.md](./docs/architecture/backend-map.md)
- 테스트 계약: [docs/testing/codex-harness.md](./docs/testing/codex-harness.md)
- PR review gate: [docs/operations/pr-review-gate.md](./docs/operations/pr-review-gate.md)
- Jira Board Sync: [docs/operations/jira-board-sync.md](./docs/operations/jira-board-sync.md)
- Obsidian error ledger: [docs/operations/obsidian-error-ledger.md](./docs/operations/obsidian-error-ledger.md)
