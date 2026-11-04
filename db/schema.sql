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
    password_hash VARCHAR(128) NULL,
    salt VARCHAR(128) NULL,
    is_active TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS Doctor (
    SSN INT PRIMARY KEY,
    Name VARCHAR(100) NOT NULL,
    Surname VARCHAR(100) NOT NULL,
    Gender VARCHAR(20) NOT NULL,
    Address VARCHAR(500),
    Date_Of_Birth DATE NOT NULL,
    Department_Name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(128) NULL,
    salt VARCHAR(128) NULL,
    is_active TINYINT NOT NULL DEFAULT 1,
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

INSERT IGNORE INTO Department (Department_Name) VALUES
    ('Cardiology'), ('Neurology'), ('Diagnostics'), ('Orthopedics');
