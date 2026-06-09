package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.auth.service.AuthenticatedUser;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "인증된 사용자의 기본 프로필 응답입니다.")
record AuthUserResponse(
	@Schema(description = "사용자 UUID입니다.", example = "018f6f55-6fb1-7d62-a711-25f7c6d16a28")
	UUID id,
	@Schema(description = "Google OAuth 계정에서 확인한 이메일 주소입니다.", example = "member@studypot.dev")
	String email,
	@Schema(description = "서비스에서 표시할 사용자 닉네임입니다.", example = "현우")
	String nickname,
	@Schema(description = "사용자 프로필 이미지 URL입니다.", example = "https://cdn.studypot.dev/profiles/member.png")
	String profileImage,
	@Schema(description = "사용자 자기소개입니다.", example = "백엔드와 Vue를 함께 공부합니다.")
	String bio,
	@Schema(description = "관심 학습 주제 목록입니다.", example = "[\"Spring Boot\", \"JPA\"]")
	List<String> preferredTopics,
	@Schema(description = "사용자 숙련도입니다.", example = "intermediate")
	String skillLevel
) {

	static AuthUserResponse from(AuthenticatedUser user) {
		return new AuthUserResponse(
			user.id(),
			user.email(),
			user.nickname(),
			user.profileImage(),
			user.bio(),
			user.preferredTopics(),
			user.skillLevel()
		);
	}
}
