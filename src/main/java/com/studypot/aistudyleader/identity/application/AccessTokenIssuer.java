package com.studypot.aistudyleader.identity.application;

import com.studypot.aistudyleader.identity.domain.IdentityUser;
import java.time.Instant;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(IdentityUser user, Instant now);
}
