package com.studypot.aistudyleader.curriculum.service;

public class CurriculumAccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public CurriculumAccessDeniedException(String message) {
		super(message);
	}
}
