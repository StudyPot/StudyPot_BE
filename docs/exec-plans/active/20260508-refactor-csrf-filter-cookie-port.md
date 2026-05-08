# EXEC_PLAN: [refactor] CSRF 필터 분리 및 AuthTokenCookiePort 읽기 메서드 제거

- Task slug: `refactor-csrf-filter-cookie-port`
- Base branch: `develop`
- Feature branch: `codex/refactor-csrf-filter-cookie-port`
- Status: `in_progress`

## Required Reads
- [x] AGENTS.md
- [x] ARCHITECTURE.md
- [x] docs/index.md

## Related Docs
- [x] docs/specs/adr/ADR-20260508-oauth2-cookie-login.md

## Related Feature IDs
- [x] identity-core

## Doc Notes
- `BrowserCsrfProtectionFilter`를 `SecurityConfiguration` 내부 클래스에서 별도 top-level 클래스로 추출해 가독성 개선
- `AuthTokenCookiePort`에서 `HttpServletRequest` 의존 메서드(accessToken, refreshToken, cookieValue) 제거 — 읽기는 어댑터 구현체(`AuthTokenCookieIssuer`)에서만 제공
- `CookieBearerTokenResolver`와 `AuthController`는 읽기 메서드 사용 시 `AuthTokenCookieIssuer`를 직접 참조

## Goal
헥사고날 아키텍처 원칙에 맞게 `AuthTokenCookiePort` 인터페이스를 쓰기 전용으로 정리하고,
`SecurityConfiguration`의 가독성을 높이기 위해 `BrowserCsrfProtectionFilter`를 별도 클래스로 분리한다.

## Approach
1. `BrowserCsrfProtectionFilter`를 `SecurityConfiguration` 내부 클래스에서 별도 파일로 추출한다.
2. `AuthTokenCookiePort`에서 `HttpServletRequest` 의존 메서드를 제거하고 `@Override` 정리한다.
3. `CookieBearerTokenResolver`와 `AuthController`가 `AuthTokenCookieIssuer`를 직접 참조하도록 수정한다.
4. `BrowserCsrfProtectionFilter` 단위 테스트를 추가한다.
5. 전체 테스트 통과를 확인한다.

## Step Plan
- [x] `BrowserCsrfProtectionFilter` 별도 클래스로 추출
- [x] `AuthTokenCookiePort` 읽기 메서드 제거
- [x] `CookieBearerTokenResolver` 수정 — `AuthTokenCookieIssuer` 직접 참조
- [x] `AuthController` 수정 — `AuthTokenCookieIssuer` 직접 참조
- [x] `JwtSecurityConfiguration` 빈 반환 타입 수정
- [x] `BrowserCsrfProtectionFilter` 단위 테스트 추가
- [x] `./gradlew test` 통과 확인

## Done Criteria
- `SecurityConfiguration`에 inner class가 없다.
- `AuthTokenCookiePort`에 `HttpServletRequest` import가 없다.
- 전체 테스트가 통과한다.
