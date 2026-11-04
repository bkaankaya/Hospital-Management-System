# Legacy Java product quality and release gates

This checklist is for a **polished local educational desktop release**. It is *not* approval for clinical use.

## Existing stack (must remain pinned)

- [x] Java Swing UI, no web frontend
- [x] Java 11 source compatibility, JDK 17 test compiler
- [x] MySQL Connector/J 8.2.0
- [x] Maven Compiler Plugin 3.11.0, Shade Plugin 3.4.1
- [x] Maven project version 2.0.0
- [x] No extra application dependency introduced
- [x] CI guard against unapproved technology changes

## Safety and functional implementation

- [x] Credential-only patient/doctor authentication
- [x] Server-database persistent account lockout fields
- [x] GUI cross-role navigation guard (not a trusted backend boundary)
- [x] Database-level conflict rejection for appointments
- [x] Patient upcoming appointment view and doctor schedule view
- [x] Appointment cancellation authorization by doctor ID at DAO level
- [x] Prevent cancel of past or medical-record-linked appointment
- [x] Authenticated encryption of new clinical notes
- [x] Operational metadata audit triggers and migration script
- [x] Loopback-only default JDBC; verified TLS for explicitly enabled remote endpoints
- [ ] Dedicated per-user server-side authorization boundary
- [ ] Independent security penetration test and clinical/privacy legal review
- [ ] Full encryption/migration of historical and demographic data
- [ ] Tested database restoration, key rotation, audit actor identity and alerting
- [ ] Real clinical workflow acceptance and accessibility audit across operating systems

## CI gate

On each push to `main`, verify:
- The legacy dependency version guard passes.
- Java models/crypto/authorization smoke tests pass.
- MySQL integration, audit, lockout, schedule and concurrent booking checks pass.
- Swing GUI button and navigation tests pass with Xvfb.

A checkmark describes the **implementation status**, not an independent certification. Record the exact commit and Actions URL before publishing a build. Stop if a required test is queued, skipped, or failed.
