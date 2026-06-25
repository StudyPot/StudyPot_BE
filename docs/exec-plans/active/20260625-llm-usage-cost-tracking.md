# EXEC_PLAN: [fix] LLM 사용기록 total_cost_usd 비용 계산/백필

- Task slug: `llm-usage-cost-tracking`
- Base branch: `develop`
- Feature branch: `codex/llm-usage-cost-tracking`
- Worktree: `/Users/hyunwoo/Developer/Projects/StudyPot`
- Port: `TBD`
- Log dir: `/Users/hyunwoo/Developer/Projects/StudyPot/.codex/logs/llm-usage-cost-tracking`
- Jira issue: ``
- Jira URL:
- Jira summary:
- Status: `draft`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/external-api-integrations.md
- [x] docs/exec-plans/active/20260513-spt-42-llm-usage-llm.md

## Related Feature IDs
- [x] n/a-harness

## Doc Notes
- AGENTS.md: DB/API/AI 변경은 change-control 대상이나 본 작업은 기존 `llm_usage.total_cost_usd`(V1에 이미 존재) 컬럼을 채우는 **데이터 백필 + 애플리케이션 버그 수정**이며 스키마/계약(DDL) 변경이 없으므로 LOCKED 스펙 변경에 해당하지 않음.
- ARCHITECTURE.md: 계층 규칙(domain은 Spring/infra 비의존)에 따라 단가표는 순수 도메인(`llm/domain/LlmModelPricing`)에 두고, infrastructure(`OpenAiLlmProvider`)가 이를 사용하도록 배치.
- docs/external-api-integrations.md / 어드민 LLM 사용기록 페이지가 비용 추적을 표방하나 실제 값이 0으로 적재되던 모순을 해소.

## Goal
운영 `llm_usage.total_cost_usd`가 모든 행에서 0으로 적재되어 비용 추적이 동작하지 않는 문제를 해결한다. 신규 호출은 토큰×모델단가로 비용을 계산해 저장하고, 과거 데이터는 동일 공식으로 백필한다.

## Approach
- 근본 원인: `OpenAiLlmProvider`가 성공/실패 경로 모두 `totalCostUsd`를 `BigDecimal.ZERO`로 하드코딩, 단가표/비용계산 로직 부재.
- `LlmModelPricing`(순수 도메인) 신설: 모델 prefix → (입력/출력 USD per 1M) 표, `costUsd = (in*inP + out*outP)/1e6`를 소수 6자리(decimal(12,6) 정합, HALF_UP)로 계산. 단가 미등록 모델은 null 반환(=$0과 구분).
- `OpenAiLlmProvider`의 ZERO 하드코딩 2곳을 `LlmModelPricing.costUsd(model, in, out)`로 교체.
- Flyway `V14` 백필: 토큰×단가 재계산, COALESCE로 미등록 모델 보존, `total_cost_usd`가 0/NULL인 행만 갱신(재실행 안전).
- 단가(OpenAI 2026-06): gpt-5-nano 0.05/0.40, gpt-5-mini 0.25/2.00, gpt-5 1.25/10.00, gpt-5.1 1.25/10.00, gpt-5.2 1.75/14.00, gpt-5.4(-mini/-nano)/gpt-5.5, gpt-4o(-mini)/gpt-4.1 계열 보강.

## Step Plan
1. 기록 경로 추적(LlmUsage/LlmStructuredResponse/LlmCallFailure/OpenAiLlmProvider) 및 단가 로직 부재 확인.
2. `LlmModelPricing` 도메인 + 단위 테스트(LlmModelPricingTest) 추가.
3. `OpenAiLlmProvider` 비용 계산 연결 + provider 테스트 비용 단언 갱신.
4. `V14__llm_usage_total_cost_usd_backfill.sql` 백필 마이그레이션 추가.
5. `./gradlew check build --no-daemon` 통과 후 PR → develop(자동 배포).

## Done Criteria
- `./gradlew check build` 통과(신규 LlmModelPricingTest + provider 비용 단언 포함).
- 신규 SUCCESS/FAILED 호출이 모델·토큰 기반 total_cost_usd로 저장됨.
- V14 적용 시 과거 gpt-* 행이 토큰×단가로 재계산되고, 미등록 모델/이미 산정된 행은 보존됨.
