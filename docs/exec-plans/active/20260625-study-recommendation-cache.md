# EXEC_PLAN: [feat] 다음 스터디 추천 캐싱

- Task slug: `study-recommendation-cache`
- Base branch: `develop`
- Feature branch: `codex/study-recommendation-cache`
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/feature-coverage-matrix.md

## Related Feature IDs
- [x] study-group-core

## Doc Notes
- DB 베이스라인(MySQL8, UUIDv7 BINARY(16), 유연값 JSON)을 그대로 따른다: `study_recommendation`은 `group_id BINARY(16)` PK + `ai_suggestions`/`popular_topics` JSON 컬럼으로 구성한다.
- `study-group-core` feature(`study_group` 테이블)에 종속되는 추천 결과 캐시이므로 FK는 `study_group(id)` ON DELETE CASCADE로 그룹 삭제 시 같이 정리한다.
- 추천은 부가 기능(LLM/집계 실패 시 빈 결과 폴백)이라는 기존 설계를 유지한다. 빈 결과는 캐시하지 않아 일시적 LLM 미구성/실패가 영구 고정되지 않도록 한다.

## Goal
완료한 스터디의 '다음 스터디 추천'을 매 요청마다 LLM 호출 + 인기주제 집계로 다시 생성하던 것을, 그룹별로 1회만 생성·저장하고 이후에는 저장값을 그대로 반환하도록 바꾼다.

## Approach
`study_recommendation` 캐시 테이블을 추가하고, `StudyRecommendationService.recommend()`가 캐시 조회 → miss 시 생성 → first-write-wins로 저장 → 정답 재조회 순으로 동작하게 한다. 프론트엔드 호출 계약(`GET /groups/{groupId}/recommendations`)은 그대로 둔다.

## Step Plan
- [x] V10 마이그레이션으로 `study_recommendation` 테이블 추가.
- [x] `recommend()`에 캐시 read/write 경로 추가, `readSuggestions`가 LLM 출력과 캐시 배열을 모두 허용하도록 보강.
- [x] 캐시 hit(무 LLM 호출) / miss 시 저장 단위 테스트 추가.
- [x] `./gradlew check build --no-daemon` 통과 확인.
- [ ] 커밋 및 PR 생성.

## Done Criteria
- 같은 완료 그룹의 추천 재요청 시 LLM/집계를 다시 돌리지 않고 저장된 값을 반환한다.
- LLM·인기주제가 모두 비면 캐시하지 않아 다음 요청에서 재시도된다.
- `./gradlew check build --no-daemon`가 통과한다.

## Verification
- [x] `./gradlew test --tests '*StudyRecommendationServiceTest'`: PASS
- [ ] `./gradlew check build --no-daemon`: pending pre-commit run
