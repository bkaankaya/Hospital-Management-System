# Trusted Java gateway operations and recovery

**Scope:** An isolated demonstration or controlled engineering staging environment. No clinical production sign-off.

## 1. Version and host separation

Use the pinned Maven configuration, JDK 17+ to build, MySQL 8.x and Java 11-compatible application bytecode. No framework or dependency upgrade is required.

The Swing desktop and the trusted Java service are **separate processes**. A real deployment should place the gateway on a managed host whose MySQL access is restricted by firewall to that host. A desktop must receive **only** `HOSPITAL_GATEWAY_URL` and public TLS trust anchors. Never copy JDBC/admin/medical-key settings to end users.

## 2. New MySQL schema

Apply `db/schema.sql` with a database administrator **only on a new/empty database**. Establish a dedicated MySQL gateway user with only the required operations on Patient, Doctor, Appointment, Department, Room, Nurse, medicine, Medical_Records and the `Add` association table. Grant SELECT but **no INSERT/UPDATE/DELETE** on `Audit_Event`. The admin-owned MySQL triggers must be installed with an account permitted to create triggers. The example under `db/ci/least_privilege.sql` is restricted to disposable GitHub CI and contains synthetic credentials — never copy those credentials into real deployment.

For an existing schema: restore a separate backup first, inspect existing columns/keys/triggers, and apply `001_secure_accounts.sql`, `002_audit_events.sql`, `003_private_addresses.sql` in that order **only if they are not yet applied**. They are not safely repeatable migrations.

## 3. Create keys and certificates

On the trusted server, create a protected key directory (POSIX: owner-only `0700`, files `0600`; Windows: restrict NTFS ACLs to the service identity).

Use Java's CSPRNG-based key generator:

```sh
java -cp target/hospital-management-system-2.0.0.jar db.KeyFileGenerator /secure/hospital-keys/medical01.key
java -cp target/hospital-management-system-2.0.0.jar db.KeyFileGenerator /secure/backup/backup.key
```

The key-file generator requires an owner-only POSIX filesystem and deliberately refuses to create keys on filesystems where private creation permissions cannot be guaranteed. For a Windows **server**, provision equivalent key files using an approved Windows key-management procedure and verify restrictive NTFS ACLs before starting the gateway; clients do not need these keys.

These keys must be different, unique, recoverably backed up and inaccessible to desktop clients. Generate a PKCS12 TLS server keystore with the matching hostname/SAN via an approved certificate authority and store its password securely. Do not use self-signed CI certificates for real users.

## 4. Run Java gateway

Export protected settings **on the trusted host only**:

| Variable | Purpose |
| --- | --- |
| `HOSPITAL_DB_URL` | MySQL JDBC URL to gateway-accessible DB |
| `HOSPITAL_DB_USER` / `HOSPITAL_DB_PASSWORD` | Non-root, least-privileged gateway DB credentials |
| `HOSPITAL_ADMIN_PASSWORD` | Separate, strong administrator secret |
| `HOSPITAL_DATA_KEYRING_DIR` | Restricted key directory |
| `HOSPITAL_DATA_ACTIVE_KEY_ID` | Newest encryption key ID, e.g. `medical01` |
| `HOSPITAL_SERVER_KEYSTORE` | Valid PKCS12 TLS server certificate/key file |
| `HOSPITAL_SERVER_KEYSTORE_PASSWORD` | Keystore password |
| `HOSPITAL_SERVER_BIND` / `HOSPITAL_SERVER_PORT` | Defaults to `127.0.0.1` / `8443` |
| `HOSPITAL_SERVER_ALLOW_REMOTE` | Explicitly `true` only when remote binding has been firewall-restricted and approved |

```sh
java -cp target/hospital-management-system-2.0.0.jar db.GatewayServer
```

Server startup refuses MySQL root credentials, missing protected medical key configuration, unavailable database, or missing TLS certificate. Remote MySQL access requires explicit opt-in and `sslMode=VERIFY_IDENTITY`.

## 5. Run Swing desktop

Configure `HOSPITAL_GATEWAY_URL=https://<trusted-server-name>:8443/api` and a **trusted certificate** in the client's Java truststore. Never disable hostname validation. The desktop needs no DB or encryption secret.

```sh
java -jar target/hospital-management-system-2.0.0.jar
```

The application must fail closed if the gateway cannot be reached. Sessions expire on inactivity and are revoked on logout. Old direct-DAO mode is for isolated GitHub fixtures only.

## 6. Backup, restoration and historical data

Create an SQL export with a privileged backup-only DB identity. Immediately convert it to a protected authenticated archive using a **separate** backup key (not the medical encryption key):

```sh
java -cp target/hospital-management-system-2.0.0.jar db.BackupVault encrypt backup.sql backup.hbak
java -cp target/hospital-management-system-2.0.0.jar db.BackupVault decrypt backup.hbak /restricted/restore/backup.sql
```

The decrypt command writes only to a new output path and does not publish output if the GCM authentication tag fails. Manage temporary plaintext and key files carefully; deletion from SSDs does not guarantee forensic erasure.

**Mandatory recovery drill:** Restore into a second isolated schema, compare record counts, inspect foreign keys/triggers and verify that a synthetic record decrypts using the retained old key. Retain an off-host protected copy of the archive and independent escrow for the correct keys.

For migrations, first run a scan:

```sh
java -cp target/hospital-management-system-2.0.0.jar db.LegacyDataMigrator --scan
```

For a deliberately approved migration/rotation, configure `HOSPITAL_MIGRATION_BACKUP_FILE`, `HOSPITAL_MIGRATION_BACKUP_SHA256` (checksum of the actual backup artifact), `HOSPITAL_MIGRATION_APPROVAL=YES_BACKUP_RESTORED`, `HOSPITAL_DATA_KEYRING_DIR` and `HOSPITAL_DATA_ACTIVE_KEY_ID` on the trusted server. Keep `HOSPITAL_DATA_KEY_BASE64` temporarily **on the trusted server only** if old `enc:v1` fields require it. Then run:

```sh
java -cp target/hospital-management-system-2.0.0.jar db.LegacyDataMigrator --apply
```

Migration is batched, uses transactional optimistic updates, is repeatable, and refuses ambiguous addresses/unknown ciphertext formats. It converts historical prescriptions, diagnoses and Patient/Doctor/Nurse address fields, **not all demographic columns**. It never runs on client startup.

For key rotation, add a new owner-restricted `<new-id>.key` to the server-only directory, retain the old keys, set `HOSPITAL_DATA_ACTIVE_KEY_ID` to the new ID and repeat a backed-up, verified `--apply`. Keep old keys available for retired backups until retention ends.

## 7. Acceptance, recovery and incident response

Require green Java smoke, MySQL, real TLS/role, GUI, restored backup, key-rotation and tamper-rejection checks at the exact commit. Maintain an incident plan, backup retention schedule, TLS certificate renewal, OS-level access audit, key recovery tests and independent penetration testing.

If compromised, isolate the gateway/DB, preserve available audit evidence and rotate credentials/keys using an approved migration. Real patient data cannot be used until the production blockers in `SECURITY.md` are closed.
