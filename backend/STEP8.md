# Step 8 — Exams & Results

Implemented in the existing com.eduflow.backend packages, with existing JWT authentication, enrollment history, assigned-teacher checks, React Router, shared Axios, semantic theme tokens, and reusable controls. No credentials, application.properties, existing module entities, or historical records were replaced.

## Features and rules

### Exam lifecycle and eligibility

- Exams belong to existing batches. ADMIN manages all; TEACHER manages only currently assigned batches. New/updated exams require a batch that is neither COMPLETED nor CANCELLED and a schedule within its batch dates.
- DRAFT → PUBLISHED → COMPLETED (when results publish), or DRAFT/PUBLISHED → CANCELLED if no assessment history exists. Cancellation requires a reason. No exam/history deletion endpoint exists.
- Codes are normalized to uppercase and protected by a database unique key. End must follow start, total marks must be positive, passing marks cannot exceed total, and online duration must be 1–1440 minutes.
- Schedules and attempt timestamps are UTC Instants. The UI explicitly converts Asia/Kolkata date/time inputs into UTC. The server derives an institute-local eligibilityDate from the scheduled start instant.
- Enrollment must cover the exam's institute-local date: enrollmentDate ≤ exam date, withdrawalDate absent or strictly after exam date, completionDate absent or on/after exam date. Re-enrollments are deduplicated. Later withdrawal and batch completion preserve historical access.
- Students see eligible PUBLISHED/COMPLETED exams, never drafts/cancellations. Publishing the exam does not publish results. All metadata/questions become immutable after exam publication, even before attempts.

### Question bank

- Draft MCQs support create/edit/delete and atomic full-list reordering. Maximum 200 questions, 2–10 options each, exactly one correct option, positive two-decimal marks, deterministic display order.
- Total question marks must equal totalMarks before online publication.
- Staff question DTOs include correctness flags. Student question DTOs never include those flags; the secure attempt API is their question source. Correct option IDs and awarded marks appear only in the owning student's published result review.
- Draft deletes remove only unpublished questions/options, never attempts or results.

### Timed attempts and grading

- Student identity resolves from the validated security context and linked Student record. Client user IDs, marks, deadlines, and correctness flags are ignored.
- One attempt per student/exam is enforced by a database unique key. Repeated start resumes that same attempt; it never resets the clock.
- expiresAt = min(server start + duration, exam end). A start outside the availability window is rejected unless resuming an existing attempt.
- All attempt writes acquire the exam lock first, serializing start/save/submit/publication and avoiding conflicting lock orders. Writes use READ_COMMITTED isolation. Answer writes use the attempt version to reject stale tabs.
- Answers are persisted transactionally. Each selected option must belong to its stated question and exam. Duplicate question entries are rejected. A null selected option clears an answer.
- The answer endpoint accepts a partial set of questions and upserts them; omitted answers remain saved.
- Deadlines are enforced independently of browser time. At/after expiry, new answers are rejected. A late save returns HTTP 409 with an AUTO_SUBMITTED AttemptView, while committing evaluation of previously saved answers.
- A bounded server job checks up to 200 expired attempts every 15 seconds. Failed finalizations are logged and retried. Reads, resumes, submits, and result publication also reconcile expired attempts. After an application restart, overdue attempts are recovered on the next job cycle.
- Correct = question marks; wrong/unanswered = zero; no negative marking. Evaluation stores per-answer awards, attempt score/status, and exactly one ExamResult transactionally. Repeated submission is idempotent. Submitted attempts cannot be edited.
- The React timer anchors the server deadline to performance.now(), rather than trusting the local wall clock. Answers save on selection, with explicit unsaved/error/retry states. Refresh restores the persisted attempt using its URL. Submission has confirmation and duplicate-submit locking. On expiry, the UI requests finalization and retries interrupted requests.

### Offline marks, publication, corrections

- ADMIN/assigned TEACHER enter offline marks only after exam end. Draft bulk input is fully validated before writes, so one invalid student or mark rolls back the batch. Each student has at most one result per exam. Maximum 500 entries per request.
- Offline publication requires persisted marks for every eligible student. Absence requires an explicit zero; blanks are never silently counted as zero.
- Online publication requires the availability window to end, all existing attempts to be finalized, and at least one evaluated result. Students who never started have no result and are excluded from online score averages/pass rates.
- Result publication stores actor/time and changes exam status to COMPLETED.
- Published offline marks can change only through the explicit correction operation with a mandatory reason. The immutable audit stores previous/new marks, actor, and timestamp. The original result remains; scores/rankings update consistently.
- Online automatic results cannot be manually corrected in V1; published questions and awards remain immutable.
- Percentage = obtained / total × 100, rounded HALF_UP to two decimals. Grade policy uses the rounded percentage: A+ ≥90, A ≥80, B ≥70, C ≥60, D ≥50, F otherwise. Pass/fail independently compares obtainedMarks with passingMarks. Zero-denominator calculations return null, although valid exams require positive totals.
- Staff-only competition ranks handle ties as 1,1,3. Students cannot access individual peer results or leaderboards.

### Analytics and UI

- /exams: searchable, batch/type/status-filtered, paginated directory.
- /exams/new and /exams/:id/edit: validated draft metadata forms in institute-local time.
- /exams/:id: metadata, lifecycle actions, question builder, preview, reorder, and links to gradebook/results.
- /exams/:id/take: server-derived countdown, question palette, previous/next, saved selection restore, network retry, submission confirmation, and no unpublished scores.
- /exams/:id/gradebook: actual eligible roster, marks/remarks, transactional draft bulk save and explicit result publication.
- /exams/:id/results: published staff results, aggregate marks, tied rankings, offline corrections and audit history.
- /my-results and /exams/:id/result: own published marks, grade, pass/fail, permitted question review and Recharts trend; unpublished results show a pending state.
- /exams/analytics: SQL-backed exam counts, published score averages/pass rates, batch/course performance, exam-wise distribution, and student progress filter. All score analytics use published results and normalize cross-exam scores to percentages. Exam counts remain scoped to authorized batches; student filters affect score analytics.
- Admin/teacher/student dashboards include authorized upcoming/available exams, completion/publication metrics, grading queues or personal performance.
- Recharts, loading/error/retry/empty states, keyboard-accessible controls, responsive tables, and existing light/dark design tokens are reused. No production demo exam data is included.
- Summary charts are bounded to 100 groups/recent results. My Results trends reflect the current result page. Staff grading/result endpoints are scoped to assigned batches; student analytics contain only their own individual scores.

## API inventory

All routes use the existing JWT Bearer token.

| Method | Path | Access |
|---|---|---|
| POST / GET | /api/exams | create: ADMIN/assigned TEACHER; list: authorized staff or eligible STUDENT |
| GET / PUT | /api/exams/{id} | read: scoped; update: authorized staff, DRAFT only |
| POST | /api/exams/{id}/publish | authorized staff |
| POST | /api/exams/{id}/cancel | authorized staff, no history |
| GET | /api/exams/batches/{batchId} | scoped |
| GET | /api/exams/me | STUDENT |
| POST / GET | /api/exams/{id}/questions | authorized staff |
| PUT / DELETE | /api/exams/{id}/questions/{questionId} | authorized staff, DRAFT only |
| PUT | /api/exams/{id}/questions/order | authorized staff, DRAFT only |
| POST | /api/exams/{id}/attempts/start | eligible STUDENT |
| GET | /api/exams/attempts/{attemptId} | owning STUDENT |
| PUT | /api/exams/attempts/{attemptId}/answers | owning STUDENT, active/unexpired |
| POST | /api/exams/attempts/{attemptId}/submit | owning STUDENT |
| PUT / GET | /api/exams/{id}/offline-results | authorized staff |
| POST | /api/exams/{id}/results/publish | authorized staff |
| GET | /api/exams/{id}/results | authorized staff, published only |
| GET | /api/exams/{id}/leaderboard | authorized staff, published only |
| GET | /api/exams/me/results | STUDENT, own published only |
| GET | /api/exams/results/students/{studentId} | own STUDENT or authorized staff |
| GET | /api/exams/{id}/result | owning eligible STUDENT, pending or published review |
| POST | /api/exams/{id}/results/{resultId}/correct | authorized staff, published OFFLINE only |
| GET | /api/exams/{id}/results/{resultId}/corrections | authorized staff |
| GET | /api/exams/analytics | scoped ADMIN/TEACHER/STUDENT |

PARENT has no exam access. No student-facing leaderboard is provided.

Directory parameters: search, batchId, examType, status, page, size, sort. Sort fields: id/title/startDateTime/status/examCode with ,asc or ,desc. Size is bounded to 100. PageResponse uses existing content/page/size/totalElements/totalPages.

Writes affecting draft exam/question rules, offline marks, publication, cancellation, and corrections require the latest exam version. Question request includes version; DELETE takes ?version=. Answer save includes the attempt version. Refresh after a 409 rather than overriding concurrent work.

Example answer save:

    PUT /api/exams/attempts/5/answers
    {"version":0,"answers":[{"questionId":10,"selectedOptionId":42}]}

Example offline marks:

    PUT /api/exams/1/offline-results
    {"version":2,"results":[{"studentId":3,"obtainedMarks":8.50,"remarks":"Checked"}]}

Example correction:

    POST /api/exams/1/results/7/correct
    {"version":4,"obtainedMarks":9.00,"reason":"Rechecked question 2"}

## Database and configuration

Seven new tables: exams, exam_questions, exam_options, exam_attempts, exam_answers, exam_results, exam_result_corrections.

Foreign keys reuse batches, students, and users. Unique keys protect exam code, one attempt per exam/student, one answer per attempt/question, and one result per exam/student. Indexes support batch scheduling, question order, and bounded expiry scans. Marks use DECIMAL(10,2); instants use microsecond datetime storage.

database/step8-exams-schema.sql is an additive MySQL 8.0.16+ script with CREATE TABLE IF NOT EXISTS and no changes to existing module tables/rows. Select the existing eduflow schema in Workbench and run it. The current development ddl-auto=update also creates the new tables on startup. No migration framework, credentials, or application.properties changes were introduced.

Optional environment configuration:

- APP_EXAMS_EXPIRY_ENABLED (default true).
- APP_EXAMS_EXPIRY_DELAY_MS (default 15000).

The expiry job must remain enabled in a running installation; disabling it is intended for deterministic tests. No external queues, services, dependencies, or proctoring tools were added.

## Created/modified files

Backend additions under src/main/java/com/eduflow/backend:

- entity: ExamRecordBase, ExamType, ExamStatus, ExamAttemptStatus, Exam, ExamQuestion, ExamOption, ExamAttempt, ExamAnswer, ExamResult, ExamResultCorrection.
- dto/ExamDtos.java.
- repository: ExamRepository, ExamQuestionRepository, ExamOptionRepository, ExamAttemptRepository, ExamAnswerRepository, ExamResultRepository, ExamResultCorrectionRepository.
- service: ExamAccess, ExamService, ExamAttemptService, ExamExpiryJob, ExamResultService, ExamAnalyticsService.
- config/ExamClockConfig.java; controller/ExamController.java; exception/ExamConflictException.java.
- Modified exception/GlobalExceptionHandler.java for consistent 409 exam conflicts.

Frontend additions:

- services/examService.js, questionService.js, examAttemptService.js, resultService.js.
- components/ExamUI.jsx, QuestionBuilder.jsx, ExamTrend.jsx, ExamDistribution.jsx, ExamDashboard.jsx.
- pages/ExamsPage.jsx, ExamFormPage.jsx, ExamDetailsPage.jsx, ExamTakePage.jsx, ExamGradebookPage.jsx, ExamResultsPage.jsx, MyResultsPage.jsx, ExamAnalyticsPage.jsx.
- Modified App.jsx, Layout.jsx, DashboardPage.jsx, RecordSelect.jsx (backward-compatible valueKey for enrollment → student selection), and Dashboard.test.jsx.

Other additions: database/step8-exams-schema.sql, src/test/java/com/eduflow/backend/ExamManagementTests.java, ExamMySqlTests.java, frontend/src/test/Exams.test.jsx.

## Manual verification

1. Login as ADMIN or a TEACHER assigned to an existing batch. Open Exams → Create Exam. Choose schedule dates inside the batch; enter total and passing marks.
2. For MCQ: add at least two options with one correct selection, positive marks, and a total matching the exam. Edit/reorder/preview questions, then publish. Verify published editing is rejected.
3. As a linked, historically eligible STUDENT, open the available online test. Start once, answer some questions, refresh/resume, and check saved answers and the unchanged deadline.
4. Inspect network responses: active/submitted attempt payloads contain no correct flags, correctOptionId, awardedMarks, or obtainedMarks. A student's /questions request is forbidden.
5. Submit with confirmation. Repeat the submit request in Postman: the same result remains. Try another student's attempt URL: 403.
6. To exercise expiry, use a one-minute exam; save an answer, close the tab, wait past its deadline and the scheduler interval, then resume. Verify AUTO_SUBMITTED and no late answers accepted.
7. After availability ends, publish online results as staff. Student sees only their own score and answer review. Non-attempts have no invented zero result.
8. For OFFLINE: publish exam metadata, wait until its end, open gradebook, enter marks for the real eligible roster, bulk save draft, then publish results. One invalid entry must prevent all writes.
9. Verify published gradebook is read-only. Use Results & corrections to change a published offline mark with a reason; inspect the audit. Test ties in the staff leaderboard.
10. Open analytics and filter batch/student; compare SQL aggregates with published results. Check upcoming/available exams, grading queues, and personal results on dashboards.
11. Check light/dark themes and mobile/tablet/desktop layouts. Login/logout, student, attendance, finance, and academic navigation must continue working.

## Verification and boundaries

Commands: .\mvnw.cmd test; frontend npm.cmd test; frontend npm.cmd run build.

Opt-in real MySQL: EDUFLOW_MYSQL_TEST=true with the existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET, then .\mvnw.cmd -Dtest=ExamMySqlTests test. Fixtures are UUID-tagged and rolled back; existing records are not modified. Schema creation is additive and retained.

No live financial/exam demo records are created by the browser smoke checks. Full write flows are covered through HTTP integration tests and frontend interaction tests. V1 deliberately has one attempt, no negative marking, no proctoring, no retakes, and no online manual score override. Grade thresholds are fixed as listed. New students are eligible according to historical enrollment dates, without a separate frozen candidate roster.

### Executed results

- Full Maven regression suite: 128 passed, 0 failed. Seven opt-in MySQL tests were skipped in that default run.
- ExamManagementTests: 22 passed, including concurrent start uniqueness, server expiry without student requests, late-save rejection with committed evaluation, answer-key privacy, transactional offline marks, and audited corrections.
- Separate ExamMySqlTests: 2 passed, 0 failed. Covers the inherited academic round trip and a combined offline/online exam round trip, grading, publication, correction, and SQL analytics. All UUID fixtures rolled back. Concurrency tests run on H2; no concurrent MySQL load test was executed.
- Frontend: 78 tests passed across 9 files. Vite production build passed.
- Live Edge browser: real MySQL-backed directory, analytics, exam creation form, dark mode, and no horizontal page overflow at 390/768/1440 pixels. Existing dashboard, finance, attendance, students navigation, and logout also checked. No live exam write/attempt was created by the browser test.
- Backend health is UP and Vite is running on ports 8080/5173. Runtime checks used the installed Java 22; Maven retains Java 21 target compatibility. A separate Java 21 runtime execution was not performed.
