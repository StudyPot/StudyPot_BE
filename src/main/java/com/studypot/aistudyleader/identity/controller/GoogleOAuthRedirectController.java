package com.studypot.aistudyleader.identity.controller;

import com.studypot.aistudyleader.identity.infrastructure.google.GoogleOAuthConfigurationException;
import com.studypot.aistudyleader.identity.infrastructure.google.GoogleOAuthProperties;
import com.studypot.aistudyleader.identity.infrastructure.google.OAuthProviderResponseException;
import com.studypot.aistudyleader.identity.infrastructure.security.AuthProperties;
import com.studypot.aistudyleader.identity.infrastructure.security.AuthTokenCookieIssuer;
import com.studypot.aistudyleader.identity.service.AuthServiceUnavailableException;
import com.studypot.aistudyleader.identity.service.AuthSessionMetadata;
import com.studypot.aistudyleader.identity.service.AuthSessionRejectedException;
import com.studypot.aistudyleader.identity.service.AuthSessionService;
import com.studypot.aistudyleader.identity.service.AuthTokenResult;
import com.studypot.aistudyleader.identity.service.GoogleOAuthLoginCommand;
import com.studypot.aistudyleader.identity.service.OAuthLoginRejectedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
class GoogleOAuthRedirectController {

	private static final String AUTHORIZATION_PATH = "/api/oauth2/authorization/google";
	private static final String CALLBACK_PATH = "/api/login/oauth2/code/google";
	private static final String STATE_COOKIE = "studypot_oauth_state";
	private static final String CODE_VERIFIER_COOKIE = "studypot_oauth_code_verifier";
	private static final String REDIRECT_URI_COOKIE = "studypot_oauth_redirect_uri";
	private static final String CODE_CHALLENGE_METHOD = "S256";
	private static final Duration TEMPORARY_COOKIE_MAX_AGE = Duration.ofMinutes(5);
	private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

	private final GoogleOAuthProperties googleOAuthProperties;
	private final AuthProperties authProperties;
	private final ObjectProvider<AuthSessionService> authSessionService;
	private final AuthTokenCookieIssuer tokenCookieIssuer;
	private final SecureRandom secureRandom = new SecureRandom();

	GoogleOAuthRedirectController(
		GoogleOAuthProperties googleOAuthProperties,
		AuthProperties authProperties,
		ObjectProvider<AuthSessionService> authSessionService,
		AuthTokenCookieIssuer tokenCookieIssuer
	) {
		this.googleOAuthProperties = googleOAuthProperties;
		this.authProperties = authProperties;
		this.authSessionService = authSessionService;
		this.tokenCookieIssuer = tokenCookieIssuer;
	}

	@GetMapping(AUTHORIZATION_PATH)
	ResponseEntity<Void> startGoogleLogin(HttpServletRequest request, HttpServletResponse response) {
		requireGoogleClientId();

		String state = randomBase64Url(32);
		String codeVerifier = randomBase64Url(32);
		String redirectUri = callbackUri(request);

		tokenCookieIssuer.addTemporaryCookie(response, STATE_COOKIE, state, TEMPORARY_COOKIE_MAX_AGE);
		tokenCookieIssuer.addTemporaryCookie(response, CODE_VERIFIER_COOKIE, codeVerifier, TEMPORARY_COOKIE_MAX_AGE);
		tokenCookieIssuer.addTemporaryCookie(response, REDIRECT_URI_COOKIE, redirectUri, TEMPORARY_COOKIE_MAX_AGE);

		URI googleAuthorizationUri = UriComponentsBuilder.fromUriString(googleOAuthProperties.authorizationUri())
			.queryParam("response_type", "code")
			.queryParam("client_id", googleOAuthProperties.clientId())
			.queryParam("redirect_uri", redirectUri)
			.queryParam("scope", String.join(" ", googleOAuthProperties.scopes()))
			.queryParam("state", state)
			.queryParam("code_challenge", codeChallenge(codeVerifier))
			.queryParam("code_challenge_method", CODE_CHALLENGE_METHOD)
			.queryParam("access_type", "offline")
			.queryParam("prompt", "consent")
			.encode()
			.build()
			.toUri();
		return redirect(googleAuthorizationUri);
	}

	@GetMapping(CALLBACK_PATH)
	ResponseEntity<Void> completeGoogleLogin(
		@RequestParam(required = false) String code,
		@RequestParam(required = false) String state,
		@RequestParam(required = false) String error,
		HttpServletRequest request,
		HttpServletResponse response
	) {
		if (!isBlank(error) || isBlank(code) || isBlank(state)) {
			return redirectFailure(response);
		}

		String expectedState = tokenCookieIssuer.cookieValue(request, STATE_COOKIE).orElse(null);
		String codeVerifier = tokenCookieIssuer.cookieValue(request, CODE_VERIFIER_COOKIE).orElse(null);
		String redirectUri = tokenCookieIssuer.cookieValue(request, REDIRECT_URI_COOKIE).orElse(null);
		if (!state.equals(expectedState) || isBlank(codeVerifier) || isBlank(redirectUri)) {
			return redirectFailure(response);
		}

		try {
			AuthTokenResult result = authSessionService().loginWithGoogle(
				new GoogleOAuthLoginCommand(code, redirectUri, codeVerifier),
				metadata(request)
			);
			tokenCookieIssuer.addTokenCookies(response, result);
			clearTemporaryCookies(response);
			return redirect(authProperties.oauth2().frontendSuccessUri());
		} catch (
			IllegalArgumentException
			| AuthServiceUnavailableException
			| AuthSessionRejectedException
			| OAuthLoginRejectedException
			| GoogleOAuthConfigurationException
			| OAuthProviderResponseException exception
		) {
			return redirectFailure(response);
		}
	}

	private AuthSessionService authSessionService() {
		return authSessionService.getIfAvailable(() -> {
			throw new AuthServiceUnavailableException("auth service is not configured.");
		});
	}

	private void requireGoogleClientId() {
		if (googleOAuthProperties.clientId() == null) {
			throw new GoogleOAuthConfigurationException("Google OAuth client id is not configured.");
		}
	}

	private String callbackUri(HttpServletRequest request) {
		ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(request);
		if (request.isSecure()) {
			builder.scheme("https");
		}
		return builder.replacePath(CALLBACK_PATH)
			.replaceQuery(null)
			.build()
			.toUriString();
	}

	private ResponseEntity<Void> redirectFailure(HttpServletResponse response) {
		clearTemporaryCookies(response);
		return redirect(authProperties.oauth2().frontendFailureUri());
	}

	private void clearTemporaryCookies(HttpServletResponse response) {
		tokenCookieIssuer.clearCookie(response, STATE_COOKIE);
		tokenCookieIssuer.clearCookie(response, CODE_VERIFIER_COOKIE);
		tokenCookieIssuer.clearCookie(response, REDIRECT_URI_COOKIE);
	}

	private static ResponseEntity<Void> redirect(URI location) {
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(location)
			.build();
	}

	private static AuthSessionMetadata metadata(HttpServletRequest request) {
		return new AuthSessionMetadata(request.getHeader("User-Agent"), request.getRemoteAddr());
	}

	private String randomBase64Url(int byteLength) {
		byte[] bytes = new byte[byteLength];
		secureRandom.nextBytes(bytes);
		return BASE64_URL.encodeToString(bytes);
	}

	private static String codeChallenge(String codeVerifier) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return BASE64_URL.encodeToString(digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
