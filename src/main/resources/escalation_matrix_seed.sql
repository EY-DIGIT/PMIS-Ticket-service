-- Run once to seed the escalation matrix
-- Update emails to real addresses before running

INSERT INTO ticket.pmis_escalation_matrix
    (uuid, priority, level, trigger_hours, emails, is_active, created_at, updated_at)
VALUES
-- P1 (Critical)
('esm-p1-l1', 'P1', 'L1',   8, '["pmis-admin@example.com","support-lead@example.com"]',  true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p1-l2', 'P1', 'L2',  16, '["pmis-admin@example.com","manager@example.com"]',        true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p1-l3', 'P1', 'L3',  20, '["pmis-admin@example.com","director@example.com"]',       true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),

-- P2 (High)
('esm-p2-l1', 'P2', 'L1',  72, '["pmis-admin@example.com","support-lead@example.com"]',  true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p2-l2', 'P2', 'L2',  96, '["pmis-admin@example.com","manager@example.com"]',        true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p2-l3', 'P2', 'L3', 108, '["pmis-admin@example.com","director@example.com"]',       true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),

-- P3 (Medium)
('esm-p3-l1', 'P3', 'L1',  72, '["pmis-admin@example.com","support-lead@example.com"]',  true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p3-l2', 'P3', 'L2',  96, '["pmis-admin@example.com","manager@example.com"]',        true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000),
('esm-p3-l3', 'P3', 'L3', 120, '["pmis-admin@example.com","director@example.com"]',       true, EXTRACT(EPOCH FROM NOW())*1000, EXTRACT(EPOCH FROM NOW())*1000)

ON CONFLICT (priority, level) DO NOTHING;
