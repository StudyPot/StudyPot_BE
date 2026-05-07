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
