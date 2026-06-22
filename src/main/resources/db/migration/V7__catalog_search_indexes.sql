alter table study_group_catalog
  add key study_group_catalog_topic_idx (topic),
  add key study_group_catalog_search_cursor_idx (deleted_at, status, favorite, starts_at, id);
