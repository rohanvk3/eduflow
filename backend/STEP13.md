# Advanced Analytics & Reports

## Implementation

The module reads existing MySQL records through the existing JWT security stack.
No tables, records, credentials, dependencies, main properties, or previous
business modules were replaced. Existing PDFBox and bundled Noto Sans generate
authenticated reports. Parent progress continues to use the existing parent
portal rather than creating a competing portal.

New backend files, relative to `src/main/java/com/eduflow/backend`:

- `dto/AnalyticsDtos.java`: filter and safe result DTOs (metrics, projected rows,
  dates, page, size, hasMore).
- `repository/AnalyticsRepository.java`: parameterized database aggregates and
  bounded projections; request text is never inserted as SQL/HQL.
- `service/AnalyticsService.java`: overview, financial, attendance, academic,
  batch, teacher, and identity-scoped student progress queries.
- `service/ReportExportService.java`: shared report preview/CSV/PDF datasets.
- `controller/AnalyticsController.java`, `controller/ReportsController.java`.

Frontend additions: `services/analyticsService.js`, `services/reportService.js`,
`pages/AnalyticsPage.jsx`, `test/Analytics.test.jsx`. App.jsx and Layout.jsx add
routes and role-aware sidebar entries without removing previous navigation.
DashboardPage.jsx adds analytics/report/progress shortcuts alongside existing panels.
Backend tests: AnalyticsTests and opt-in AnalyticsMySqlTests.

## Endpoints

All require Bearer authentication. General analytics/reports require ADMIN or
TEACHER; overview and every financial view/export require ADMIN.

| GET endpoint | Available sections |
| --- | --- |
| `/api/analytics/overview` | Current operational KPIs + selected-period analytics |
| `/api/analytics/finance/{section}` | summary, revenue-trend, outstanding, payment-methods, course-wise, batch-wise, payments |
| `/api/analytics/attendance/{section}` | summary, trend, batch-wise, student-wise |
| `/api/analytics/academics/{section}` | summary, exam-trend, course-wise, batch-wise, assignment-performance |
| `/api/analytics/batches` | Paginated comparison; sort=name,asc or name,desc |
| `/api/analytics/batches/{id}` | Authorized batch detail |
| `/api/analytics/teachers` | Paginated workload |
| `/api/analytics/teachers/{id}` | Admin or authenticated teacher's own workload |
| `/api/analytics/students/{id}/progress` | Admin, assigned teacher, owning student, or verified parent |
| `/api/reports` | Paginated report preview |
| `/api/reports/export/csv` | Authenticated UTF-8 CSV |
| `/api/reports/export/pdf` | Authenticated PDF |

Report types: REVENUE, OUTSTANDING_FEES, PAYMENT_COLLECTIONS, ATTENDANCE,
STUDENT_PERFORMANCE, EXAM_RESULTS, ASSIGNMENT_COMPLETION, BATCH_PERFORMANCE,
TEACHER_WORKLOAD. EXAM_RESULTS is an exam-level aggregate, not a public student
leaderboard. STUDENT_PERFORMANCE aggregates each authorized student's published
results. CSV/PDF use the same service and scope as report preview.

Filters: startDate, endDate, courseId, batchId, studentId, teacherId, examType,
paymentStatus where meaningful. Teacher filters restrict assigned-batch queries;
teacher workload remains own-only for TEACHER. Exam type supports ONLINE_MCQ and
OFFLINE. Payment status PAID/OUTSTANDING/OVERDUE applies to the outstanding-account
view only. A course/batch mismatch, invalid date range, missing selected entity,
or unauthorized batch/teacher fails instead of exposing another scope. Attendance
accepts threshold=75 (0–100) and reports each student's belowThreshold flag.
Pagination defaults to 10/20 depending on view; max 100 rows per page. Batch sorts
are allowlisted; other views use deterministic date/name/ID ordering.

## KPI definitions

- Date range is inclusive institute-local dates (Asia/Kolkata). Default last 30
  days, maximum 366 days. Instant queries use local midnight converted to UTC and
  an exclusive next-day upper bound.
- Active students/teachers/courses use their ACTIVE status. When selecting a
  course or batch, student counts require current active enrollment and teachers
  require active assignments. Active enrollments are rows with ACTIVE status,
  enrollmentDate <= today, no withdrawal on/before today, no completion before
  today. A student in two batches counts once per batch, twice as enrollments.
- Collections = sum SUCCESS payment amounts by paymentDate in the date range.
  Payment rows are aggregated independently of allocations: split installments
  cannot multiply a payment. VOIDED payments never contribute.
- Agreed fees = persisted finalPayableAmount snapshots, already net of agreed
  discounts. Collection rate = lifetime successful payments / agreed fees *100.
  Outstanding = agreed snapshots minus lifetime SUCCESS payments. These are
  lifetime/current ledger balances, not a historic balance as of endDate.
- Overdue = installments with dueDate < today minus their successful payment
  allocations. Partial payments reduce it correctly. Future installments are
  not overdue. No new refund/payment/cancellation policy is introduced; existing
  fee agreements, explicit adjustments, and void rules remain authoritative.
- Attendance = (PRESENT + LATE)/(PRESENT + LATE + ABSENT)*100, two decimal places,
  null when denominator is zero. Only FINALIZED records accepted through the
  existing enrollment-eligibility workflow count. Historical valid records remain
  after withdrawal. Draft/unmarked work is not absence. Monthly trends aggregate
  the existing daily count projections using the same formula.
- Exam average = average of each published result's normalized percentage;
  scores with different maximum marks have equal observation weight. Results
  require published/completed non-cancelled exams, positive max marks, and valid
  historical enrollment for the exam eligibility date. Pass rate uses each
  exam's passingMarks, not grade letters. No result means no observation;
  unattempted exams are not assigned a fabricated zero.
- Assignment cohort = published (including closed historical) assignment recipient
  snapshots whose publishedAt falls in the period. Completion = recipients with
  a persisted submission / eligible recipient rows *100. Resubmissions do not add
  another row. Draft assignments are excluded. Graded count, normalized average,
  and below50/50–79.99/80+ distributions use GRADED submissions in that cohort.
- Workload current counts: active assigned batches, distinct courses, active
  weekly timetable slots. Period counts: sessions finalized by the teacher,
  assignments created, submissions last graded, and exams created by that teacher
  scheduled in the selected period. Weekly slots are not completed classes.
  There are no invented completion counts, teacher scores, or quality rankings.

No-data percentages are null and rendered as “No data”; observed zero remains
zero. Financial amounts are database BigDecimal/DECIMAL values and INR formatted;
the browser never calculates authoritative financial balances.

## Aggregation and security

Financial totals reuse FinanceReportService; added course/batch collection queries
group payment rows directly. Attendance reuses AttendanceReportRepository. Academic
queries use AVG/SUM/COUNT and correlated EXISTS enrollment/recipient predicates.
Teacher queries use tracked creator/grader/finalizer fields. Batch pages combine
bounded per-batch aggregate queries rather than loading record histories. This
is simple and safe for the current scale; it is not a precomputed data warehouse.
No new indexes were added without an execution-plan justification.

ADMIN may read institute-wide records and finance. TEACHER predicates are included
in the database query for assigned batches; finance is never returned in teacher
batch rows. Teacher workload is own-only. Student progress resolves the linked
User and parents reuse ParentAccess.child's active VERIFIED relationship check.
Export services call these same authorized services before rendering. They do not
accept recipient lists, client totals, user roles, or JWTs in download URLs.

## UI and exports

Routes: /analytics, /analytics/finance, /analytics/attendance,
/analytics/academics, /analytics/batches, /analytics/batches/:id,
/analytics/teachers, /analytics/teachers/:id, /reports, /my-progress.
Existing /parent/.../progress remains the parent destination. Responsive charts,
tables, course/batch selectors, date filters, refresh, paging, error/retry and empty
states reuse existing components, Tailwind, Axios and dark mode.

Report-specific financial/student/teacher/exam filters appear only where relevant.
Reports are generated on filter changes or Generate report. Downloads use Axios
blobs and revoke object URLs. CSV uses the existing formula-safe escaping helper,
UTF-8 BOM, descriptive headers, filter metadata, KPIs and projected rows. PDF
includes institute name (`app.institute.name`, default EduFlow), period, filters,
UTC generation timestamp, summary KPIs, wrapped field/value tables and page numbers.
The existing bundled font is embedded; unsupported glyphs use a replacement.

Exports are bounded, not silently truncated: CSV <=2000 rows, PDF <=500;
batch comparison <=100 due to its aggregate-per-batch cost. Larger reports return
400 instructing narrower filters. No external storage or background jobs added.

## Manual verification

1. Log in as ADMIN and open Analytics. Change dates/course/batch; compare finance
   totals with the existing ledger for the same scope. Check lifetime balance labels.
2. Open Financial Analytics: switch revenue/method/course/batch/payment/account
   views. Verify a partial payment and a voided payment against the ledger.
3. Open Attendance: compare monthly/batch/student views with the existing register;
   inspect below-threshold flags and an EXCUSED record.
4. Open Academics: compare two published exams with different maximum marks.
   Confirm drafts/unpublished results do not appear; review assignment completion.
5. Follow batch/teacher detail links. Verify current weekly slots versus period
   operational counts; teacher views must contain no finance fields.
6. Reports Center: select each report type and relevant filters, then export CSV
   and PDF. Match KPIs/rows to preview; inspect long text, Unicode, headers and pages.
7. Log in as assigned/unassigned teachers, student and verified/revoked parent.
   Try another student/batch ID and financial export URLs; expect forbidden access.
8. Check widths 390/768/1440 and both themes. Existing login, navigation and old
   module workflows must remain available.

## Verification and limits

Commands: `./mvnw.cmd test`; opt-in existing-database
`EDUFLOW_MYSQL_TEST=true ./mvnw.cmd -Dtest=AnalyticsMySqlTests test` (use shell syntax);
frontend `npm test`, `npm run build`. MySQL fixtures are rollback-only.
Executed regression: 288 backend tests discovered, 271 passed and 17 optional
MySQL tests skipped, zero failures/errors. Additional opt-in AnalyticsMySqlTests:
2 passed against the existing MySQL database with rollback fixtures. Frontend:
165 tests passed across 14 files; Vite production build passed.
The final frontend command used `npm test -- --maxWorkers=2`.
An existing teacher-pagination test failed once during a parallel run; the final
complete run passed without changing that test or Teacher Management.
Focused analytics
checks exercise every report/query, normalized marks, ledger reconciliation,
date boundaries, assignment completion, finalized attendance, IDOR and exports.
The installed test runtime is JDK22 with Java21 release compilation unchanged.
The final focused backend rerun passed 29 tests after export formatting changes.
Maven package succeeded. Live Edge tested all seven staff analytics/reports routes
through Vite, Spring Boot and existing MySQL, plus authenticated CSV/PDF responses,
at 390/768/1440 widths with no runtime exceptions. These browser checks are read-only;
non-empty calculation cases use rollback API fixtures and component tests.
No real parent browser account was created. Large-volume load testing was not run.

Limits: no arbitrary pivot builder or materialized warehouse; no historical
as-of financial snapshots; no untracked class completion; no subjective rankings.
Batch comparisons use bounded aggregate-per-row queries and should be profiled
before large institute-wide exports. Results are observed-result averages, not
all-enrollment averages. Date/role-sensitive data stays server-authoritative.
