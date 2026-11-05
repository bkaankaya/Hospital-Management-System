# Security policy

## Intended scope

This project demonstrates mature defensive design using retained historical Java technologies. It must not be used with real patient, identity or medical information until an independently reviewed production deployment exists.

## Implemented and tested

- Java 11 HTTPS gateway; remote requests require valid TLS certificate trust
- Server-side allowlisted permissions and session-owned record boundaries
- Cryptographically random bearer tokens (server stores digests), idle expiry and logout invalidation
- Account status revalidation per request
- PBKDF2-HMAC-SHA256 password hashing, salt, login attempt lockout
- Server-only JDBC access by default; test-only legacy DAO mode separately gated
- Non-root MySQL identity in CI; absence of audit-ledger write permissions verified
- Prepared statements, MySQL uniqueness constraints, doctor-scoped cancellation
- AES-256-GCM for new medical prose and address fields
- Versioned keyring, re-encryption migration and checksum-guarded backup prerequisites
- Separate backup encryption key, authenticated restore, tamper rejection
- Tests for TLS, roles, same-role row separation, GUI, concurrent bookings, restore and key rotation

## Deployment-critical remaining risks

1. **Real-world deployment has not been performed or independently certified.** Secure host separation, firewall ingress, TLS certificate issuance and renewal, strict MySQL grants, OS privileges, monitoring and incident response are operational responsibilities.
2. **Not all fields are application-encrypted.** Names, birth dates and national ID fields remain relational columns; historical SQL exports might also contain plaintext. Full privacy controls require a reviewed storage and data minimization plan.
3. **User identity proofing and account recovery** need a formal secure workflow; local administrator provisioning alone is not enough for clinical use.
4. **Monitoring and audit attribution:** the database audit trigger captures record events, not a fully tamper-evident timeline of specific human operators. Centralized audit storage and alerting are required.
5. **Independent verification:** a professional penetration test, backup recovery drills on the deployment environment, disaster recovery objectives and local data protection (e.g., KVKK) review have not occurred.
6. **Environment and keys:** the JDK keyring files require restrictive OS permissions (POSIX owner-only enforced where available; Windows NTFS ACLs must be configured by the operator). Keep decryption keys out of clients, Git and general backups.

## Supported practice

The default desktop process refuses JDBC access. A separate `GatewayServer` holds DB credentials. Only supported user operations are exposed via `GatewayPolicy`. Do not open port 3306 to client devices. Never run as the MySQL root account.

**Do not run the test-only system property `hospital.ci.legacyFixtures` in real deployment.** This switch exists only to preserve independent legacy integration fixtures. The service must run with a dedicated, least-privileged DB identity and genuine TLS certificate verification.

## Key handling

- Use `db.KeyFileGenerator` to create random 256-bit medical and backup key files.
- Give each medical key a short unique ID, record the active ID only on the trusted service host, and secure keyring backups independently.
- Retain old keys until all data/backups are re-encrypted and old snapshots have expired.
- Never commit keys, dumps, export files, private PKCS12 keystores or credentials. Rotate any exposed historical secrets.
- A backup is not trustworthy until a separate restoration verifies consistency, referential integrity, audit triggers and decryption.

## Security issue reporting

Do not open public issues containing clinical records, credentials, private keys or exploit demonstrations. Prefer private security advisories or a private maintainer communication channel.
