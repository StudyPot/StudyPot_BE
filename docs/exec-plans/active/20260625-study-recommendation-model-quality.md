# EXEC_PLAN: [feat] 다음 스터디 추천 전용 모델/프롬프트 분리

- Task slug: `study-recommendation-model-quality`
- Base branch: `develop`
- Feature branch: `codex/study-recommendation-model-quality`
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/feature-coverage-matrix.md

## Related Feature IDs
- [x] ai-team-leader

## Doc Notes
- 추천 생성은 그동안 `DETAIL_KEYWORD_SUGGEST` purpose를 재사용해 기본(저가) 모델 `gpt-5-nano` + reasoning `minimal` + 256토큰으로 동작했다. 그 결과 완료 스터디마다 AI 추천이 두루뭉술하게 비슷했다.
- LLM purpose별 모델/토큰/추론 설정은 `OpenAiPurposeModels`/`OpenAiOutputTokenLimits`/`OpenAiReasoningEfforts` 레코드와 `studypot.ai.openai.*` properties로 관리된다. 새 용도는 이 3곳 + properties에 슬롯을 추가한다.
- `llm_usage.purpose`는 CHECK 없는 varchar(80)이라 새 purpose 추가에 마이그레이션이 필요 없다.

## Goal
완료한 스터디마다 의미 있게 다른 추천이 나오도록, 추천 AI를 전용 purpose(`STUDY_RECOMMENDATION`)로 분리해 더 좋은 모델(gpt-5-mini)·토큰·추론을 쓰고 프롬프트의 구체성/다양성 지시를 강화한다.

## Approach
`LlmUsagePurpose`에 `STUDY_RECOMMENDATION`을 추가하고, purpose별 모델/토큰/추론 레코드와 application.yml에 추천 슬롯을 더한다(기본 gpt-5-mini / 2048토큰 / low). `StudyRecommendationService`가 새 purpose를 쓰고, INSTRUCTIONS에 "완료 topic/keywords에 구체적으로 근거 + 서로 뚜렷이 다른 3개"를 강제한다. 캐싱(별도 PR) 덕분에 그룹당 1회만 호출되어 비용 영향은 작다.

## Step Plan
- [x] `LlmUsagePurpose.STUDY_RECOMMENDATION` 추가, 3개 설정 레코드 switch/슬롯 확장.
- [x] application.yml + deploy.yml 에 study-recommendation 모델/토큰/추론 env 추가.
- [x] `StudyRecommendationService` purpose 교체 + 프롬프트 강화.
- [x] 관련 테스트(LlmUsageTest 계약, PurposeModels/Properties/Provider) 업데이트.
- [x] `./gradlew check build --no-daemon` 통과.
- [ ] 커밋/푸시/PR.

## Done Criteria
- 추천 생성이 `STUDY_RECOMMENDATION` purpose로 동작하고 기본 모델이 gpt-5-mini 이다.
- purpose별 모델/토큰/추론 설정이 kebab-case properties로 바인딩된다(테스트 보강).
- `./gradlew check build --no-daemon`가 통과한다.

## Verification
- [x] `./gradlew test --tests '*openai.*' --tests '*StudyRecommendationServiceTest' --tests '*llm.*'`: PASS
- [x] `./gradlew check build --no-daemon`: PASS
