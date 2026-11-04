# Security policy and threat model

## Scope

This public repository is an educational desktop prototype with enhanced defensive controls. **It is not certified or approved to store real clinical or identifying data.** Use synthetic patient records and development-only credentials.

## Security controls currently implemented

- PBKDF2-HMAC-SHA256 (600,000 rounds, random salts); stored password verification only
- Password-protected patient/doctor sign-in, fail-closed unprovisioned accounts
- Persistent failed-login counters and a 15-minute lockout; basic in-process admin throttle
- Prepared JDBC statements for active booking, identity verification and schedule queries
- MySQL unique slot constraints, scoped cancellations and rejection of linked clinical-record cancellation
- AES-GCM encryption of **new** prescription/diagnosis text using a separately supplied persistent key
- JDBC loopback-only defaults; optional remote connection requires explicit enablement and verified server TLS identity
- Database-level audit metadata for critical clinical and scheduling writes
- Short-retention test screenshots and isolated MySQL CI fixtures

## Residual risk — deployment blockers

| Severity | Exposure | Acceptance requirement |
| --- | --- | --- |
| BLOCKER | Shared direct MySQL credentials in distributed desktop clients | Trusted server authorization boundary, individual user session checks and database least privilege |
| BLOCKER | Historical records, addresses, names and identifiers are not all encrypted | Key-management and backfilled data protection with verified restore |
| BLOCKER | Full clinical security review and applicable privacy requirements incomplete | Threat model review, independent penetration test, privacy/data-retention sign-off |
| HIGH | Administrator secret supplied via environment variable | Dedicated administrator identity and secure deployment-bound secret storage |
| HIGH | Audit metadata identifies database records, not individual authenticated actors | Server-side tamper-evident actor attribution and retention controls |
| HIGH | Database backup, key rotation and disaster recovery not independently exercised | Documented, tested RPO/RTO, restore drills, monitoring and incident procedures |
| MEDIUM | Account lockout can be used to temporarily deny a victim access | Abuse monitoring, support-guided unlock and policy review |

**No production-readiness claim is made until all BLOCKER items are resolved.** For an actual hospital application, the architecture and controls must undergo independent design review.

## Secret hygiene

Never commit `.env` files, real `login.properties`, access tokens, database backups, patient exports, medical-data keys or credential screenshots. Configuration is supplied locally through `HOSPITAL_DB_*`, `HOSPITAL_ADMIN_PASSWORD` and `HOSPITAL_DATA_KEY_BASE64`. A synthetic key in CI is **not** a deployment key.

If a secret has appeared in Git history, revoke/rotate it. Deleting it from the latest commit is insufficient because previous commits remain accessible.

## Reporting vulnerabilities

Avoid posting real patient records, secrets, or proof-of-concept exploitation details in a public GitHub issue. Use the repository's private vulnerability reporting mechanism if available; otherwise request a private reporting channel without sharing sensitive details publicly.
