-- Run ONCE after a verified database backup. Never apply over live data without a maintenance window.
-- Keeps the legacy 32-bit SSN primary keys and their foreign-key relationships intact.
-- National_ID preserves the full 11 digits (including leading zeros) in a separate field.
-- Existing accounts with NULL or legacy SHA-256 password_hash remain unable to sign in until provisioned.
USE hospital;
ALTER TABLE Patient
    ADD COLUMN National_ID VARCHAR(11) NULL,
    MODIFY COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN Failed_Attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN Locked_Until DATETIME(6) NULL,
    ADD UNIQUE KEY uq_patient_national_id (National_ID);
ALTER TABLE Doctor
    ADD COLUMN National_ID VARCHAR(11) NULL,
    MODIFY COLUMN password_hash VARCHAR(255) NULL,
    ADD COLUMN Failed_Attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN Locked_Until DATETIME(6) NULL,
    ADD UNIQUE KEY uq_doctor_national_id (National_ID);
