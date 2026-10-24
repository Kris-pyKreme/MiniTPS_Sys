-- Run this once AFTER schema.sql
USE clinic_tps;

CREATE TABLE IF NOT EXISTS staff_users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    salt          VARCHAR(32)  NOT NULL,
    password_hash CHAR(64)     NOT NULL,          -- legacy seed hash; upgraded to PBKDF2 on login
    full_name     VARCHAR(100) NOT NULL,
    role          ENUM('Admin','Doctor','Nurse','Receptionist','Cashier') DEFAULT 'Receptionist'
);

-- Existing passwords are preserved when this file is imported again.
-- Default development accounts (reset these in Admin > Staff & requests before use):
--   admin  / admin123
--   staff1 / staff123
INSERT INTO staff_users (username, salt, password_hash, full_name, role) VALUES
 ('admin',  'a1b2c3', SHA2(CONCAT('a1b2c3','admin123'),256), 'System Administrator', 'Admin'),
 ('staff1', 'd4e5f6', SHA2(CONCAT('d4e5f6','staff123'),256), 'Front Desk Staff',     'Receptionist')
ON DUPLICATE KEY UPDATE username=staff_users.username;

-- Add staff through Admin > Staff & requests so passwords use PBKDF2.

-- Main automatically adds doctor_id, password_scheme, credential_version, and
-- password_requests on first launch. Create Doctor/Nurse accounts in the Admin workspace.
