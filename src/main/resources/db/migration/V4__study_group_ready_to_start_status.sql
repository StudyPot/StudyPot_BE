alter table study_group
  drop check study_group_status_check;

alter table study_group
  add constraint study_group_status_check
  check (status in ('DRAFT','ONBOARDING','READY_TO_START','ACTIVE','COMPLETED','ARCHIVED'));
