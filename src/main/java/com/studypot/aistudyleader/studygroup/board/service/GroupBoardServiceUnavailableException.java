package com.studypot.aistudyleader.studygroup.board.service;

public class GroupBoardServiceUnavailableException extends RuntimeException {

	public GroupBoardServiceUnavailableException(String message) {
		super(message);
	}

	public GroupBoardServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}
}
