package com.studypot.aistudyleader.curriculum.repository;

public class CurriculumPersistenceException extends RuntimeException {

	public CurriculumPersistenceException(String message) {
		super(message);
	}

	public CurriculumPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
