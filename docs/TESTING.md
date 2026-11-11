# Testing

Run the commands in the root README. Maven Wrapper tests use JUnit 5, Spring Boot Test, MockMvc, Security Test, Mockito and test-scoped H2. Test storage points to a random directory under the OS temporary directory; file-focused suites also supply their own isolated locations. Normal tests disable background expiry/notification schedulers to avoid timing interference. Explicit scheduler tests call work directly with controlled clocks.

Suites cover authentication, student/teacher management, academic access, attendance, fee accounting/voids/receipts, exams, assignments, uploads, parent authorization, notification events/deduplication/concurrency, analytics and administration. `ProductionReadinessTests` covers connection closure, sanitized database failure, production required configuration and the MySQL database-name guard. Administration tests verify readiness authorization.

Frontend Vitest/jsdom and Testing Library cover modules and controlled Axios fixtures. RouteAuthorization tests cover protected redirects and role boundaries. ESLint checks JavaScript correctness and React hook ordering/dependencies. These are component/integration tests; they do not establish real-browser end-to-end success.

## Optional MySQL verification

Provision a disposable **separate** MySQL 8 database named `eduflow_test_<suffix>`. Create a restricted test-only user with privileges solely on that schema. Set `EDUFLOW_TEST_DB_URL`, `EDUFLOW_TEST_DB_USERNAME`, `EDUFLOW_TEST_DB_PASSWORD` and `EDUFLOW_MYSQL_TEST=true`, then run the complete Maven suite. The test initializer rejects database URLs outside that naming pattern before schema initialization. Normal `DB_*` values are no longer used. Never point these variables at a real dataset. Hibernate update and settings initialization may run even when fixture transactions roll back. A name guard is an accident-prevention control, not a substitute for database privileges.

## Release workflow checks still requiring execution

On isolated staging with synthetic users, execute every ADMIN, TEACHER, STUDENT and PARENT workflow listed in the Step 15 specification. Verify file content, receipt ownership and ledger totals after each payment/void. Test exam cutoff with server time and reconnect, revoked parent links, disabled accounts and notification restart deduplication. Verify rollback after injected storage/database errors. Record exact versions, fixture IDs and results; never use existing student data.

Playwright is configured separately from Vitest. Follow [VERIFICATION.md](VERIFICATION.md) to start a dedicated loopback MySQL instance and test-only backend, build the frontend, then run `npm run test:e2e`. The **21 Chromium scenarios** serve the actual production dist build and cover four roles, resources, financial/academic/parent workflows and 84 desktop/tablet/mobile page checks, with real API setup and no HTTP mocks. `npm run test:scheduler-restart` separately proves durable notification behavior through four real production-profile Java 21 JVM starts. See [LOCAL_ACCEPTANCE.md](LOCAL_ACCEPTANCE.md) for explicit coverage and limits. Use isolated MySQL for target-volume load, lock races and backup restoration. HTTPS, every possible form/modal and other browser engines are not certified by these local tests.
