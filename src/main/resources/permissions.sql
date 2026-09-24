-- ===== seed: roles =====
INSERT INTO sys_roles (role_code, role_name, role_desc, is_root) VALUES
    ('root', 'Root', 'Acesso total ao sistema, ignora checagem de permissões', TRUE),
    ('admin_users', 'Administrador de Usuários', 'Gerencia usuários; sem acesso ao acervo', FALSE),
    ('warehouse_op', 'Operador de Acervo', 'Gerencia departamentos, locais e livros; sem acesso a usuários', FALSE),
    ('loan_op', 'Operador de Empréstimo', 'Consulta acervo e controla quantidade de livros', FALSE)
ON CONFLICT (role_code) DO NOTHING;

-- ===== seed: permissions (resource:action) =====
INSERT INTO sys_permissions (permission_code, resource, action, description) VALUES
    ('users:create', 'users', 'create', 'Criar usuário'),
    ('users:edit', 'users', 'edit', 'Editar usuário'),
    ('users:view', 'users', 'view', 'Visualizar usuário'),
    ('users:delete', 'users', 'delete', 'Excluir usuário'),

    ('org_local:create', 'org_local', 'create', 'Criar local'),
    ('org_local:edit', 'org_local', 'edit', 'Editar local'),
    ('org_local:view', 'org_local', 'view', 'Visualizar local'),
    ('org_local:delete', 'org_local', 'delete', 'Excluir local'),

    ('org_depart:create', 'org_depart', 'create', 'Criar departamento'),
    ('org_depart:edit', 'org_depart', 'edit', 'Editar departamento'),
    ('org_depart:view', 'org_depart', 'view', 'Visualizar departamento'),
    ('org_depart:delete', 'org_depart', 'delete', 'Excluir departamento'),

    ('org_books:create', 'org_books', 'create', 'Cadastrar livro'),
    ('org_books:edit', 'org_books', 'edit', 'Editar livro'),
    ('org_books:view', 'org_books', 'view', 'Visualizar livro'),
    ('org_books:delete', 'org_books', 'delete', 'Excluir livro'),
    ('org_books:alter_quantity', 'org_books', 'alter_quantity', 'Alterar quantidade em estoque do livro')
ON CONFLICT (permission_code) DO NOTHING;

-- ===== seed: role x permission =====
-- root: nao precisa de linhas aqui, is_root=true faz bypass na checagem
INSERT INTO sys_role_permissions (role_code, permission_code) VALUES
    ('admin_users', 'users:create'),
    ('admin_users', 'users:edit'),
    ('admin_users', 'users:view'),
    ('admin_users', 'users:delete'),

    ('warehouse_op', 'org_local:create'),
    ('warehouse_op', 'org_local:edit'),
    ('warehouse_op', 'org_local:view'),
    ('warehouse_op', 'org_local:delete'),
    ('warehouse_op', 'org_depart:create'),
    ('warehouse_op', 'org_depart:edit'),
    ('warehouse_op', 'org_depart:view'),
    ('warehouse_op', 'org_depart:delete'),
    ('warehouse_op', 'org_books:create'),
    ('warehouse_op', 'org_books:edit'),
    ('warehouse_op', 'org_books:view'),
    ('warehouse_op', 'org_books:delete'),

    ('loan_op', 'org_local:view'),
    ('loan_op', 'org_depart:view'),
    ('loan_op', 'org_books:view'),
    ('loan_op', 'org_books:alter_quantity')
    ON CONFLICT DO NOTHING;
