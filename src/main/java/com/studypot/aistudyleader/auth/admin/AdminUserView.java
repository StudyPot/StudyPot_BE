package com.studypot.aistudyleader.auth.admin;

import java.util.UUID;

/** 운영자 사용자 관리 화면에 노출할 최소 사용자 정보입니다. */
public record AdminUserView(UUID id, String email, String nickname, String plan) {
}
