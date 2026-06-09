package com.studypot.aistudyleader.auth.repository;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import com.studypot.aistudyleader.auth.domain.EmailAddress;
import com.studypot.aistudyleader.auth.domain.OAuthAccount;
import com.studypot.aistudyleader.auth.domain.OAuthProvider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "studypot.auth.in-memory-repository.enabled", havingValue = "true")
class InMemoryAuthRepositoryConfiguration {

	@Bean
	@ConditionalOnMissingBean(AuthAccountRepository.class)
	AuthAccountRepository inMemoryAuthAccountRepository() {
		return new InMemoryAuthAccountRepository();
	}

	private static final class InMemoryAuthAccountRepository implements AuthAccountRepository {

		private final Map<UUID, AuthUser> usersById = new ConcurrentHashMap<>();
		private final Map<String, UUID> userIdsByEmail = new ConcurrentHashMap<>();
		private final Map<String, OAuthAccount> oauthAccountsByLiveKey = new ConcurrentHashMap<>();

		@Override
		public Optional<AuthUser> findActiveUser(UUID userId) {
			return Optional.ofNullable(usersById.get(userId));
		}

		@Override
		public Optional<AuthUser> findActiveUserByEmail(EmailAddress email) {
			return Optional.ofNullable(userIdsByEmail.get(email.liveKey()))
				.map(usersById::get);
		}

		@Override
		public Optional<OAuthAccount> findActiveOAuthAccount(OAuthProvider provider, String providerUserId) {
			return Optional.ofNullable(oauthAccountsByLiveKey.get(provider.liveKey(providerUserId)));
		}

		@Override
		public AuthUser save(AuthUser user) {
			UUID existingUserId = userIdsByEmail.putIfAbsent(user.email().liveKey(), user.id());
			if (existingUserId != null && !existingUserId.equals(user.id())) {
				throw new AuthUniquenessConflictException("Auth user email is already reserved.");
			}
			usersById.put(user.id(), user);
			return user;
		}

		@Override
		public OAuthAccount save(OAuthAccount account) {
			oauthAccountsByLiveKey.put(account.providerAccountLiveKey(), account);
			return account;
		}
	}
}
