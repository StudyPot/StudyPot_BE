# StudyPot Deployment

이 문서는 StudyPot 백엔드를 Docker 이미지로 만들고 `oracle-was`에서 실행하는 배포 계약이다.

## 서버 역할
- `oracle-was`: Spring Boot API 컨테이너만 실행한다.
- `oracle-db`: MySQL 8 컨테이너를 실행한다.
- 현재 공용 DB 연결 경로는 `oracle-was` 공인 IP만 `oracle-db:3306`에 접근하도록 제한한다.
- 장기적으로는 같은 VCN 또는 VCN peering처럼 private 경로를 우선한다.

## Redis/RabbitMQ 운영 배치
- Redis/RabbitMQ boundary is approved by [CR-20260519-redis-rabbitmq-realtime-infra](../specs/change-requests/CR-20260519-redis-rabbitmq-realtime-infra.md) and [ADR-20260519-redis-rabbitmq-realtime-infra](../specs/adr/ADR-20260519-redis-rabbitmq-realtime-infra.md).
- 현재 `deploy/docker-compose.prod.yml`은 `studypot-api`만 실행한다. Redis와 RabbitMQ 컨테이너를 이 ADR에서 추가하지 않는다.
- Redis/RabbitMQ production activation is a separate deployment task. 그 작업은 placement, capacity, compose/env pass-through, credentials, actuator health, smoke verification, and rollback을 함께 기록해야 한다.
- 작은 `oracle-was`에는 API 부팅, Flyway, datasource, Swagger/health 검증 여유가 우선이다. Redis/RabbitMQ를 같은 호스트에 올리는 선택은 메모리/CPU/디스크 여유와 재시작 영향이 확인된 뒤에만 허용한다.
- Redis를 켜려면 `STUDYPOT_RATE_LIMIT_ENABLED`, `STUDYPOT_REDIS_HOST`, `STUDYPOT_REDIS_PORT`, `STUDYPOT_REDIS_HEALTH_ENABLED`, 장애 정책 값을 production compose 환경에 명시적으로 전달해야 한다.
- RabbitMQ를 켜려면 `STUDYPOT_NOTIFICATION_RABBITMQ_ENABLED`, `STUDYPOT_RABBITMQ_HOST`, `STUDYPOT_RABBITMQ_PORT`, `STUDYPOT_RABBITMQ_USERNAME`, `STUDYPOT_RABBITMQ_PASSWORD`, `STUDYPOT_RABBITMQ_HEALTH_ENABLED`, exchange/queue/routing-key 값을 production compose 환경에 명시적으로 전달해야 한다.
- 현재 worker는 `studypot-api` 프로세스 안의 RabbitMQ listener다. 별도 `studypot-worker` 컨테이너는 후속 배포 작업에서 같은 image를 재사용할지, worker-only property를 둘지, API와 worker health check를 분리할지 결정한다.
- MySQL은 계속 durable source of truth다. Redis는 rate limit/TTL lock, RabbitMQ는 async dispatch/DLQ boundary만 소유한다.

## rumiclean 전체 이관
`rumiclean` full-stack 배포는 `deploy/rumiclean/docker-compose.yml`을 사용한다. 기본 Oracle compose와 분리되어 있으며, GitHub Actions는 `STUDYPOT_DEPLOY_COMPOSE_FILE=deploy/rumiclean/docker-compose.yml` secret이 설정될 때 이 compose를 업로드한다. Secret이 없으면 기존 `deploy/docker-compose.prod.yml`을 계속 사용한다.

운영 서버 경로:

```text
/home/ec2-user/compose-studypot/docker-compose.yml
/home/ec2-user/compose-studypot/.env
/home/ec2-user/compose-studypot/.runtime.env
/home/ec2-user/compose-studypot/.image.env
/home/ec2-user/compose-studypot/.previous-image.env
/home/ec2-user/compose-studypot/backups/
```

서비스 배치:

- `studypot-api`: Spring Boot API. Caddy가 접근할 수 있도록 `compose-cleanb_default` external network에도 붙는다.
- `studypot-mysql`: StudyPot 전용 MySQL 8. 기존 RumiClean MySQL을 재사용하지 않는다.
- `studypot-redis`: StudyPot 전용 Redis. rate limit과 TTL lock 전용이며 persistence는 사용하지 않는다.
- `studypot-rabbitmq`: StudyPot 전용 RabbitMQ. notification async dispatch와 broker retry/DLQ boundary 전용이다.

DNS와 Caddy:

- `studypot.rumiclean.com`은 `rumiclean` public IP를 바라봐야 한다.
- Caddy route는 `deploy/rumiclean/Caddyfile.studypot` 내용을 `/home/ec2-user/compose-cleanb/Caddyfile`에 추가한다.
- `studypot-api`는 GitHub Actions Deploy health check를 위해 host loopback `127.0.0.1:${STUDYPOT_HTTP_PORT:-8080}`에만 바인딩한다. public API 진입점은 계속 Caddy의 `https://studypot.rumiclean.com`이다.
- RabbitMQ management UI는 public route로 열지 않는다. 필요하면 SSH tunnel로만 확인한다.
- `STUDYPOT_MYSQL_JDBC_PARAMS`는 compose-local MySQL 기본값으로 `useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8&connectionTimeZone=UTC`를 사용한다. MySQL을 별도 TLS endpoint로 옮길 때는 이 값을 TLS 검증용 JDBC parameter와 truststore 설정으로 교체한다.
- `STUDYPOT_RABBITMQ_ERL_ARGS` 기본값은 작은 호스트용 Erlang VM scheduler 인자인 `+S 1:1 +sbwt none +sbwtdcpu none +sbwtdio none`이다. RabbitMQ 설정값은 이 환경변수에 넣지 않는다.
- Google OAuth 성공/실패 리디렉션은 `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`와 `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`가 가리키는 프론트엔드 handler가 실제로 응답해야 완료된다. 현재 `deploy/rumiclean/Caddyfile.studypot` snippet은 API-only catch-all이므로, 같은 `studypot.rumiclean.com` 도메인에 프론트엔드를 붙일 때는 `/api/*`, `/actuator/*`, Swagger/OpenAPI 경로만 `studypot-api`로 보내고 `/auth/success` 및 `/auth/failure`를 포함한 프론트 경로는 프론트엔드 upstream으로 보내도록 Caddy route를 분리한다.

Oracle 배포는 rollback 대상으로 보존한다. 이관 작업 중에는 `oracle-was`의 `studypot-api`, `oracle-db`의 `studypot` schema, 기존 `.env`, `.image.env`, `.previous-image.env`를 삭제하지 않는다.

### DB migration 절차
Oracle DB에서 timestamped dump를 만든다.

```bash
ssh oracle-db
backup_dir=/tmp/studypot-migration-$(date -u +%Y%m%dT%H%M%SZ)
mkdir -p "${backup_dir}"
docker exec oracle-db-mysql sh -lc 'mysqldump --single-transaction --routines --triggers --hex-blob -uroot -p"${MYSQL_ROOT_PASSWORD}" studypot' > "${backup_dir}/studypot.sql"
gzip "${backup_dir}/studypot.sql"
```

`rumiclean`으로 복사하고 StudyPot 전용 MySQL에 restore한다.

```bash
scp oracle-db:/tmp/studypot-migration-*/studypot.sql.gz /tmp/
scp /tmp/studypot.sql.gz rumiclean:/home/ec2-user/compose-studypot/backups/
ssh rumiclean
cd /home/ec2-user/compose-studypot
docker compose --env-file .env --env-file .image.env -f docker-compose.yml up -d studypot-mysql
gzip -dc backups/studypot.sql.gz | docker exec -i studypot-mysql sh -lc 'mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" studypot'
docker exec studypot-mysql sh -lc 'mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -D studypot -e "select installed_rank, version, success from flyway_schema_history order by installed_rank;"'
```

### Runtime smoke
`rumiclean`에서 full stack을 시작한다.

```bash
cd /home/ec2-user/compose-studypot
docker compose --env-file .env --env-file .image.env -f docker-compose.yml up -d
docker compose --env-file .env --env-file .image.env -f docker-compose.yml ps
docker exec studypot-redis sh -lc 'redis-cli -a "${STUDYPOT_REDIS_PASSWORD}" ping'
docker exec studypot-rabbitmq rabbitmq-diagnostics -q ping
docker exec studypot-mysql sh -lc 'mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" -D studypot -e "select count(*) from flyway_schema_history where success = 1;"'
docker exec studypot-api wget -q -O- http://127.0.0.1:8080/actuator/health
curl -fsS http://127.0.0.1:${STUDYPOT_HTTP_PORT:-8080}/actuator/health
```

외부 DNS와 HTTPS 연결을 확인한다.

```bash
curl -fsS https://studypot.rumiclean.com/actuator/health
```

### Rollback
API rollback은 이전 image tag를 사용한다.

```bash
ssh rumiclean
cd /home/ec2-user/compose-studypot
previous_image="$(sed -n 's/^STUDYPOT_PREVIOUS_IMAGE=//p' .previous-image.env | tail -n 1)"
test -n "${previous_image}"
printf 'STUDYPOT_IMAGE=%s\n' "${previous_image}" > .image.env
docker compose --env-file .env --env-file .image.env -f docker-compose.yml up -d --force-recreate studypot-api
curl -fsS https://studypot.rumiclean.com/actuator/health
```

전체 rollback이 필요하면 DNS/Caddy route를 Oracle 이전 경로로 되돌리고, Oracle `oracle-was`와 `oracle-db`의 보존된 배포를 사용한다. 이관 task에서는 Oracle 리소스를 삭제하지 않는다.

## 필수 서버 파일
`oracle-was`의 배포 디렉터리는 기본값으로 `/opt/studypot`를 사용한다.

```text
/opt/studypot/docker-compose.yml
/opt/studypot/.env
/opt/studypot/.runtime.env
/opt/studypot/.image.env
/opt/studypot/.previous-image.env
```

- `docker-compose.yml`: repository의 `deploy/docker-compose.prod.yml`을 업로드한다.
- `.env`: 서버에만 보관하는 runtime 환경변수와 secret을 담는다. 정상 GitHub Actions 배포에서 프론트엔드 OAuth redirect/CORS 값은 `.runtime.env`가 원본이다.
- `.runtime.env`: GitHub Actions가 관리하는 배포 runtime override 파일이다. OAuth frontend success/failure URI, CORS allowed origins, AI provider key/base URL/model/API mode처럼 GitHub Secrets에서 주입하는 값을 담으며 repository에는 커밋하지 않는다. This is the GitHub Actions-managed runtime override surface.
- `.image.env`: 배포 workflow가 현재 배포할 GHCR image tag를 쓴다.
- `.previous-image.env`: 배포 직전의 image tag를 rollback 참고용으로 남긴다.

## 서버 `.env` 예시
아래 값은 형식 예시다. 실제 secret은 GitHub Secrets 또는 서버 파일에만 둔다.

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<oracle-db-host>:3306/studypot?serverTimezone=UTC&characterEncoding=utf8&connectionTimeZone=UTC
SPRING_DATASOURCE_USERNAME=studypot
SPRING_DATASOURCE_PASSWORD=replace-with-db-password

STUDYPOT_AUTH_JWT_SECRET=replace-with-at-least-32-character-secret
STUDYPOT_AUTH_JWT_ISSUER=https://api.example.com

STUDYPOT_CORS_ALLOW_CREDENTIALS=true
STUDYPOT_AUTH_COOKIE_DOMAIN=example.com
STUDYPOT_AUTH_COOKIE_SECURE=true
STUDYPOT_AUTH_COOKIE_SAME_SITE=Lax

STUDYPOT_GOOGLE_CLIENT_ID=
STUDYPOT_GOOGLE_CLIENT_SECRET=
STUDYPOT_AUTH_OAUTH2_BACKEND_CALLBACK_URI=

# GitHub Actions writes these values to .runtime.env during normal deployments.
# Keep fallback values here only for manual compose runs.
STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI=https://app.example.com/auth/success
STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI=https://app.example.com/auth/failure
STUDYPOT_CORS_ALLOWED_ORIGINS=https://app.example.com

STUDYPOT_AI_OPENAI_API_KEY=
STUDYPOT_AI_OPENAI_BASE_URL=https://api.openai.com/v1
STUDYPOT_AI_OPENAI_MODEL=gpt-4o-mini
STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST=
STUDYPOT_AI_OPENAI_API_MODE=responses
STUDYPOT_AI_OPENAI_CONNECT_TIMEOUT=5s
STUDYPOT_AI_OPENAI_READ_TIMEOUT=120s
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_DETAIL_KEYWORD_SUGGEST=256
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_CURRICULUM_GENERATE=4096
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_RETROSPECTIVE_FEEDBACK=2048
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_TEAM_LEAD_CHAT=1536
```

`STUDYPOT_GOOGLE_CLIENT_ID`와 `STUDYPOT_GOOGLE_CLIENT_SECRET`는 Spring `studypot.oauth.google.client-id/client-secret`로 매핑되어 OAuth2 login filter를 켠다. 값이 비어 있으면 `/api/oauth2/authorization/google`은 Google로 redirect하지 못하고 `/error` 401처럼 보일 수 있다.

## 수동 배포
수동 배포는 GitHub Actions 자동화를 붙이기 전에 Dockerfile, compose, DB 연결, Flyway, health check를 분리해서 확인하기 위한 1회성 검증이다.

로컬 또는 CI에서 이미지를 만든다.

```bash
docker buildx build --platform linux/amd64 --load -t studypot-api:manual .
docker save studypot-api:manual | gzip > studypot-api-manual.tar.gz
scp studypot-api-manual.tar.gz oracle-was:/tmp/studypot-api-manual.tar.gz
scp deploy/docker-compose.prod.yml oracle-was:/opt/studypot/docker-compose.yml
```

`oracle-was`에서 이미지를 로드하고 실행한다.

```bash
ssh oracle-was
sudo docker load -i /tmp/studypot-api-manual.tar.gz
cd /opt/studypot
printf 'STUDYPOT_IMAGE=studypot-api:manual\n' > .image.env
sudo docker compose --env-file .env --env-file .image.env -f docker-compose.yml up -d studypot-api
sudo docker compose --env-file .env --env-file .image.env -f docker-compose.yml ps
curl -fsS http://127.0.0.1:8080/actuator/health
```

DB migration 상태는 `oracle-db`에서 확인한다.

```bash
ssh oracle-db
sudo docker exec -i oracle-db-mysql sh -lc 'mysql -ustudypot -p -D studypot -e "select installed_rank, version, success from flyway_schema_history order by installed_rank;"'
```

## GitHub Actions 배포
`.github/workflows/deploy.yml`은 `develop` push와 `workflow_dispatch`에서 실행한다.

Workflow 단계:
1. `./gradlew check build --no-daemon`
2. `linux/amd64` Docker image build
3. GHCR에 image push
   - `ghcr.io/<owner>/studypot-api:<commit-sha>`
   - `ghcr.io/<owner>/studypot-api:latest`
4. `oracle-was`에 compose file 업로드
5. GitHub Secrets의 frontend OAuth/CORS 및 AI runtime 값을 `.runtime.env`로 업로드
6. `oracle-was`에서 GHCR login, `docker compose pull`, `docker compose up -d`, health check

Health check는 `docker compose up -d --force-recreate` 직후 단발로 판정하지 않는다. 작은 WAS에서 Spring Boot, Flyway, datasource 초기화가 1분 안팎 걸릴 수 있으므로 workflow는 `curl -fsS http://127.0.0.1:8080/actuator/health`를 5초 간격으로 최대 120초까지 재시도한다. 대기 중에는 `Waiting for studypot-api health` 로그와 container health/status를 남기고, container가 `unhealthy`가 되거나 제한 시간을 넘기면 `docker logs --tail=160 studypot-api`를 출력한 뒤 실패한다.

필수 GitHub Secrets:

```text
STUDYPOT_DEPLOY_HOST
STUDYPOT_DEPLOY_USER
STUDYPOT_DEPLOY_SSH_KEY
STUDYPOT_DEPLOY_KNOWN_HOSTS
STUDYPOT_DEPLOY_DIR
STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI
STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI
STUDYPOT_CORS_ALLOWED_ORIGINS
STUDYPOT_AI_OPENAI_API_KEY
STUDYPOT_AI_OPENAI_BASE_URL
STUDYPOT_AI_OPENAI_MODEL
STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST
STUDYPOT_AI_OPENAI_API_MODE
STUDYPOT_AI_OPENAI_CONNECT_TIMEOUT
STUDYPOT_AI_OPENAI_READ_TIMEOUT
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_DETAIL_KEYWORD_SUGGEST
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_CURRICULUM_GENERATE
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_RETROSPECTIVE_FEEDBACK
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_TEAM_LEAD_CHAT
```

- `STUDYPOT_DEPLOY_HOST`: `oracle-was` 접속 호스트
- `STUDYPOT_DEPLOY_USER`: 기본값은 `opc`
- `STUDYPOT_DEPLOY_SSH_KEY`: `oracle-was` 접속 private key
- `STUDYPOT_DEPLOY_KNOWN_HOSTS`: `ssh-keyscan -H <host>` 결과를 사람이 확인한 뒤 등록한 known_hosts 값. 이 secret은 필수이며 workflow는 값이 없으면 실패한다.
- `STUDYPOT_DEPLOY_DIR`: 기본값은 `/opt/studypot`
- `STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI`: OAuth callback 성공 후 Spring success handler가 이동시킬 프론트엔드 route. Netlify 운영값은 `https://studypot.netlify.app/auth/success`다.
- `STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI`: OAuth callback 실패 후 Spring failure handler가 이동시킬 프론트엔드 route. Netlify 운영값은 `https://studypot.netlify.app/auth/failure`다.
- `STUDYPOT_CORS_ALLOWED_ORIGINS`: credentialed browser API 호출을 허용할 프론트엔드 origin 목록. 쉼표로 구분하며 Netlify 운영 origin은 `https://studypot.netlify.app`다.
- `STUDYPOT_AI_OPENAI_API_KEY`: 운영 AI provider key. 값은 로그, PR, repository 파일에 남기지 않는다.
- `STUDYPOT_AI_OPENAI_BASE_URL`: 기본값은 `https://api.openai.com/v1`; GMS 사용 시 `https://gms.ssafy.io/gmsapi/api.openai.com/v1`.
- `STUDYPOT_AI_OPENAI_MODEL`: 기본값은 `gpt-4o-mini`; GMS 사용 시 발급 계약에 맞는 모델명을 사용한다.
- `STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST`: 세부 키워드 추천 전용 모델 override. 값이 비어 있으면 `STUDYPOT_AI_OPENAI_MODEL`을 그대로 사용한다. GMS nano 모델을 붙일 때는 이 값만 nano 계열로 설정하고 커리큘럼/회고/채팅은 전역 모델을 유지한다.
- `STUDYPOT_AI_OPENAI_API_MODE`: 기본값은 `responses`; GMS `chat/completions` 호환 endpoint 사용 시 `chat-completions`.
- `STUDYPOT_AI_OPENAI_CONNECT_TIMEOUT`: 운영 provider 연결 제한. 기본값은 `5s`.
- `STUDYPOT_AI_OPENAI_READ_TIMEOUT`: 운영 provider 응답 대기 제한. GMS curriculum/chat처럼 구조화 출력이 길어질 수 있는 경로를 위해 배포 기본값은 `120s`.
- `STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_DETAIL_KEYWORD_SUGGEST`: 세부 키워드 추천 출력 상한. 기본값은 `256`.
- `STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_CURRICULUM_GENERATE`: 커리큘럼 생성 출력 상한. 기본값은 `4096`.
- `STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_RETROSPECTIVE_FEEDBACK`: 회고 피드백 출력 상한. 기본값은 `2048`.
- `STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_TEAM_LEAD_CHAT`: AI 팀장 채팅 출력 상한. 기본값은 `1536`.
- GHCR push와 remote pull은 workflow의 `GITHUB_TOKEN`을 사용한다. Workflow 권한에는 `packages: write`가 있어야 한다.

## Rollback
배포 workflow는 `.image.env`에 있던 직전 image tag를 `.previous-image.env`에 보존한다. 직전 image로 되돌릴 때는 이 값을 `.image.env`로 옮기고 compose를 다시 적용한다.

```bash
ssh oracle-was
cd /opt/studypot
previous_image="$(sed -n 's/^STUDYPOT_PREVIOUS_IMAGE=//p' .previous-image.env | tail -n 1)"
test -n "${previous_image}"
printf 'STUDYPOT_IMAGE=%s\n' "${previous_image}" > .image.env
sudo docker compose --env-file .env --env-file .image.env -f docker-compose.yml pull studypot-api
sudo docker compose --env-file .env --env-file .image.env -f docker-compose.yml up -d studypot-api
curl -fsS http://127.0.0.1:8080/actuator/health
```

## 완료 기준
- `docker build`가 성공한다.
- GitHub Actions가 GHCR에 `latest`와 commit SHA tag를 push한다.
- `oracle-was`에서 `studypot-api` 컨테이너가 계속 running 상태다.
- `curl -fsS http://127.0.0.1:8080/actuator/health`가 성공한다.
- 앱 로그에 datasource, Flyway, auth property 오류가 없다.
- `oracle-db`의 `studypot.flyway_schema_history`에 migration 성공 이력이 있다.
- GitHub Actions `Deploy` workflow가 merged commit에서 성공한다.
- `oracle-was`에는 rollback에 사용할 직전 image tag가 `.previous-image.env`로 남는다.
- DB password, JWT secret, OAuth secret, OpenAI key는 repository 파일에 남기지 않는다.
