package com.studypot.aistudyleader.studygroup.board.domain;

public enum GroupBoardType {
	NOTICE("공지", "그룹 운영 공지 게시판입니다."),
	QUESTION("질문", "학습 질문과 답변을 남기는 게시판입니다."),
	RESOURCE("자료 공유", "학습 자료와 링크를 공유하는 게시판입니다."),
	RETROSPECTIVE("회고", "주차 회고와 학습 기록을 남기는 게시판입니다.");

	private final String displayName;
	private final String description;

	GroupBoardType(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}

	public String displayName() {
		return displayName;
	}

	public String description() {
		return description;
	}
}
