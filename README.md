# Hospital Management System

**Professional legacy Java engineering** · Java Swing · JDBC · MySQL · Maven

A classic, desktop-first hospital database management application. Its technology choices are intentional: Java Swing for the user interface, Java 11 source compatibility, MySQL 8.x for relational storage, and the existing Maven/JDBC dependency versions. The focus is on testable workflows, security boundaries, recovery and maintainable engineering — **not replacing the stack with newer frameworks**.

> **Status:** Advanced historical-stack engineering demo with a separately deployable trusted Java HTTPS service. Comprehensive synthetic-data CI exists. **Not certified for real clinical or personal data**: demographic fields and legacy backups still need a reviewed production privacy plan, dedicated deployment hardening, user identity proofing, independent penetration testing and operational sign-off.

## Architecture

```text
[Java Swing desktop]  HTTPS + validated TLS certificate
          |                    |
          +------------------->|
                               v
                     [Trusted Java 11 gateway]
                      sessions / RBAC / row scope
                               |
                         JDBC (server only)
                               |
                        [MySQL 8.x]
              constraints / audit triggers / data
```

The distributed Swing client never needs a MySQL password or a medical encryption key. **Direct JDBC is blocked in normal desktop runtime**. Server operations use the identity stored in authenticated server sessions; patient and doctor record identifiers from the client cannot override that identity. The network gateway is written entirely with the Java 11 JDK HTTPS and HTTP libraries.

## Features

- Patient, doctor and administrator accounts with PBKDF2-HMAC-SHA256 passwords (600,000 iterations), salts, persistent failed-attempt counters and lockouts
- A responsive Swing UI with nonblocking database requests, accessible input names and role-specific dashboards
- Patient booking and upcoming appointment lists; doctor schedule and doctor-scoped cancellation
- Transactional MySQL appointment uniqueness and database audit triggers
- Administrator account registration, patient search and department statistics
- **Server-only** database credentials; fail-closed role and row-scoped HTTPS request handling
- AES-256-GCM encryption of new clinical prose and addresses
- Versioned, owner-restricted on-host keyring, plus repeatable historical-data backfill and key rotation
- Independent AES-GCM encrypted backups, verified restoration and tamper detection
- CI coverage for Java, MySQL, Swing, TLS, access control, cross-user isolation, recovery and rotation

## Intentionally unchanged technologies

| Component | Retained version/technology |
| --- | --- |
| Primary language and interface | Java Swing, Java source release 11 |
| Build JDK | 17 in GitHub CI |
| Database | MySQL 8.x, InnoDB |
| JDBC driver | MySQL Connector/J 8.2.0 |
| Maven Compiler Plugin | 3.11.0 |
| Maven Shade Plugin | 3.4.1 |
| Project version | 2.0.0 |

No Spring Boot, JavaScript UI, React or new runtime dependency is used. Changes to versions or frameworks require the project owner's explicit approval.

## Getting started with synthetic data

Build using the existing, pinned Maven configuration:

```sh
mvn -B -DskipTests package
```

1. On an isolated MySQL 8 host, create a **new empty** `hospital` database with `db/schema.sql`. Never run the fresh schema over existing medical records.
2. Provision a separate limited-privilege gateway database identity. The CI-only template `db/ci/least_privilege.sql` uses synthetic credentials; **never reuse them outside CI**.
3. On the trusted Java service host, configure JDBC credentials, a valid TLS PKCS12 server certificate, a strong server-side administrator secret and protected medical keyring.
4. Start the trusted gateway using `java -cp target/hospital-management-system-2.0.0.jar db.GatewayServer`.
5. On the client computer, configure only `HOSPITAL_GATEWAY_URL=https://your-verified-host:8443/api` and the Java truststore containing the server's trusted issuer. Launch with `java -jar target/hospital-management-system-2.0.0.jar`.

**Never distribute `HOSPITAL_DB_PASSWORD`, `HOSPITAL_ADMIN_PASSWORD`, `HOSPITAL_DATA_KEYRING_DIR`, or any key files with the Swing client.** Remote gateway binding must be explicitly authorized and protected by firewall rules and certificate validation.

For exact environment variables, certificate guidance and safe commands, read [Operations & Recovery](docs/OPERATIONS.md).

## Existing data and encryption migration

A pre-existing database must be backed up and **restored successfully to an isolated copy before any migration**.

- Apply `db/migrations/001_secure_accounts.sql`, then `002_audit_events.sql`, then `003_private_addresses.sql` **only if those changes are absent**, after comparing the schema. These migrations are not automatic or generally idempotent.
- Perform `db.LegacyDataMigrator --scan` first.
- Only on the trusted database host, use `--apply` with a verified backup file + SHA-256 digest, a manually verified restore approval, and a valid versioned encryption keyring.
- Previously encrypted clinical data can be re-encrypted under a new key ID without changing dependency versions. Retain old keys while backups require them.
- `db.BackupVault` encrypts and decrypts SQL backup artifacts with an **independent** AES-256-GCM key and never publishes a failed-authentication restore.
- `db.KeyFileGenerator` creates new random owner-only key files without printing key material.

**Migration is never executed on desktop application startup.** A bad or ambiguous record stops the operation instead of silently deleting data.

## Test and release evidence

The GitHub Actions workflows run a disposable MySQL 8 instance, provision a separate non-root gateway database account, start a real JDK HTTPS server with a synthetic certificate, verify role and cross-user access behavior, and test Swing GUI flows. Backup tests restore MySQL to a second schema, compare counts and audit triggers, reject tampered archives, migrate and rotate synthetic encryption keys.

The version guard in `ci/verify-legacy-stack.sh` rejects accidental dependency or framework upgrades.

**Passing CI is not a substitute for a completed hospital deployment, full privacy compliance, a vulnerability assessment or medical safety certification.** Detailed release blockers are tracked in [Release Checklist](docs/RELEASE_CHECKLIST.md).

## Documentation

- [System architecture](docs/ARCHITECTURE.md)
- [Security and outstanding risks](SECURITY.md)
- [Operations, migration, backup and restore](docs/OPERATIONS.md)
- [Quality and production release checklist](docs/RELEASE_CHECKLIST.md)

This project demonstrates that thoughtful architecture, data integrity and defensive engineering are possible with classic Java technologies.
