# Deployment and recovery (without containers)

These are operator instructions and templates, not executed deployments. Do not publish or create paid resources until the owner approves a concrete release. See the release report for current blockers.

## Prerequisites

Use a supported Linux distribution, Java **21** JDK for building and Java 21 runtime for serving, MySQL 8, Nginx and an HTTPS certificate. Node 24/npm are needed only on the build host. Verify `java -version` and `node --version`. Select a private/managed MySQL service with encrypted connections, a CA/truststore suitable for `sslMode=VERIFY_IDENTITY`, restricted network access, and a DML-only runtime account. A separate operator migration account applies reviewed DDL. Never change existing credentials as part of installation.

Create an unprivileged `eduflow` service account, `/opt/eduflow/releases/<release>/`, `/etc/eduflow/` and `/var/lib/eduflow/files/`. Keep releases read-only to the runtime user; allow writes only in the persistent files directory. Give the directory owner `eduflow`, mode 0700. Use root:eduflow mode 0640 for `/etc/eduflow/backend.env`. Configure a firewall: public 80/443, administration from trusted networks, no public 8080/3306. Tune memory and connection pool to host capacity after measurement.

## Configuration

Copy [the environment template](../deploy/backend.env.example), replace all placeholders privately and keep it outside the release tree. Required values are DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, FRONTEND_ORIGINS and absolute FILE_STORAGE_DIR. JWT_SECRET must be random Base64 with at least 32 decoded bytes (`openssl rand -base64 32`). Origins are comma-separated explicit origins with no trailing slash or wildcard. Default JWT lifetime is 3600 seconds. Default file size is 10 MiB; allowed configured range is 1 KiB–50 MiB, and multipart request capacity allows ten files plus overhead. Match the Nginx body limit to the configured request maximum. Do not put secrets in Vite variables.

The application currently supports Asia/Kolkata and INR only; `TZ=Asia/Kolkata` aligns runtime operations but does not enable other institute zones. `prod` preserves existing application.properties and overrides schema handling with `validate`, hides server error details, disables SQL logging and binds to loopback. Production fails on missing/blank required values, malformed/short JWT key, relative storage paths or non-validation schema mode.

## Build and install

From the reviewed source revision on a build host:

```sh
cd backend
bash mvnw clean test
bash mvnw clean package
cd ../frontend
npm ci
npm run lint
npm test
VITE_API_BASE_URL=/api npm run build
```

Do not skip failing tests. Copy the executable JAR to `/opt/eduflow/releases/<release>/backend.jar` and `dist` contents to its `frontend/` directory. Record source revision, runtime versions, test logs and SHA-256 checksums. Adopt and verify the schema using [DATABASE.md](DATABASE.md) **before** startup. Never run Hibernate update/create on the production dataset. Reuse existing storage without moving/deleting files; reconcile physical paths with metadata before changing FILE_STORAGE_DIR.

For a manual foreground smoke test, use a protected process environment and `java -jar backend.jar --spring.profiles.active=prod`. For normal operation install [eduflow.service](../deploy/eduflow.service) in `/etc/systemd/system/`, inspect paths and JVM memory limits, point `/opt/eduflow/current` to the release, then run `systemctl daemon-reload` and `systemctl enable --now eduflow` as an operator. These commands are not run by this milestone. Configure journald retention and host disk/memory monitoring. Use `journalctl -u eduflow` and request IDs for diagnostics; restrict log access.

## Nginx and HTTPS

Adapt [nginx.conf.example](../deploy/nginx.conf.example) for the approved domain, paths and certificate. Preserve `/api` in proxy requests. The React fallback returns index.html for client routes; missing hashed assets return 404. No uploaded file directory is served statically. Index HTML is not cached; hashed assets are immutable. `nginx -t` must pass before reload. Coordinate frontend/backend releases to avoid loading incompatible assets.

Obtain a certificate only after domain and publication approval. Use the distribution-supported ACME client, verify renewal with its dry-run command and enable automated renewal. To bootstrap HTTP validation before a certificate exists, use a temporary HTTP-only ACME configuration, then enable the HTTPS template. Confirm TLS, redirects, API Authorization headers, upload limits, deep-link refresh, downloads and login rate limiting on staging. Add HSTS after HTTPS/renewal is verified. If adding more proxies, explicitly configure trusted forwarders; never blindly trust client-supplied forwarded headers.

## Health and release verification

`curl --fail https://<domain>/api/health` verifies process liveness only. The authenticated ADMIN `/api/health/readiness` endpoint checks database connectivity; use a restricted operational session rather than committing a monitoring token. Run periodic synthetic transactions and alert on 5xx, 503, repeated scheduler warnings, restarts, disk pressure and database connection exhaustion. There is no external monitoring service provisioned by this repository.

Before switching traffic, perform isolated four-role workflows, exam expiry, payment reconciliation, file access checks and a verified backup restore. Record browser viewport tests and MySQL concurrency evidence. Recheck after restart for persistent files and notification deduplication.

## Consistent backup

Agree RPO/RTO and retention with the institute. Suggested starting retention: 7 daily, 4 weekly, 12 monthly encrypted snapshots, adjusted to policy. Store backups off-host with access logging, encryption, checksums and a separate key; test recovery at least monthly. Retention deletion must be reviewed by an operator and must never target live storage.

Use `mysql_config_editor set --login-path=eduflow-backup --host=<host> --user=<backup-user> --password` interactively. Passwords are prompted, never embedded in scripts/command arguments. Protect the credential file and backups. For managed MySQL follow the provider's CA and TLS options. Use a backup account with required SELECT/SHOW VIEW/TRIGGER/EVENT privileges rather than the application user.

Pause traffic and stop the backend to coordinate DB and file backups (including schedulers). Set `EDUFLOW_DB_NAME`, `EDUFLOW_BACKUP_DIR` and `EDUFLOW_FILES_DIR` to reviewed paths. The following creates a new snapshot; do not overwrite an existing snapshot:

```sh
set -eu
umask 077
: "${EDUFLOW_DB_NAME:?}" "${EDUFLOW_BACKUP_DIR:?}" "${EDUFLOW_FILES_DIR:?}"
snapshot="$EDUFLOW_BACKUP_DIR/$(date -u +%Y%m%dT%H%M%SZ)"
mkdir "$snapshot"
mysqldump --login-path=eduflow-backup --single-transaction --quick \
  --routines --events --triggers --no-tablespaces --set-gtid-purged=OFF \
  --skip-add-drop-table "$EDUFLOW_DB_NAME" > "$snapshot/database.sql"
tar -C "$EDUFLOW_FILES_DIR" -czf "$snapshot/private-files.tar.gz" .
sha256sum "$snapshot/database.sql" "$snapshot/private-files.tar.gz" > "$snapshot/SHA256SUMS"
```

Verify dump exit status and checksums, record release version/schema ledger/storage path and restart the backend promptly, even if backup fails. Encrypt the snapshot before off-host transfer and validate retrieval. Omitting `--databases` avoids CREATE DATABASE/USE statements; `--skip-add-drop-table` avoids DROP TABLE directives. Review the dump before import and pass a new empty isolated target schema explicitly. On Windows use mysqldump `--result-file` rather than PowerShell 5 `>` redirection, which can encode SQL as UTF-16. See [the executable isolated rehearsal](VERIFICATION.md).

## Isolated restore rehearsal and incident recovery

Restore only on an isolated MySQL instance and private filesystem. Ensure the destination has no production network route or privileged credentials. Reject unexpected CREATE DATABASE/USE/DROP TABLE directives and import into a newly created empty schema using `mysql --login-path=<isolated-restore> <new-empty-schema> < database.sql` in a Linux shell. Never overwrite an existing schema or rewrite schema names with broad text substitution. Verify checksums, extract files into a new directory, restore ownership/permissions and start the matching release pointing exclusively at the restored instance. Disable schedulers initially; run ledger/allocation/void reconciliation, record counts and attachment download checks, then verify scheduler recovery with synthetic fixtures. Measure elapsed recovery time and document achieved RPO/RTO. Actual local rehearsal evidence is recorded in the readiness report; it does not establish an off-host production recovery time.

For an incident: stop writes, preserve current evidence, select a verified snapshot, obtain owner approval for any real-data replacement, restore database and files from the **same** snapshot on a new instance, validate, then switch configuration/traffic. Never overwrite the existing database during a rehearsal. Revoke potentially compromised secrets and monitor after recovery.

## Rollback

Keep the previous immutable JAR/frontend release and protected environment/config snapshot. Stop writes, switch the current symlink back only if the prior application is compatible with the current schema, restart and run health plus role smoke tests. Do not run reverse DDL against live financial history. For incompatible schema changes, use the verified paired DB/file restore procedure on a new instance and explicitly assess data loss before switching traffic. Preserve the failed release and logs for investigation.
