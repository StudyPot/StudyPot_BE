# FE ↔ BE 연동 점검 — 발견 사항 기록

## 🎯 수정 작업 분류 (목표: 전부 완료)
**FE (StudyPot_FE, dev)**: #1, #7, #11, #15, #22, (#12·#13의 FE 파트), (#14의 FE 파트) — ✅완료: #20, #21
**BE (StudyPot, develop)**: #3, #4, #5, #8, #9, #10, #16, #17, #19, (#12·#13의 BE 파트), (#14의 BE 파트), #18(A~D), #2(follow)
**보류/제외**: #6(참고)

진행 상태는 각 항목 "상태:" 라인에서 갱신.

---


정적 계약 대조(FE `apiClient` 호출 표면 vs BE 컨트롤러 매핑) 결과. 기준: FE `dev`, BE `develop` (둘 다 최신화 완료).
상태: 🔴 확정 버그 · 🟠 계약 갭(BE 미구현) · ⚪ 미사용/참고 · ✅ 확인·해결

---

## 검증 완료 페이지
- ① GroupsPage(전체 그룹) — ✅ 계약 정상 (단 진행률은 #1 참고)
- ② GroupCreatePage(그룹 생성) — ✅ 정상
- ⑨ GroupOverviewPage(그룹 홈) — 🔴 #1, 개선요청 #7·#8
- ⑦ GroupBoardPage(게시판) — 엔드포인트 11/11 정상, 단 🔴 #9·#10·#11
- ③ GroupMyPage(팀원) — 계약·응답필드 정상, 단 🔴 #12("이번 주 완료"에 게시글 포함)
- ④ GroupTodoPage(Todo) — 계약 정상, 단 🔴 #14(완료 해제 불가)·🎨 #15(잠금 툴팁)
- ⑤ GroupRetrospectivePage(회고) — 🔴 #16·🟠 #17·🔴 #18(생애주기 구조 변경 — 주차 점진 생성/종료처리/잠금/알림)
- ⑥ GroupAiPage(AI 팀장) — ✅ 사용자 확인 OK (상세검증 생략)
- ⑧ GroupOnboardingPage(온보딩) — ✅ 사용자 확인 OK (상세검증 생략)
- 셸/공통(WorkspacePage·AppShell) — 일부 #5 관련

---

## 발견 목록

### 🔴 #1 커리큘럼 진행률 불일치 (메인 5% vs 홈 0%) — 상태: ✅ FE 해결(적용)
> FE 적용: `curriculumPct`를 `group.progressPercent` 기반 computed로 변경, `getCurriculum` 재계산·import 제거
> (`GroupOverviewPage.vue`). 프리뷰에서 링 72%(BE값) 표시·에러 없음 확인. (BE SELECT_WEEK_PROGRESS_BY_GROUPS의
> `c.status='ACTIVE'` 필터 미적용 잠재건은 BE 작업으로 분리.)
- **현상**: 같은 그룹인데 GroupsPage 카드 5%, GroupOverviewPage 링 0%.
- **원인**: `GET /groups/{id}` 단건 응답이 BE 계산 `progressPercent`(5%)를 이미 주는데
  (`StudyGroupController.java:197-198`), 홈은 이를 무시하고 `getCurriculum().weeks`로 **재계산**
  (`GroupOverviewPage.vue:289-293`). 두 경로의 집계 대상 커리큘럼 범위가 달라 값이 갈림.
- **부차 원인(잠재)**: BE `SELECT_WEEK_PROGRESS_BY_GROUPS`(CurriculumJdbcSql.java:377)에
  `c.status='ACTIVE'` 필터가 없어, 그룹에 비활성(미삭제) 커리큘럼이 있으면 분모가 부풀 수 있음.
- **권장 수정**: 홈도 `group.progressPercent` 사용 → `curriculumPct = group.value?.progressPercent ?? 0`.
  FE/BE 로직 중복도 함께 제거.

### 🟠 #2 follow 3종 — BE 미구현 — 상태: ✅ BE 해결(적용, 신규 모듈)
> BE: 신규 `follow` 모듈(controller/service/repository/domain) + Flyway **V10 `user_follow`** 테이블.
> `POST /users/{id}/follow`(토글, 자기팔로우 422·없는유저 404), `GET /users/me/following`, `GET /users/me/followers`
> → `{userId,nickname,email,bio,mutual,followedAt}`(mutual=맞팔). ApiExceptionHandler에 예외 매핑 추가.
> FE 응답 타입(FollowUser/FollowToggleResult)과 일치 → FE 변경 불필요. 전체 851 테스트 통과(아키텍처 규칙 포함).

### 🟠 #3·#4 리뷰 질문/수정 — ⛔ 보류(설계 결정 필요) — 상태: 보류
- **FE 호출**: `GET /reviews/questions`, `PATCH /reviews/me` (`reviewsApi.ts`).
- **근본 원인(단순 미구현 아님)**: **FE↔BE 리뷰 모델이 근본적으로 다름.**
  - FE: 설문형 리뷰 `{ answers: RetroAnswers(척도/서술 질문 답변) }`, 질문 목록(`RetroQuestion[]`) 필요.
  - BE: 별점형 리뷰 `{ rating(1~5), content }` (`ReviewController` POST/GET/stats).
  - 즉 questions/PATCH만 추가해도 POST/GET 자체가 페이로드 불일치라 작동 불가.
- **결정 필요**: 리뷰를 (a) 설문형으로 BE 재설계 or (b) FE를 별점형으로 단순화 or (c) 레거시 폐기.
  review 라우트는 사이드바 미연결 레거시라 현재 사용자 영향 없음. **product 결정 전 구현 보류.**

### 🟠 #5 프로필 편집 저장 — BE 미구현 — 상태: ✅ BE 해결(적용)
- **FE 호출**: `PATCH /users/me` (`currentUser.ts:8`, `updateCurrentUser`).
- **BE 적용**: `AuthController`에 `PATCH /users/me`(UpdateUserRequest: nickname+bio) 추가. AuthUser에 `bio` 필드/검증(≤500)
  + `updateProfile`, AuthenticatedUser/UserResponse에 bio 노출, `AuthAccountRepository.updateProfile`(UPDATE_USER_PROFILE),
  FIND SQL에 bio select. (users.bio 컬럼 기존 존재 — 마이그레이션 불필요) 테스트 fake 갱신, compileTestJava 통과.

### ⚪ #6 BE에만 있고 FE 미호출 (참고, 오류 아님)
- `GET /groups/{id}/activity-heatmap`
- `GET /groups/{id}/reviews/stats`
- `POST /groups/{id}/weeks/{weekId}/next-week-plan`

---

## 그룹 홈(GroupOverviewPage) 개선 요청

### 🎨 #7 주간 학습 활동 차트 — FE 전용 (API 변경 없음) — 상태: ✅ 해결(적용)
> FE 적용(`GroupOverviewPage.vue`): (a) 막대 컬럼을 `group`으로, hover 시 막대 위 개수 라벨(`group-hover:opacity-100`).
> (b) `barScale`(=팀 전체 max) 추가해 막대 높이 분모를 팀 max로 고정 → '나' 토글 시 비율만큼 줄어듦. 강조는 현재 뷰 max 유지.
데이터(`barValues`)는 이미 존재. 렌더링만 수정.
- **(a) hover 시 막대 위에 개수 라벨 표시**: 현재 `:title`(브라우저 기본 툴팁)뿐(`GroupOverviewPage.vue:481`). 막대 상단에 hover 숫자 라벨 추가.
- **(b) Y축 max를 팀 전체 기준으로 고정**: 현재 `maxBar`가 뷰(team/me)에 따라 재계산돼(`:131-134`)
  "나"로 바꿔도 막대가 꽉 참. → 분모를 **항상 팀 max로 고정**해 "나"는 팀 대비 비율로 줄어들어 보이게.
  (예: 팀 10이 천장, 내가 4면 4/10 높이) 토글 시 줄어드는 효과도 자연스럽게 확보.

### 🟠 #8 최근 활동에 "완료한 TODO" 표시 — BE API 변경 필요 — 상태: ✅ 해결(적용)
> BE: 신규 `GET /groups/{id}/activity-feed?limit` → `[{memberId, memberNickname, taskTitle, completedAt}]`
> (RecentTaskActivity 도메인 + SELECT_RECENT_TASK_ACTIVITY + service membership 체크). FE: `getRecentActivity` +
> 홈 최근활동을 「OO님이 «과제» 완료」로 표시(피드 미제공 시 기존 방식 폴백). 전체 851 테스트 통과, vue-tsc 통과.
- **현재**: `GET /groups/{id}/learning-activity` 응답은 날짜별 개수만(`MemberActivityRow{ memberId, memberNickname, dailyActivity:[{date,count}] }`).
  과제 이름·어떤 TODO인지 없음 → "최근 활동"이 "OO님이 학습했어요"까지만 표시 가능.
- **원인**: BE `GroupActivityHeatmap`이 DONE todo+게시글을 카운트로만 집계(`CurriculumController.java:268-271`).
- **권장(A안)**: 전용 엔드포인트 `GET /groups/{id}/activity-feed` → 최근 완료 이벤트 N개
  `[{memberId, nickname, taskTitle, completedAt}]`. (최근활동 카드와 1:1, 차트 데이터는 그대로 유지)
- 대안(B안): learning-activity 응답에 완료 task 목록 확장(차트엔 불필요한 데이터까지 실림 — 비추천).

---

## 게시판(GroupBoardPage)

### 🔴 #9 게시글 정렬이 BE에서 무시됨 — 상태: ✅ BE 해결(적용)
> BE 적용: `GroupBoardPostSort` enum(createdAt/commentCount × asc/desc, 고정글 우선) 추가. 컨트롤러에 `sort`/`order`
> RequestParam, 쿼리 레코드·리포지토리에 전달, ORDER BY 동적 생성. keyset 커서는 기본(createdAt desc)에서만,
> 그 외 정렬은 단일 페이지(nextCursor=null). FE는 기존 sort/order 전송 그대로 동작. compileTestJava 통과.
- **현상**: 정렬 드롭다운 3개 중 2개가 먹통(에러 없이 조용히).
  | FE 옵션 | 실제 |
  |---|---|
  | 최신순 (createdAt:desc) | ✅ 우연히 BE 기본값과 일치 |
  | 오래된순 (createdAt:asc) | ❌ 무시 — 여전히 최신순 |
  | 댓글 많은순 (commentCount:desc) | ❌ 무시 — 여전히 최신순 |
- **원인**: FE는 `?sort&order` 전송(`boardApi.ts:37-38,53-54`)하나 BE `listPosts`/`listAllPosts`는
  `cursor`/`pageSize`만 받음(`GroupBoardController.java:101-103,132-134`). 서비스는 "고정글 우선+최신순" 하드코딩.
  Spring이 미지정 쿼리파라미터를 조용히 무시 → no-op.
- **권장 수정**: BE 목록에 `sort`(createdAt|commentCount)·`order`(asc|desc) RequestParam + 정렬 로직 추가.
  (또는 FE에서 동작 안 하는 옵션 제거)
- **부차(버그 아님)**: FE `loadPosts`가 응답 `pageInfo`/`cursor` 미사용 → 첫 20개만 표시(더보기/무한스크롤 없음).

- 엔드포인트 계약: 11종(`boards`, `boards/{b}/posts`, `posts`, `posts/{id}` CRUD, 댓글 CRUD) 모두 BE 매핑 존재 ✅

### 🔴 #10 기존 그룹에 AI 팀장(LEADER_REPORT) 보드 누락 — 상태: ✅ BE 해결(적용)
> BE 적용: `GroupBoardService.listBoards`를 "보드 0개일 때만 삽입" → "누락된 기본 타입만 reconcile 삽입"으로 변경
> (EnumSet 비교, self-heal·멱등). 기존 그룹도 게시판 재진입 시 AI 팀장 탭 자동 출현.
- **현상**: 게시판 탭에서 "AI 팀장(팀장 리포트)" 보드/태그가 안 보임.
- **원인**: `LEADER_REPORT`는 나중에 추가된 보드 타입(`GroupBoardType.java:8`). 그런데 `listBoards`는
  그룹에 보드가 **하나도 없을 때만** 기본 보드를 삽입(`GroupBoardService.java:56-60`) →
  기존 4개 보드(NOTICE/QUESTION/RESOURCE/RETROSPECTIVE)를 이미 가진 **기존 그룹은 백필 안 됨**.
  Flyway에도 백필 마이그레이션 없음(V1~V9 확인).
- **부분 완화**: `WeeklyReportScheduler`가 리포트 게시 시 `findOrCreateBoardId(LEADER_REPORT)`로 on-demand 생성
  (`WeeklyReportScheduler.java:188`) → 첫 주차 리포트가 올라오면 그때 보드 출현. 그 전엔 누락.
- **영향**: 신규 그룹은 정상(5개 전부 생성). 기능 추가 이전의 기존 그룹만 누락.
- **요구 동작(확정)**: `✦ AI 팀장` 탭은 **항상 표시**되어야 함 — 기존 그룹이든, 주차가 한 번도 시작 안 했든,
  리포트가 아직 한 건도 없든 무조건 탭/태그가 보여야 한다. (스크린샷 기준: 전체·공지·질문·자료공유와
  나란히 `✦ AI 팀장` 탭이 항상 노출. 빈 보드여도 탭은 존재.) → on-demand 생성에 의존하면 안 됨.
- **채택 수정**: **(A) `listBoards` reconcile** — "보드 0개일 때만 삽입"을 "누락된 기본 타입(LEADER_REPORT 포함)을
  채워 넣는 reconcile 삽입"으로 변경(self-heal·멱등). 다음 게시판 진입 시 기존 그룹도 탭 자동 출현, 리포트 0건이어도 노출.
  - (대안 B) Flyway로 기존 전체 그룹 LEADER_REPORT 백필 — 1회성. A와 병행 가능하나 A만으로 충분.

### 🎨 #11 상대시간 라벨 실시간 갱신 — FE 전용 — 상태: ✅ 해결(적용)
> FE 적용(`GroupBoardPage.vue`): 반응형 `nowMs=ref(Date.now())` 30초 interval(onUnmounted 정리),
> `formatRelativeDate`가 `nowMs.value` 기준 diff 계산. 게시글·댓글 동시 적용.
- **현상**: 게시글/댓글의 "방금·1분 전·2분 전"이 렌더 시점에 고정 → 시간 지나도 안 바뀜.
- **원인**: `formatRelativeDate`(`GroupBoardPage.vue:657`)가 반응형 상태를 안 읽는 순수 함수 → 리렌더 때만 갱신.
- **수정(FE, 쉬움)**: 반응형 `now = ref(Date.now())`를 `setInterval`(30초)로 갱신, `onUnmounted`에서 정리.
  `formatRelativeDate`가 `now.value` 기준으로 diff 계산하도록 변경 → 게시글·댓글 동시 적용.

---

## 팀원(GroupMyPage)

### 🔴 #12 "이번 주 완료"에 게시글 작성이 포함됨 — 상태: ✅ 해결(적용, #13과 통합)
> BE: `SELECT_GROUP_DONE_ACTIVITY_COUNTS` UNION에 activity_type 추가, todo_count/post_count 분리 집계.
> GroupActivityCount/Heatmap.MemberActivity/DailyActivityResponse에 todoCount·postCount 추가(count=합산 유지).
> FE: 팀원 `thisWeekCount`가 `todoCount`(폴백 count)만 합산 → 게시글 제외. compileJava/vue-tsc 통과.
- **현상**: 팀원 카드의 "이번 주 완료"(`thisWeekCount`, `GroupMyPage.vue:200-206`)가 TODO 완료뿐 아니라
  **게시글 작성까지 합산**해서 올라감. 라벨("완료") 의미와 불일치.
- **원인**: `learning-activity` 일별 카운트 SQL `SELECT_GROUP_DONE_ACTIVITY_COUNTS`(CurriculumJdbcSql.java:391)가
  `task_completion(DONE)` **UNION ALL** `group_board_post(PUBLISHED)`를 합쳐 `count(activity_id)`로 냄.
  홈 "주간 학습 활동"과 팀원 "이번 주 완료"가 **동일한 합산 카운트를 공유**.
- **요구**: 팀원 "이번 주 완료" = **TODO 완료만**. 홈 "주간 학습 활동" = 활동 잔디(합산) 유지 → 둘을 별개로.
- **채택 해법(→ #13과 통합)**: `learning-activity` 응답을 `{date, todoCount, postCount}`로 분리(count는 호환용 유지).
  팀원 "이번 주 완료"는 `todoCount`만 합산. **엔드포인트 분리 불필요 — #13과 한 엔드포인트 공유.**
- 그 외 팀원 페이지 계약/응답필드는 정상(onboarding·ai-manager·members 일치).

### 🎨 #13 홈 주간 학습 활동 — TODO/게시글 누적(stacked) 막대 — 상태: ✅ 해결(적용)
> FE(`GroupOverviewPage.vue`): `barParts`(일자별 {todo,post,total}) 도입, 막대를 TODO(진한 그린)+게시글(연한 그린)
> 누적 렌더, Y축은 팀 total max 고정, hover 라벨 "TODO n · 글 m". BE 카운트 분리는 #12와 공유.
> BE 미배포 구간엔 `todoCount ?? count`/`postCount ?? 0` 폴백으로 graceful.
- **요청**: 홈 "주간 학습 활동" 막대를 TODO 완료 + 게시글 작성으로 **나눠 누적 그래프**로 표시
  (TODO=기존 그린, 게시글=연한/다른 색으로 위에 쌓기).
- **핵심 이점**: 응답에 `todoCount/postCount` 분리만 있으면 **홈(누적 표시) + 팀원(#12, todo만)이 같은 엔드포인트 공유**.
- **BE 변경(작음)**: `SELECT_GROUP_DONE_ACTIVITY_COUNTS`(CurriculumJdbcSql.java:401-418)의 UNION에 type 컬럼 추가 후
  바깥에서 `sum(case when type='todo'…)`, `sum(case when type='post'…)`로 분리 집계.
- **FE 변경**: DailyActivity 타입에 `todoCount/postCount` 추가, 홈 막대를 stacked로 렌더.
- **#12와 동일 작업으로 동시 해결.** (#8의 task 제목은 별개 보강)

## Todo(GroupTodoPage)

### 🔴 #14 완료 해제(toggle) 불가 — 상태: ✅ BE 해결(적용)
> BE(`TaskCompletion`): `update()`의 TODO 차단 제거, `todo(now)`로 완료기록 비우고 TODO 복귀 허용, `canTransitionTo` 전부 허용.
> 완료 카운트는 전부 task_completion 행 status에서 파생되므로 별도 카운터 재계산 불필요(retro required_done·report·learning-activity·FE doneCount 자동 반영).
> FE는 이미 해제 시 TODO 전송 → 그대로 동작. 전체 851 테스트 통과.
- **현상**: 과제를 완료(DONE)하면 체크 해제가 안 됨.
- **원인**: FE 체크박스가 해제 시 `DONE → TODO`를 전송(`GroupTodoPage.vue:181`, `setStatus(id,'TODO')` →
  `POST /tasks/{id}/completion/me {status:'TODO'}`). 그러나 BE `TaskCompletion`은 **TODO 복귀를 설계상 차단**
  (`TaskCompletion.java:78-80` `update()`에서 TODO 예외, `:137-140` `canTransitionTo`가 `!= TODO`).
  DONE↔INCOMPLETE↔SKIPPED 자유 전환은 허용하나 초기 TODO로는 못 돌아감 → 400 거부.
- **수정 방향**:
  - (A·권장, 네 의도) **BE 변경**: 완료 해제(=완료기록 삭제/초기화로 TODO 복귀) 허용 → 깔끔한 DONE↔미체크 토글.
    **부수: `MemberWeekProgress` 완료/미완료 카운트 재계산 필요**(= "TODO 개수 변경"). FE `doneCount`는 reactive라 자동.
  - (B) FE만: uncheck를 `INCOMPLETE`(`markTaskIncomplete`)로 → BE 수정 없이 해제되나 "미완료" 상태로 남음.
- 참고: enum은 FE/BE 모두 `TODO|DONE|INCOMPLETE|SKIPPED` 일치. 완료 엔드포인트 4종 계약 정상.

### 🎨 #15 다음 주차 잠금 툴팁 — 즉시 표시 + 그린 스타일 — FE 전용 — 상태: ✅ 해결(적용)
> FE 적용(`GroupTodoPage.vue`): native `title` 제거, `group/lock` 래퍼 + 그린(`bg-primary`) 커스텀 툴팁
> `group-hover/lock:opacity-100`(duration-100, 지연 없음).
- **현상**: 잠긴 다음 주차 버튼 툴팁이 native `title`(`GroupTodoPage.vue:278`)이라 hover 후 지연 표시.
- **수정(FE)**: 서비스 톤(그린 하이라이트) 커스텀 툴팁으로 교체, hover 즉시 노출.

## 회고(GroupRetrospectivePage)

### 🔴 #16 회고 주차 잠금 오작동 — 미래 주차가 안 잠김 — 상태: ✅ BE 해결(적용)
> BE 적용(`JdbcRetrospectiveRepository.mapWeekOverview`): unlock = `ended || (started && allRequiredDone)`.
> PENDING 미래 주차는 잠금(requiredTotal==0 vacuous-truth 제거), 종료(COMPLETED) 주차는 작성 허용(#18-C 조건2 일부 포함).
- **현상**: 현재 1주차인데 2주차+ 모든 주차가 열려 있음(잠겨야 함).
- **원인**: overview unlock 판정(`JdbcRetrospectiveRepository.java:200`)
  `unlocked = requiredTotal == 0 || requiredDone >= requiredTotal`. `required_total`=그 주차 required 태스크 수
  (`RetrospectiveJdbcSql.SELECT_RETROSPECTIVE_OVERVIEW`). **태스크가 아직 없는 미래 주차(PENDING)는
  required_total=0 → 무조건 unlocked** (vacuous-truth 버그). FE는 `!unlocked && status==='PENDING'`일 때만 잠금
  표시(`GroupRetrospectivePage.vue:198`)라, BE가 unlocked=true를 주면 다 열림.
- **권장 수정**: BE unlock 게이트에 **주차 상태 반영** — 시작 안 한(PENDING) 주차는 잠금,
  또는 `requiredTotal==0`을 unlocked로 보지 않기(시작된 주차 + 모든 required done일 때만 unlock).

### 🟠 #17 회고 질문 미생성 — 기존 그룹은 1주차도 빈 질문 — 상태: ✅ BE 해결(적용, fallback)
> BE(`JdbcRetrospectiveRepository.readQuestions`): retrospective_questions가 null/blank/빈 배열이면 `DEFAULT_QUESTIONS`
> (LIKERT_5 3 + TEXT 1) 반환 → 기존 그룹도 회고 작성 가능. 신규 주차는 AI 질문 사용(#18-A에서 점진 생성).
- **현상**: AI가 커리큘럼 생성 시 회고 질문까지 만들어야 하는데, 이 그룹은 1주차에도 질문이 없음.
- **원인**: `questions`는 `curriculum_week.retrospective_questions`(JSON)에서 읽음. null/blank면 빈 배열
  (`JdbcRetrospectiveRepository.java:211-214`). 회고 질문 컬럼(Flyway V7/V8) 추가 **이전에 생성된 커리큘럼**은
  미채움 → 기존 그룹 누락. #10(LEADER_REPORT)과 같은 데이터 갭. 신규 그룹은 정상.
- **권장 수정**: 기존 ACTIVE 커리큘럼의 빈 주차에 회고 질문 **백필/재생성**(AI 또는 기본 질문셋),
  또는 빈 경우 기본 질문 fallback 제공.

## 🔴 #18 커리큘럼/회고 생애주기 — 구조 변경 (대형) — 상태: 미해결

> 사용자 의도: 커리큘럼은 **주차마다 점진 생성**(다음 주차는 직전 주차 회고들을 입력으로). 현재는 시작 시 전체 주차를
> 한 번에 생성. 이에 맞춰 주차 종료 처리·회고 잠금·알림도 함께 바뀌어야 함. (#16·#17은 A로 상당부분 흡수)

### 현재 로직 (확인 결과)
1. **시작 시 전체 주차 생성**: `CurriculumService.startStudy`(:108)가 `CurriculumSprintPlanner.fixedWeeklyWindows(startsAt,endsAt)`로
   전체 기간을 주차 window로 쪼갬 → AI가 **모든 주차(week1..N) + tasks + 회고질문을 한 번에 생성**
   (`ProviderBackedCurriculumGenerator`, `totalWeeks==expectedWeekCount` 강제), 전부 starts_at/ends_at과 함께 저장.
2. **WeekLifecycleScheduler**(5분, `:78`): 시간 기준 상태 전이만 — `ends_at<=now`→COMPLETED, `starts_at<=now<ends_at`→IN_PROGRESS.
   미완료 확정/TODO 잠금/다음주 생성 **없음**.
3. **WeeklyReportScheduler**(15분, `:135`): `ends_at<=now`(7일 lookback) 주차에 리포트 게시(**즉시, 30분 지연 없음**) →
   직후 `regenerateNextWeekAutomatically`로 **이미 존재하는** 다음 PENDING 주차의 task를 **교체**(`replaceNextWeekTasks`).
4. **RetrospectiveReminderScheduler**(10분, `:56`): IN_PROGRESS 주차 마감 **1시간 전 전 활성멤버**에게 리마인더.
   **제출자 제외 필터 없음**(SQL에 not-exists 회고 없음) → 이미 적은 사람한테도 감.
5. **TaskCompletion.done()**(`:100`): "마감 경과 여부와 무관하게 완료 허용" → **주차 끝나도 TODO 완료 가능**, 미완료 자동확정 없음.
6. **회고 unlock**(#16): `requiredTotal==0 || requiredDone>=requiredTotal` → 미래 주차 다 열림.

### 변경할 점

**A. 커리큘럼 점진 생성 (핵심 구조 변경)**
- 시작 시 **1주차만 생성**(week1 tasks+회고질문). 전체 기간 window 사전 분할 폐기(또는 1주차 window만).
- 주차 종료+리포트 생성 시, 리포트+그룹 회고들을 입력으로 **다음 주차를 신규 CREATE**(현재의 "기존 PENDING 주차 task 교체"가 아니라 insert).
- 영향: `CurriculumService.startStudy`(1주차 생성), `ProviderBackedCurriculumGenerator`(단일 주차), `CurriculumSprintPlanner`,
  `NextWeekPlanService.regenerateNextWeekAutomatically`(`findNextRegenerableWeek`/`replaceNextWeekTasks` → 신규 주차 insert),
  repository(다음 주차 insert). 마지막 주차(그룹 endsAt 도달) 시 생성 중단.
- **부수 해결**: #16(미래주차 안잠김)·#17(전 주차 회고질문 일괄생성)은 미래 주차가 애초에 없으니 자연 해소.

**B. 주차 종료 처리 — 미완료 확정 + TODO 잠금**
- 주차 COMPLETED 전이 시: 그 주차의 DONE 아닌 task → **INCOMPLETE로 확정**(레코드 생성/갱신).
- 종료 후 TODO 완료 **차단**: `completeMyTask`/`TaskCompletion.done()`이 주차 COMPLETED/`ends_at` 경과면 DONE 거부(현재 허용 로직 변경).
- 영향: `WeekLifecycleScheduler.completeEndedWeeks`(미완료 확정 추가), `TaskCompletion`/`CurriculumService.completeMyTask`(마감 후 DONE 차단).

**C. 회고 작성(잠금) 조건 변경**
- 작성 가능 = (1) 그 주차 TODO 모두 완료 **OR** (2) 주차 종료됨 **&** 리포트 아직 미게시.
- 리포트 게시 후 닫힘(제출=readonly / 미제출=마감). + #16 unlock 로직(`requiredTotal==0` vacuous-true) 교정.
- 영향: `JdbcRetrospectiveRepository.mapWeekOverview`+overview SQL(unlock 산정에 주차 상태·리포트 존재 반영),
  `RetrospectiveService`(제출 가드, `:140`).

**D. 알림**
- ✅ **적용**: 기존 1시간 전 리마인더에 **미제출자만** 필터 추가(`SELECT_DUE_SOON_REMINDERS`에
  `not exists(retrospective for week+member)`). 전체 851 테스트 통과.
- ⏳ 남음: **리포트 생성 10분 전** 리마인더(미제출자만, `ends_at+20분`) + 리포트 타이밍 `ends_at+30분` 지연
  (`WeeklyReportScheduler` 쿼리 `ends_at <= now - 30m`) + `NotificationCommandFactory` 신규 알림타입.

### ⏳ #18 진행 상태 (부분 적용)
- ✅ **D-필터** 적용(미제출자만 1시간 리마인더). **#16**(회고 잠금)에서 C 조건2 일부 선반영.
- ⏳ **A/B/C/D-나머지 보류** — 사유: A(커리큘럼 점진 생성)는 AI 생성 프롬프트·스터디 시작 검증
  (`requireGenerationMatchesSprintWindows`)·`NextWeekPlanService`(교체→insert)·스케줄러 타이밍을 동시에 바꾸는
  **대형 재설계**라 별도 설계+테스트 사이클 필요(근-프로덕션 코드에 일괄 투입 시 스터디 생성/전이 깨질 위험).
  단계 권장: ① A 단독 브랜치(단일주 생성→리포트시 다음주 insert) + 통합테스트 → ② B(종료 처리/잠금) → ③ C(리포트 후 닫기) → ④ D-나머지.

## 알림(Notification)

### 🔴 #19 온보딩 제출 알림이 전 멤버에게 발송됨 — 방장 전용이어야 함 — 상태: ✅ BE 해결(적용)
> BE 적용: `OnboardingService.notifyOwnerOfSubmission`이 `findOwnerUserId(groupId, submitterMemberId)`(OWNER 1명, 제출자=방장이면 빈값)로만 발송.
> `SELECT_OWNER_USER_ID_EXCLUDING_MEMBER` 추가. 테스트 fake 갱신.
- **현상**: 멤버가 온보딩을 제출하면 "팀원이 온보딩을 했어요" 알림이 **다른 멤버들에게도** 뜸. 방장만 받아야 함.
- **원인**: `OnboardingService.notifyOtherMembersOfSubmission`(`:82-90`)이 `findOtherMemberUserIds`로
  **제출자 뺀 전 멤버**에게 발송. SQL `SELECT_OTHER_MEMBER_USER_IDS`(`OnboardingJdbcSql.java:115`)에
  권한(OWNER) 필터 없음 — PENDING_ONBOARDING/ACTIVE 전원.
- **권장 수정**: 수신자를 **방장(`permission='OWNER'`)만**으로 변경(기존 `SELECT_OWNER_USER_ID...` 패턴 재사용).
  제출자==방장이면 스킵. 메서드명도 `notifyOwnerOfSubmission`류로.

## 사이드바(AppShell rail)

### ✅ #20 현재 탭에 hover 시 연한 초록으로 덮임 — FE 전용 — 상태: **해결**
- **현상**: 활성 탭(진한 초록 `bg rgba(25,195,125,0.22)`)에 hover 시 base의 `hover:bg-[rgba(25,195,125,0.11)]`(연한)이
  `:hover` 명시도로 덮어써 활성 강조가 사라짐. (`AppShell.vue:591-595` 채널 RouterLink `exact-active-class`)
- **수정(적용)**: `exact-active-class`의 배경에 `!`(important) 부여 →
  `!bg-[rgba(25,195,125,0.22)]`, onboard는 `!bg-[var(--color-hover)]`. important가 비-important hover를 항상 이겨 유지.
- **검증**: 프리뷰 raw CSS 확인 — 활성 규칙 `background-color: …0.22 !important`, hover 규칙 `…0.11`(non-important).
  활성 Todo 탭 진한 초록 유지, 콘솔 에러 없음.

### ✅ #21 새 그룹 팝오버로 마우스 옮기면 레일 드로어가 닫힘 — FE 전용 — 상태: **해결**
- **현상**: "새 그룹 만들기"의 팝오버(방 생성/코드 참여)는 레일 바깥 fixed로 뜨는데, 거기로 마우스를 옮기면
  레일 `@mouseleave="closeRail"`가 발동해 드로어가 닫혀버림.
- **수정(적용)**: `closeRail`에 가드 추가 — `if (showCreateMenu.value) return`. 팝오버가 열려 있는 동안 레일 유지.
  (`AppShell.vue:52`) 이미 클릭 토글이라 "토글/계속 열림" 둘 다 충족.
- **검증**: 프리뷰에서 hover→추가클릭→레일 mouseleave(220ms) 후 팝오버·레일 라벨 모두 유지(168px) 확인.
  스크린샷에 드로어+팝오버 동시 노출. 회귀(팝오버 닫힘 시 정상 닫힘)는 early-return 가드라 코드상 보장. 콘솔 에러 없음.

### 🎨 #22 레일 접힘 상태 아이콘 아래 이름 라벨이 살짝 잘림 — FE 전용 — 상태: ✅ 해결(적용)
> FE 적용(`AppShell.vue`): 라벨 5곳 `leading-none`→`leading-[1.3]`으로 세로 여유 확보(한글 위/아래 클립 해소).
- **현상**: 사이드바 접힘(레일) 상태에서 아이콘 아래 작은 이름 라벨이 살짝 잘려 보임.
- **원인(추정)**: 라벨 `class="max-w-[60px] truncate text-[10px] font-medium leading-none"`(`AppShell.vue:432-436`).
  - 세로(유력): `leading-none`(line-height 1) + `truncate`의 overflow-hidden이 10px 한글 글자 위/아래를 깎음.
  - 가로: `max-w-[60px]`(컨테이너 `w-[72px]`)라 긴 이름은 ellipsis로 잘림.
- **수정 방향(FE, 미적용)**: line-height 살짝 키우기(`leading-none`→`leading-[1.15]`/`leading-tight`)로 세로 클립 해소,
  필요 시 `max-w`를 64~66px로 약간 확대하거나 라벨 높이에 여유 패딩. (해당 패턴은 Home/그룹/추가 라벨 다수에 동일 적용 필요)

## 아직 안 본 페이지 (다음 검증 대상)
④ GroupTodoPage · ⑤ GroupRetrospectivePage · ⑥ GroupAiPage · ⑧ GroupOnboardingPage ·
GroupCurriculumPage · GroupJoinPage · BookmarksPage · ⑩ 레일/알림/룰 관련.
→ 호출 경로는 BE에 존재 확인됨(정적 diff). 값 계산/의미 단위 버그는 페이지별로 더 봐야 함.

---

## 요약 (총 22건 · ✅해결 19 · ⛔보류 2 · ⏳부분 1)
| # | 분류 | 한줄 | 상태 |
|---|---|---|---|
| 1 | 🔴 버그 | 진행률 메인5%/홈0% | ✅ FE 적용 |
| 2 | 🟠 갭 | follow 3종 미구현 | ✅ BE 적용(신규 모듈+V10) |
| 3 | 🟠 갭 | reviews/questions 미구현 | ⛔ 보류(FE/BE 모델 상이, product 결정) |
| 4 | 🟠 갭 | PATCH reviews/me 미구현 | ⛔ 보류(#3과 동일) |
| 5 | 🟠 갭 | PATCH users/me(프로필저장) 미구현 | ✅ BE 적용 |
| 6 | ⚪ 참고 | BE에만 있고 FE 미호출 3개 | — (오류 아님) |
| 7 | 🎨 개선 | 홈 차트 hover라벨 + Y축 팀max 고정 | ✅ FE 적용 |
| 8 | 🟠 개선 | 최근활동에 완료 TODO 제목 | ✅ BE+FE 적용 |
| 9 | 🔴 버그 | 게시판 정렬 2/3 먹통 | ✅ BE 적용 |
| 10 | 🔴 버그 | 기존그룹 AI팀장 보드 누락 | ✅ BE 적용(reconcile) |
| 11 | 🎨 개선 | 상대시간 라벨 실시간 갱신 | ✅ FE 적용 |
| 12 | 🔴 버그 | 팀원 "이번주 완료"에 게시글 포함 | ✅ BE+FE 적용 |
| 13 | 🎨 개선 | 홈 주간활동 TODO/게시글 누적막대 | ✅ BE+FE 적용 |
| 14 | 🔴 버그 | Todo 완료 해제 불가 | ✅ BE 적용 |
| 15 | 🎨 개선 | 다음주차 잠금 툴팁 즉시표시·그린 | ✅ FE 적용 |
| 16 | 🔴 버그 | 회고 미래 주차가 안 잠김 | ✅ BE 적용 |
| 17 | 🟠 갭 | 회고 질문 미생성(기존 그룹) | ✅ BE 적용(기본질문 fallback) |
| 18 | 🔴 대형 | 커리큘럼 점진 생성/종료처리/잠금/알림 개편 | ⏳ 부분(D-필터 적용, A/B/C/D-나머지 보류) |
| 19 | 🔴 버그 | 온보딩 알림 전 멤버 발송(방장 전용) | ✅ BE 적용 |
| 20 | ✅ 해결 | 사이드바 활성탭 hover 덮임 | ✅ FE 적용·검증 |
| 21 | ✅ 해결 | 새 그룹 팝오버 시 레일 닫힘 | ✅ FE 적용·검증 |
| 22 | 🎨 개선 | 레일 접힘 이름 라벨 잘림 | ✅ FE 적용 |

**검증**: BE 전체 851 테스트 통과 + FE `vue-tsc` 통과. FE 변경은 프리뷰 콘솔 에러 없음.
**미배포 graceful**: #8·#12·#13의 FE는 BE 신규 응답이 배포되기 전엔 폴백(기존 count/방식)으로 동작.
**남은 작업**: #3·#4(설계 결정), #18 A/B/C/D-나머지(대형 재설계).

**묶음 작업**:
- #12+#13 = `learning-activity` 응답에 `todoCount/postCount` 분리(한 엔드포인트 공유). #8 = task 제목 별도 보강.
- #16+#17 = #18-A(주차 점진 생성)로 대부분 흡수. #18은 A~D 4파트 구조 변경.
- 데이터 갭 3종(#10 보드, #17 회고질문, #1 비활성커리큘럼) = 기존 그룹 백필 마이그레이션으로 묶음 가능.
