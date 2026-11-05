-- Only after an offline verified backup, on the trusted MySQL host.
-- Retains historical plaintext Address until db.LegacyDataMigrator --apply verifies
-- each row and replaces it with an authenticated ciphertext.
USE hospital;
ALTER TABLE Patient ADD COLUMN Address_Cipher TEXT NULL;
ALTER TABLE Doctor ADD COLUMN Address_Cipher TEXT NULL;
ALTER TABLE Nurse ADD COLUMN Address_Cipher TEXT NULL;
DROP TRIGGER IF EXISTS audit_patient_address_changed;
DROP TRIGGER IF EXISTS audit_doctor_address_changed;
CREATE TRIGGER audit_patient_address_changed AFTER UPDATE ON Patient FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    SELECT 'PATIENT_ADDRESS_UPDATED', 'PATIENT', NEW.SSN
    WHERE NOT (OLD.Address <=> NEW.Address) OR NOT (OLD.Address_Cipher <=> NEW.Address_Cipher);
CREATE TRIGGER audit_doctor_address_changed AFTER UPDATE ON Doctor FOR EACH ROW
    INSERT INTO Audit_Event (Event_Type, Subject_Type, Subject_Ref)
    SELECT 'DOCTOR_ADDRESS_UPDATED', 'DOCTOR', NEW.SSN
    WHERE NOT (OLD.Address <=> NEW.Address) OR NOT (OLD.Address_Cipher <=> NEW.Address_Cipher);
