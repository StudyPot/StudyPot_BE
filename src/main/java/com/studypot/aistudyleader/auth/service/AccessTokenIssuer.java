package com.studypot.aistudyleader.auth.service;

import com.studypot.aistudyleader.auth.domain.AuthUser;
import java.time.Instant;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(AuthUser user, Instant now);
}
