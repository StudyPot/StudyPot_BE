package com.studypot.aistudyleader.studygroup.service;

/**
 * 호스트 스터디 개수 한도를 초과해 새 스터디를 생성할 수 없을 때 던진다.
 * 플랜/한도/현재 개수를 담아 클라이언트가 "왜 안 되는지"를 표시할 수 있게 한다.
 */
public class StudyGroupQuotaExceededException extends RuntimeException {

	private final String plan;
	private final int limit;
	private final int current;

	public StudyGroupQuotaExceededException(String plan, int limit, int current) {
		super("study group host quota exceeded for plan " + plan + ": limit=" + limit + " current=" + current);
		this.plan = plan;
		this.limit = limit;
		this.current = current;
	}

	public String plan() {
		return plan;
	}

	public int limit() {
		return limit;
	}

	public int current() {
		return current;
	}
}
