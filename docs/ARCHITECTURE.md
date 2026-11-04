# Architecture — preserved Java desktop stack

## Technical identity

Hospital Management System is a **Java Swing desktop application**, not a React website or microservice. The Java 11 source compatibility, Maven 3.11.0 compiler plugin, Maven Shade 3.4.1, MySQL Connector/J 8.2.0 and MySQL 8.x remain unchanged. JDK 17 is used in CI.

```text
Patient / Doctor / Admin
          |
   Swing panels (EDT)
          |
   MainFrame + AccessPolicy  [UI navigation only]
          |
  SwingWorker (background DB calls)
          |
 Java DAO / SecureAccounts
          |
    JDBC / MySQL 8.x
          |
InnoDB constraints + DB triggers
```

### Boundaries

1. **UI:** Swing widgets, accessible input labels, sortable tables, role navigation, confirmations and nonblocking workers.
2. **Application code:** credential checks, prepared statements, appointment validation and GCM encryption for newly created clinical text.
3. **MySQL:** foreign keys, exclusive doctor/patient time slots, transaction semantics, account lockout fields and metadata audit triggers.
4. **Operational control:** database access restricted to a loopback host by default. Remote access is explicit and requires `sslMode=VERIFY_IDENTITY`.

**Critical security limitation:** The executable contains a shared database connection, so a user who extracts those credentials can bypass Java UI checks. Neither Swing role controls nor audit triggers solve row-level authorization. A trusted Java service tier (still possible with the legacy language/toolchain, subject to a separate architecture decision), distinct database identities and additional controls are prerequisites for real multi-user clinical use. No such service is claimed to exist today.

### Integrity constraints

- Two appointments cannot occupy the same date/time for the same doctor.
- A patient cannot be double-booked for the same date/time.
- Doctor cancellations are scoped to that doctor, reject past/current completed appointments and reject appointments with linked clinical records.
- The legacy unvalidated booking and unscoped cancellation methods throw errors.
- New appointment and clinical-record writes are audited by MySQL triggers; audits record **event type and internal record number**, never medical prose.
- Authentication uses PBKDF2-HMAC-SHA256; new clinical prose uses AES-256-GCM.
- National identifiers are separate string fields; existing integer foreign keys remain unchanged.

### Change policy

**No upgrades without owner approval.** Never change `pom.xml` versions or introduce another framework as a cosmetic modernization. CI includes an automated legacy stack guard. Security/quality changes are performed within existing Java, JDBC and MySQL interfaces, with tests and explicit migration plans.
