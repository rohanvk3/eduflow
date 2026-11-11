# Step 7 — Fees & Payments

Implemented in the existing com.eduflow.backend layered packages. Existing authentication, student, teacher, course, enrollment, timetable, and attendance modules are retained. No credentials or application.properties were changed.

## Financial rules

- All authoritative money uses BigDecimal and DECIMAL(14,2), in INR. Inputs with more than two decimal places are rejected; totals are calculated server-side.
- Course has no catalog price field. FeeStructure supplies dated, course-specific components. Active applicability periods cannot overlap, including shared boundary dates. A course row lock serializes this check.
- Each active enrollment can get exactly one fee account. The account snapshots all fee components, structure name, original total, agreed discount, and final payable amount. Structure amounts and dates become immutable once referenced. Name/status remain editable; revised fees require a new structure. Existing agreements are not repriced.
- Agreement dates cannot be in the future and must fall inside the selected active structure's effective period. Enrollment must be active when an account is created. Later withdrawal retains the financial agreement and payment history.
- A complete schedule must sum exactly to final payable. Installment numbers are unique per account. Dates cannot precede the agreement. Amounts are fixed after schedule creation. Due dates can change before any payment; every schedule is frozen once payment history exists, even if later voided. Account versions reject stale edits.
- Outstanding = agreed final payable minus SUCCESS payments. Installment balances use allocations of SUCCESS payments only. Overdue means an unpaid amount whose due date is before today in Asia/Kolkata. Due-today and future installments are not overdue. Zero payable accounts require no payment.
- Payments require positive amounts and positive allocations that sum to the payment amount. Cross-account allocations, duplicates, installment overpayments, and account overpayments are rejected transactionally.
- Writes take a pessimistic fee-account lock and use READ_COMMITTED isolation. Payment, allocation, version update, and void audit commit together.
- A client-generated idempotency key is unique globally. An identical successful retry returns HTTP 200 with the original payment; first creation returns 201. Reusing the key for changed data or a voided payment returns 409. The React form preserves the retry key in sessionStorage, including after interrupted responses.
- Non-cash payments require a reference. References are unique per payment method, case-insensitively, and remain reserved after voiding. Cash references are optional. Receipt identifiers use UUIDs with a database unique constraint.
- Successful payments cannot be edited or deleted. Voiding requires an administrator, a reason, and a permanent audit containing actor, timestamp, previous/new status, and original amount. Allocations remain stored; all balances exclude their voided parent payment.
- Receipts use persisted data and [Apache PDFBox 3.0.8](https://pdfbox.apache.org/download.cgi), with an embedded OFL-licensed Noto Sans font. Text wraps, long words split, and new pages are added as needed. Unsupported font glyphs render as question marks. Voided documents prominently state they are invalid. Remaining balance is the current balance at generation time.

## REST endpoints

All paths below are under /api/fees. Authorization uses the existing JWT and Spring method security.

| Method | Path | Access |
|---|---|---|
| POST / GET | /structures | ADMIN |
| GET / PUT | /structures/{id} | ADMIN |
| PATCH | /structures/{id}/deactivate | ADMIN |
| POST / GET | /accounts | ADMIN |
| GET | /accounts/{id} | ADMIN or owning STUDENT |
| GET | /students/{studentId} | ADMIN or owning STUDENT |
| GET | /me | STUDENT |
| POST | /accounts/{id}/installments | ADMIN |
| GET | /accounts/{id}/installments | ADMIN or owning STUDENT |
| PUT | /installments/{id} | ADMIN |
| POST / GET | /payments | ADMIN |
| GET | /payments/{id} | ADMIN or owning STUDENT |
| POST | /payments/{id}/void | ADMIN |
| GET | /payments/{id}/receipt | ADMIN or owning STUDENT |
| GET | /dashboard | ADMIN |
| GET | /reports | ADMIN |
| GET | /reports/export | ADMIN |

TEACHER and PARENT receive no finance access. Ownership resolves the linked Student through the authenticated User; client student IDs cannot override it. Unlinked students receive an explicit empty portal response.

List endpoints use existing PageResponse: content, page, size, totalElements, totalPages. Size is bounded to 100. Structure filters: search, courseId, status. Account filters: search, courseId, batchId, studentId, status. Payment filters: search, courseId, batchId, studentId, feeAccountId, fromDate, toDate, status. Lists accept allowlisted sort=field,asc/desc.

Reports accept courseId, batchId, studentId, fromDate, toDate, page, size. Date filters apply to payment dates; agreed fees and outstanding balances are lifetime figures for the selected account cohort. Overdue uses today's institute date. Today's and this month's collections use current local dates independently of the selected report range. Default range is the trailing 12 calendar months through today; maximum span is ten years.

Financial aggregations run in SQL and never join payment allocations into collection totals, preventing double counting. Course/batch summaries show up to 100 groups. CSV includes successful and voided ledger rows with their status and the same filters; voids do not enter collection KPIs. Exports are bounded to 10,000 rows, quote/escape every cell, and neutralize spreadsheet formula prefixes. PDF/CSV downloads use authenticated Axios blob requests.

## Database

Six new tables: fee_structures, student_fee_accounts, fee_installments, fee_payments, payment_allocations, payment_void_audits. Foreign keys reuse existing courses, students, enrollments, and users. Unique keys protect enrollment accounts, installment numbers, receipt numbers, idempotency keys, external references, allocation pairs, and one audit per void. Date/status/course/student indexes support queries.

database/step7-finance-schema.sql is an additive MySQL 8.0.16+ script. In MySQL Workbench select the existing eduflow database and execute the script; it creates only missing finance tables. It has no DROP, TRUNCATE, DELETE, or edits to existing module tables. The current development application's existing ddl-auto=update also creates these tables at startup. No migration framework or credential changes were introduced.

Optional institute name configuration: APP_INSTITUTE_NAME through Spring's relaxed binding for app.institute.name; defaults to EduFlow. Restart after changing it.

## Frontend routes

/finance, /finance/reports, /finance/structures, /finance/structures/new, /finance/structures/:id/edit, /finance/accounts, /finance/accounts/:id, /finance/payments, /my-fees.

ADMIN navigation now includes Finance Overview, Fee Structures, Fee Accounts, Payments, Reports. STUDENT gets My Fees. Dashboard finance placeholders now load live collection/outstanding KPIs. Recharts show real monthly and method aggregates. All screens reuse the existing semantic theme tokens, dark mode, sidebar, shared controls, tables, dialogs, and Axios JWT handling.

The payment form has exact-cent client validation, allocation controls, a review step, submission locking, persisted retry keys, backend error feedback, and receipt actions after confirmation. Fee account details expose the complete snapshot, schedule, history, receipts, and audited voids. Student details are read-only. Account selectors use server-side search/pagination.

## Files

Created:

- entity: FinanceEntity, FeeStructure, StudentFeeAccount, FeeInstallment, FeePayment, PaymentAllocation, PaymentVoidAudit, FeeAccountStatus, FeePaymentStatus, PaymentMethod.
- dto/FinanceDtos.java.
- repository: FeeStructureRepository, StudentFeeAccountRepository, FeeInstallmentRepository, FeePaymentRepository, PaymentAllocationRepository, PaymentVoidAuditRepository.
- service: FinanceService, FinanceReportService, FinanceReceiptService.
- controller/FinanceController.java; exception/FinanceConflictException.java.
- src/main/resources/fonts/NotoSans-Regular.ttf and OFL.txt.
- database/step7-finance-schema.sql.
- frontend/src/services/financeService.js.
- frontend/src/components: FinanceUI, FinanceCharts, FinanceDashboard, FeeAccountActions.
- frontend/src/pages: FinancePage, FinancePaymentsPage, FeeStructuresPage, FeeStructureFormPage, FeeAccountsPage, FeeAccountDetailsPage, MyFeesPage.
- tests: FinanceManagementTests.java, FinanceMySqlTests.java, frontend/src/test/Finance.test.jsx.

Modified: pom.xml (PDFBox dependency), GlobalExceptionHandler (safe finance/conflict responses), frontend App.jsx, Layout.jsx, DashboardPage.jsx, Dashboard.test.jsx.

## Manual verification

1. Start the existing backend and Vite frontend with the existing environment. Login as ADMIN; open Finance → Fee Structures.
2. Create a dated structure for an existing course. Verify an overlapping active structure is rejected. Check component totals.
3. Open Fee Accounts → Create fee account. Select an existing batch, active student enrollment, applicable fee structure, agreement date, and discount. Verify duplicate enrollment accounts are rejected.
4. Open the account and create a complete installment schedule matching the final payable. Test a mismatched sum. Before payments, edit a due date.
5. Record a payment: enter an amount, date, method/reference, and allocations; review before confirmation. Check paid/outstanding balances, installment status, payment history, and PDF receipt.
6. In Postman use Bearer authentication. Replay an identical POST /api/fees/payments with the same idempotencyKey: expect the same payment and HTTP 200. Change the amount under that key: expect 409. Attempt overpayment: expect 409.
7. Void the payment with a reason. Verify its audit, restored balances, retained history, and invalid/voided PDF label.
8. Open Finance Reports. Filter course/batch/student/date and compare SQL-backed totals with the visible ledger; download CSV. A payment allocated across multiple installments must be counted only once.
9. Login as the linked STUDENT. Open My Fees → account. Verify schedule, balances, receipts, and no write controls. Access another account/payment ID: expect 403. TEACHER/PARENT must be denied finance endpoints.
10. Check mobile/tablet/desktop and dark/light mode. Confirm existing login/logout and student/attendance navigation still operate.

## Test commands and boundaries

- .\mvnw.cmd test — default suite uses disposable H2, including 23 finance integration tests, real JWT/security, transactional writes, concurrency races, aggregates, voids, and PDF text extraction.
- npm.cmd test and npm.cmd run build inside frontend.
- EDUFLOW_MYSQL_TEST=true plus existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET, then .\mvnw.cmd -Dtest=FinanceMySqlTests test — UUID-tagged MySQL fixtures roll back after each test. No existing rows are edited. Schema creation is additive and retained.

Deliberately excluded: online gateways, refunds, fee-account cancellation/charge adjustments, payroll, and parent linkage. Account status has reserved CLOSED/CANCELLED values but no un-audited transition endpoint. Payment voids are the implemented audited adjustment operation. Installment amount redistribution after schedule creation is not supported. Browser input/display uses decimal strings and formatting; balances always come from the backend.

## Executed verification

- Full Maven suite: 106 passed, 0 failed; 5 opt-in MySQL tests skipped in the default run.
- Separate opt-in FinanceMySqlTests run: 2 passed, 0 failed (finance round trip and inherited academic round trip), with UUID-tagged fixtures rolled back.
- FinanceManagementTests: 23 passing integration tests, including concurrent overpayment and same-key races. These concurrency tests use H2; the separate MySQL round trip is not a concurrent load test.
- Frontend: 65 tests passed across 8 files; production Vite build succeeded.
- Live Edge browser against Vite, Spring Boot, and MySQL: finance overview, structures, accounts, payments, reports, empty states, fee structure/account forms, dark mode, student navigation, logout, and no horizontal page overflow at 390/768/1440 pixels.
- Financial write flows were exercised by HTTP integration tests and mocked-component interaction tests. Live browser checks intentionally made no financial writes to existing data.
- Local servers are running on ports 8080 and 5173. This machine's installed Java runtime is 22; Maven retains the requested Java 21 compilation target. A separate Java 21 runtime execution was not performed.
