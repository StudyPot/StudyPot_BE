# 04 ERD / 데이터 모델

## Entity Set
- Identity/Auth: `users`, `oauth_account`, `refresh_token`
- Group/Onboarding/Rules: `study_group`, `group_member`, `group_onboarding_response`, `member_availability_slot`, `group_rule`, `rule_violation`
- Curriculum/Todo: `curriculum`, `curriculum_week`, `weekly_task`, `member_week_progress`, `task_completion`
- AI/Retrospective: `retrospective`, `ai_conversation`, `ai_conversation_message`, `llm_usage`
- Operations: `notification`

## Mermaid
```mermaid
erDiagram
    users {
        binary id PK
        varchar email
        varchar nickname
    }
    oauth_account {
        binary id PK
        binary user_id FK
        varchar provider
    }
    refresh_token {
        binary id PK
        binary user_id FK
        varchar token_hash
    }
    study_group {
        binary id PK
        binary created_by FK
        varchar topic
        json detail_keywords
        varchar status
        varchar invite_code
    }
    group_member {
        binary id PK
        binary group_id FK
        binary user_id FK
        varchar permission
        varchar status
    }
    group_onboarding_response {
        binary id PK
        binary group_id FK
        binary member_id FK
        json keyword_skill_levels
        json task_preferences
    }
    member_availability_slot {
        binary id PK
        binary onboarding_response_id FK
        binary member_id FK
        tinyint day_of_week
    }
    group_rule {
        binary id PK
        binary group_id FK
        varchar rule_type
    }
    rule_violation {
        binary id PK
        binary rule_id FK
        binary member_id FK
        binary task_completion_id FK
    }
    curriculum {
        binary id PK
        binary group_id FK
        json onboarding_summary
    }
    curriculum_week {
        binary id PK
        binary curriculum_id FK
        int week_number
    }
    weekly_task {
        binary id PK
        binary curriculum_week_id FK
        varchar task_type
    }
    member_week_progress {
        binary id PK
        binary curriculum_week_id FK
        binary member_id FK
        varchar status
    }
    task_completion {
        binary id PK
        binary progress_id FK
        binary weekly_task_id FK
        binary member_id FK
        varchar status
    }
    retrospective {
        binary id PK
        binary progress_id FK
        binary curriculum_week_id FK
        binary member_id FK
        json ai_feedback
    }
    ai_conversation {
        binary id PK
        binary group_id FK
        binary member_id FK
        binary retrospective_id FK
    }
    ai_conversation_message {
        binary id PK
        binary conversation_id FK
        binary llm_usage_id FK
    }
    notification {
        binary id PK
        binary group_id FK
        binary recipient_user_id FK
        binary related_onboarding_response_id FK
        binary related_week_id FK
        binary related_task_completion_id FK
        binary related_retrospective_id FK
        varchar channel
        varchar status
        timestamp read_at
    }
    llm_usage {
        binary id PK
        binary user_id FK
        binary group_id FK
        varchar purpose
    }

    users ||--o{ oauth_account : has
    users ||--o{ refresh_token : owns
    users ||--o{ study_group : creates
    users ||--o{ group_member : joins
    users ||--o{ notification : receives
    study_group ||--o{ group_member : includes
    study_group ||--o{ group_onboarding_response : collects
    group_member ||--o| group_onboarding_response : submits
    group_onboarding_response ||--o{ member_availability_slot : contains
    group_member ||--o{ member_availability_slot : has
    study_group ||--o{ group_rule : defines
    group_rule ||--o{ rule_violation : records
    group_member ||--o{ rule_violation : gets
    study_group ||--o| curriculum : owns
    curriculum ||--o{ curriculum_week : has
    curriculum_week ||--o{ weekly_task : contains
    curriculum_week ||--o{ member_week_progress : tracks
    group_member ||--o{ member_week_progress : works
    member_week_progress ||--o{ task_completion : has
    weekly_task ||--o{ task_completion : assigned
    member_week_progress ||--o{ retrospective : triggers
    retrospective ||--o{ ai_conversation : opens
    ai_conversation ||--o{ ai_conversation_message : contains
    llm_usage ||--o{ curriculum : generates
    llm_usage ||--o{ retrospective : analyzes
    llm_usage ||--o{ ai_conversation_message : backs
    study_group ||--o{ notification : sends
```

## MySQL8 Baseline
- UUIDv7 as `BINARY(16)`.
- Structured context as `JSON`.
- Timestamps as `TIMESTAMP(6)`.
- Schema draft: `docs/specs/db-schema-v1.sql`.
- Discord integration and external delivery channels are not MVP entities.
