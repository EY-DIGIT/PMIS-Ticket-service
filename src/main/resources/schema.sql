-- ============================================================
-- PMIS Ticket & SLA Management  (FR-35 / FR-36 / FR-37)
-- Run once against the pmis database
-- ============================================================

CREATE TABLE IF NOT EXISTS pmis_ticket (
    uuid                VARCHAR(64)     NOT NULL PRIMARY KEY,
    ticket_number       VARCHAR(64)     NOT NULL UNIQUE,
    tenant_id           VARCHAR(64)     NOT NULL,
    category            VARCHAR(32)     NOT NULL CHECK (category IN ('INCIDENT','SERVICE_REQUEST','CHANGE','PROBLEM')),
    sub_category        VARCHAR(128),
    priority            VARCHAR(16)     NOT NULL CHECK (priority IN ('CRITICAL','HIGH','MEDIUM','LOW')),
    title               VARCHAR(512)    NOT NULL,
    description         TEXT,
    status              VARCHAR(32)     NOT NULL CHECK (status IN ('OPEN','IN_PROGRESS','PENDING','RESOLVED','CLOSED','CANCELLED')),
    project_id          VARCHAR(64),
    activity_id         VARCHAR(64),
    task_id             VARCHAR(64),
    parent_ticket_uuid  VARCHAR(64)     REFERENCES pmis_ticket(uuid),
    assignee_uuid       VARCHAR(64),
    assignee_name       VARCHAR(256),
    assignee_email      VARCHAR(256),
    sla_deadline        BIGINT,
    sla_breached        BOOLEAN         NOT NULL DEFAULT FALSE,
    sla_breached_at     BIGINT,
    baseline_ref        VARCHAR(128),
    contract_ref        VARCHAR(128),
    reported_by_uuid    VARCHAR(64)     NOT NULL,
    reported_by_name    VARCHAR(256),
    reported_by_email   VARCHAR(256),
    created_at          BIGINT          NOT NULL,
    updated_at          BIGINT,
    resolved_at         BIGINT,
    closed_at           BIGINT
);

CREATE INDEX IF NOT EXISTS idx_ticket_project    ON pmis_ticket(project_id);
CREATE INDEX IF NOT EXISTS idx_ticket_activity   ON pmis_ticket(activity_id);
CREATE INDEX IF NOT EXISTS idx_ticket_task       ON pmis_ticket(task_id);
CREATE INDEX IF NOT EXISTS idx_ticket_assignee   ON pmis_ticket(assignee_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_status     ON pmis_ticket(status);
CREATE INDEX IF NOT EXISTS idx_ticket_category   ON pmis_ticket(category);
CREATE INDEX IF NOT EXISTS idx_ticket_parent     ON pmis_ticket(parent_ticket_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_sla        ON pmis_ticket(sla_deadline, sla_breached);

-- ---- SLA Config ----
CREATE TABLE IF NOT EXISTS pmis_ticket_sla_config (
    uuid             VARCHAR(64)  NOT NULL PRIMARY KEY,
    category         VARCHAR(32)  NOT NULL,
    priority         VARCHAR(16)  NOT NULL,
    sla_hours        INT          NOT NULL,
    escalation_hours INT,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       BIGINT       NOT NULL,
    updated_at       BIGINT,
    CONSTRAINT uq_sla_category_priority UNIQUE (category, priority)
);

-- Insert P1 / P2 / P3 for PMIS Support
INSERT INTO ticket.pmis_ticket_sla_config
    (uuid, category, priority, sla_hours, first_response_hours, clock_type, is_active, created_at)
VALUES
    (gen_random_uuid(), 'PMIS Support', 'P1',  24,  2,  'CALENDAR_HOURS',  true, extract(epoch from now())::bigint * 1000),
    (gen_random_uuid(), 'PMIS Support', 'P2',  72,  8,  'CALENDAR_HOURS',  true, extract(epoch from now())::bigint * 1000),
    (gen_random_uuid(), 'PMIS Support', 'P3', 120, 24,  'BUSINESS_HOURS',  true, extract(epoch from now())::bigint * 1000)
ON CONFLICT ON CONSTRAINT uq_sla_category_priority DO NOTHING;

-- ---- Comments ----
CREATE TABLE IF NOT EXISTS pmis_ticket_comment (
    uuid              VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid       VARCHAR(64)  NOT NULL REFERENCES pmis_ticket(uuid),
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

CREATE INDEX IF NOT EXISTS idx_ticket_comment_ticket ON pmis_ticket_comment(ticket_uuid);
CREATE INDEX IF NOT EXISTS idx_ticket_comment_time   ON pmis_ticket_comment(created_at DESC);

-- ---- Attachments ----
CREATE TABLE IF NOT EXISTS pmis_ticket_attachment (
    uuid              VARCHAR(64)  NOT NULL PRIMARY KEY,
    ticket_uuid       VARCHAR(64)  NOT NULL REFERENCES pmis_ticket(uuid),
    comment_uuid      VARCHAR(64)  REFERENCES pmis_ticket_comment(uuid),
    file_name         VARCHAR(512) NOT NULL,
    mime_type         VARCHAR(128),
    size_bytes        BIGINT,
    file_url          TEXT         NOT NULL,
    uploaded_by_uuid  VARCHAR(64),
    uploaded_at       BIGINT       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ticket_attachment_ticket ON pmis_ticket_attachment(ticket_uuid);

-- ---- Bulk Operations (FR-35.5) ----
CREATE TABLE IF NOT EXISTS pmis_ticket_bulk_operation (
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
