# AI Study Leader Architecture

이 문서는 작업 시작 전에 읽는 아키텍처 요약 문서다.

## 현재 구조
- Stack adapter: `Spring Boot planned`
- Repository: `/Users/hyunwoo/Documents/New project 3`
- Verification command: `TODO: set verification command`
- Remote: `https://github.com/StudyPot/StudyPot_BE.git`
- Intended PR target: `develop`
- Locked backend baseline: Java 21 + Gradle + Spring Boot
- Locked API baseline: REST `/api/v1` + OpenAPI 3.1
- Locked DB baseline: PostgreSQL + UUIDv7 + JSONB
- Work tracking source of truth: Jira `SPT` Board

## 계층 규칙
- 기존 프로젝트의 계층과 모듈 경계를 우선한다.
- public API, domain/service, persistence, configuration 책임을 섞지 않는다.
- shared code 변경은 테스트와 문서 갱신을 함께 수행한다.

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
- 요구사항 v1: [docs/specs/requirements-v1.md](./docs/specs/requirements-v1.md)
- API 계약 v1: [docs/specs/api-contract-v1.md](./docs/specs/api-contract-v1.md)
- 도메인 ERD 노트: [docs/specs/domain-erd.md](./docs/specs/domain-erd.md)
- DB 계약 v1: [docs/specs/db-contract-v1.md](./docs/specs/db-contract-v1.md)
- AI 계약 v1: [docs/specs/ai-contract-v1.md](./docs/specs/ai-contract-v1.md)
- Discord 계약 v1: [docs/specs/discord-contract-v1.md](./docs/specs/discord-contract-v1.md)
- Auth/Permission v1: [docs/specs/auth-permissions-v1.md](./docs/specs/auth-permissions-v1.md)
- QA 수용 기준 v1: [docs/specs/qa-acceptance-v1.md](./docs/specs/qa-acceptance-v1.md)
- 변경 통제 v1: [docs/specs/change-control-v1.md](./docs/specs/change-control-v1.md)
- 기능 커버리지 매트릭스: [docs/specs/feature-coverage-matrix.md](./docs/specs/feature-coverage-matrix.md)
- 구조 상세: [docs/architecture/backend-map.md](./docs/architecture/backend-map.md)
- 테스트 계약: [docs/testing/codex-harness.md](./docs/testing/codex-harness.md)
- PR review gate: [docs/operations/pr-review-gate.md](./docs/operations/pr-review-gate.md)
- Jira Board Sync: [docs/operations/jira-board-sync.md](./docs/operations/jira-board-sync.md)
- Obsidian error ledger: [docs/operations/obsidian-error-ledger.md](./docs/operations/obsidian-error-ledger.md)
