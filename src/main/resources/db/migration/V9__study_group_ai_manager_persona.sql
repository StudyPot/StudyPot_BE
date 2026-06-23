-- AI 팀장(AI Manager) 퍼소나: 그룹별 1개. study_group 에 컬럼으로 보관한다.
alter table study_group
  add column ai_persona text null,
  add column ai_persona_updated_by binary(16) null,
  add column ai_persona_updated_at timestamp(6) null;

-- 감사 메타데이터이므로 사용자 삭제를 막지 않고 참조만 비운다(ON DELETE SET NULL).
alter table study_group
  add constraint study_group_ai_persona_updated_by_fk
    foreign key (ai_persona_updated_by) references users (id)
    on delete set null;
