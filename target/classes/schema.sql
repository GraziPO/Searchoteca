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
    genre VARCHAR(50),
    depart_code VARCHAR,
    local_code  VARCHAR,
    FOREIGN KEY (depart_code) REFERENCES org_depart(depart_code),
    FOREIGN KEY (local_code) REFERENCES org_local(local_code)
);
