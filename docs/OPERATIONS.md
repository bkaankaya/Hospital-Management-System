# Local operations and recovery runbook

This describes an **isolated educational Java Swing demonstration**, not hospital deployment.

## Prepare environment

Use JDK 17+, Maven, local MySQL 8.x. Preserve the versions currently pinned in `pom.xml`. Run the database service only on loopback for the desktop demo; block external MySQL exposure in the firewall.

Supply secrets through protected environment variables rather than committed files:

- `HOSPITAL_DB_URL` — example local URL `jdbc:mysql://127.0.0.1:3306/hospital`
- `HOSPITAL_DB_USER` and `HOSPITAL_DB_PASSWORD` — dedicated demo database credentials, not root
- `HOSPITAL_ADMIN_PASSWORD` — local administrator secret of at least 12 characters
- `HOSPITAL_DATA_KEY_BASE64` — a persistent random **32-byte** key encoded as Base64

For a **new, empty** `hospital` database, run `db/schema.sql` with an administrative MySQL account. For existing installations, back up first, then review `001_secure_accounts.sql` followed by `002_audit_events.sql`. Do not reapply a non-idempotent migration; inspect columns, triggers and indexes before running it.

**The desktop database account must never be granted TRIGGER, CREATE USER, SUPER, DROP, GRANT OPTION or modification access to `Audit_Event`.** A trusted database administrator applies the migrations and owns the triggers. Normal table-level permissions must be restricted to only the operations the demo needs. Since this is a shared client account, such permissions do not implement per-person authorization.

## Build and execute

```sh
mvn -B -DskipTests package
java -jar target/hospital-management-system-2.0.0.jar
```

For Windows, use `baslat.bat`. The JDBC application will refuse remote endpoints by default. If remote access is ever approved, `HOSPITAL_ALLOW_REMOTE_DB=true` and `sslMode=VERIFY_IDENTITY` are required, together with trusted CA certificates. Never use `trustServerCertificate=true` or `allowPublicKeyRetrieval=true` with a remote server.

## Operational checks

1. Confirm app starts with no default or embedded production credentials.
2. Register *synthetic* patients and doctors with secure initial passwords.
3. Verify successful and failed logins, lockout, role navigation and logout.
4. Book a slot; confirm no double-booking; cancel as the correct doctor.
5. Ensure linked clinical records cannot be cancelled out from under the record.
6. Inspect `Audit_Event` entries for appointment and clinical write events.
7. Confirm encrypted new prescription fields are not plaintext in SQL results.
8. Check GitHub Java smoke and MySQL + GUI workflows against the exact commit.

## Backup and restore drill

Take backups while connected as a privileged **backup-only** database account, not the application user. Example on an isolated demo environment:

```sh
mysqldump -u hospital_backup -p --single-transaction --routines --triggers --events hospital > hospital-demo-backup.sql
```

Restrict backup file permissions and encrypt the exported archive outside Git; plaintext backups may contain identifiers and legacy clinical text. Restore into a **fresh isolated database**, verify row counts, trigger presence, foreign keys, booking constraints, and the ability to decrypt a synthetic GCM fixture with the original key. Do not overwrite the original database for a test restore.

Keep the medical data key separate from the database backup. Loss of the key means encrypted medical text cannot be recovered; possession of both backup and key can expose the data.

## Incident handling

On suspected compromise, disconnect database access, secure logs and backups, rotate exposed MySQL/admin credentials and encryption keys according to a planned migration, and investigate the affected records. Audit metadata does not currently prove which specific person made a change.

## Release gate

Use `docs/RELEASE_CHECKLIST.md` for the explicit local-demo checklist and remaining production blockers. No release should be called medically production-ready while those blockers remain.
