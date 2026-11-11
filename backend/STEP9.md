# Step 9 — Assignments & Homework

Implemented inside the existing com.eduflow.backend layered architecture, using existing Student, Teacher, Batch, Course, enrollment history, JWT, and teacher-assignment authorization. Existing modules, credentials, and application.properties are preserved. No new dependencies or external services were added.

## Assignment lifecycle and eligibility

- ADMIN manages institute assignments. TEACHER manages only currently assigned batches. PARENT has no assignment access.
- Metadata is created as DRAFT. New/edited drafts require an editable batch, positive maximum marks, and a future deadline.
- DRAFT → PUBLISHED → CLOSED. Publishing requires a future deadline and at least one eligible enrolled student. Closing is explicit and cannot be reversed.
- At publication, AssignmentRecipient snapshots the eligible roster using existing enrollment date, withdrawal date, and completion date history in Asia/Kolkata. Duplicate enrollment histories are deduplicated by student.
- Students joining after publication do not automatically receive older assignments. Publish a new assignment for a changed cohort. This policy keeps completion denominators and assigned-work history stable.
- Students see their eligible PUBLISHED assignments. An owner who already submitted can also read a CLOSED assignment as history. Closed assignments with no owned submission are hidden from students.
- All published metadata, deadlines, maximum marks, late rules, and teaching attachments are immutable. Draft edits and lifecycle changes require the current assignment version.
- UTC Instants control deadlines/publication/submission/grading. The UI displays and edits dates in Asia/Kolkata. LocalDateTime createdAt/updatedAt fields use the institute timezone.

## Submission and grading policy

- Exactly one submission record per assignment/student, protected by a unique constraint and an assignment-row lock. JWT resolves the submitting Student; client IDs cannot select another student.
- First submission requires PUBLISHED status, membership in the publication roster, and a valid current enrollment. It must contain nonblank text or at least one valid file.
- Text/file creation is atomic. Multipart creation can contain only files and no text; there is no empty placeholder-submission phase.
- At or after dueDateTime, a first submission is rejected unless allowLateSubmission is true. Accepted late work is labeled LATE and retains a separate late flag even after grading.
- File content is fully validated before a final server-time acceptance check. That captured instant controls both the deadline decision and submittedAt, so validation delays cannot turn a disallowed late upload into an accepted submission.
- Revisions are allowed only strictly before the deadline, while the assignment is PUBLISHED and the work is ungraded. A late first submission cannot subsequently be revised.
- Updates require the current submission version. Each revision snapshots prior text, submittedAt, status, file IDs, version, actor, and time. Replaced files become inactive but remain privately stored and downloadable through authorized history.
- Omitting keepAttachmentIds preserves existing active files. Supplying an explicit list retains only those current owned attachments; new files can be included in the same multipart update. An update cannot make the submission empty.
- Supplementary attachment uploads count as revisions and are subject to the same ownership, version, deadline, and ungraded-work checks. At most ten active files per submission.
- CLOSED assignments reject new submissions and revisions, but retain existing work, file downloads, review, grading, and history.
- ADMIN/assigned TEACHER can grade from zero through maxMarks. Grades and feedback are immediately published to the owner; there is no draft-grading state.
- Every initial grade and correction is recorded in SubmissionGradeHistory. Corrections require a nonblank reason and preserve previous/new marks, previous/new feedback, grader, and time. Students cannot change grades or edit graded work.
- BigDecimal/DECIMAL(10,2) handles marks. More than two fractional digits is rejected. No submission, archived file, revision, or grade-history deletion API exists.

## Private files

Supported: PDF, DOCX, PNG, JPG/JPEG. Client MIME is ignored for trust decisions. Metadata MIME is derived after actual content validation.

- PDF: PDFBox parses the document; encrypted, empty, oversized-page-count, overly complex, active-script/launch-action, and embedded-file documents are rejected.
- DOCX: bounded ZIP expansion, duplicate/path traversal checks, secure XML parsing, correct Word document/content types/package relationships. Macros, executable/embedded binary parts, external relationships, and XML external entities are rejected.
- PNG/JPEG: signature, reader format, dimensions, pixel limit, and actual decoding are checked.
- Filenames cannot contain control characters, path separators, drive separators, or traversal sequences. Storage keys are random UUID .bin names and never appear in API DTOs.
- AssignmentFileStorage is the adapter interface; LocalAssignmentFileStorage is the current implementation. Files live outside the frontend/public directories and are never served by a static route or executed.
- Downloads require existing JWT authorization and ownership/batch checks, force Content-Disposition: attachment, use the validated MIME, set nosniff, and disable caching.
- Newly stored files register transaction rollback cleanup. Draft teaching-file deletion happens after a successful database commit. Partial storage failures clean their incomplete file. Callback cleanup failures are logged for operational recovery.
- Keep the private storage directory and MySQL backups together. Archived submission files intentionally remain stored. This implementation does not include antivirus scanning or cloud storage; the adapter supports a future implementation.

Configuration via environment, without editing application.properties:

| Variable | Default | Purpose |
|---|---|---|
| APP_ASSIGNMENTS_STORAGE_DIR | user.home/.eduflow/assignment-files | Private absolute storage directory |
| APP_ASSIGNMENTS_MAX_FILE_BYTES | 10485760 | Per-file limit, configurable from 1 KiB to 50 MiB |

Multipart request size is bounded to ten file limits plus metadata overhead. Each assignment/submission allows ten active files. The authenticated upload-policy endpoint supplies frontend limits. File validation rejects unsupported content with 400, oversized uploads with 413, storage failures with a sanitized 500, and stale/closed/graded-state conflicts with 409.

Rollback cleanup covers normal transaction failures. Abrupt process/OS failure or cleanup I/O failure can leave an orphaned binary; no automatic orphan sweeper is implemented, to avoid deleting historical files.

## Statistics

SQL aggregates and typed database criteria calculate actual authorized statistics:

- Total assignments, currently published assignments, assignments created by the current staff member.
- Eligible assignment/student opportunities, submissions, pending active work, late work, graded work, overdue assignments.
- Completion = submitted / publication-roster opportunities × 100; grading progress = graded / submitted × 100.
- Average obtained marks and mean normalized percentage from graded submissions.
- Batch-wise completion, upcoming deadlines, recent submitted/graded work and owner feedback.
- A Recharts activity trend groups original first-submission records by institute-local day and batch over the last 30 days. Resubmissions do not add activity; the chart displays up to ten batches from a bounded 300-group query.

Resubmissions never create extra denominator or submission counts. Drafts never enter student pending totals. Workload pending/overdue counts include only open PUBLISHED assignments; per-assignment historical review can still show unsubmitted recipients after closure. Student statistics contain only their own opportunities/work. Staff statistics use assigned institute/batch scope.

Zero-denominator percentages and average marks remain null/No data. Batch summaries are bounded to 100 groups and dashboard upcoming/recent lists to five rows. Dates and due-date filters use the institute timezone.

Dashboards refresh every 30 seconds, on window focus, and immediately after same-browser mutations through an application event. This is polling with immediate local invalidation, not a WebSocket push system.

## REST endpoints

All paths below are under /api/assignments and use the existing JWT.

| Method | Path | Access |
|---|---|---|
| POST / GET | / | create: ADMIN/assigned TEACHER; list: scoped staff/student |
| GET / PUT | /{id} | scoped read; draft update: authorized staff |
| POST | /{id}/publish | authorized staff, DRAFT only |
| POST | /{id}/close | authorized staff, PUBLISHED only |
| GET | /batches/{batchId} | scoped |
| GET | /me | STUDENT |
| GET | /stats | scoped ADMIN/TEACHER/STUDENT |
| GET | /upload-policy | ADMIN/TEACHER/STUDENT |
| POST / GET | /{id}/attachments | upload: staff DRAFT only; read: scoped |
| GET | /attachments/{attachmentId}/download | authorized staff or eligible student/history owner |
| DELETE | /attachments/{attachmentId} | authorized staff, DRAFT only |
| POST | /{id}/submissions | eligible STUDENT, own work |
| GET | /{id}/submissions | authorized staff |
| GET | /{id}/pending-recipients | authorized staff |
| GET | /{id}/my-submission | STUDENT, own work |
| GET / PUT | /submissions/{submissionId} | owner or assigned staff read; owner-only update |
| GET | /me/submissions | STUDENT, own work |
| POST / GET | /submissions/{submissionId}/attachments | upload: ungraded owner before deadline; read: owner/authorized staff |
| GET | /submission-attachments/{attachmentId}/download | owner or authorized staff |
| PUT | /submissions/{submissionId}/grade | authorized staff |
| GET | /submissions/{submissionId}/history | owner or authorized staff |

Directory filters: batchId, status, search, fromDate, toDate, page, size, sort. STUDENT accounts additionally support pendingOnly=true, which returns only eligible open assignments without their own submission. The My Assignments pending-work shortcut uses this server-side filter, and student upcoming-deadline lists exclude submitted work. Dates filter dueDateTime, inclusively in IST. Sort fields are id/title/dueDateTime/status/maxMarks with ,asc or ,desc. Page size is bounded to 100; the existing PageResponse structure is reused.

Staff submission lists accept search/status/page/size. Pending recipients have a separate paginated/searchable endpoint. Status LATE selects currently ungraded late submissions; a graded late submission has status GRADED and late=true.

Examples:

    POST /api/assignments
    {"title":"Algebra practice","description":"Show working","batchId":2,
     "maxMarks":100,"dueDateTime":"2026-11-20T12:30:00Z","allowLateSubmission":false}

    POST /api/assignments/1/publish
    {"version":0}

    POST /api/assignments/1/submissions
    {"submissionText":"My working"}

    PUT /api/assignments/submissions/5
    {"version":0,"submissionText":"Revised working","keepAttachmentIds":[7]}

    PUT /api/assignments/submissions/5/grade
    {"version":1,"obtainedMarks":85,"feedback":"Good work"}

For grade corrections include reason. For file-only/text-plus-file submission, send multipart/form-data: part data has Content-Type application/json and the same submission DTO; repeat files parts for selected binaries. Creation and update support both JSON and multipart at the same URL. Teaching/supplementary file upload uses files parts and ?version=currentVersion. Axios supplies multipart boundaries automatically.

## Database safety

Seven new tables: assignments, assignment_recipients, assignment_attachments, assignment_submissions, submission_attachments, submission_revisions, submission_grade_history.

Foreign keys reuse batches, students, and users. Unique keys enforce publication recipients, one submission per student/assignment, and immutable revision versions. Indexes support batch/deadline queries and submission status/date queries. Large response/revision text uses MEDIUMTEXT to support the validated 20,000-character limit with Unicode, without exceeding MySQL row size. Assignment descriptions use TEXT.

database/step9-assignments-schema.sql is additive MySQL 8.0.16+ DDL. Select the existing eduflow database in Workbench and execute it. There are no DROP/TRUNCATE/DELETE statements and no modifications to existing module tables or credentials. Current development ddl-auto=update also creates the new tables at startup.

## Frontend and files

Routes: /assignments, /assignments/new, /assignments/:id, /assignments/:id/edit, /assignments/:id/submit, /assignments/:id/submissions, /assignments/submissions/:submissionId, /my-assignments.

The disabled sidebar placeholder is replaced with working role-aware links. Existing sidebar/topbar, JWT Axios handling, UI controls, theme tokens and light/dark mode are reused.

- Draft creation saves metadata first, then optional teaching files, and preserves the saved draft ID/version if uploads fail. Publishing is a separate confirmed action on the review/detail page.
- Uploads show progress through transfer and server validation. Text and selected files remain available after submission errors. Unsaved text is retained in per-user, per-assignment sessionStorage for retry; selected files stay in component memory.
- Students return to actual saved work, can revise only under the server's policy, download owned attachments, see grade/feedback and historical versions.
- Teacher review includes actual counts, search, status filters, pagination, pending recipients, private work/files, grade form, correction reason and audit history.
- Admin/teacher/student dashboards retain existing features and add actual homework metrics, deadlines, submission/grading progress, batch completion and feedback.

Created backend files under src/main/java/com/eduflow/backend:

- entity: AssignmentRecordBase, AssignmentStatus, SubmissionStatus, Assignment, AssignmentRecipient, AssignmentAttachment, AssignmentSubmission, SubmissionAttachment, SubmissionRevision, SubmissionGradeHistory.
- repository: AssignmentRepository, AssignmentRecipientRepository, AssignmentAttachmentRepository, AssignmentSubmissionRepository, SubmissionAttachmentRepository, SubmissionRevisionRepository, SubmissionGradeHistoryRepository.
- dto/AssignmentDtos.java.
- service: AssignmentAccess, AssignmentService, SubmissionService, AssignmentStatisticsService, AssignmentFileStorage, LocalAssignmentFileStorage, AssignmentFileValidator, AssignmentFiles.
- config/AssignmentConfig.java; controller/AssignmentController.java.
- exception: AssignmentConflictException, AssignmentUploadTooLargeException, AssignmentStorageException.
- GlobalExceptionHandler.java modified for sanitized assignment, file-size and storage errors.

Created frontend files:

- services/assignmentService.js and submissionService.js.
- components/AssignmentUI.jsx, AssignmentDashboard.jsx, and AssignmentActivity.jsx.
- pages/AssignmentsPage.jsx, AssignmentFormPage.jsx, AssignmentDetailsPage.jsx, AssignmentSubmitPage.jsx, AssignmentReviewPage.jsx, SubmissionDetailsPage.jsx, MyAssignmentsPage.jsx.
- test/Assignments.test.jsx.

Modified frontend files: App.jsx, components/Layout.jsx, pages/DashboardPage.jsx, test/Dashboard.test.jsx.

Other additions: database/step9-assignments-schema.sql; AssignmentManagementTests.java; AssignmentMySqlTests.java; this guide.

## Manual verification

1. Login as ADMIN or a TEACHER assigned to an existing batch. Create a draft with a future IST deadline, maximum marks and optional instructions/files.
2. Upload a real PDF/DOCX/PNG/JPG. Verify an executable renamed to .pdf is rejected, oversized uploads return 413, and drafts are not accessible to students.
3. Confirm publication. The eligible roster must be nonempty and is snapshotted. Published instructions/files/marks/deadlines are immutable.
4. Login as a linked enrolled STUDENT. Open My Assignments → pending homework → assignment. Submit text, files only, or both. Verify a blank submission fails and duplicate POST returns 409 without a duplicate row.
5. Return/refresh to see saved work. Before deadline and grading, revise it; inspect previous text and files in history. Removing the only file with no text must fail.
6. Try another student's submission/attachment URL: expect 403. Unassigned TEACHER/PARENT must also be denied.
7. As authorized staff, review submissions. Filter Pending to see actual missing students; check pagination/search. Grade the work and publish feedback.
8. As owner, verify immediate marks/feedback and read-only graded work. Teacher correction must require a reason and retain prior marks/feedback in the audit.
9. Test deadline boundaries and late rules. At the exact deadline, ordinary first submission fails; late-enabled first work is LATE. Revisions always stop at the deadline. Closing stops all new work/revisions while preserving history.
10. Check dashboards and batch completion against actual eligible recipients, without double-counting revisions. Focus/refresh or wait 30 seconds for updates from another browser.
11. Verify light/dark themes, mobile/tablet/desktop layouts and existing login, student, attendance, finance and exam navigation.

## Testing commands and limits

- .\mvnw.cmd test — disposable H2 regression tests, including real JWT requests, transactional file operations and concurrent submission protection.
- frontend npm.cmd test and npm.cmd run build.
- Opt-in MySQL: set EDUFLOW_MYSQL_TEST=true with existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET, then .\mvnw.cmd -Dtest=AssignmentMySqlTests test. UUID-tagged fixtures roll back; upload files are deleted by rollback callbacks. Existing rows are not edited. Schema creation remains additive.

V1 limitations: publication roster is fixed; late first submissions cannot be revised; published teaching files/rules are immutable; grades publish immediately; no parent linking, antivirus service, object-storage adapter, or WebSocket push is implemented. Archived work is intentionally retained. Operational file cleanup after abrupt crashes requires reconciliation rather than automatic deletion.

## Executed verification

Step 9 completion: 22 assignment backend tests passed, including a deadline recheck after file validation, rollback cleanup, authorization, audited grades/revisions, and concurrent duplicate prevention. AssignmentMySqlTests passed both tests against the existing MySQL database with rollback fixtures. All 94 frontend tests passed at that checkpoint, and npm run build succeeded.

Step 10's full regression and expanded frontend suite also retain these tests. Read-only live Edge checks covered the MySQL-backed assignment directory, creation form/upload policy, student portal, 390/768/1440 widths, themes, existing module navigation, and logout. Browser checks did not create assignment records or files; HTTP integration tests covered submission/grading writes.
