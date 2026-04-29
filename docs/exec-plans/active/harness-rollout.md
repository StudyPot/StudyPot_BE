# Harness Rollout

## 목표
- Codex가 이 저장소에서 구현 전후로 같은 계획/검증 루프를 돌릴 수 있게 만든다.

## 체크리스트
- [x] `AGENTS.md` 추가
- [x] `ARCHITECTURE.md` 추가
- [x] `docs/` 문서 허브 추가
- [x] task/worktree scripts 추가
- [x] hook scripts 추가
- [x] Obsidian mirror 구조 추가
- [x] PR review gate docs 추가
- [x] create-pr / verify-pr-ready / finish-pr 흐름 추가

## 결정 로그
- 기본 검증 명령은 `TODO: set verification command`로 둔다.
- 구현 기준은 repo 문서에 둔다.
- Obsidian은 세션 컨텍스트, 명세 미러, 에러 레저, handoff에 사용한다.
- PR target은 `develop`을 기본으로 둔다.
- subagent review pass marker는 기본적으로 finish gate에서 요구하되, bootstrap/harness 작업은 명시 env로 끌 수 있게 둔다.

## 후속 과제
- 프로젝트별 핵심 사용자 여정 테스트 정하기
- CI에 검증 명령 반영하기
- `develop` 기본 브랜치 전략 확정하기
- feature coverage matrix가 필요한 경우 `docs/specs/` 추가하기
