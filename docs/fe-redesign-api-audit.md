# FE 리디자인 — 화면별 API 전수 조사 & 백엔드 갭 분석

기준: FE `apiBaseUrl = /api/v1`. BE 컨트롤러 12개 매핑과 대조.
범례: ✅ 존재 · 🆕 신규 필요 · ♻️ 수정/보강 필요 · 💤 레거시(사이드바 미연결)

## 화면별 API

### ① 전체 그룹 (GroupsPage)
| 메서드·경로 | FE 함수 | 상태 |
|---|---|---|
| GET `/groups?q,status,sort,order` | listGroups | ✅ |
| GET `/groups/summary` | getGroupSummary | 🆕 (그룹 수·이번주 활동 수 부제목) |
| GET `/bookmarks` · POST `/groups/{id}/bookmark` | listBookmarks/toggleBookmark | ✅ |
| (카드) StudyGroup.memberCount, progressPercent | — | ♻️ 응답 보강 |

### ② 그룹 홈 (GroupOverviewPage)
| GET `/groups/{id}` | getGroup | ✅ |
| GET `/groups/{id}/members` | listGroupMembers | ✅ |
| GET `/groups/{id}/learning-activity` | getGroupMembersActivity | ✅ |
| GET `/groups/{id}/curriculum` | getCurriculum | ✅ |
| POST `/groups/{id}/start` | startStudy | ✅ |
| DELETE/PATCH `/groups/{id}` | delete/updateGroup | ✅ |

### ③ 팀원 (GroupMyPage)
| GET `/groups/{id}/onboarding` | getGroupOnboardings | ✅ |
| GET `/groups/{id}/members` | listGroupMembers | ✅ |
| GET `/groups/{id}/learning-activity` | getGroupMembersActivity | ✅ |
| GET `/groups/{id}/ai-manager` | getAiManager | 🆕 |
| PATCH `/groups/{id}/ai-manager` | updateAiManager | 🆕 |

### ④ Todo (GroupTodoPage)
| GET `/groups/{id}/curriculum` | getCurriculum | ✅ |
| GET `/groups/{id}/weeks/current` | getCurrentWeek | ✅ |
| GET `/weeks/{weekId}` | getWeek | 🆕 (주차 네비게이션) |
| GET `/weeks/{weekId}/tasks` | listWeeklyTasks | ✅ |
| GET/PUT `/weeks/{weekId}/progress/me` | get/updateMyWeekProgress | ✅ |
| POST `/tasks/{taskId}/completion/me[/done|/skip|/incomplete]` | complete/skip/... | ✅ |

### ⑤ 회고 (GroupRetrospectivePage)
| GET `/groups/{id}/retrospectives/overview` | getRetrospectiveOverview | ✅ |
| GET·POST `/weeks/{weekId}/retrospectives/me` | get/submitRetrospective | ✅ |
| GET `/groups/{id}/retrospectives/me` | listMyRetrospectives | ✅ |
| GET `/weeks/{weekId}/tasks` | (잠김 진행률) | ✅ |

### ⑥ AI 팀장 (GroupAiPage)
| POST `/groups/{id}/ai-conversations` | openAiConversation | ✅ |
| POST·GET `/ai-conversations/{id}/messages` | send/list | ✅ |
| GET `/ai-conversations/{id}/stream` (SSE) | subscribe | ✅ |

### ⑦ 게시판 (GroupBoardPage)
| GET `/groups/{id}/boards` · `/posts` · `/boards/{b}/posts` | list* | ✅ |
| GET·PATCH·DELETE `/groups/{id}/posts/{postId}` | get/update/delete | ✅ |
| POST `/groups/{id}/boards/{b}/posts` | create | ✅ |
| 댓글 CRUD `/posts/{p}/comments`, `/comments/{c}` | * | ✅ |

### ⑧ 그룹 생성 (GroupCreatePage)
| POST `/groups` | createGroup | ✅ |
| POST `/groups/detail-keyword-suggestions` | suggestDetailKeywords | ✅ |

### ⑨ 온보딩 (GroupOnboardingPage)
| GET·POST `/groups/{id}/onboarding/me` | get/submitMyOnboarding | ✅ |

### ⑩ 레일/알림/공통 (AppShell, NotificationBell)
| POST `/groups/join` · `/groups/{id}/join` | joinGroupByInviteCode/join | ✅ |
| GET `/users/me` | getCurrentUser | ✅ |
| GET `/users/me/notifications` (+stream) | list/SSE | ✅ |
| POST `/notifications/{id}/read` · `/users/me/notifications/read-all` | mark | ✅ |
| GET `/groups/{id}/notifications` | listGroupNotifications | ✅ |

### ⑪ 비핵심/레거시
| GET `/groups/{id}/reviews/questions`, PATCH `/reviews/me` | reviewsApi | 🆕(💤 group-review는 사이드바 미연결 레거시) |
| PATCH `/users/me` | updateCurrentUser | 🆕 (프로필 편집) |
| POST `/users/{id}/follow`, GET `/users/me/following|followers` | follow | 🆕 (팔로우 페이지) |
| GET `/groups/{id}/llm-usage` | listGroupLlmUsage | ✅ |
| 규칙 `/rules*`, `/rule-violations*` | rule | ✅ |

## 백엔드 작업 정리 & 진행 상태

### ✅ 구현·배포 완료 (develop, 5종 전부 완료)
1. ✅ **GET `/weeks/{weekId}`** — 주차 단건 상세. Todo 주차 네비게이션의 비현재 주차 404 해소.
   PR [#263](https://github.com/StudyPot/StudyPot_BE/pull/263), SPT-157, develop 51440f9.
2. ✅ **StudyGroupResponse += `memberCount`** — 그룹 카드 '멤버 N/M'. 목록은 IN 배치 카운트(N+1 제거).
   PR [#265](https://github.com/StudyPot/StudyPot_BE/pull/265), SPT-158, develop 28b4642.
3. ✅ **GET·PATCH `/groups/{groupId}/ai-manager`** → `{ groupId, persona, updatedAt, updatedByNickname }`.
   study_group 컬럼(ai_persona/updated_by FK ON DELETE SET NULL/updated_at, Flyway V9). owner만 PATCH, 멤버 읽기.
   PR [#267](https://github.com/StudyPot/StudyPot_BE/pull/267), SPT-159, develop e4d6107. FE: 팀원 AI 팀장 카드.
4. ✅ **GET `/groups/summary`** → `{ groupCount, weeklyActivityCount }`.
   groupCount=내 그룹 수, weeklyActivityCount=내 DONE 완료수(최근7일/rolling 168h, UTC clock). 전용 GroupSummaryController(스터디+커리큘럼 집계).
   PR [#269](https://github.com/StudyPot/StudyPot_BE/pull/269), SPT-160, develop a0237c0. FE: GroupsPage 부제목.
5. ✅ **StudyGroupResponse += `progressPercent`** — 그룹 카드 진행바.
   커리큘럼 주차 진행도 (완료주차+진행주차·0.5)/전체주차 ×100 (0~100 clamp, 미생성=null→FE 상태 기반 fallback). IN 배치.
   PR [#271](https://github.com/StudyPot/StudyPot_BE/pull/271), SPT-161, develop bf7466d. FE: GroupsPage 진행바.

### 후속(비핵심/레거시)
- PATCH `/users/me`(프로필 편집), follow 3종(`/users/{id}/follow`, `/users/me/following|followers`), reviews `/reviews/questions`·PATCH `/reviews/me`(group-review는 사이드바 미연결 레거시).

## 비고
- FE는 미존재 API를 모두 graceful 처리(옵셔널/try-catch)하므로 현재도 화면은 깨지지 않음. 위 보강은 "실데이터" 표시를 위한 것.
- 각 BE 작업은 codex 하니스(PR→CodeRabbit→review-gate→배포)로 prod 무중단 단계 적용.
