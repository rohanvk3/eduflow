# Security and operations

Bearer tokens are kept in sessionStorage and attached by centralized Axios. CSRF is disabled because authentication is explicitly supplied in headers rather than automatic cookies; revisit this if cookie authentication is introduced. Explicit CORS origins reject wildcard/path origins. Keep frontend and API on the same HTTPS origin where practical.

Backend authorization is authoritative: role checks plus student ownership, verified/non-revoked parent links, active enrollments and assigned teacher batches. UI route guards improve navigation but cannot authorize data. Tokens reload current account status and security version. Passwords are BCrypt hashes; DTOs omit hashes. Password recovery is an administrator-assisted flow; no email delivery is configured.

No production signing secret is shipped. Generate a random Base64 key of at least 32 decoded bytes in a secret manager, inject at runtime and restrict the environment file to the service user/root. Rotate it after suspected exposure; rotation invalidates all tokens. Keep database credentials and uploaded files out of Git, frontend env files, logs and support tickets. `.gitignore` prevents routine artifact/env additions but is not a secret scanner.

Provision the first administrator through a separately reviewed operator process on the target installation. Public registration only creates STUDENT. Do not ship a default administrator or modify real accounts as a side effect of startup. The repository has no automated initial administrator bootstrap command; this is an operational prerequisite.

Private files use generated UUID keys, non-followed symbolic links, file signature/type checks and authorized download endpoints. Restrict the persistent directory to the service user, outside the webroot. Use encrypted backups and retention policies aligned with student privacy requirements. No upload scanning service is installed; assess malware scanning policy before accepting untrusted documents at scale.

Production rejects blank required configuration and schema auto-update. Errors omit stack traces and attach generated request IDs. Liveness exposes status only; database readiness requires ADMIN. Actuator/configuration endpoints are not exposed. Terminate TLS and rate-limit login/recovery at the reverse proxy. Trust forwarded headers only if explicitly configured behind a controlled proxy.

Before release review dependency advisories, run isolated cross-role and ownership regression tests, verify backup recovery, enable operational alerting, and verify restricted DB/network permissions. Report suspected vulnerabilities privately to the institute operator with a request ID and minimal reproduction, never actual student records.
