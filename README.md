# Hospital Management System

A traditional **Java-only** desktop hospital-management learning project built with **Java Swing, JDBC, and MySQL**. Existing dependency and tool versions are preserved. The GUI supports patients, doctors, and a local administrator.

> **Security scope:** Authentication, account lockout, and new medical-record encryption have been added. This is still a **local educational application, not an approved system for real patient data**. Direct desktop-to-MySQL credentials, historical plaintext records, unencrypted demographic fields, and the absence of a server-side authorization boundary require additional architecture and security review before any clinical use.

## Features

- Responsive Java Swing login, role dashboards, registration forms, appointment booking, and address updates
- Patient booking with a 14-day window and MySQL uniqueness constraints against double-booking
- Doctor-scoped appointment cancellation
- Administrator patient search using a sortable Swing table, and department medicine statistics
- Nonblocking database operations via `SwingWorker`
- Password-protected patient and doctor sign-in (no identity-only fallback)
- PBKDF2-HMAC-SHA256 password hashes with **600,000 iterations**, unique random 16-byte salts, and constant-time verification
- Persistent **5 failed attempts / 15-minute** patient/doctor lockout; basic in-process administrator login throttling
- Optional **11-digit national identifier stored as text**, distinct from the existing numeric internal database key
- AES-256-GCM encryption of **new prescription and diagnosis fields** using a key supplied outside Git
- A console-only administrator authorization step for enrolling or resetting legacy accounts that do not have a current PBKDF2 password

## Retained technology

| Layer | Technology |
| --- | --- |
| Language and GUI | Java Swing, Java source compatibility `--release 11` |
| Build JDK | JDK 17 or newer |
| Database | MySQL 8.x and JDBC |
| JDBC library | MySQL Connector/J 8.2.0 |
| Maven Compiler Plugin | 3.11.0 |
| Maven Shade Plugin | 3.4.1 |
| Application version | 2.0.0 |

No React, web frontend, new UI framework, or dependency version upgrade is required.

## Setup: new local database

1. Install JDK 17+, Apache Maven, and MySQL 8.x.
2. Create a **new empty local database** using `db/schema.sql`. Do **not** run this setup script over existing populated data.
3. Create a dedicated local MySQL application user with only the privileges necessary for the demo. **Do not use the MySQL root account for normal application operation.**
4. Set the following environment variables in your terminal or operating-system secret manager:
   - `HOSPITAL_DB_URL` — JDBC URL for your MySQL `hospital` database
   - `HOSPITAL_DB_USER` — dedicated application database username
   - `HOSPITAL_DB_PASSWORD` — its password
   - `HOSPITAL_ADMIN_PASSWORD` — a distinct, strong 12+ character local administrator password
   - `HOSPITAL_DATA_KEY_BASE64` — a securely generated, **persistent** Base64-encoded 32-byte AES key for medical records
5. Keep those secrets outside the repository and preserve the medical encryption key securely. **Losing the key makes newly encrypted medical records unrecoverable.** Never reuse the synthetic key found in the GitHub Actions test fixture.

Build and start from the repository root:

```sh
mvn -DskipTests package
java -jar target/hospital-management-system-2.0.0.jar
```

On Windows, `baslat.bat` builds and starts the same Java application.

### Setting up a pre-existing database

**Do not automatically overwrite or reimport an existing database.** Back up MySQL first and verify recovery on a disposable copy. Run `db/migrations/001_secure_accounts.sql` **once**, only after checking that its columns and indexes do not already exist.

The migration leaves old integer `SSN` primary keys and appointment foreign keys intact. It adds `National_ID VARCHAR(11)`, a longer `password_hash`, and persistent account-lockout columns to both Patient and Doctor tables. Existing accounts remain inaccessible through password login until an administrator provisions them. Legacy SHA-256 passwords are not accepted and are never silently reused.

For an existing user, run the interactive account-provisioning command from a real terminal **after** setting the database and administrator environment variables:

```sh
java -cp target/hospital-management-system-2.0.0.jar db.CredentialProvisioner
```

The tool asks for the administrator password, role, existing internal numeric ID, 11-digit identifier, and a new user password. It does not take secrets as command-line arguments and does not overwrite a current PBKDF2 credential. Keep the historical database backup available while checking the migration.

## Account model and controls

- Newly registered patients and doctors receive an administrator-assigned password (12–128 characters containing letters and numbers).
- Login accepts either the **11-digit identifier** or an existing short internal account number **plus a password**. Entering just a name, surname, or numeric ID is no longer sufficient.
- National IDs are stored as strings to preserve leading zeros. Their 11-digit format is checked, but **format checking does not verify legal identity or ownership**.
- The existing integer `SSN` primary keys remain internal references. Do not insert 11-digit identifiers into those columns.
- Invalid, disabled, locked, unknown, or unprovisioned accounts fail closed. Password errors use generic messages.
- Patient and doctor account lockouts are stored in MySQL; the administrator password remains a local environment secret and administrator throttling is only in process.
- Role-separated screens use `AccessPolicy`. Because the desktop app connects directly to MySQL, these are **not independently enforceable server-side permissions**.

## Medical data encryption

New rows written through `MedicalRecordsDAO` encrypt the `Prescription` and `Diagnosis` values using AES-256-GCM with a random 12-byte nonce and 128-bit authentication tag. Encryption fails closed if `HOSPITAL_DATA_KEY_BASE64` is absent or invalid.

This **does not retroactively encrypt old records**, patient addresses, names, identifiers, or backups. Legacy unencrypted data requires a separate, backed-up migration and key-management review; it is not converted automatically. Existing unsupported ciphertext formats are rejected instead of being silently decrypted through a weak legacy cipher.

## Verification

GitHub Actions runs:

- Java compile and pure-Java security/model smoke tests
- MySQL 8 integration tests for database operations, appointments, credential authentication, account lockout, 11-digit ID preservation, and encrypted new medical records
- Xvfb-backed Java Swing GUI smoke tests for login buttons, role-based navigation, and appointment availability
- Short-lived GUI screenshots as CI artifacts

Passing these tests demonstrates the covered behavior; it is **not** a penetration test or a production-security certification.

## Known limitations and required production work

1. **Never load real patient information into this demonstration build.** A proper backend with server-side role and row-level authorization is still required; the desktop application currently holds a database connection credential.
2. Existing records and backups may still contain plaintext personal and medical information. A careful data-migration and key-rotation plan is required.
3. A complete identity-proofing, password-recovery, administrator-account, audit-logging, backup/restore and retention design must be completed before deployment.
4. MySQL network access, least-privilege accounts, TLS configuration and protected secret storage must be hardened for the target environment.
5. Medical privacy requirements (including applicable Turkish KVKK rules) require operational and legal review.
6. The project intentionally retains its legacy MySQL schema relationships and tool versions; no major architectural or dependency upgrade was performed.

This repository demonstrates the original Java Swing / JDBC / MySQL workflows while making the most dangerous legacy login and encryption paths fail closed.
