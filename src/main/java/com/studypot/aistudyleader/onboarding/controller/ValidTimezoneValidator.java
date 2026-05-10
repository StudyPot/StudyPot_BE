package com.studypot.aistudyleader.onboarding.controller;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.ZoneId;

public final class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		try {
			ZoneId.of(value.strip());
			return true;
		} catch (DateTimeException exception) {
			return false;
		}
	}
}
