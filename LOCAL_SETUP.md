# Local setup and safe migration

See `README.md` for all features, version pins and security limitations.

1. Install JDK 17+, Apache Maven and MySQL 8.x.
2. New database: use `db/schema.sql` only on a **new, empty** local database.
3. Existing database: back up, verify restore and apply `db/migrations/001_secure_accounts.sql` ONCE after reviewing the schema.
4. Configure `HOSPITAL_DB_URL`, `HOSPITAL_DB_USER`, `HOSPITAL_DB_PASSWORD`, `HOSPITAL_ADMIN_PASSWORD`, and persistent `HOSPITAL_DATA_KEY_BASE64` outside Git.
5. Build with `mvn -DskipTests package`; start with `java -jar target/hospital-management-system-2.0.0.jar` or Windows `baslat.bat`.
6. Existing accounts cannot log in with name + ID. Provision a password interactively: `java -cp target/hospital-management-system-2.0.0.jar db.CredentialProvisioner`.
7. Use synthetic records only. Direct desktop-to-MySQL authorization and older unencrypted records prevent production clinical use.

Pinned dependencies: MySQL Connector/J 8.2.0, Maven Compiler Plugin 3.11.0, Maven Shade Plugin 3.4.1; release 11 bytecode; application 2.0.0.
