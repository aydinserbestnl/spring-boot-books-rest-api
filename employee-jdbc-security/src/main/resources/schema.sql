CREATE TABLE IF NOT EXISTS employee (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS app_users (
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  active BOOLEAN NOT NULL,
  PRIMARY KEY (username)
);

CREATE TABLE IF NOT EXISTS user_roles (
  username VARCHAR(50) NOT NULL,
  role VARCHAR(50) NOT NULL,
  UNIQUE (username, role),
  FOREIGN KEY (username) REFERENCES app_users (username)
);
