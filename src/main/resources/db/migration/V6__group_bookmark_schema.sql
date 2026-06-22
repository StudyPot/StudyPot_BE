create table group_bookmark (
  id binary(16) not null,
  user_id binary(16) not null,
  group_id binary(16) not null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  primary key (id),
  unique key group_bookmark_user_group_uidx (user_id, group_id),
  key group_bookmark_user_idx (user_id),
  constraint group_bookmark_user_fk foreign key (user_id) references users (id),
  constraint group_bookmark_group_fk foreign key (group_id) references study_group (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
