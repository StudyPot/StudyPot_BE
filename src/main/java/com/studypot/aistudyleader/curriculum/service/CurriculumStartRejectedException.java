package com.studypot.aistudyleader.curriculum.service;

public class CurriculumStartRejectedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CurriculumStartRejectedException(String message) {
		super(message);
	}

	public CurriculumStartRejectedException(String message, Throwable cause) {
		super(message, cause);
	}
}
