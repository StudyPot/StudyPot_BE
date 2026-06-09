alter table group_board_comment
  add column parent_comment_id binary(16) null after post_id,
  add key group_board_comment_parent_idx (parent_comment_id, created_at),
  add constraint group_board_comment_parent_fk foreign key (parent_comment_id) references group_board_comment (id);
