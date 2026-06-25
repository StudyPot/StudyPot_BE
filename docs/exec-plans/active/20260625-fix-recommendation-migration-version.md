# EXEC_PLAN: [fix] 추천 캐시 마이그레이션 버전 충돌 해결

- Task slug: `fix-recommendation-migration-version`
- Base branch: `develop`
- Feature branch: `codex/fix-recommendation-migration-version`
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
- Flyway 마이그레이션 버전은 유일해야 한다. develop에 `V10__user_follow_schema.sql`, `V11`, `V12`가 먼저 들어와 있어 PR #305의 `V10__study_recommendation_cache.sql`이 V10 중복으로 부팅 시 Flyway validate 실패를 유발했다.
- 이미 적용된 V10~V12 와 무관한 다음 빈 번호 `V13`으로 올려 충돌만 제거한다. 테이블 정의(내용)는 그대로다.

## Goal
PR #305 머지 후 develop 배포가 `Found more than one migration with version 10`으로 실패하는 문제를, 추천 캐시 마이그레이션을 V13으로 재번호링해 해결한다.

## Approach
`V10__study_recommendation_cache.sql` 을 `V13__study_recommendation_cache.sql` 으로 rename(내용 동일). 다른 V10~V12 마이그레이션은 그대로 둔다.

## Step Plan
- [x] develop 최신 마이그레이션 번호 확인(V10 user_follow, V11, V12 존재 → V13 비어 있음).
- [x] 추천 캐시 마이그레이션을 V13으로 rename.
- [x] `./gradlew check build --no-daemon` 통과 확인.
- [ ] 커밋/푸시/PR 후 develop 재배포 성공 확인.

## Done Criteria
- `src/main/resources/db/migration` 에 중복 버전이 없다(V13 단일).
- develop 배포(Deploy)가 성공하고 `study_recommendation` 테이블이 생성된다.
- `./gradlew check build --no-daemon`가 통과한다.

## Verification
- [ ] `./gradlew check build --no-daemon`: pending pre-push run
- [ ] GitHub Actions Deploy on develop: green
