-- 과거 llm_usage 행의 total_cost_usd 백필.
-- 적재 당시 비용 계산 로직이 없어 모든 행이 0(또는 NULL)으로 저장되어 비용 추적이 동작하지 않았다.
-- 토큰 사용량 × 모델별 공식 단가(USD per 1M tokens, OpenAI 2026-06 기준)로 재계산한다.
--   total_cost_usd = (input_tokens * inPrice + output_tokens * outPrice) / 1,000,000  (소수 6자리, decimal(12,6))
-- 단가 미등록 모델은 CASE 가 NULL 을 산출 → COALESCE 로 기존 값을 유지(덮어쓰지 않음).
-- 이미 0/NULL 이 아닌 비용이 적재된 행은 WHERE 로 보호하여 재실행해도 안전(idempotent)하다.
-- (`* 1.000000` 은 div_precision_increment 설정과 무관하게 division 전에 scale 을 확보하기 위함)

update llm_usage
set total_cost_usd = coalesce(
  round(
    (
      input_tokens * (
        case
          when model like 'gpt-5.5%'      then 5.00
          when model like 'gpt-5.4-nano%' then 0.20
          when model like 'gpt-5.4-mini%' then 0.75
          when model like 'gpt-5.4%'      then 2.50
          when model like 'gpt-5.2%'      then 1.75
          when model like 'gpt-5.1%'      then 1.25
          when model like 'gpt-5-nano%'   then 0.05
          when model like 'gpt-5-mini%'   then 0.25
          when model like 'gpt-5%'        then 1.25
          when model like 'gpt-4o-mini%'  then 0.15
          when model like 'gpt-4o%'       then 2.50
          when model like 'gpt-4.1-nano%' then 0.10
          when model like 'gpt-4.1-mini%' then 0.40
          when model like 'gpt-4.1%'      then 2.00
        end
      )
      + output_tokens * (
        case
          when model like 'gpt-5.5%'      then 30.00
          when model like 'gpt-5.4-nano%' then 1.25
          when model like 'gpt-5.4-mini%' then 4.50
          when model like 'gpt-5.4%'      then 15.00
          when model like 'gpt-5.2%'      then 14.00
          when model like 'gpt-5.1%'      then 10.00
          when model like 'gpt-5-nano%'   then 0.40
          when model like 'gpt-5-mini%'   then 2.00
          when model like 'gpt-5%'        then 10.00
          when model like 'gpt-4o-mini%'  then 0.60
          when model like 'gpt-4o%'       then 10.00
          when model like 'gpt-4.1-nano%' then 0.40
          when model like 'gpt-4.1-mini%' then 1.60
          when model like 'gpt-4.1%'      then 8.00
        end
      )
    ) * 1.000000 / 1000000,
    6
  ),
  total_cost_usd
)
where (total_cost_usd is null or total_cost_usd = 0)
  and model like 'gpt-%';
