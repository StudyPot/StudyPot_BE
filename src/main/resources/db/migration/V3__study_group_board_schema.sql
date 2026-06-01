-- SPT-123 Study group board/post/comment schema.
-- Default board rows are created lazily by the application for each group.

create table group_board (
  id binary(16) not null,
  group_id binary(16) not null,
  board_type varchar(40) not null,
  name varchar(80) not null,
  description text null,
  display_order int not null,
  is_default tinyint(1) not null default 1,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  group_board_type_live_key varchar(120) generated always as (case when deleted_at is null then concat(hex(group_id), ':', board_type) else null end) stored,
  primary key (id),
  unique key group_board_group_type_live_uidx (group_board_type_live_key),
  unique key group_board_id_group_uidx (id, group_id),
  key group_board_group_order_idx (group_id, display_order),
  constraint group_board_group_fk foreign key (group_id) references study_group (id),
  constraint group_board_type_check check (board_type in ('NOTICE','QUESTION','RESOURCE','RETROSPECTIVE')),
  constraint group_board_display_order_check check (display_order > 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table group_board_post (
  id binary(16) not null,
  group_id binary(16) not null,
  board_id binary(16) not null,
  author_member_id binary(16) not null,
  title varchar(200) not null,
  content text not null,
  is_pinned tinyint(1) not null default 0,
  status varchar(40) not null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  primary key (id),
  unique key group_board_post_id_group_uidx (id, group_id),
  key group_board_post_board_cursor_idx (board_id, is_pinned, created_at, id),
  key group_board_post_group_status_idx (group_id, status, created_at),
  key group_board_post_author_idx (author_member_id, created_at),
  constraint group_board_post_board_group_fk foreign key (board_id, group_id) references group_board (id, group_id),
  constraint group_board_post_author_fk foreign key (author_member_id) references group_member (id),
  constraint group_board_post_title_check check (char_length(title) between 1 and 200),
  constraint group_board_post_content_check check (char_length(content) between 1 and 10000),
  constraint group_board_post_status_check check (status in ('PUBLISHED','DELETED'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table group_board_comment (
  id binary(16) not null,
  group_id binary(16) not null,
  post_id binary(16) not null,
  author_member_id binary(16) not null,
  content text not null,
  status varchar(40) not null,
  created_at timestamp(6) not null default current_timestamp(6),
  updated_at timestamp(6) not null default current_timestamp(6) on update current_timestamp(6),
  deleted_at timestamp(6) null,
  primary key (id),
  key group_board_comment_post_cursor_idx (post_id, created_at, id),
  key group_board_comment_group_status_idx (group_id, status, created_at),
  key group_board_comment_author_idx (author_member_id, created_at),
  constraint group_board_comment_post_group_fk foreign key (post_id, group_id) references group_board_post (id, group_id),
  constraint group_board_comment_author_fk foreign key (author_member_id) references group_member (id),
  constraint group_board_comment_content_check check (char_length(content) between 1 and 3000),
  constraint group_board_comment_status_check check (status in ('PUBLISHED','DELETED'))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;
