# Local setup (existing dependency versions preserved)

This project is a Java Swing desktop application backed by MySQL. It does not use any API keys.

1. Install JDK 17+ and Apache Maven. Install and start a local MySQL 8.x server.
2. For a **new empty database only**, apply `db/schema.sql` using your MySQL client. Do not run a schema import over existing production data.
3. Configure your own local database URL, username and password in `src/db/login.properties`. Do not commit actual credentials. The default code reads this file relative to the repository root.
4. From the repository root run `mvn -DskipTests package` and then `java -jar target/hospital-management-system-2.0.0.jar`. Windows: run `baslat.bat`.
5. The CI smoke suite checks Java compilation and pure Java model/security helpers. It does **not** verify MySQL integration or a graphical end-to-end workflow.

Existing versions stay pinned to the values in the repository's `pom.xml.bak`: MySQL Connector/J 8.2.0, Maven Compiler Plugin 3.11.0, Maven Shade Plugin 3.4.1, application 2.0.0.

**Known limitations:** The legacy SSN fields are Java `int`, so 11-digit identifiers are not supported by the old forms; they need an explicit coordinated migration before use. The legacy login accepts identity fields without a password. Use only synthetic/local development data until authentication and full database tests are completed.

For the administrator insert menu, configure a local `HOSPITAL_ADMIN_PASSWORD` environment variable with 12+ characters before launching. This password is not an API key, is never stored in GitHub, and has no default value. Patient and doctor login continue to use the legacy local identity fields.
