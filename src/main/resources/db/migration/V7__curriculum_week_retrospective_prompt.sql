-- 커리큘럼 생성 시 주차별 TODO를 바탕으로 만든 회고 프롬프트(질문/가이드)를 저장한다.
-- nullable: AI가 생성하지 못했거나 과거 데이터에는 비어 있을 수 있다.
alter table curriculum_week
  add column retrospective_prompt text null after sprint_goal;
