CREATE TABLE IF NOT EXISTS org_depart(
    depart_id SERIAL PRIMARY KEY,
    depart_code VARCHAR(20) UNIQUE,
    depart_name VARCHAR(100) NOT NULL UNIQUE,
    depart_desc VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS org_local(
    local_id SERIAL PRIMARY KEY,
    local_code VARCHAR(20) UNIQUE,
    local_name VARCHAR(100) NOT NULL UNIQUE,
    local_desc VARCHAR(100) NOT NULL,
    depart_code VARCHAR,
    FOREIGN KEY (depart_code) REFERENCES org_depart(depart_code)
);

CREATE TABLE IF NOT EXISTS org_books(
    book_id SERIAL PRIMARY KEY,
    isbn VARCHAR(15) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    author VARCHAR(100) NOT NULL,
    rel_year INT,
    publisher VARCHAR(50),
    genre VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS org_copies(
    copy_id SERIAL PRIMARY KEY,
    isbn VARCHAR(25) REFERENCES org_books(isbn),
    depart_code VARCHAR REFERENCES org_depart(depart_code),
    local_code  VARCHAR REFERENCES org_local(local_code),
    copy_index INT NOT NULL,
    copy_custom_id VARCHAR(60) GENERATED ALWAYS AS (isbn || depart_code || local_code || copy_index :: text) STORED,
    status VARCHAR(20) DEFAULT 'DISPONÍVEL',
    UNIQUE (isbn,copy_index)
);

CREATE TABLE IF NOT EXISTS org_users(
    user_id SERIAL PRIMARY KEY,
    custom_id VARCHAR(20) UNIQUE,
    username VARCHAR(20) NOT NULL UNIQUE,
    complete_name VARCHAR(100) NOT NULL,
    email VARCHAR(40) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_code VARCHAR(20) NOT NULL,
    FOREIGN KEY (role_code) REFERENCES sys_roles(role_code)
);

CREATE TABLE IF NOT EXISTS sys_roles(
    role_id SERIAL PRIMARY KEY,
    role_code VARCHAR(20) UNIQUE NOT NULL,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_desc VARCHAR(100) NOT NULL,
    is_root BOOLEAN NOT NULL DEFAULT FALSE
    );


CREATE TABLE IF NOT EXISTS sys_role_permissions(
    role_code VARCHAR(20) NOT NULL REFERENCES sys_roles(role_code) ON DELETE CASCADE,
    permission_code VARCHAR(50) NOT NULL REFERENCES sys_permissions(permission_code) ON DELETE CASCADE,
    PRIMARY KEY (role_code, permission_code)
    );

-- ===== seed: roles =====
INSERT INTO sys_roles (role_code, role_name, role_desc, is_root) VALUES
    ('root', 'Root', 'Administrador principal com acesso total ao sistema', TRUE),
    ('local_admin', 'Administrador local', 'Gerencia usuários e acervo', FALSE),
    ('warehouse_op', 'Operador de Acervo', 'Gerencia departamentos, locais e livros; sem acesso a usuários', FALSE),
    ('loan_op', 'Operador de Empréstimo', 'Consulta acervo e controla quantidade de livros', FALSE)
    ON CONFLICT (role_code) DO NOTHING;

-- ===== seed: permissions (resource:action) =====
INSERT INTO sys_permissions (permission_code, resource, action, description) VALUES
    ('org_users:create', 'org_users', 'create', 'Criar usuários'),
    ('org_users:edit', 'org_users', 'edit', 'Editar usuários'),
    ('org_users:view', 'org_users', 'view', 'Visualizar usuários'),
    ('org_users:delete', 'org_users', 'delete', 'Excluir usuários'),

    ('org_local:create', 'org_local', 'create', 'Criar locais'),
    ('org_local:edit', 'org_local', 'edit', 'Editar locais'),
    ('org_local:view', 'org_local', 'view', 'Visualizar locais'),
    ('org_local:delete', 'org_local', 'delete', 'Excluir locais'),

    ('org_depart:create', 'org_depart', 'create', 'Criar departamentos'),
    ('org_depart:edit', 'org_depart', 'edit', 'Editar departamentos'),
    ('org_depart:view', 'org_depart', 'view', 'Visualizar departamentos'),
    ('org_depart:delete', 'org_depart', 'delete', 'Excluir departamentos'),

    ('org_books:create', 'org_books', 'create', 'Cadastrar livros'),
    ('org_books:edit', 'org_books', 'edit', 'Editar livros'),
    ('org_books:view', 'org_books', 'view', 'Visualizar livros'),
    ('org_books:delete', 'org_books', 'delete', 'Excluir livros'),
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
