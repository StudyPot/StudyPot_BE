package com.studypot.aistudyleader.auth.admin;

/** 운영자 허용목록에 없는 사용자가 사용자 플랜 관리 API에 접근할 때 발생합니다. */
public class AdminUserAccessDeniedException extends RuntimeException {

	public AdminUserAccessDeniedException(String message) {
		super(message);
	}
}
