# Historical-stack quality and release acceptance

The goal is a professionally engineered **Java Swing/JDBC/MySQL** project without a dependency upgrade, not an unverified healthcare production claim.

## Pinned stack

- [x] Java 11 source compatibility with JDK 17 CI
- [x] Java Swing GUI preserved
- [x] MySQL Connector/J 8.2.0, compiler 3.11.0, Shade 3.4.1, project 2.0.0 preserved
- [x] No additional UI/runtime framework or dependency
- [x] CI guard rejects unapproved version changes

## Security architecture and controlled data access

- [x] Trusted Java HTTPS service (JDK only)
- [x] Deny-by-default, role-limited operations verified on real HTTPS requests
- [x] Patient and doctor record scoping derived from authenticated server sessions
- [x] Client direct-JDBC access denied outside explicit synthetic CI mode
- [x] Client logout revokes bearer token
- [x] CI non-root DB account cannot write/erase audit ledger
- [x] Persistent account lockout and password hash checks
- [x] Local workstation idle lock
- [x] MySQL uniqueness and atomic appointment constraints

## Data preservation and restore

- [x] AES-GCM new clinical and address fields
- [x] Versioned medical keyring and old-key reading while migrating
- [x] SQL backup encryption using an independent AES-GCM key
- [x] Tamper rejection without publishing a bad restore
- [x] Real secondary MySQL schema restore checked for row counts, triggers and decryption
- [x] Guarded, repeatable data backfill; rerun makes no changes
- [x] Synthetic medical key rotation checked end to end
- [ ] Historical names, birth dates and national identifiers still need privacy/storage review
- [ ] Real database backup/restore and migration in the target environment not performed

## Clinical deployment blockers

- [ ] Managed hardened service/DB hosts, firewall restrictions and least-privileged credentials verified in target environment
- [ ] CA-issued TLS certificate, truststore deployment and certificate renewal automated
- [ ] Actual user identity proofing, secure password recovery, incident workflow and actor-attributed audit
- [ ] Complete sensitive-field data minimization/encryption and retention requirements
- [ ] Real encrypted-backup retention, escrow, rotation and disaster recovery exercise
- [ ] Security penetration testing, access control review, usability/accessibility and medical regulatory assessment
- [ ] Sign-off by responsible product, security, operations and privacy owners

CI passing proves covered functionality and security constraints in the **synthetic fixture**, not regulatory or clinical production readiness. Do not close blockers without supporting deployment evidence.
