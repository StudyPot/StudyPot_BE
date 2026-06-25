-- 완료한 스터디의 '다음 스터디 추천'을 그룹별로 1회 생성 후 캐시한다.
-- 이후 같은 그룹의 추천 요청은 LLM/집계를 다시 돌리지 않고 저장된 값을 그대로 반환한다(first-write-wins).
create table study_recommendation (
  group_id binary(16) not null,
  ai_suggestions json not null,
  popular_topics json not null,
  created_at timestamp(6) not null default current_timestamp(6),
  primary key (group_id),
  constraint study_recommendation_group_fk
    foreign key (group_id) references study_group (id)
    on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
