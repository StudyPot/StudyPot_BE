package com.studypot.aistudyleader.auth.service;

public record SignupCommand(
	String email,
	String nickname,
	String password
) {
}
