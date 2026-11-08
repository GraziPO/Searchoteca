-- Acrescentar ao permissions.sql (não substituir o arquivo).

-- 1) Na lista de INSERT INTO sys_permissions:
INSERT INTO sys_permissions (permission_code, resource, action, description) VALUES
    ('dashboard:view', 'dashboard', 'view', 'Visualizar dashboard administrativo')
ON CONFLICT (permission_code) DO NOTHING;

-- 2) Root já vê tudo (is_root = TRUE). Só descomente se outro papel também for "gerente":
-- INSERT INTO sys_role_permissions (role_code, permission_code) VALUES
--     ('admin_users', 'dashboard:view')
-- ON CONFLICT DO NOTHING;

-- 3) Se o banco já tem a permissão com o erro de digitação 'dasboard', corrija:
UPDATE sys_permissions SET resource = 'dashboard' WHERE permission_code = 'dashboard:view';

-- 4) Índices opcionais (no schema.sql). loans_open já existe na main.
CREATE INDEX IF NOT EXISTS idx_copies_status     ON org_copies(status);
CREATE INDEX IF NOT EXISTS idx_receipts_created  ON org_receipts(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_time_action ON auditTrail_logs(time_stamp, action);
