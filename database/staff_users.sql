-- Run this once AFTER schema.sql
USE clinic_tps;

CREATE TABLE IF NOT EXISTS staff_users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    salt          VARCHAR(32)  NOT NULL,
    password_hash CHAR(64)     NOT NULL,          -- SHA-256 of (salt + password), never plain text
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('Admin','Receptionist','Doctor','Cashier') DEFAULT 'Receptionist'
);

-- Default accounts (change these passwords before your demo/submission!)
--   admin  / admin123
--   staff1 / staff123
INSERT INTO staff_users (username, salt, password_hash, full_name, role) VALUES
 ('admin',  'a1b2c3', SHA2(CONCAT('a1b2c3','admin123'),256), 'System Administrator', 'Admin'),
 ('staff1', 'd4e5f6', SHA2(CONCAT('d4e5f6','staff123'),256), 'Front Desk Staff',     'Receptionist');

-- To add another staff member later:
-- INSERT INTO staff_users (username, salt, password_hash, full_name, role)
-- VALUES ('nurse1', 'x9y8z7', SHA2(CONCAT('x9y8z7','theirPassword'),256), 'Nurse Joy', 'Receptionist');
