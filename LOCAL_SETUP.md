# Local setup — keep the legacy versions

This is a Java Swing + JDBC + MySQL **local demonstration**. It requires no API keys.

1. Install **JDK 17+**, Maven and local **MySQL 8.x**.
2. For a **new empty database only**, run `db/schema.sql`. Back up existing data before any schema migration.
3. Configure local database credentials in `src/db/login.properties` or environment variables `HOSPITAL_DB_URL`, `HOSPITAL_DB_USER`, `HOSPITAL_DB_PASSWORD`. The environment variables can be used even if the properties file is absent.
4. Set `HOSPITAL_ADMIN_PASSWORD` (12+ characters) for the administrator UI. Do not commit real credentials.
5. From the repository root run `mvn -DskipTests package` and `java -jar target/hospital-management-system-2.0.0.jar`. On Windows `baslat.bat` builds and starts the same jar.
6. GitHub CI tests Java compilation, pure Java smoke checks, isolated MySQL integration and the **real Swing UI under Xvfb**; the GUI workflow creates screenshot evidence retained for three days.

Retained versions: MySQL Connector/J **8.2.0**, Maven Compiler Plugin **3.11.0**, Maven Shade Plugin **3.4.1**, application version **2.0.0**, `--release 11`.

**Security/legacy boundaries:** The existing SSN fields are Java `int`, not 11-digit identity strings. The patient/doctor login only matches numeric ID + name/surname and has no password; do not use real patient data. Administrator login is disabled until a local strong password is provided. Encryption helper functions are not end-to-end protection for the entire database.
