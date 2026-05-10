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
}
