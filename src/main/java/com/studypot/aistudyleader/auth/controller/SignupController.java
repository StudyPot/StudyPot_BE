package com.studypot.aistudyleader.auth.controller;

import com.studypot.aistudyleader.auth.service.SignupCommand;
import com.studypot.aistudyleader.auth.service.SignupResult;
import com.studypot.aistudyleader.auth.service.SignupService;
import com.studypot.aistudyleader.auth.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.global.api.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원가입", description = "이메일 기반 회원가입과 이메일 중복 확인 API입니다.")
@RestController
@RequiredArgsConstructor
class SignupController {

	private final ObjectProvider<SignupService> signupService;

	@Operation(summary = "이메일 회원가입", description = "이메일, 닉네임, 비밀번호를 검증하고 신규 사용자를 생성합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "201", description = "신규 사용자가 생성됨"),
		@ApiResponse(responseCode = "422", description = "이메일, 닉네임, 비밀번호가 유효하지 않거나 이미 등록됨")
	})
	@PostMapping(ApiPaths.V1 + "/auth/signup")
	@ResponseStatus(HttpStatus.CREATED)
	SignupResponse signup(@Valid @RequestBody SignupRequest request) {
		return SignupResponse.from(signupService().signup(new SignupCommand(
			request == null ? null : request.email(),
			request == null ? null : request.nickname(),
			request == null ? null : request.password()
		)));
	}

	@Operation(summary = "이메일 중복 확인", description = "회원가입 전에 이메일 사용 가능 여부를 확인합니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "이메일 사용 가능 여부 반환"),
		@ApiResponse(responseCode = "422", description = "이메일 형식이 유효하지 않음")
	})
	@GetMapping(ApiPaths.V1 + "/auth/signup/email-availability")
	EmailAvailabilityResponse emailAvailability(@RequestParam String email) {
		return new EmailAvailabilityResponse(email, signupService().isEmailAvailable(email));
	}

	private SignupService signupService() {
		return signupService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("signup service is not configured.");
		});
	}

	@Schema(description = "이메일 회원가입 요청입니다.")
	private record SignupRequest(
		@Schema(description = "로그인 이메일입니다.", example = "member@studypot.dev")
		@NotBlank
		@Email
		String email,
		@Schema(description = "서비스 표시 닉네임입니다.", example = "현우")
		@NotBlank
		@Size(max = 80)
		String nickname,
		@Schema(description = "8자 이상의 비밀번호입니다.", example = "studypot123")
		@NotBlank
		@Size(min = 8)
		String password
	) {
	}

	@Schema(description = "이메일 회원가입 완료 응답입니다.")
	private record SignupResponse(UUID id, String email, String nickname) {

		private static SignupResponse from(SignupResult result) {
			return new SignupResponse(result.id(), result.email(), result.nickname());
		}
	}

	@Schema(description = "회원가입 이메일 사용 가능 여부 응답입니다.")
	private record EmailAvailabilityResponse(String email, boolean available) {
	}
}
