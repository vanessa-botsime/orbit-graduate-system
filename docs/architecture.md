# Architecture

## System overview

```mermaid
flowchart LR
    user([User]) --> web[React frontend]
    web -->|REST + JWT| api[Spring Boot API]
    api --> db[(PostgreSQL)]
    api -->|publish events| kafka{{Kafka topic<br/>orbit.application-events}}
    kafka --> audit[Audit consumer]
    audit --> db
    kafka -.-> pipeline[Data pipeline<br/>bronze / silver / gold]
    kafka -.-> sentinel[orbit-sentinel<br/>security monitor]
    api -.-> gmail[Gmail API]
    api -.-> cal[Google Calendar API]
```

Solid lines exist in the repo. Dotted lines are planned (see `TASK_BOARD.md`).

## Data model

```mermaid
erDiagram
    USERS ||--o{ JOB_APPLICATIONS : owns
    USERS ||--o{ AUDIT_LOGS : "events about"
    JOB_APPLICATIONS ||--o{ AUDIT_LOGS : "logged in"

    USERS {
        uuid id PK
        string email UK
        string password_hash
        timestamptz created_at
    }
    JOB_APPLICATIONS {
        uuid id PK
        uuid owner_id FK
        string company
        string role_title
        string status
        date deadline
        int match_score
        string risk_level
    }
    AUDIT_LOGS {
        uuid id PK
        uuid event_id UK
        string event_type
        uuid owner_id
        uuid application_id
        timestamptz occurred_at
    }
```

## Request flow: changing an application's status

```mermaid
sequenceDiagram
    actor U as User
    participant W as React app
    participant A as ApplicationController
    participant S as ApplicationService
    participant D as PostgreSQL
    participant K as Kafka
    participant C as AuditConsumer

    U->>W: Move card to Interview
    W->>A: PUT /api/applications/{id} (JWT)
    A->>S: update(ownerId, id, request)
    S->>D: load by id AND owner, save
    S->>K: publish STATUS_CHANGED
    S-->>A: updated application
    A-->>W: 200 OK
    K->>C: deliver event
    C->>D: insert audit_logs (skip if event_id exists)
```

## Package layout (backend)

| Package | Responsibility |
|---|---|
| `config` | JWT service and filter, security rules, CORS |
| `auth`, `user` | Registration, login, user entity |
| `application` | Application CRUD, due-soon query, status rules |
| `events` | Event record, Kafka publisher, audit consumer and log |
| `tools` | CV match and posting risk checks |
| `planner` | Free-slot logic for focus blocks (Calendar API pending) |
