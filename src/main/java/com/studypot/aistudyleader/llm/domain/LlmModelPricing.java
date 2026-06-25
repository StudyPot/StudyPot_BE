package com.studypot.aistudyleader.llm.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 모델별 토큰 단가(USD per 1M tokens) 표와 호출 비용 계산.
 *
 * <p>{@code total_cost_usd = inputTokens/1e6 * inputPricePerMillion + outputTokens/1e6 * outputPricePerMillion}
 * 으로 계산하며, 저장 컬럼 {@code llm_usage.total_cost_usd decimal(12,6)} 에 맞춰 소수점 6자리(HALF_UP)로 반올림한다.
 *
 * <p>단가는 OpenAI 공식 가격(2026-06 기준)을 따른다. 모델명은 접두사(prefix)로 매칭하므로
 * {@code gpt-5-nano-2025-08-07} 처럼 날짜 접미사가 붙어도 {@code gpt-5-nano} 단가로 매칭된다.
 * 더 구체적인(긴) 접두사가 먼저 매칭되도록 길이 내림차순으로 평가한다.
 *
 * <p>알 수 없는 모델은 {@code null} 을 반환한다 — "무료($0)"가 아니라 "단가 미등록"임을 구분해
 * 운영에서 {@code where total_cost_usd is null and status='SUCCESS'} 로 탐지할 수 있게 한다.
 */
public final class LlmModelPricing {

	private static final BigDecimal TOKENS_PER_MILLION = BigDecimal.valueOf(1_000_000L);

	/** {@code llm_usage.total_cost_usd decimal(12,6)} 컬럼과 동일한 소수 자릿수. */
	private static final int COST_SCALE = 6;

	private static final List<ModelPrice> PRICES = List.of(
		// GPT-5.x 현행 계열
		new ModelPrice("gpt-5.5", "5.00", "30.00"),
		new ModelPrice("gpt-5.4-nano", "0.20", "1.25"),
		new ModelPrice("gpt-5.4-mini", "0.75", "4.50"),
		new ModelPrice("gpt-5.4", "2.50", "15.00"),
		new ModelPrice("gpt-5.2", "1.75", "14.00"),
		new ModelPrice("gpt-5.1", "1.25", "10.00"),
		// GPT-5 최초 계열 (운영 현재 사용: nano, mini)
		new ModelPrice("gpt-5-nano", "0.05", "0.40"),
		new ModelPrice("gpt-5-mini", "0.25", "2.00"),
		new ModelPrice("gpt-5", "1.25", "10.00"),
		// GPT-4 계열 (과거 데이터/폴백)
		new ModelPrice("gpt-4o-mini", "0.15", "0.60"),
		new ModelPrice("gpt-4o", "2.50", "10.00"),
		new ModelPrice("gpt-4.1-nano", "0.10", "0.40"),
		new ModelPrice("gpt-4.1-mini", "0.40", "1.60"),
		new ModelPrice("gpt-4.1", "2.00", "8.00")
	);

	private static final List<ModelPrice> PRICES_BY_PREFIX_LENGTH = PRICES.stream()
		.sorted(Comparator.comparingInt((ModelPrice price) -> price.prefix().length()).reversed())
		.toList();

	private LlmModelPricing() {
	}

	/**
	 * 모델/토큰 사용량으로부터 USD 비용을 계산한다.
	 *
	 * @return 계산된 비용(소수 6자리). 모델 단가가 등록되지 않았으면 {@code null}.
	 */
	public static BigDecimal costUsd(String model, long inputTokens, long outputTokens) {
		if (model == null || model.isBlank()) {
			return null;
		}
		if (inputTokens < 0 || outputTokens < 0) {
			throw new IllegalArgumentException("token counts must not be negative");
		}
		ModelPrice price = priceFor(model);
		if (price == null) {
			return null;
		}
		BigDecimal cost = price.inputPerMillion().multiply(BigDecimal.valueOf(inputTokens))
			.add(price.outputPerMillion().multiply(BigDecimal.valueOf(outputTokens)));
		return cost.divide(TOKENS_PER_MILLION, COST_SCALE, RoundingMode.HALF_UP);
	}

	/** 등록된 단가가 있는지 여부. */
	public static boolean isPriced(String model) {
		return model != null && !model.isBlank() && priceFor(model) != null;
	}

	private static ModelPrice priceFor(String model) {
		String normalized = model.strip().toLowerCase(Locale.ROOT);
		for (ModelPrice price : PRICES_BY_PREFIX_LENGTH) {
			if (normalized.startsWith(price.prefix())) {
				return price;
			}
		}
		return null;
	}

	private record ModelPrice(String prefix, BigDecimal inputPerMillion, BigDecimal outputPerMillion) {

		private ModelPrice(String prefix, String inputPerMillion, String outputPerMillion) {
			this(prefix, new BigDecimal(inputPerMillion), new BigDecimal(outputPerMillion));
		}
	}
}
