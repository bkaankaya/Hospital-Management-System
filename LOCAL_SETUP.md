# Local build / gateway setup

This project intentionally preserves Java Swing + Java 11 source compatibility + JDBC + MySQL 8.x. There is **no dependency version upgrade**.

Build with `mvn -B -DskipTests package`. The Swing desktop is now an HTTPS client; do not pass JDBC credentials to it. Configure only `HOSPITAL_GATEWAY_URL` plus certificate trust and run `java -jar target/hospital-management-system-2.0.0.jar`.

The trusted Java gateway is a **separate process** started with `java -cp target/hospital-management-system-2.0.0.jar db.GatewayServer`, using server-only MySQL credentials, TLS PKCS12 certificate, administrator secret and medical keyring. The default gateway binds only to loopback.

For a new demo database use `db/schema.sql` once. For existing data, back up and restore first; review `db/migrations/001_secure_accounts.sql`, `002_audit_events.sql`, `003_private_addresses.sql`, then use `db.LegacyDataMigrator --scan` and explicitly approved `--apply`. The `db.BackupVault` utility protects SQL exports using an independently generated backup key.

**Do not use real patient data.** Consult `docs/OPERATIONS.md`, `SECURITY.md` and `docs/RELEASE_CHECKLIST.md` for complete commands, environment variables and remaining production blockers.
