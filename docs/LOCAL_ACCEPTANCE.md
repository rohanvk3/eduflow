# Local release acceptance matrix

This maps the Step 15 workflow groups to executable evidence. Tests use Java 21, a separately initialized loopback MySQL 8.0.46 instance, synthetic identities and private test storage. They never use development database credentials. The browser suite serves **compiled frontend/dist through Vite preview**, with a proxy to the real backend; no HTTP responses are mocked. API setup is deliberately distinguished from form interaction. See the readiness report for the actual latest results/verdict.

## Four-role workflows

| Workflow group | Real browser / HTTP evidence | Additional backend evidence |
|---|---|---|
| ADMIN login and institute configuration | Login form, settings API update and branding/settings page | Authentication/Administration suites |
| Teacher/student/course/batch setup | Create teacher and student, create course/batch, assign teacher, enroll, configure timetable through real APIs; open corresponding detail pages | Academic, Teacher, Student and MySQL suites |
| Fee agreement and payment | Main suite creates agreement/installments, records partial payment through form, checks ledger and downloads receipt | Finance suite, including allocation rollback/concurrency, plus MySQL |
| Full payment/replay/overpayment/void | Extended suite reconciles full settlement, idempotent replay, rejected overpayment, voided totals and financial report | Finance regression tests exclude voids and preserve allocations |
| Parent creation and relationship | ADMIN creates parent profile, requests/verifies links; pending links denied; original revoked link must be requested again before verification | ParentPortal and ParentMySql suites |
| Announcements, analytics, exports, audits | Assigned teacher publishes batch notice; unrelated batch denied; admin/teacher analytics; CSV/PDF download bytes; audit API/page | Analytics, Notification and Administration suites |
| TEACHER batches and timetable | Assigned batch/timetable API reads and browser detail pages; unrelated batch denied | Academic/Attendance scope tests |
| Attendance | Actual browser bulk marking, draft save, finalization and read-only final roster | Attendance H2/MySQL tests |
| Assignment publication/submission/grading | Real publication API, browser text/PNG submission and teacher grading/feedback form | Assignment H2/MySQL tests |
| Assignment edge cases | Actual resubmission/history, deadline passage, prohibited revisions, rejected late work and accepted-late policy | Stale version, storage failure/rollback, ownership tests |
| Online exams/results | Real question/publication APIs, browser start/autosave/submit; results publish only after server end time | Exam H2/MySQL suites |
| Offline exams | Excessive marks rejected, valid marks accepted, unpublished result hidden, published result shown in student browser | Offline gradebook/ranking/correction tests |
| Exam cutoff | Client-supplied duration/expiry ignored, duplicate starts return same attempt, unrelated user denied, real time passage produces AUTO_SUBMITTED | Server clock/expiry and answer-key privacy tests |
| Resources | Teacher browser upload/publish/replacement/archive; student preview/download; bytes compared for original/current versions; executable and oversized uploads rejected | Resource H2/MySQL and assignment file rollback tests |
| STUDENT academic workspace | Enrolled courses/batch, timetable, attendance, results, progress, assignments and resources pages | Dashboard, Analytics and ownership suites |
| STUDENT financial workspace | My Fees → own account details, installment/history view, receipt download and refresh; no payment controls | API ownership retained; route regression verifies only ADMIN/STUDENT detail navigation |
| PARENT workspace | Switch between two verified children; attendance/fees/results/assignments/progress; authorized receipt; pending/unrelated/revoked denial; cannot write academic/financial data | ParentPortal, ParentMySql, parent notification tests |
| Authentication/account lifecycle | Invalid login, protected navigation, logout; real password change/reset, deactivation and role changes revoke JWTs; sole administrator self-removal rejected | JWT expiry/tamper, last-admin guard, recovery token expiry/reuse and linked-role constraints |

## Real scheduler restarts

Run `npm run test:scheduler-restart` from frontend after the isolated browser schema has been initialized and a production JAR built. This is an independent process verifier, not a Playwright test and not an in-memory Spring-context restart. It launches **four real production-profile Java 21 JVMs** on loopback port 18083, using the isolated synthetic schema and consistent per-run signing key.

1. With scheduling disabled, register six synthetic recipients, opt one out, publish a queued notice and schedule another for a future time. Confirm no delivery has occurred.
2. Stop the JVM, then boot with real `@Scheduled` execution enabled (500 ms initial delay, 250 ms interval, one recipient per transaction). Observe a committed partial delivery and terminate that JVM.
3. Boot again, wait for durable targets and overdue scheduled publication to finish. Repeatedly assert exactly one notification per event/enabled recipient and zero for the opt-out. Check cross-user read rejection and unread-count decrement.
4. Boot a fourth time and confirm read state persisted. All child JVMs are stopped in a finally block.

No test directly invokes tick/delivery/reminder methods or changes the clock for this proof. Actual JDBC transactions, durable cursor/target rows and real wall time are used. Detailed JSON/logs are retained privately under the run's `.verification/` directory. Backend reminder tests separately verify exam/assignment/fee windows, verified-parent targeting, settled installment exclusions and concurrent delivery. This local proof does not claim delivery to email/SMS; notifications are in-app.

## UI and performance limits

The viewport scenario navigates 28 major role/route combinations at desktop 1440×1000, tablet 768×1024 and mobile 390×844, waits for requests to settle, rejects rendered error alerts, and checks horizontal page overflow. This verifies functional layouts, not pixel-perfect screenshots of every possible modal/data size. Chromium is covered; Firefox/WebKit and assistive-technology review remain documented limitations.

The finance query regression compares one-account and twelve-account list requests using Hibernate statement statistics and verifies partial/voided overdue balances. The corresponding MySQL suite exercises the native aggregate. This is a bounded-query regression, **not** a production throughput/latency benchmark. Parent deep histories and target-volume load still require sizing/profile work before a large rollout.

## Reproduction order

1. Activate the portable Java 21 session as in VERIFICATION.md; resume/start only isolated MySQL.
2. Run backend clean package with MySQL opt-in; do not compile or clean a shared target while another test/startup reads it.
3. Start a fresh BrowserFixtureServer schema, wait for its ready marker, then build the frontend and run `npm run test:e2e`. The suite is stateful; use a new synthetic schema for a full rerun rather than deleting records.
4. Run `npm run test:scheduler-restart` against that isolated schema; its own six synthetic recipients are unique each invocation.
5. Stop the fixture server, run the paired restore verification and actual production-JAR recovery check. Preserve files/evidence and shut down the isolated services afterward.

Existing populated target schemas must be compared on an authorized isolated clone, and Linux Nginx/systemd/TLS/off-host backups must be checked on the approved target before any public rollout. Those are deployment-environment prerequisites; these local tests do not authorize publishing or purchases.
