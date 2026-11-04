USE hospital;
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
