package com.studypot.aistudyleader.studygroup.board.repository;

public class GroupBoardPersistenceException extends RuntimeException {

	public GroupBoardPersistenceException(String message) {
		super(message);
	}

	public GroupBoardPersistenceException(String message, Throwable cause) {
		super(message, cause);
	}
}
