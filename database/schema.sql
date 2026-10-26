-- KlinikAccess  |  run this once in MySQL Workbench / phpMyAdmin
CREATE DATABASE IF NOT EXISTS clinic_tps;
USE clinic_tps;

CREATE TABLE IF NOT EXISTS patients (
    patient_id      INT AUTO_INCREMENT PRIMARY KEY,
    first_name      VARCHAR(50)  NOT NULL,
    last_name       VARCHAR(50)  NOT NULL,
    birth_date      DATE         NOT NULL,
    sex             ENUM('Male','Female') NOT NULL,
    contact_number  VARCHAR(20),
    address         VARCHAR(150),
    email           VARCHAR(100),
    date_registered TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS doctors (
    doctor_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name       VARCHAR(100) NOT NULL,
    specialization  VARCHAR(100),
    contact_number  VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS appointments (
    appointment_id   INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT NOT NULL,
    doctor_id        INT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status           ENUM('Scheduled','Completed','Cancelled','No-show') DEFAULT 'Scheduled',
    reason           VARCHAR(200),
    date_created     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(doctor_id)
);

CREATE TABLE IF NOT EXISTS consultations (
    consultation_id  INT AUTO_INCREMENT PRIMARY KEY,
    appointment_id   INT NOT NULL UNIQUE,
    diagnosis        TEXT,
    treatment_notes  TEXT,
    prescription     TEXT,
    consultation_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    date_recorded    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE
);

-- Changes vs. the earlier draft: consultation_id is UNIQUE (one bill per consultation)
-- and patient_id cascades (so deleting a patient doesn't get blocked by their bills).
CREATE TABLE IF NOT EXISTS billing (
    bill_id          INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT NOT NULL,
    consultation_id  INT NOT NULL UNIQUE,
    consultation_fee DECIMAL(10,2) NOT NULL,
    other_charges    DECIMAL(10,2) DEFAULT 0.00,
    total_amount     DECIMAL(10,2) NOT NULL,
    payment_status   ENUM('Unpaid','Paid','Partial') DEFAULT 'Unpaid',
    payment_method   ENUM('Cash','Card','GCash','Insurance') NULL,
    billing_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id)      REFERENCES patients(patient_id)           ON DELETE CASCADE,
    FOREIGN KEY (consultation_id) REFERENCES consultations(consultation_id) ON DELETE CASCADE
);

-- Seed data so the Appointments tab has doctors to pick from
INSERT INTO doctors (full_name, specialization, contact_number) VALUES
 ('Dr. Maria Santos',  'General Medicine', '09171234567'),
 ('Dr. Jose Reyes',    'Pediatrics',       '09181234567'),
 ('Dr. Ana Villanueva','Dermatology',      '09191234567');
