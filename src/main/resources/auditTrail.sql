CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON auditTrail_logs(time_stamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_username    ON auditTrail_logs(username);

-- Permissão para consultar a auditoria (root recebe automaticamente)
INSERT INTO sys_permissions (permission_code, resource, action, description) VALUES
    ('audit:view', 'audit', 'view', 'Visualizar trilha de auditoria')
    ON CONFLICT (permission_code) DO NOTHING;