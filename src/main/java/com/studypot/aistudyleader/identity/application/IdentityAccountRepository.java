package com.studypot.aistudyleader.identity.application;

import com.studypot.aistudyleader.identity.domain.EmailAddress;
import com.studypot.aistudyleader.identity.domain.IdentityUser;
import com.studypot.aistudyleader.identity.domain.OAuthAccount;
import com.studypot.aistudyleader.identity.domain.OAuthProvider;
import java.util.Optional;
import java.util.UUID;

public interface IdentityAccountRepository {

	Optional<IdentityUser> findActiveUser(UUID userId);

	Optional<IdentityUser> findActiveUserByEmail(EmailAddress email);

	Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId);

	IdentityUser save(IdentityUser user);

	OAuthAccount save(OAuthAccount account);
}
