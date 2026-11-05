-- Disposable GitHub Actions fixture only. Never install this credential in a deployed system.
CREATE USER IF NOT EXISTS 'hospital_ci'@'%' IDENTIFIED BY 'ci-isolated-hospital-user-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Patient TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Doctor TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Appointment TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Department TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Room TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Nurse TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.medicine TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.Medical_Records TO 'hospital_ci'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON hospital.`Add` TO 'hospital_ci'@'%';
GRANT SELECT ON hospital.Audit_Event TO 'hospital_ci'@'%';
