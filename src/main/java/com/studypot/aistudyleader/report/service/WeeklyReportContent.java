package com.studypot.aistudyleader.report.service;

/**
 * AI가 생성한 주차 리포트 본문입니다. (게시판 글 제목/본문)
 */
public record WeeklyReportContent(String title, String body) {

	public WeeklyReportContent {
		title = requireText(title, "title");
		body = requireText(body, "body");
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank.");
		}
		return value.strip();
	}
}
