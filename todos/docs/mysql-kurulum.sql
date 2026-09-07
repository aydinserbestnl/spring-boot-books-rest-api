-- Workbench'te root ile bağlanıp bunu bir kere çalıştırman yeterli

CREATE DATABASE IF NOT EXISTS tododb;
CREATE USER IF NOT EXISTS 'userTodos'@'localhost' IDENTIFIED BY 'pwTodos';
GRANT ALL PRIVILEGES ON tododb.* TO 'userTodos'@'localhost';
FLUSH PRIVILEGES;
