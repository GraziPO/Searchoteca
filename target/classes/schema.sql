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
    copy_custom_code VARCHAR(60) GENERATED ALWAYS AS (isbn || depart_code || local_code || copy_index :: text) STORED,
    status VARCHAR(20) DEFAULT 'DISPONÍVEL',
    UNIQUE (isbn,copy_index)
);

CREATE TABLE IF NOT EXISTS sys_roles(
    role_id SERIAL PRIMARY KEY,
    role_code VARCHAR(20) UNIQUE NOT NULL,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_desc VARCHAR(100) NOT NULL,
    is_root BOOLEAN NOT NULL DEFAULT FALSE
    );
CREATE TABLE IF NOT EXISTS org_users(
    user_id SERIAL PRIMARY KEY,
    custom_id VARCHAR(30) UNIQUE,
    username VARCHAR(30) NOT NULL UNIQUE,
    complete_name VARCHAR(100) NOT NULL,
    email VARCHAR(40) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role_code VARCHAR(30) REFERENCES sys_roles(role_code),
    status BOOLEAN NOT NULL DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS sys_permissions(
    permission_id SERIAL PRIMARY KEY,
    permission_code VARCHAR(50) UNIQUE NOT NULL,  -- ex: 'org_books:create'
    resource VARCHAR(30) NOT NULL,                -- ex: 'users', 'org_books'
    action VARCHAR(30) NOT NULL,                  -- ex: 'create', 'view', 'alter_quantity'
    description VARCHAR(100)
    );

CREATE TABLE IF NOT EXISTS sys_role_permissions(
    role_code VARCHAR(20) NOT NULL REFERENCES sys_roles(role_code) ON DELETE CASCADE,
    permission_code VARCHAR(50) NOT NULL REFERENCES sys_permissions(permission_code) ON DELETE CASCADE,
    PRIMARY KEY (role_code, permission_code)
    );