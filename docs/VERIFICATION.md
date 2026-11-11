# Reproducing isolated release verification

## Java 21 prerequisite

The system-default `java` remains Oracle Java 22, and global `mvn` is not on PATH. A portable **Eclipse Temurin JDK 21.0.13+9 LTS** has now been downloaded into the ignored workspace `.verification/toolchains/` directory. Its archive SHA-256 was verified against official Adoptium API metadata: `A4DBAA05FCA4BD89D84584E58C459BD732EFEBB1722AF80AB13C6516832004F9`. No registry, user/machine environment settings or global Java installation were changed.

Run `./scripts/Get-PortableJdk21.ps1` to reproduce the download/checksum verification using the official [Adoptium JDK 21 metadata API](https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse). It caches a verified ZIP and extraction and records the directory, version, source URL and hash in `.verification/jdk21.json`. Future invocations may select a newer Java 21 patch from that API; the readiness report records the exact patch actually tested. This follows the portable [archive installation approach](https://adoptium.net/installation/archives).

```powershell
$jdk = Get-Content .verification/jdk21.json -Raw | ConvertFrom-Json
$env:JAVA_HOME = $jdk.jdkHome
if (!(Test-Path "$env:JAVA_HOME\bin\javac.exe")) { throw 'Select a JDK, not a JRE' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
.\backend\mvnw.cmd -version
```

All three must report Java 21 before recording Java 21 acceptance. Use the existing wrapper; a global Maven installation is not required. These environment changes affect only this session. Keep the same session-local JAVA_HOME/PATH when running backend tests, packaging and the production verification launcher. Do not use setx or edit global environment settings.

## Isolated MySQL and browser sequence (Windows)

The scripts use installed MySQL 8 binaries and never connect to the normal development server. `.verification/` is ignored and contains synthetic data, temporary credentials and evidence. It is ACL-restricted to the current Windows user and SYSTEM. Do not publish its state/fixture files or Playwright traces containing test login data.

1. Run `./scripts/Start-IsolatedVerification.ps1`. Supply `-MySqlBin '<path>'` if needed. This initializes a new data directory, starts a new loopback-only instance on a dynamically selected port, sets generated local credentials and creates restricted test schemas. It never resets or modifies an existing instance. Its generated state is reached through `.verification/current.json`.
   To reuse the preserved synthetic databases/files from an earlier completed run, use `./scripts/Resume-IsolatedVerification.ps1` instead. It starts only the recorded data directory on loopback and refuses an occupied port; it never initializes or resets schemas. Stop it again after verification using the recorded operator configuration.
2. Run the backend suite with the generated test-only credentials:

```powershell
$pointer = Get-Content .verification/current.json -Raw | ConvertFrom-Json
$state = Get-Content $pointer.stateFile -Raw | ConvertFrom-Json
$env:EDUFLOW_TEST_DB_URL = "jdbc:mysql://127.0.0.1:$($state.port)/eduflow_test_suite"
$env:EDUFLOW_TEST_DB_USERNAME = 'eduflow_test'
$env:EDUFLOW_TEST_DB_PASSWORD = $state.testPassword
$env:EDUFLOW_MYSQL_TEST = 'true'
Push-Location backend
.\mvnw.cmd clean test
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
Pop-Location
```

3. **After Maven finishes**, run `./scripts/Start-BrowserVerification.ps1` in another terminal. This creates a fresh `eduflow_test_browser_<random>` schema for every run, compiles a launcher from test sources and serves the real backend at 127.0.0.1:18081. Seed refusal protects nonempty schemas; no production seed endpoint or profile is shipped. Do not compile or clean the same Maven target concurrently with tests or this server's startup. Wait for `ISOLATED_BROWSER_FIXTURES_READY`.
4. From `frontend`, run `npm ci`, `npx playwright install chromium`, `npm run build`, then `npm run test:e2e`. Playwright starts Vite **preview of the compiled dist build** on 127.0.0.1:15173 with a proxy to that backend. No requests are mocked. The expanded **21 serial scenarios** use real login, payment, attendance, resource upload/versioning, submission/grading, exam attempt, download and role navigation UI; controlled setup/publication and isolation assertions also use actual HTTP APIs. The viewport matrix checks 28 role/route pairs at three sizes (84 checks). JSON results, failure screenshots and traces are ignored under `frontend/test-results/`. Fixture passwords are random, not shipped defaults. Vitest is explicitly restricted to `src/test/` so it does not execute Playwright specs. See [LOCAL_ACCEPTANCE.md](LOCAL_ACCEPTANCE.md) for coverage and limits.
   Run `npm run test:scheduler-restart` for the independent four-JVM production-profile restart test. It uses the same isolated synthetic schema on loopback port 18083, real scheduled execution and transactional batches, with unique recipients each invocation. It stops its owned child JVMs in a finally block; do not compile/replace the JAR during that run.
5. Stop only the BrowserFixtureServer process launched by this run, after verifying its PID and command line. Before rerunning the whole E2E suite, restart the launcher for a fresh schema; tests intentionally do not delete/reset data to support reruns. No browser/fixture service is deployed publicly.
6. With writes stopped, run `./scripts/Verify-IsolatedRestore.ps1`. It makes a consistent SQL dump without CREATE DATABASE/USE/DROP TABLE directives, archives actual uploaded files, creates a new empty `eduflow_test_restore_<random>` schema, restores without rewriting SQL, compares every table checksum and each file's SHA-256, and verifies the restored logo reference resolves. It refuses unexpected paths/schema identifiers and preserves all previous snapshots. Results and dump/archive hashes are in the run's `restore-result.json`.
7. Rebuild with `mvnw.cmd clean package` and the same isolated MySQL test variables. Run `./scripts/Start-RestoredVerification.ps1` in another terminal, then `./scripts/Check-RestoredVerification.ps1` once it starts. These use the production JAR with prod, restored schema/files, a generated JWT key and loopback port 18082. They verify schema validation, health, login, ledger/parent authorization and recovered authorized download bytes. Never substitute a production URL. Stop that verification JAR before running `./scripts/Verify-IsolatedBaseline.ps1`; the latter creates another fresh empty schema, imports the shipped baseline and starts the same JAR for prod validation. Confirm startup/liveness before stopping it. The continuation report records both successful checks.
8. Stop the verification JAR and then gracefully shut down **only the generated** MySQL instance using `mysqladmin --defaults-extra-file=<run>/operator.cnf shutdown`. Preserve evidence; these scripts do not delete data directories, user files or snapshots.

Playwright currently runs Chromium on local Windows, covering desktop plus tablet/mobile viewports. It is not certification of other browser engines, production HTTPS, all possible workflows, production traffic load or Linux systemd/Nginx behavior. See the readiness report for actual results and remaining gates.
