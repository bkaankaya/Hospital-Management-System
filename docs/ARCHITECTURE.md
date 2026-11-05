# Legacy Java architecture and trust boundaries

## Version-pinned design

The application remains Java Swing + JDBC + MySQL. The Java 11 JDK's built-in `HttpsServer` and `HttpClient` provide a separate trusted process without adding dependencies or replacing the desktop UI.

```text
UNTRUSTED DESKTOP                    TRUSTED SERVICE HOST                 DATABASE
Swing UI [no SQL secrets] ---TLS---> GatewayServer [sessions + roles] ---> MySQL 8
GatewayClient/GatewayBridge           explicit API allowlist               row data
short-lived bearer token              owner-only encryption key files      audit triggers
```

**Credential separation is mandatory at deployment.** The Swing client only receives `HOSPITAL_GATEWAY_URL` and the public trust root for HTTPS. It must not receive MySQL login details, administrator secrets, medical keyring access or backup encryption keys. Database network ACLs must only accept connections from the trusted service host.

### Core authorization design

- No unrecognized operation is permitted (`GatewayPolicy` is deny-by-default).
- Every API request requires a server-issued, cryptographically random bearer token, except for login.
- The server stores only token digests, expires idle sessions after 15 minutes and imposes a maximum 8-hour lifetime.
- Every request rechecks that the patient or doctor account remains active.
- Row IDs for scheduling, cancellations and address updates are derived from the authenticated server session, **not client-provided patient/doctor IDs**.
- Gateway registration and administrator search actions are restricted to administrator sessions.
- MySQL grants give the gateway access to required tables but not direct modification of `Audit_Event`. Audit history is written by administrator-owned triggers.
- The UI role guard remains useful for navigation but is never the trusted access decision.

This is a **restricted custom binary HTTPS protocol**, not public JSON REST. The server limits payload sizes, validates request shapes, returns generic errors and avoids logging patient values. TLS must verify the actual hostname; never install an accept-all trust manager.

### Java-only CI mode

The isolated fixture mode `-Dhospital.ci.legacyFixtures=true` allows old direct-DAO testing without needing the gateway in every JVM. It is strictly for GitHub's disposable test environment. Do not use it to launch a distributed desktop, and do not distribute database credentials with the client. CI also starts a real HTTPS gateway and tests authorization with the fixture mode disabled.

### Data protection

- New prescriptions, diagnoses and addresses use AES-GCM.
- A versioned `enc:v2:<key-id>:<ciphertext>` envelope identifies the encryption key; owners retain older keys until all data and backups have been migrated.
- Old `enc:v1` clinical text requires the legacy encryption key during migration and cannot be decrypted without it.
- The separate `BackupVault` protects SQL exports with an unrelated AES-GCM key.
- Migration updates are ordered, small-batch, optimistic and transactional; records with ambiguous legacy encryption state fail instead of being overwritten.

### Residual data concerns

Names, dates of birth and national identifiers remain queryable in MySQL, so they are not fully encrypted at the application field level. The gateway provides access isolation, not full field encryption for these fields. A real clinical deployment requires a separately reviewed data minimization, privacy and storage encryption strategy plus appropriate operating controls.

### Security scope

The new Java service closes the client-side SQL-credential exposure **when deployed according to this design**. Running desktop, gateway and root-privileged MySQL in the same exposed user session is not a secure deployment. See `SECURITY.md` for the remaining blockers.
