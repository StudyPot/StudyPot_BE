# 외부 API · 생성형 API 활용 정리

StudyPot(AI Study Leader) 백엔드가 연동하는 외부 서비스와, 생성형 AI(OpenAI) 활용 방식을 정리한 문서입니다.

## 한눈에 보기

| 구분 | 서비스 | API | 용도 | 핵심 위치 |
|---|---|---|---|---|
| 인증 | **Google OAuth2 / OpenID Connect** | Authorization Code Flow | 소셜 로그인, 사용자 식별 | `auth/infrastructure/google` |
| 생성형 AI | **OpenAI** | Responses API (`/v1/responses`) | 커리큘럼 생성, AI 팀장 채팅, 회고 피드백, 키워드 추천 등 | `curriculum/infrastructure/openai` |

> 알림은 외부 서비스(이메일·푸시·Discord)를 쓰지 않고 서비스 내부 `IN_APP` + SSE로 처리합니다(설계상 외부 API 의존 없음).
> 생성형 AI 공급자는 `OPENAI` / `ANTHROPIC` 두 종류를 스키마상 지원하지만, 현재 구현·운영은 **OpenAI 단일**입니다.

---

## 1. Google OAuth2 (소셜 로그인)

### 용도
- 구글 계정으로 회원가입/로그인. 사용자의 이메일·프로필을 받아 `users` / `oauth_account` 레코드를 생성·갱신합니다.

### 인증 흐름 (Authorization Code Flow)
1. 프론트엔드가 사용자를 **Google 인가 화면**으로 보냄
2. 인가 후 Google이 **백엔드 콜백**으로 `code` 전달
3. 백엔드가 `code`를 **토큰 엔드포인트**에서 access token으로 교환
4. **UserInfo 엔드포인트**에서 사용자 프로필(email·이름) 조회
5. 사용자 조회/생성 → 자체 **JWT 발급** (쿠키 + `Authorization` 헤더 토큰 병행 — 모바일/크로스도메인 대응)
6. 프론트엔드 `frontend-success-uri`로 리다이렉트

### 엔드포인트 / 스코프

| 항목 | 값 |
|---|---|
| Authorization URI | `https://accounts.google.com/o/oauth2/v2/auth` |
| Token URI | `https://oauth2.googleapis.com/token` |
| UserInfo URI | `https://openidconnect.googleapis.com/v1/userinfo` |
| Scopes | `openid`, `email`, `profile` |

### 주요 클래스
- `auth/infrastructure/google/GoogleOAuthClient` — 토큰 교환·UserInfo 조회
- `auth/infrastructure/google/GoogleOAuthConfiguration` / `GoogleOAuthProperties`
- `auth/service/GoogleOAuthLoginCommand` — 로그인 유스케이스

### 설정(환경변수)
```
STUDYPOT_GOOGLE_CLIENT_ID
STUDYPOT_GOOGLE_CLIENT_SECRET
STUDYPOT_GOOGLE_AUTHORIZATION_URI   # 기본 accounts.google.com/...
STUDYPOT_GOOGLE_TOKEN_URI           # 기본 oauth2.googleapis.com/token
STUDYPOT_GOOGLE_USERINFO_URI        # 기본 openidconnect.googleapis.com/v1/userinfo
STUDYPOT_GOOGLE_SCOPES              # 기본 openid,email,profile
STUDYPOT_AUTH_OAUTH2_BACKEND_CALLBACK_URI
STUDYPOT_AUTH_OAUTH2_FRONTEND_SUCCESS_URI
STUDYPOT_AUTH_OAUTH2_FRONTEND_FAILURE_URI
```

---

## 2. OpenAI (생성형 AI)

서비스의 핵심 차별화 기능(AI 팀장)을 담당하는 생성형 AI 연동입니다.

### 2.1 연동 방식
| 항목 | 값 |
|---|---|
| Base URL | `https://api.openai.com/v1` |
| API 모드 | **Responses API** (`api-mode=responses`) |
| 출력 형식 | 자유 텍스트가 아닌 **엄격한 `json_schema`(strict)** — 형식 어긋나면 거부·재시도 |
| 공급자 추상화 | `LlmProvider` 인터페이스 → `OpenAiLlmProvider` 구현 |
| 전송 계층 | `RestClientOpenAiResponsesTransport` (Spring `RestClient`) |

### 2.2 용도(purpose)별 모델 분리
호출 용도마다 **다른 모델**을 매핑합니다. 비용이 큰 작업만 상위 모델을 쓰고, 대부분은 저가 모델로 처리합니다. (`OpenAiPurposeModels`)

| 용도 (`LlmUsagePurpose`) | 모델(기본) | 출력 토큰 상한 | 설명 |
|---|---|---|---|
| `TEAM_LEAD_CHAT` | `gpt-5-nano` | 4,096 | AI 팀장 채팅 응답 |
| `CURRICULUM_GENERATE` / `CURRICULUM_REGENERATE_WEEK` | `gpt-5-mini` | 16,384 | 커리큘럼(주차별 목표·과제·자료) 생성 |
| `RETROSPECTIVE_ANALYZE` / `RETROSPECTIVE_FEEDBACK` | `gpt-5-nano` | 2,048 | 주차 회고 분석·피드백 |
| `NEXT_WEEK_ADJUST` | `gpt-5-nano` | 2,048 | 회고 기반 다음 주 계획 조정 |
| `WEEKLY_REPORT` | `gpt-5-nano` | 2,048 | 주간 그룹 리포트 |
| `DETAIL_KEYWORD_SUGGEST` | `gpt-5-nano` | 256 (reasoning=minimal) | 그룹 생성 시 상세 키워드 추천 |

> 기본 모델은 `STUDYPOT_AI_OPENAI_MODEL`(기본 `gpt-5-nano`), 용도별 오버라이드는 `STUDYPOT_AI_OPENAI_MODEL_<PURPOSE>`로 환경변수만 바꿔 재배포 없이 조정 가능합니다. 비우면 기본 모델로 폴백합니다.

### 2.3 신뢰성 — 사실 기반(grounding) + 민감정보 차단
- **그라운딩**: 진실의 원천은 공급된 스터디 사실(DB)뿐. 컨텍스트에 없는 주제·진도·완료는 단정하지 않고, 근거가 얕으면 추측 대신 구체적 질문 1개를 던집니다.
- **프롬프트 새니타이징** (`LlmPromptSanitizer`): 모델에 보내기 전, 프롬프트/컨텍스트에서 `apiKey`·`oauthToken`·`cookie`·`secret` 등 민감 키와 자격증명 할당식을 탐지해 **`[REDACTED]`로 마스킹**합니다.

### 2.4 비용 · 사용량 추적
모든 LLM 호출은 `llm_usage` 테이블에 적재되어 사용량/비용을 추적합니다. (`LlmUsage` 도메인 / `LlmUsageController`)

| 필드 | 내용 |
|---|---|
| `purpose`, `provider`, `model` | 호출 용도 / 공급자(OPENAI) / 모델명 |
| `inputTokens`, `outputTokens` | 입력·출력 토큰 수 |
| `totalCostUsd` | 추정 비용(USD) |
| `latencyMs`, `status` | 지연시간 / 결과(SUCCESS·FAILED·TIMEOUT) |
| `createdDateUtc` | 일자(집계용) |

### 2.5 호출 빈도 제한(rate limit)
남용·비용 폭주 방지를 위해 용도별 호출 한도를 둡니다. (`studypot.rate-limit`, 기본 비활성·환경별 설정)

| 대상 | 기본 한도 |
|---|---|
| AI 팀장 채팅 | 5회 / 60초 |
| 커리큘럼 생성 | 3회 / 10분 |
| 회고 피드백 | 2회 / 24시간 |

### 2.6 비동기 처리 (AI 팀장 채팅)
- AI 팀장 채팅 응답 생성은 **RabbitMQ로 비동기 처리**합니다: 사용자 메시지 저장 → 큐에 작업 발행 → worker가 생성 → **SSE로 전달**(인메모리).
- 브로커가 없는 로컬/테스트 환경을 위해 **동기 폴백 경로**를 함께 유지합니다(`studypot.ai.conversation.rabbitmq.enabled`).

### 2.7 설정(환경변수)
```
STUDYPOT_AI_OPENAI_API_KEY                       # OpenAI API 키 (시크릿)
STUDYPOT_AI_OPENAI_BASE_URL                      # 기본 https://api.openai.com/v1
STUDYPOT_AI_OPENAI_API_MODE                      # 기본 responses
STUDYPOT_AI_OPENAI_MODEL                         # 기본(저가) 모델, 기본 gpt-5-nano
STUDYPOT_AI_OPENAI_MODEL_CURRICULUM_GENERATE     # 기본 gpt-5-mini
STUDYPOT_AI_OPENAI_MODEL_RETROSPECTIVE_FEEDBACK  # 비우면 기본 모델
STUDYPOT_AI_OPENAI_MODEL_TEAM_LEAD_CHAT          # 비우면 기본 모델
STUDYPOT_AI_OPENAI_MODEL_DETAIL_KEYWORD_SUGGEST  # 비우면 기본 모델
STUDYPOT_AI_OPENAI_MAX_OUTPUT_TOKENS_*           # 용도별 출력 토큰 상한
STUDYPOT_AI_OPENAI_REASONING_EFFORT_*            # 용도별 추론 강도
STUDYPOT_AI_OPENAI_CONNECT_TIMEOUT / READ_TIMEOUT
```

---

## 3. 시크릿 · 키 관리
- 운영 시크릿(OpenAI 키, Google client secret 등)은 **코드에 하드코딩하지 않고** 환경변수로 주입합니다.
- 배포(GitHub Actions)는 **GitHub Secrets → 서버 `.runtime.env` 생성 → 컨테이너 주입** 경로를 사용합니다.
- 로컬 개발용 `config/application-local.yml`은 **gitignore** 처리되어 저장소에 커밋되지 않습니다.

## 4. 데이터 흐름 요약
```
[사용자] --Google OAuth2--> [백엔드] --JWT 발급--> [사용자]

[AI 기능 요청]
  → LlmPromptSanitizer (민감정보 마스킹)
  → OpenAiLlmProvider (Responses API, 용도별 모델, json_schema strict)
  → OpenAI (api.openai.com/v1)
  → 응답 검증/파싱
  → llm_usage 적재(토큰·비용·지연·상태)
  ※ AI 팀장 채팅은 RabbitMQ worker 생성 → SSE 전달(동기 폴백 유지)
```

---

### 관련 코드
- 인증: `auth/infrastructure/google/`, `auth/service/GoogleOAuthLoginCommand`
- 생성형 AI: `curriculum/infrastructure/openai/` (`OpenAiLlmProvider`, `OpenAiPurposeModels`, `OpenAiOutputTokenLimits`, `RestClientOpenAiResponsesTransport`)
- 신뢰성/비용: `llm/service/LlmPromptSanitizer`, `llm/domain/LlmUsage`, `llm/controller/LlmUsageController`
