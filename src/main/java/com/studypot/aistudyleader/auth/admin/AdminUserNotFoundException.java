package com.studypot.aistudyleader.auth.admin;

/** 운영자 사용자 관리에서 대상 사용자를 찾을 수 없을 때 발생합니다. */
public class AdminUserNotFoundException extends RuntimeException {

	public AdminUserNotFoundException(String message) {
		super(message);
	}
}
