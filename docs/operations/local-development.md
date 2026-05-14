# Local Development

This page records the local backend smoke contract for real development work.

## Required Local File
- Keep real secrets in ignored `config/application-local.yml`.
- Start from `config/application-local.example.yml`.
- Do not put local secrets under `src/main/resources`; Gradle can package resource files into build artifacts.
- Do not commit Google client secrets, JWT secrets, database passwords, or local OAuth JSON files.

## Database
- Local verification expects MySQL 8.0 or newer that accepts TCP connections. The baseline schema and local database creation use MySQL 8 collation behavior such as `utf8mb4_0900_ai_ci`.
- Defaults:
  - Host: `127.0.0.1`
  - Port: `3306`
  - Database: `studypot`
  - User: `root`
- Override with:
  - `STUDYPOT_LOCAL_DB_HOST`
  - `STUDYPOT_LOCAL_DB_PORT`
  - `STUDYPOT_LOCAL_DB_NAME`
  - `STUDYPOT_LOCAL_DB_USER`
- `STUDYPOT_LOCAL_DB_PASSWORD`
- `STUDYPOT_LOCAL_CONFIG` if the local config lives outside `config/application-local.yml`.

## Redis Rate Limit
- Redis-backed rate limiting is disabled by default for local and test runs.
- Keep `STUDYPOT_RATE_LIMIT_ENABLED=false` when Redis is not running.
- To exercise the Redis path locally, start Redis and enable the rate limiter:

```bash
docker run --name studypot-redis -p 6379:6379 -d redis:7
export STUDYPOT_RATE_LIMIT_ENABLED=true
export STUDYPOT_REDIS_HOST=localhost
export STUDYPOT_REDIS_PORT=6379
export STUDYPOT_REDIS_HEALTH_ENABLED=true
```

- Local defaults:
  - Host: `localhost`
  - Port: `6379`
  - `/api/v1/users/me`: 60 requests per 60 seconds per authenticated user.
- Redis keys are short-lived counters such as `rate:users-me:user:{userId}:60s`; they do not replace MySQL-owned records.
- Redis actuator health is disabled by default with `STUDYPOT_REDIS_HEALTH_ENABLED=false` so local health checks do not require Redis unless the Redis path is being exercised.
- If the rate limiter is enabled, Redis failures fail closed by default with `STUDYPOT_RATE_LIMIT_FAIL_CLOSED=true`. Set it to `false` only for local experiments that should fail open.

## Google Login
- Browser login starts at `https://localhost:8080/api/oauth2/authorization/google` when the local backend is serving HTTPS on port `8080`.
- Browser OAuth authorization, state storage, PKCE, callback handling, Google token exchange, and Google user-info loading are handled by Spring Security `oauth2-client`.
- If local HTTPS is not enabled, use the matching HTTP URL and register the matching callback URL in Google Cloud.
- Google OAuth Authorized redirect URI:
  - HTTPS local: `https://localhost:8080/api/login/oauth2/code/google`
  - HTTP local fallback: `http://localhost:8080/api/login/oauth2/code/google`
- The backend sends Google the fixed callback URI from `STUDYPOT_AUTH_OAUTH2_BACKEND_CALLBACK_URI`; do not derive it from request `Host` headers.
- Frontend redirect targets are configured with:
  - `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`
  - `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`
- Frontend origins allowed to send credentialed requests are configured with `STUDYPOT_CORS_ALLOWED_ORIGINS`, for example `https://localhost:3000`.
- Token cookies are HttpOnly. Browser JavaScript should call backend APIs with credentials included rather than reading token values directly.
- Cookie-backed unsafe requests must echo the readable `XSRF-TOKEN` cookie in the `X-XSRF-TOKEN` header. Bearer-token API requests are not required to send this CSRF header.
- For plain HTTP-only local smoke runs, set `STUDYPOT_AUTH_COOKIE_SECURE=false`; keep it `true` for HTTPS local and production-like testing.

## Smoke Verification
Run:

```bash
scripts/task/verify-local-dev.sh
```

The script verifies:
- MySQL connectivity and local database creation.
- Spring Boot `local` profile startup.
- Flyway migration history and all 19 ERD baseline tables.
- `/actuator/health`.
- `/v3/api-docs`.
- `/swagger-ui.html`.
- `POST /api/v1/auth/refresh` returns `401` for an invalid token instead of `503 auth service is not configured`.

The app log is written under `build/local-dev/`.
