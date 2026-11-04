-- MySQL 8.x local development schema. Apply only to a NEW/EMPTY local database.
-- Never run this over production data without reviewing an export/backup first.
CREATE DATABASE IF NOT EXISTS hospital CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hospital;

CREATE TABLE IF NOT EXISTS Department (
    Department_Name VARCHAR(80) PRIMARY KEY
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Patient (
    SSN INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Surname VARCHAR(100) NOT NULL,
    Gender VARCHAR(20) NOT NULL,
    Address VARCHAR(500),
    Date_Of_Birth DATE NOT NULL,
    National_ID VARCHAR(11) NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    salt VARCHAR(128) NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    Failed_Attempts INT NOT NULL DEFAULT 0,
    Locked_Until DATETIME(6) NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Doctor (
    SSN INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Surname VARCHAR(100) NOT NULL,
    Gender VARCHAR(20) NOT NULL,
    Address VARCHAR(500),
    Date_Of_Birth DATE NOT NULL,
    Department_Name VARCHAR(80) NOT NULL,
    National_ID VARCHAR(11) NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    salt VARCHAR(128) NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
    Failed_Attempts INT NOT NULL DEFAULT 0,
    Locked_Until DATETIME(6) NULL,
    CONSTRAINT fk_doctor_department FOREIGN KEY (Department_Name) REFERENCES Department(Department_Name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Appointment (
    app_id INT AUTO_INCREMENT PRIMARY KEY,
    Date DATE NOT NULL,
    Time TIME NOT NULL,
    Patient_SSN INT NOT NULL,
    Doctor_SSN INT NOT NULL,
    CONSTRAINT fk_appointment_patient FOREIGN KEY (Patient_SSN) REFERENCES Patient(SSN),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (Doctor_SSN) REFERENCES Doctor(SSN),
    CONSTRAINT uq_appointment_doctor_slot UNIQUE (Doctor_SSN, Date, Time),
    CONSTRAINT uq_appointment_patient_slot UNIQUE (Patient_SSN, Date, Time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Nurse (
    SSN INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Surname VARCHAR(100) NOT NULL,
    Gender VARCHAR(20) NOT NULL,
    Address VARCHAR(500),
    Date_Of_Birth DATE NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Room (
    Room_ID INT PRIMARY KEY,
    Department_Name VARCHAR(80) NOT NULL,
    CONSTRAINT fk_room_department FOREIGN KEY (Department_Name) REFERENCES Department(Department_Name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS medicine (
    Barcode_No INT PRIMARY KEY,
    Medicine_Name VARCHAR(150) NOT NULL,
    Department_Name VARCHAR(80) NOT NULL,
    consumed TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_medicine_department FOREIGN KEY (Department_Name) REFERENCES Department(Department_Name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Medical_Records (
    Record_No INT AUTO_INCREMENT PRIMARY KEY,
    Prescription TEXT,
    Diagnosis TEXT,
    Patient_SSN INT NOT NULL,
    CONSTRAINT fk_record_patient FOREIGN KEY (Patient_SSN) REFERENCES Patient(SSN)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `Add` (
    Doctor_SSN INT NOT NULL,
    Date DATE NOT NULL,
    Time TIME NOT NULL,
    Record_No INT NOT NULL UNIQUE,
    PRIMARY KEY (Doctor_SSN, Date, Time, Record_No),
    CONSTRAINT fk_record_doctor FOREIGN KEY (Doctor_SSN) REFERENCES Doctor(SSN),
    CONSTRAINT fk_add_record FOREIGN KEY (Record_No) REFERENCES Medical_Records(Record_No)
) ENGINE=InnoDB;

-- Operational audit metadata only; do not store identifiers, names or medical text.
-- Install by database administrator. The desktop application user must NOT
-- receive UPDATE/DELETE/INSERT privileges on Audit_Event.
CREATE TABLE IF NOT EXISTS Audit_Event (
    Event_ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    Event_Type VARCHAR(50) NOT NULL,
    Subject_Type VARCHAR(40) NOT NULL,
    Subject_Ref INT NOT NULL,
    Recorded_At TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_event_time (Recorded_At),
    INDEX idx_audit_subject (Subject_Type, Subject_Ref)
) ENGINE=InnoDB;

CREATE TRIGGER audit_appointment_created AFTER INSERT ON Appointment FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    VALUES ('APPOINTMENT_CREATED', 'APPOINTMENT', NEW.app_id);

CREATE TRIGGER audit_appointment_cancelled AFTER DELETE ON Appointment FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    VALUES ('APPOINTMENT_CANCELLED', 'APPOINTMENT', OLD.app_id);

CREATE TRIGGER audit_medical_record_created AFTER INSERT ON Medical_Records FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    VALUES ('MEDICAL_RECORD_CREATED', 'MEDICAL_RECORD', NEW.Record_No);

CREATE TRIGGER audit_patient_address_changed AFTER UPDATE ON Patient FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    SELECT 'PATIENT_ADDRESS_UPDATED', 'PATIENT', NEW.SSN
    WHERE NOT (OLD.Address <=> NEW.Address);

CREATE TRIGGER audit_doctor_address_changed AFTER UPDATE ON Doctor FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    SELECT 'DOCTOR_ADDRESS_UPDATED', 'DOCTOR', NEW.SSN
    WHERE NOT (OLD.Address <=> NEW.Address);

INSERT IGNORE INTO Department (Department_Name) VALUES
    ('Cardiology'), ('Neurology'), ('Diagnostics'), ('Orthopedics');
