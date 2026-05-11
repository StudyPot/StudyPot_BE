package com.studypot.aistudyleader.curriculum.service;

public class CurriculumGenerationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CurriculumGenerationException(String message) {
		super(message);
	}

	public CurriculumGenerationException(String message, Throwable cause) {
		super(message, cause);
	}
}
