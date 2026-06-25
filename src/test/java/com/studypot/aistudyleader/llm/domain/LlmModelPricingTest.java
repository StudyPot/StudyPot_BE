package com.studypot.aistudyleader.llm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LlmModelPricingTest {

	@Test
	void computesCostForGpt5NanoUsingPublishedRates() {
		// 6652 input * $0.05/1M + 376 output * $0.40/1M = 0.0003326 + 0.0001504 = 0.000483
		assertThat(LlmModelPricing.costUsd("gpt-5-nano", 6652, 376))
			.isEqualByComparingTo(new BigDecimal("0.000483"));
	}

	@Test
	void computesCostForGpt5Mini() {
		// 1000 * $0.25/1M + 500 * $2.00/1M = 0.00025 + 0.001 = 0.00125
		assertThat(LlmModelPricing.costUsd("gpt-5-mini", 1000, 500))
			.isEqualByComparingTo(new BigDecimal("0.001250"));
	}

	@Test
	void computesCostForGpt52() {
		// 11 * $1.75/1M + 6 * $14.00/1M = 0.00001925 + 0.000084 = 0.00010325 -> 0.000103
		assertThat(LlmModelPricing.costUsd("gpt-5.2", 11, 6))
			.isEqualByComparingTo(new BigDecimal("0.000103"));
	}

	@Test
	void matchesModelNamesByPrefixIgnoringDateSuffixAndCase() {
		assertThat(LlmModelPricing.costUsd("GPT-5-NANO-2025-08-07", 1_000_000, 0))
			.isEqualByComparingTo(new BigDecimal("0.050000"));
	}

	@Test
	void prefersMoreSpecificPrefixOverGeneralGpt5() {
		// gpt-5-nano must not fall back to the base gpt-5 ($1.25) rate
		assertThat(LlmModelPricing.costUsd("gpt-5-nano", 1_000_000, 0))
			.isEqualByComparingTo(new BigDecimal("0.050000"));
		assertThat(LlmModelPricing.costUsd("gpt-5", 1_000_000, 0))
			.isEqualByComparingTo(new BigDecimal("1.250000"));
	}

	@Test
	void returnsNullForUnknownModelToDistinguishUnpricedFromFree() {
		assertThat(LlmModelPricing.costUsd("some-unknown-model", 1000, 1000)).isNull();
		assertThat(LlmModelPricing.costUsd(null, 1000, 1000)).isNull();
		assertThat(LlmModelPricing.isPriced("some-unknown-model")).isFalse();
		assertThat(LlmModelPricing.isPriced("gpt-5-nano")).isTrue();
	}

	@Test
	void rejectsNegativeTokenCounts() {
		assertThatThrownBy(() -> LlmModelPricing.costUsd("gpt-5-nano", -1, 0))
			.isInstanceOf(IllegalArgumentException.class);
	}
}
