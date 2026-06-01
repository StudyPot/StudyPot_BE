package com.studypot.aistudyleader.studygroup.board.service;

public class InvalidGroupBoardRequestException extends RuntimeException {

	private final String field;

	public InvalidGroupBoardRequestException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String field() {
		return field;
	}
}
