-- ============================================================
-- PMIS Ticket Management — Full Schema
-- Schema: ticket
-- ============================================================

-- ---- Ticket ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket (
    uuid                    VARCHAR(64)     NOT NULL PRIMARY KEY,
    ticket_number           VARCHAR(64)     NOT NULL UNIQUE,
    ticket_type             VARCHAR(32),
    category                VARCHAR(32)     NOT NULL,
    priority                VARCHAR(8)      NOT NULL CHECK (priority IN ('P1','P2','P3')),
    title                   VARCHAR(512)    NOT NULL,
    description             TEXT,
    status                  VARCHAR(32)     NOT NULL CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','PENDING','SENT_BACK','RESOLVED','CLOSED','CANCELLED')),
    project_id              VARCHAR(64),
    project_name            VARCHAR(256),
    activity_id             VARCHAR(64),
    activity_name           VARCHAR(256),
    task_id                 VARCHAR(64),
    task_name               VARCHAR(256),
    parent_ticket_uuid      VARCHAR(64)     REFERENCES ticket.pmis_ticket(uuid),
    assignee_uuid           VARCHAR(64),
    assignee_name           VARCHAR(256),
    assignee_email          VARCHAR(256),
    reported_by_uuid        VARCHAR(64)     NOT NULL,
    reported_by_name        VARCHAR(256),
    reported_by_email       VARCHAR(256),
    first_response_deadline BIGINT,
    first_response_breached BOOLEAN         NOT NULL DEFAULT FALSE,
    first_response_at       BIGINT,
    sla_deadline            BIGINT,
    sla_breached            BOOLEAN         NOT NULL DEFAULT FALSE,
    sla_breached_at         BIGINT,
    sla_paused_at           BIGINT,
    sla_status              VARCHAR(16),
    total_paused_ms         BIGINT          NOT NULL DEFAULT 0,
    baseline_ref            VARCHAR(128),
    contract_ref            VARCHAR(128),
    created_at              BIGINT          NOT NULL,
    updated_at              BIGINT,
    resolved_at             BIGINT,
    closed_at               BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ticket_project    ON ticket.pmis_ticket(project_id);
CREATE INDEX IF NOT EXISTS idx_ticket_activity   ON ticket.pmis_ticket(activity_id);
CREATE INDEX IF NOT EXISTS idx_ticket_task       ON ticket.pmis_ticket(task_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignee   ON ticket.pmis_ticket(assignee_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_status     ON ticket.pmis_ticket(status);
CREATE INDEX IF NOT EXISTS idx_ticket_priority   ON ticket.pmis_ticket(priority);
CREATE INDEX IF NOT EXISTS idx_ticket_parent     ON ticket.pmis_ticket(parent_ticket_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_sla        ON ticket.pmis_ticket(sla_deadline, sla_breached);

-- ---- SLA Config ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_sla_config (
    uuid                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    category                VARCHAR(32)  NOT NULL,
    priority                VARCHAR(8)   NOT NULL,
    first_response_hours    INT,
    sla_hours               INT          NOT NULL,
    clock_type              VARCHAR(16)  NOT NULL DEFAULT 'CALENDAR_HOURS',
    escalation_50_pct_roles TEXT,
    escalation_75_pct_roles TEXT,
    escalation_breach_roles TEXT,
    is_active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT,
    CONSTRAINT uq_sla_category_priority UNIQUE (category, priority)
);

INSERT INTO ticket.pmis_ticket_sla_config
    (uuid, category, priority, first_response_hours, sla_hours, clock_type, is_active, created_at)
VALUES
    (gen_random_uuid(), 'PMIS Support', 'P1',  2,   24,  'CALENDAR_HOURS', true, extract(epoch from now())::bigint * 1000),
    (gen_random_uuid(), 'PMIS Support', 'P2',  8,   72,  'CALENDAR_HOURS', true, extract(epoch from now())::bigint * 1000),
    (gen_random_uuid(), 'PMIS Support', 'P3', 24,  120,  'CALENDAR_HOURS', true, extract(epoch from now())::bigint * 1000)
ON CONFLICT ON CONSTRAINT uq_sla_category_priority DO NOTHING;

-- ---- Working Calendar ----
CREATE TABLE IF NOT EXISTS ticket.pmis_sla_working_calendar (
    uuid            VARCHAR(64)  NOT NULL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    timezone        VARCHAR(64)  NOT NULL DEFAULT 'Asia/Kolkata',
    work_day_start  INT          NOT NULL DEFAULT 9,
    work_day_end    INT          NOT NULL DEFAULT 18,
    work_days       VARCHAR(128) NOT NULL DEFAULT 'MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY',
    holidays        TEXT,                           -- JSON array e.g. ["2026-01-26","2026-08-15","2026-10-02"]
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      BIGINT       NOT NULL,
    updated_at      BIGINT
);

-- ---- Comments ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_comment (
    uuid              VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid       VARCHAR(64)  NOT NULL REFERENCES ticket.pmis_ticket(uuid),
    comment_type      VARCHAR(16)  NOT NULL DEFAULT 'COMMENT'
                          CHECK (comment_type IN ('COMMENT','STATUS_CHANGE','ASSIGNMENT','SYSTEM')),
    body              TEXT,
    previous_status   VARCHAR(32),
    new_status        VARCHAR(32),
    previous_assignee VARCHAR(64),
    new_assignee      VARCHAR(64),
    author_uuid       VARCHAR(64)  NOT NULL,
    author_name       VARCHAR(256),
    author_email      VARCHAR(256),
    created_at        BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_comment_ticket ON ticket.pmis_ticket_comment(ticket_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_comment_time   ON ticket.pmis_ticket_comment(created_at DESC);

-- ---- Attachments ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_attachment (
    uuid              VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid       VARCHAR(64)  NOT NULL REFERENCES ticket.pmis_ticket(uuid),
    comment_uuid      VARCHAR(64)  REFERENCES ticket.pmis_ticket_comment(uuid),
    file_name         VARCHAR(512) NOT NULL,
    mime_type         VARCHAR(128),
    size_bytes        BIGINT,
    file_url          TEXT         NOT NULL,
    uploaded_by_uuid  VARCHAR(64),
    uploaded_at       BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachment_ticket ON ticket.pmis_ticket_attachment(ticket_uuid);

-- ---- Documents ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_document (
    uuid              VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid       VARCHAR(64)  NOT NULL REFERENCES ticket.pmis_ticket(uuid),
    original_name     VARCHAR(512) NOT NULL,
    stored_name       VARCHAR(512) NOT NULL,
    file_path         TEXT         NOT NULL,
    mime_type         VARCHAR(128),
    size_bytes        BIGINT,
    uploaded_by_uuid  VARCHAR(64),
    uploaded_by_name  VARCHAR(256),
    uploaded_at       BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_document_ticket ON ticket.pmis_ticket_document(ticket_uuid);

-- ---- Bulk Operations ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_bulk_operation (
    uuid            VARCHAR(64)  NOT NULL PRIMARY KEY,
    operation_type  VARCHAR(32)  NOT NULL,
    ticket_uuids    TEXT[]       NOT NULL,
    payload         JSONB,
    total_count     INT          NOT NULL,
    success_count   INT          DEFAULT 0,
    failed_count    INT          DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    performed_by    VARCHAR(64)  NOT NULL,
    created_at      BIGINT       NOT NULL,
    completed_at    BIGINT
);

-- ---- Escalation Matrix ----
CREATE TABLE IF NOT EXISTS ticket.pmis_escalation_matrix (
    uuid          VARCHAR(64)  NOT NULL PRIMARY KEY,
    priority      VARCHAR(8)   NOT NULL,
    level         VARCHAR(4)   NOT NULL,
    trigger_hours INT          NOT NULL,
    emails        TEXT         NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    BIGINT       NOT NULL,
    updated_at    BIGINT       NOT NULL,
    CONSTRAINT uq_escalation_priority_level UNIQUE (priority, level)
);

INSERT INTO ticket.pmis_escalation_matrix
    (uuid, priority, level, trigger_hours, emails, is_active, created_at, updated_at)
VALUES
    ('esm-p1-l1', 'P1', 'L1',   8, '["pmis-admin@example.com","support-lead@example.com"]', true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p1-l2', 'P1', 'L2',  16, '["pmis-admin@example.com","manager@example.com"]',       true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p1-l3', 'P1', 'L3',  20, '["pmis-admin@example.com","director@example.com"]',      true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p2-l1', 'P2', 'L1',  72, '["pmis-admin@example.com","support-lead@example.com"]', true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p2-l2', 'P2', 'L2',  96, '["pmis-admin@example.com","manager@example.com"]',       true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p2-l3', 'P2', 'L3', 108, '["pmis-admin@example.com","director@example.com"]',      true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p3-l1', 'P3', 'L1',  72, '["pmis-admin@example.com","support-lead@example.com"]', true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p3-l2', 'P3', 'L2',  96, '["pmis-admin@example.com","manager@example.com"]',       true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000),
    ('esm-p3-l3', 'P3', 'L3', 120, '["pmis-admin@example.com","director@example.com"]',      true, extract(epoch from now())::bigint*1000, extract(epoch from now())::bigint*1000)
ON CONFLICT ON CONSTRAINT uq_escalation_priority_level DO NOTHING;

-- ---- SLA Escalation Log (SLA breach milestones: RISK_50, RISK_75, BREACH, FIRST_RESPONSE) ----
CREATE TABLE IF NOT EXISTS ticket.pmis_sla_escalation_log (
    uuid             VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid      VARCHAR(64)  NOT NULL REFERENCES ticket.pmis_ticket(uuid),
    escalation_level VARCHAR(32)  NOT NULL,   -- RISK_50 | RISK_75 | BREACH | FIRST_RESPONSE | POST_BREACH_24H
    notified_roles   TEXT,                    -- JSON array e.g. ["MANAGER","LEAD"]
    escalated_at     BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_escalation_ticket_level ON ticket.pmis_sla_escalation_log(ticket_uuid, escalation_level);

-- ---- Ticket Escalation Log (matrix-based escalation: L1, L2, L3 per priority) ----
CREATE TABLE IF NOT EXISTS ticket.pmis_ticket_escalation_log (
    uuid          VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid   VARCHAR(64)  NOT NULL REFERENCES ticket.pmis_ticket(uuid),
    ticket_number VARCHAR(64),
    priority      VARCHAR(8)   NOT NULL,
    level         VARCHAR(4)   NOT NULL,
    trigger_hours INT          NOT NULL,
    emails_sent   TEXT         NOT NULL,
    triggered_at  BIGINT       NOT NULL,
    CONSTRAINT uq_escalation_log_ticket_level UNIQUE (ticket_uuid, level)
);

CREATE INDEX IF NOT EXISTS idx_escalation_log_ticket ON ticket.pmis_ticket_escalation_log(ticket_uuid);

-- ---- Workflow Config ----
CREATE TABLE IF NOT EXISTS ticket.pmis_workflow_config (
    business_service VARCHAR(64)  NOT NULL PRIMARY KEY,
    workflow_json    TEXT         NOT NULL,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       BIGINT       NOT NULL,
    updated_at       BIGINT       NOT NULL,
    updated_by       VARCHAR(128)
);
