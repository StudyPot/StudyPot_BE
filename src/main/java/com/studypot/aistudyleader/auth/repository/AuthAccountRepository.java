package com.studypot.aistudyleader.auth.repository;

import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import java.util.Optional;
import java.util.UUID;

public interface AuthAccountRepository {

	Optional<AuthUser> findActiveUser(UUID userId);

	Optional<AuthUser> findActiveUserByEmail(EmailAddress email);

	Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId);

	AuthUser save(AuthUser user);

	OAuthAccount save(OAuthAccount account);

	/** 프로필(닉네임/자기소개) 부분 수정. 대상 활성 사용자가 없으면 false. */
	boolean updateProfile(UUID userId, String nickname, String bio, java.time.Instant updatedAt);
}
