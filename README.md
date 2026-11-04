# Hospital Management System (Java Swing)

A traditional **Java-only desktop learning project** for hospital record and appointment workflows. It uses Swing for the graphical interface, JDBC for database access, and MySQL. Existing project dependency versions are intentionally retained.

> **Local demonstration only. Do not use real patient or medical data.** Patient/doctor login still matches legacy numeric ID, name and surname without a password. The local administrator password is provided through an environment variable. This is not production-grade authentication or medical-data protection.

## Working screens

- Patient, doctor and local administrator login tabs with responsive Swing layouts
- Patient appointment booking for the next 14 days with conflict rejection in MySQL
- Doctor appointment viewing and doctor-scoped cancellation
- Patient and doctor address updates
- Administrator registration of patient and doctor records
- Administrator patient search with a sortable JTable
- Department-based medicine statistics
- Asynchronous JDBC interactions so long-running operations do not freeze the Swing UI

## Existing stack — no upgrades

| Layer | Existing technology |
| --- | --- |
| Application | Java Swing; source compiled with `--release 11` |
| Database | MySQL 8.x via JDBC |
| Connector | MySQL Connector/J **8.2.0** |
| Build | Maven Compiler Plugin **3.11.0**, Maven Shade Plugin **3.4.1** |
| Application version | **2.0.0** |

Use **JDK 17 or later** and Apache Maven to build using the project's current pom.xml. The application source code remains Java; there is no web UI or separate backend server.

## Local run

1. Clone this repository and open a terminal at the project root.
2. Install and start MySQL 8.x. Apply `db/schema.sql` **only to a new, empty local development database**; do not reimport it over existing data.
3. Configure local credentials in `src/db/login.properties` or the environment variables `HOSPITAL_DB_URL`, `HOSPITAL_DB_USER` and `HOSPITAL_DB_PASSWORD`. Override the sample credentials before use.
4. Set `HOSPITAL_ADMIN_PASSWORD` to a strong **12+ character** password for the administrator screen. No password is hardcoded in Java.
5. Build and launch:

```sh
mvn -DskipTests package
java -jar target/hospital-management-system-2.0.0.jar
```

On Windows, use `baslat.bat` from the project directory. If MySQL is unreachable, configuration is missing, or the database is empty, login screens will show a failure rather than fabricating records.

## Verification

The GitHub Actions workflows verify **Java 11 source compatibility** with JDK 17, a dependency-free model/security smoke test, an isolated MySQL 8 integration test (CRUD and concurrent booking), and an Xvfb-backed Swing GUI smoke test. The GUI test exercises patient and doctor login buttons, appointment availability, screen navigation, and creates short-retention screenshot artifacts.

## Known limitations

- The legacy `SSN` database column is **INT** (positive 32-bit IDs). It cannot hold 11-digit Turkish identity numbers. Changing that would require a separately planned schema/data migration.
- Patient and doctor identity lookup is **not password authentication**. Do not deploy for real users without a coordinated authentication and authorization redesign.
- The legacy SQL medicine statistics include an academic barcode-sum metric, not clinical inventory valuations.
- The UI smoke checks important workflows, but does not establish that every possible interaction works on every desktop environment.

The original DAO learning project is preserved; no external tutorial or third-party repository code was copied into this repair.
