-- =====================================================================================
-- Carga inicial do BANCO DE TESTE (H2). Mesmo conteúdo de permissions.sql e
-- auditTrail.sql, escrito com MERGE (o H2 não aceita ON CONFLICT do PostgreSQL).
-- O MERGE torna o script idempotente: pode rodar mais de uma vez no mesmo banco.
-- =====================================================================================

-- ===== roles =====
MERGE INTO sys_roles (role_code, role_name, role_desc, is_root) KEY (role_code) VALUES
    ('root', 'Root', 'Acesso total ao sistema, ignora checagem de permissões', TRUE),
    ('admin_users', 'Administrador de Usuários', 'Gerencia usuários; sem acesso ao acervo', FALSE),
    ('warehouse_op', 'Operador de Acervo', 'Gerencia departamentos, locais e livros; sem acesso a usuários', FALSE),
    ('loan_op', 'Operador de Empréstimo', 'Consulta acervo e controla quantidade de livros', FALSE),
    ('students', 'students', 'Consulta Acervo e empréstimos de usuário', FALSE);

-- ===== permissões =====
MERGE INTO sys_permissions (permission_code, resource, action, description) KEY (permission_code) VALUES
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
    ('org_books:alter_quantity', 'org_books', 'alter_quantity', 'Alterar quantidade em estoque do livro'),

    ('copies:create', 'copies', 'create', 'Criar cópia de livro'),
    ('copies:view', 'copies', 'view', 'Visualizar cópia de livro'),
    ('copies:edit', 'copies', 'edit', 'Editar cópia de livro'),
    ('copies:delete', 'copies', 'delete', 'Desativar cópia de livro'),

    ('loans:create', 'loans', 'create', 'Criar emprestimo'),
    ('loans:view', 'loans', 'view', 'Visualizar emprestimo'),
    ('loans:return', 'loans', 'return', 'retornar item do emprestimo'),
    ('loans:cancel', 'loans', 'cancel', 'Cancelar Emprestimo'),
    ('loans:view_own', 'loans', 'view_own', 'Visualizar próprios emprestimos'),

    ('students:create', 'students', 'create', 'cadastrar aluno'),
    ('students:view', 'students', 'view', 'Visualizar aluno'),

    ('audit:view', 'audit', 'view', 'Visualizar trilha de auditoria');

-- ===== role x permissão (root não precisa: is_root = TRUE recebe todas) =====
MERGE INTO sys_role_permissions (role_code, permission_code) KEY (role_code, permission_code) VALUES
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

    ('warehouse_op', 'copies:create'),
    ('warehouse_op', 'copies:view'),

    ('warehouse_op', 'org_books:create'),
    ('warehouse_op', 'org_books:edit'),
    ('warehouse_op', 'org_books:view'),
    ('warehouse_op', 'org_books:delete'),

    ('loan_op', 'org_depart:view'),
    ('loan_op', 'org_local:view'),
    ('loan_op', 'org_books:view'),
    ('loan_op', 'loans:create'),
    ('loan_op', 'loans:return'),
    ('loan_op', 'loans:view'),
    ('loan_op', 'loans:cancel'),
    ('loan_op', 'students:create'),
    ('loan_op', 'students:view'),

    ('students', 'loans:view_own');

-- ===== usuário root de bootstrap (mesmo do permissions.sql; senha: root123) =====
MERGE INTO org_users (custom_id, username, complete_name, email, password, role_code, status) KEY (username) VALUES
    ('ROOT001', 'root', 'Administrador Root', 'root@searchoteca.local',
     '$2b$10$qfAEE4NXf6S71kG5xVSVZ.elB96UNKE/1GQZY7NRlGahPxtDabz5K', 'root', TRUE);
