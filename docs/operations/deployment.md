# StudyPot Deployment

이 문서는 StudyPot 백엔드를 Docker 이미지로 만들고 `oracle-was`에서 실행하는 배포 계약이다.

## 서버 역할
- `oracle-was`: Spring Boot API 컨테이너만 실행한다.
- `oracle-db`: MySQL 8 컨테이너를 실행한다.
- 현재 공용 DB 연결 경로는 `oracle-was` 공인 IP만 `oracle-db:3306`에 접근하도록 제한한다.
- 장기적으로는 같은 VCN 또는 VCN peering처럼 private 경로를 우선한다.

## 필수 서버 파일
`oracle-was`의 배포 디렉터리는 기본값으로 `/opt/studypot`를 사용한다.

```text
/opt/studypot/docker-compose.yml
/opt/studypot/.env
/opt/studypot/.image.env
/opt/studypot/.previous-image.env
```

- `docker-compose.yml`: repository의 `deploy/docker-compose.prod.yml`을 업로드한다.
- `.env`: 서버에만 보관하는 runtime 환경변수와 secret을 담는다.
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
STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI=https://app.example.com/auth/success
STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI=https://app.example.com/auth/failure

STUDYPOT_CORS_ALLOWED_ORIGINS=https://app.example.com
STUDYPOT_CORS_ALLOW_CREDENTIALS=true
STUDYPOT_AUTH_COOKIE_DOMAIN=example.com
STUDYPOT_AUTH_COOKIE_SECURE=true
STUDYPOT_AUTH_COOKIE_SAME_SITE=Lax

STUDYPOT_GOOGLE_CLIENT_ID=
STUDYPOT_GOOGLE_CLIENT_SECRET=
STUDYPOT_AUTH_OAUTH2_BACKEND_CALLBACK_URI=
STUDYPOT_AI_OPENAI_API_KEY=
```

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
5. `oracle-was`에서 GHCR login, `docker compose pull`, `docker compose up -d`, health check

필수 GitHub Secrets:

```text
STUDYPOT_DEPLOY_HOST
STUDYPOT_DEPLOY_USER
STUDYPOT_DEPLOY_SSH_KEY
STUDYPOT_DEPLOY_KNOWN_HOSTS
STUDYPOT_DEPLOY_DIR
```

- `STUDYPOT_DEPLOY_HOST`: `oracle-was` 접속 호스트
- `STUDYPOT_DEPLOY_USER`: 기본값은 `opc`
- `STUDYPOT_DEPLOY_SSH_KEY`: `oracle-was` 접속 private key
- `STUDYPOT_DEPLOY_KNOWN_HOSTS`: `ssh-keyscan -H <host>` 결과를 사람이 확인한 뒤 등록한 known_hosts 값. 이 secret은 필수이며 workflow는 값이 없으면 실패한다.
- `STUDYPOT_DEPLOY_DIR`: 기본값은 `/opt/studypot`
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
