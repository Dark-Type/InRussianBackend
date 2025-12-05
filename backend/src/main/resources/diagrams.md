# Domain model and ER diagram

Assumptions
- You already have Course, Section, Theme, Task, Badge tables.
- PostgreSQL is used (recommendation for JSONB, partial indexes, materialized views, and triggers).
- Users can take sections in any order; themes are internal grouping.
- Tasks belong to a theme; themes belong to a section; sections belong to a course.

Goals
- Track per-user task attempts (tries, time spent, solved).
- Maintain incremental stats for sections and courses (percentage, averages).
- Award badges on events (section completion, streaks, speed, etc.).
- Support a per-user task queue with retry (re-enqueue failed tasks later), optionally batching by theme.

Entity additions

- user_course_enrollment: user starts or follows a course.
- user_task_attempt: immutable facts of attempts (idempotent).
- user_task_state: current state for a user-task (progress, next due time for queueing).
- user_section_progress: denormalized, per-user per-section stats.
- user_course_progress: denormalized, per-user per-course stats.
- badge_rule: declarative rules; join with user_badge on award.
- user_badge: awards.
- user_task_queue: explicit queue entries (optional if you use next_due_at in user_task_state as an implicit queue).
- idempotency_key: optional cross-request dedupe (you can also use attempt_id for this).

ER diagram (Mermaid)

```mermaid
erDiagram
    USER ||--o{ USER_COURSE_ENROLLMENT : enrolls
    COURSE ||--o{ SECTION : contains
    SECTION ||--o{ THEME : groups
    THEME ||--o{ TASK : has

    USER ||--o{ USER_TASK_ATTEMPT : makes
    TASK ||--o{ USER_TASK_ATTEMPT : attempted

    USER ||--o{ USER_TASK_STATE : owns
    TASK ||--o{ USER_TASK_STATE : state

    USER ||--o{ USER_SECTION_PROGRESS : tracks
    SECTION ||--o{ USER_SECTION_PROGRESS : tracked

    USER ||--o{ USER_COURSE_PROGRESS : tracks
    COURSE ||--o{ USER_COURSE_PROGRESS : tracked

    BADGE ||--o{ BADGE_RULE : definedBy
    USER ||--o{ USER_BADGE : earns
    BADGE ||--o{ USER_BADGE : award

    USER ||--o{ USER_TASK_QUEUE : queued
    TASK ||--o{ USER_TASK_QUEUE : entry

    USER {
      uuid id PK
      text handle
      timestamptz created_at
    }

    COURSE {
      uuid id PK
      text title
      int total_tasks_cached
    }

    SECTION {
      uuid id PK
      uuid course_id FK
      text title
      int total_tasks_cached
      int order_index NULL  // For display only
    }

    THEME {
      uuid id PK
      uuid section_id FK
      text title
      int order_index NULL
    }

    TASK {
      uuid id PK
      uuid theme_id FK
      text type
      jsonb content
      bool active
      int difficulty NULL
    }

    USER_COURSE_ENROLLMENT {
      uuid id PK
      uuid user_id FK
      uuid course_id FK
      text status // active, paused, completed
      timestamptz started_at
      timestamptz completed_at NULL
      unique (user_id, course_id)
    }

    USER_TASK_ATTEMPT {
      uuid id PK // client-provided idempotent id or server-generated + idempotency_key
      uuid user_id FK
      uuid task_id FK
      uuid theme_id FK
      uuid section_id FK
      uuid course_id FK
      int tries
      int time_spent_ms
      bool solved
      jsonb payload NULL
      timestamptz created_at
      text source // mobile, web
    }

    USER_TASK_STATE {
      uuid user_id FK
      uuid task_id FK
      uuid theme_id FK
      uuid section_id FK
      uuid course_id FK
      text status // new, in_progress, solved
      int success_count
      int failure_count
      int total_tries
      bigint total_time_ms
      int average_time_ms
      timestamptz last_attempt_at NULL
      timestamptz next_due_at NULL // used for queue scheduling
      int queue_priority DEFAULT 0
      primary key (user_id, task_id)
    }

    USER_SECTION_PROGRESS {
      uuid user_id FK
      uuid section_id FK
      uuid course_id FK
      int solved_tasks
      int total_tasks
      numeric percent_complete // solved/total * 100
      bigint total_time_ms
      int average_time_ms
      timestamptz updated_at
      primary key (user_id, section_id)
    }

    USER_COURSE_PROGRESS {
      uuid user_id FK
      uuid course_id FK
      int solved_tasks
      int total_tasks
      numeric percent_complete
      bigint total_time_ms
      int average_time_ms
      timestamptz updated_at
      primary key (user_id, course_id)
    }

    BADGE {
      uuid id PK
      text code unique
      text name
      text description
      jsonb media
      bool active
    }

    BADGE_RULE {
      uuid id PK
      uuid badge_id FK
      text type // section_completed, streak, speed, daily, etc.
      jsonb criteria // e.g., {"sectionId": "..."} or thresholds
      bool active
    }

    USER_BADGE {
      uuid id PK
      uuid user_id FK
      uuid badge_id FK
      jsonb context // {"sectionId": "...", "courseId": "..."}
      timestamptz awarded_at
      unique (user_id, badge_id, coalesce((context->>'sectionId'), ''))
    }

    USER_TASK_QUEUE {
      uuid id PK
      uuid user_id FK
      uuid task_id FK
      uuid section_id FK
      uuid course_id FK
      timestamptz due_at
      int priority
      text state // queued, reserved, done, expired
      timestamptz enqueued_at
      timestamptz dequeued_at NULL
      timestamptz visibility_timeout_at NULL
      unique (user_id, task_id, state) // to avoid duplicates
    }
```

Notes
- total_tasks_cached on Section/Course can be maintained by triggers when tasks are added/removed, making percent computation O(1).
- Either keep an explicit queue table (USER_TASK_QUEUE) or use implicit scheduling with USER_TASK_STATE.next_due_at and priority.
- user_task_attempt is append-only. All aggregates derive from it and/or incrementally update denormalized tables.