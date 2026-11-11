# EduFlow — final readiness report

Updated 2026-11-11 after the remaining local acceptance work. This supersedes the earlier NOT READY assessment based on unavailable Java 21, skipped MySQL tests and missing browser/restart evidence.

## 1. Executive summary and verdict

**READY WITH DOCUMENTED LIMITATIONS.** The identified local functional acceptance blockers are resolved and the checks below passed. The remaining conditions concern the approved deployment environment, target-volume sizing, existing-schema adoption and broader browser/accessibility review; they are explicitly not represented as verified.

This is readiness of the tested local release artifact, **not authorization to publish** and not an assertion that an unconfigured production host has passed acceptance. No public deployment, purchase, global Java change, development database reset, real-account mutation or user-upload deletion was performed. All new data and processes were isolated and synthetic. Verification servers were stopped after execution, preserving files and evidence.

## 2. Latest executed checks

| Check | Result |
|---|---|
| Runtime | Portable Eclipse Temurin **21.0.13+9 LTS**, Maven Wrapper **3.10.0**, MySQL **8.0.46**, Node **24.22.1**, Playwright **1.65.0** |
| Backend `mvnw.cmd clean package` with MySQL opt-in | **329 tests; 0 failures, 0 errors, 0 skipped; BUILD SUCCESS**, executable JAR produced, 1m49s |
| Included real MySQL cases | **All 19 passed**, ten opt-in suites, isolated eduflow_test_suite schema |
| Frontend lint | **0 errors/warnings** |
| Frontend Vitest | **184 tests in 16 files passed** |
| Frontend production build | Passed; browser acceptance serves the actual compiled dist |
| Real browser suite | **21 passed, 0 failed/flaky/skipped**, Chromium, approximately **3.9 minutes** |
| Responsive/error-state route matrix | **28 role/route pairs × 3 viewports = 84 checks** passed |
| Real scheduler restart test | **Four production-profile JVM starts**, durable queued/partial/scheduled delivery, opt-out/deduplication/read persistence checks passed |
| Latest paired restore | **48 table checksums, 13 file hashes and 4 distinct database file references verified**, 6.22 seconds on the tiny local fixture |
| Actual production-JAR recovery smoke | Java 21/prod/schema validate, health/readiness, login, ledger, grade, recovered file and revoked parent access passed |
| npm audit | **0 known vulnerabilities** reported |

The global/default Java remains Oracle Java 22. Only child-session JAVA_HOME/PATH select the portable JDK. Its archive SHA-256 matched official Adoptium metadata: `A4DBAA05FCA4BD89D84584E58C459BD732EFEBB1722AF80AB13C6516832004F9`. Runtime provenance and package hash are retained under ignored `.verification/`. A global Maven installation remains unnecessary.

## 3. Module implementation and workflow coverage

Existing modules were preserved. The precise action-to-test mapping is in [LOCAL_ACCEPTANCE.md](LOCAL_ACCEPTANCE.md).

| Module | Current evidence |
|---|---|
| Authentication/accounts | Actual valid/invalid login, route protection, refresh/logout, password change/admin-assisted reset, deactivation and role changes revoke JWTs; sole-admin self-removal rejected; backend expiry/tampering/reuse tests pass |
| Students/teachers | ADMIN creates actual records and opens detail pages; teacher ownership/isolation and student private-data rejection pass |
| Courses/batches/enrollment/timetable | Actual creation, teacher assignment, enrollment, schedules and assigned/enrolled timetable reads; unrelated batch denied |
| Attendance | Browser marks and saves draft, finalizes and displays preserved final roster; H2/MySQL history/eligibility tests pass |
| Finance | Fee structure/agreement/installments; browser partial payment; full payment, idempotent replay, overpayment rejection, void and report reconciliation; authorized student/admin/parent receipts |
| Exams/results | Questions/publication, eligible browser attempts/autosave/submit, real cutoff, client-duration manipulation rejection, duplicate-start prevention, offline excessive-mark validation and publication privacy |
| Assignments | Publication, browser text/PNG submission, grading/feedback; real resubmission/history, deadlines and late-submission policy |
| Study resources | Browser upload/publish/version replacement, published-version isolation, preview/download byte comparison, old-version preservation, invalid/oversized rejection and archive revocation |
| Parent portal | ADMIN creates parent profile and verifies relationships; two-child switching; attendance/fees/results/assignments/progress; pending/unrelated/revoked rejection and financial-write rejection |
| Notifications/announcements | Assigned-batch targeting/isolation; actual scheduler restarts, preference opt-out, read/unread/ownership/deduplication; backend event/reminder/verified-parent tests pass |
| Analytics/reports | Admin/teacher analytics, student/parent progress and actual CSV/PDF download contents; backend scope/aggregation tests pass |
| Administration/security | Settings/branding, audit API/page, JWT revocation, protected modules and last-admin controls covered |

The browser suite is a mixture of real form actions and real API setup/assertions, **not every action through a form**. No HTTP responses are mocked. Component/service tests complement the browser workflows. Passing these scopes does not certify every modal permutation or every browser engine.

## 4. Bugs discovered and fixed

| Priority | Finding | Fix and verification |
|---|---|---|
| HIGH | The earlier centralized route guard blocked STUDENT access to own fee-account detail links, hiding installments/history/receipts | Allow only numeric account-detail navigation for ADMIN/STUDENT. Financial administration remains ADMIN-only and backend ownership remains authoritative. New route regression and real student receipt/refresh scenario pass |
| MEDIUM | Fee-account list mapping queried payment/installment/allocation totals per account | Fetch required to-one relationships and batch paid/overdue aggregates for page IDs, excluding voided payments. Query-count and reconciliation regression plus MySQL and browser tests pass |
| LOW | Long institute names could overlap sidebar/header content | Constrain flex widths and truncate branding; 84 viewport checks pass with a long synthetic institute name |
| LOW | Resource-type control lacked an explicit concise accessible name | Added explicit label; actual upload/publish form exercised |
| LOW | Recovery verifier assumed the first account/submission was the original fixture | Scope account query to the fixture student and locate the intended graded assignment; expanded-data recovery smoke passes |

Harness development also found an invalid helper name in the new backend test, label-selector assumptions and an incorrect attempt to verify a revoked relationship directly. These were corrected to existing contracts (revoked links must be requested again as pending before verification). The final package/browser runs passed completely; no security checks or failing tests were disabled. Initial failure logs were retained rather than relabeled as successful runs.

## 5. Real notification scheduler restart acceptance

`npm run test:scheduler-restart` launches the **actual packaged Java 21 JAR** against the isolated MySQL browser schema on loopback port 18083, with prod/schema validation and a consistent per-run signing key.

- First JVM: scheduling disabled; create six unique synthetic recipients, opt one out, publish an undelivered queued notice and schedule a second notice for future publication.
- Second JVM: real `@Scheduled` execution enabled with a 500 ms initial delay, 250 ms poll and one recipient per transaction. Observe a committed delivery while work is still incomplete, then terminate the actual JVM.
- Third JVM: recover queued targets and the now-overdue scheduled notice. Across repeated real ticks, each enabled controlled recipient receives exactly one notification per event; the opt-out receives none. Cross-user read returns 403 and marking read decrements unread count.
- Fourth JVM: confirm read state survives another restart. Owned processes are stopped in a finally block.

Recorded partial checkpoint: **one committed recipient** before interruption. This is real process/durable transaction/wall-clock evidence, with no direct tick calls, mocked clocks or artificial in-memory context restart. Backend tests additionally cover fee/exam/assignment reminder windows, settled installment exclusions, transactional event capture, verified parents and concurrency. Notifications are in-app; no email/SMS transport is claimed.

## 6. Backend and database evidence

The final Java 21 clean package executed **329 tests, including all 19 MySQL cases, with zero skips/failures/errors**. The added test exercises bounded finance page queries and successful/voided allocation reconciliation. MySQL cases cover Academic (1), Administration (2), Analytics (2), Assignment (2), Attendance (2), Exam (2), Finance (2), Notification (2), Parent (2), Resource (2). Counts include inherited scenarios.

Tests use dedicated EDUFLOW_TEST_DB_* values and the database-name guard on a separately initialized loopback instance. Normal development credentials were never used. Other suites use isolated H2. Earlier Java 21 clean test/package runs passed 328 cases; those historical results are superseded by the latest 329-case package run, not erased. Surefire XML confirms java.version=21.0.13.

No entity/table change was needed for the financial query fix. The **48-table schema-only baseline** remains unchanged, SHA-256 `51899B4EECA48BE955FFB91B4E576BD5FA32AF03C5828461D76AA84EAE807A6D`. It has prior fresh-schema import and Java 21 prod validation evidence. No migration engine, automatic baseline, DROP/TRUNCATE or existing-schema migration was introduced.

## 7. Frontend and browser evidence

Final lint has zero errors/warnings, Vitest passes **184 tests**, the production build succeeds and npm audit reports zero known vulnerabilities. Playwright serves the **compiled frontend/dist via Vite preview** on loopback, with the real Java/MySQL backend. Deep links and refreshes work in that local static-preview environment.

The final **21 scenarios** comprise the original eight four-role workflows plus thirteen additional administrator setup, timetable, multi-child, resource, financial, recovery, offline examination, deadline and viewport scenarios. The last scenario checks 28 role/route pairs at desktop 1440×1000, tablet 768×1024 and mobile 390×844, waits for network settling, rejects rendered error alerts and checks horizontal page overflow. JSON reports zero unexpected, flaky or skipped cases.

This does not establish the real target Nginx/HTTPS behavior, Firefox/WebKit compatibility, exhaustive assistive-technology usability or pixel-perfect visual correctness for every possible dataset/modal.

## 8. Latest database/file backup and recovery

Stopped the fixture server before a new paired snapshot. Dumped the synthetic schema with single-transaction/quick, without CREATE DATABASE/USE/DROP TABLE directives. Recovered into a **new empty isolated schema** and a new file directory. Verified all **48 table checksums**, **13 stored-file SHA-256 hashes**, and all **4 distinct database attachment/resource/logo storage references**. Previous synthetic files were preserved, including retained files from prior runs; no automatic orphan cleanup was attempted.

Measured dump/archive/restore/check time: **6.22 seconds for this small local dataset**, not a production RTO or throughput claim. Windows ZIP was used for this local rehearsal; Linux tar, off-host encryption, retention, actual-volume recovery and backup key custody remain operator validations. Private dump/archive hashes and paths are recorded in restore-result.json. No existing development database or user file was overwritten.

## 9. Production-JAR recovery smoke

Started the rebuilt Java 21 JAR with prod against the latest restored schema/storage on 127.0.0.1:18082. Hibernate validate, process liveness and ADMIN database readiness passed. Actual login and reads verified paid 300/outstanding 700 for the original fixture student, restored graded feedback, authorized recovered PNG bytes matching the stored-file hash, and revoked parent access returning 403.

The recovery-only smoke disables background jobs for snapshot stability; the independent scheduler test above enables real scheduling. All verification JVMs, preview/fixture servers and the isolated MySQL instance were stopped afterward. Portable JDK, data directories, backups, uploads and evidence remain intact.

## 10. Security improvements and regression status

The student navigation correction does not grant financial write access or authorize another account: API role/ownership controls and read-only student UI remain in force. Existing JWT/account/version checks, verified parent links, teacher scopes, upload signatures/sizes, private file serving and sanitized errors remain enabled. Notification recipient privacy and restart deduplication have direct runtime evidence.

Generated test passwords/secrets stay in the ignored ACL-restricted verification workspace. No production seed endpoint/default administrator password was added. npm audit is Node dependency evidence, not a complete independent backend vulnerability assessment.

## 11. Performance findings and limits

New Hibernate statement statistics: **one-account page = 5 statements; twelve-account page = 4 statements**. The old per-row total queries were removed from the account listing; paid and overdue totals exclude voids and retain BigDecimal precision. H2 regression and actual MySQL/browser flows pass. These are measured query counts, not fabricated latency or throughput improvements.

Current initial JS is **389.98 kB (124.15 kB gzip)**, compared with the earlier pre-splitting 447.89 kB (137.34 kB gzip); shared chart code remains 361.69 kB (106.78 kB gzip). Parent deep-history views still perform additional per-account/detail reads and report exports have existing row limits. Actual expected data volume/concurrency was not supplied; target-volume load, query plans and capacity remain a **documented rollout sizing condition**, not an unverified claim that this suite certifies arbitrary scale.

## 12. Deployment prerequisites and remaining limitations

Before an owner-approved public rollout:

- Compare any existing populated installation's schema on an authorized isolated clone and apply reviewed forward migrations. Never apply the full baseline over existing tables.
- Validate Linux Java 21/systemd permissions, static route fallback/reverse proxy, domain/TLS/renewal, trusted proxy configuration and network restrictions on the actual approved host.
- Supply private production secrets, a DML-only runtime database account, persistent private storage, first-administrator provisioning and monitored/encrypted off-host backups; rehearse recovery at target volume.
- Run prepared GitHub Actions on the source host if used. CI execution and actual target-host behavior have not been claimed.
- Profile the intended workload, especially deep parent histories/exports, and review additional browser/accessibility coverage where required.

The installation remains single-backend-instance oriented, INR/Asia/Kolkata, with administrator-assisted recovery and in-app notifications. No paid/public service was created. These deployment prerequisites require the target environment/owner decisions, not permission to alter the current development data.

## 13. Documentation and reproducibility

See [LOCAL_ACCEPTANCE.md](LOCAL_ACCEPTANCE.md) for the action-to-test matrix and real restart phases, [VERIFICATION.md](VERIFICATION.md) for safe command order, [TESTING.md](TESTING.md), [DATABASE.md](DATABASE.md), [SECURITY.md](SECURITY.md) and [DEPLOYMENT.md](DEPLOYMENT.md). README and Nginx/systemd/environment templates remain available. No Docker, extra service infrastructure or automatic deployment was introduced.

## 14. Evidence files

Latest ignored logs: acceptance-backend-package-final.log, acceptance-frontend-lint-final.log, acceptance-frontend-test-final.log, acceptance-frontend-build-final.log, acceptance-npm-audit.log, acceptance-e2e-release.log, acceptance-scheduler-release.log, acceptance-restore-release.log, acceptance-production-recovery.log and acceptance-production-check-final.log. Browser JSON: frontend/test-results/results.json. Surefire XML: backend/target/surefire-reports/.

Private `.verification/local-acceptance-result.json` records the current JAR hash, runtime, counts, browser statistics, scheduler phases, restore counts and cleanup status. Per-run scheduler logs/result, restore-result.json and production-restore-check.json preserve detailed evidence. Earlier failed/development runs remain separately logged; do not mistake them for the final passing commands. Do not publish credentials, raw SQL dumps or authentication traces indiscriminately.

## 15. Final release checklist

- [x] Portable Java 21 verified without changing global Java settings.
- [x] Backend compiles/packages; 329 tests including all 19 MySQL cases pass without skips.
- [x] Frontend lint, 184 tests and production dist build pass.
- [x] 21 real Chromium scenarios against production frontend build pass.
- [x] Four-role workflows and 84 responsive/error-state page checks have explicit coverage.
- [x] Notification scheduling, queued/partial work, deduplication and read state verified over actual JVM restarts.
- [x] Student own-fee navigation defect fixed; API isolation remains enforced.
- [x] Finance list query growth fixed and monetary/void reconciliation regression-tested.
- [x] Latest paired backup/restore validates all tables, files and file references; actual prod JAR reads recovered data.
- [x] No known unresolved critical local functional/security defect established by these checks.
- [x] Verification processes stopped; real data/global settings preserved.
- [ ] Approved target-host/schema/TLS/permissions/off-host-recovery and volume-dependent sizing conditions above completed before public rollout.
- [ ] Owner approval obtained before public or paid deployment.

## 16. Final verdict

**READY WITH DOCUMENTED LIMITATIONS.** The named local acceptance blockers are closed with actual execution evidence. Target-host validation, existing-installation adoption and capacity/browser/operational limits remain explicit conditions of a rollout. This is not a claim of unrestricted production scalability or permission to deploy publicly.