# Step 5 — Courses, batches, enrollments and weekly timetables

Implemented in the current project, using `com.eduflow.backend`. Authentication,
JWT, existing Students/Teachers, credentials and application.properties are retained.
No Attendance APIs, entities or pages have been implemented in this step.

## API

All operations use the existing Bearer token. All writes below are ADMIN only.

| Method | Endpoint | Operation |
| --- | --- | --- |
| GET, POST | /api/courses | Paginated catalog / create |
| GET, PUT | /api/courses/{id} | Detail / update |
| PATCH | /api/courses/{id}/activate | Activate |
| PATCH | /api/courses/{id}/deactivate | Deactivate (no deletion) |
| GET, POST | /api/batches | Scoped paginated list / create |
| GET, PUT | /api/batches/{id} | Scoped detail / update |
| GET | /api/batches/stats | ADMIN total/active counts |
| GET | /api/batches/my-enrollments | STUDENT's own paginated history |
| GET, POST | /api/batches/{id}/teachers | Scoped list / assign existing teacher |
| DELETE | /api/batches/{id}/teachers/{teacherId} | Soft unassign; deactivate their timetable entries |
| GET, POST | /api/batches/{id}/enrollments | Scoped paginated roster / enroll existing student |
| PATCH | /api/batches/{id}/enrollments/{enrollmentId}/withdraw | Withdraw effective today |
| GET, POST | /api/batches/{id}/schedules | Scoped timetable / add class |
| PUT | /api/batches/{id}/schedules/{scheduleId} | Edit or deactivate class |

List parameters: page (zero-based), size (1–100), search (max 100), status.
Batch lists also accept courseId. Sort order is course name/ID ascending,
batch start date/ID descending and enrollment date/ID descending. Pagination
uses the existing content/page/size/totalElements/totalPages format.
Course search covers name/code, batch search covers name/code. Wildcards are literal.
Writes return 201 for creation, 200 for update; validation 400, missing auth 401,
permission denied 403, missing record 404, conflict/concurrent change 409.

## Access

- ADMIN manages all academic records.
- TEACHER reads active course catalog, actively assigned batches, their rosters,
  assignments and timetables. Unrelated batches are denied server-side.
- STUDENT reads active course catalog, batches with their own enrollment history,
  those batches' teacher names/timetables, and their own enrollment periods.
  Students never receive other students' rosters.
- PARENT receives no new access. User roles never change automatically.
- Teacher lists use minimal DTOs with no salary or credentials. Entities are not
  returned. Identity is resolved from the existing validated security context.

## Rules and history

Course/batch codes are unique, normalized uppercase, and allow letters, digits,
hyphens and underscores. Courses have ACTIVE/INACTIVE status, description and
duration in weeks (1–520). Inactive courses cannot start new batches; existing
batches remain intact.

Batches have course, capacity (1–10000), inclusive start/end dates and
PLANNED/ACTIVE/COMPLETED/CANCELLED status. End cannot precede start. COMPLETED
requires today to be on/after end date. COMPLETED/CANCELLED batches are read-only
for normal changes. Course/date range is immutable after enrollment or timetable
history exists. Capacity cannot shrink below current active enrollments.

Enrollments reference existing active Students. Enrollment date is on/after the
student's admission, no later than today or batch end. It may precede batch start
to reserve a seat. Future enrollment dates are not accepted. Active seats count
all ACTIVE enrollment periods, including pre-start reservations.

Every withdrawal retains its original enrollment row. Withdrawal is effective
today in Asia/Kolkata (no arbitrary backdating). That date is exclusive for
eligibility: `enrollmentDate <= date < withdrawalDate`. Re-enrollment creates a
new row strictly after a prior withdrawal date, so same-day re-enrollment is not
allowed. Batch completion marks current enrollments COMPLETED and records the
inclusive batch end as completionDate. Eligibility queries do not filter by
current enrollment status or current Student status, preserving past membership.
Sessions before batch start would still be invalid under the future Attendance
module; membership dates alone do not define a valid class occurrence.

Capacity and duplicate checks are serialized with a pessimistic batch lock.
All enrollment operations must use these APIs, not direct SQL inserts. A database
unique constraint protects duplicate batch/student/enrollment-date periods.
Cancelled batches retain their enrollment history; administrators can still
withdraw an ACTIVE enrollment to close it, without deleting records.

Teacher assignments require an existing active Teacher. Batch/teacher pairs are
unique; unassignment retains the row and deactivates its active schedules.
Reassignment reuses that relationship row and does not reactivate old schedules.

Weekly schedules use DayOfWeek and minute-precision local times in Asia/Kolkata.
End must follow start; overnight classes are not supported. Active entries need
an active assigned teacher and at least one matching weekday in the batch date
window. Overlapping classes in a batch are rejected. Teacher conflicts across
batches consider weekday, overlapping time and actual shared occurrences within
the overlapping batch date windows; adjacent classes are allowed. Non-overlapping
batch windows and terminal batches do not reserve teacher time.
Writes lock the batch then teacher with READ_COMMITTED timetable transactions,
preventing concurrent requests from booking the same teacher slot.
Inactive schedule rows remain available to ADMIN; members see active slots only.
Room is descriptive: room capacity/conflict management is outside this step.

## Frontend

Working routes: /courses, /courses/:id, /batches, /batches/:id,
/batches/my-enrollments. Existing routes remain intact. Courses/Batches sidebar
entries replace placeholders. Catalogs support search, status filters, pagination,
create/edit and course activation. The batch workspace includes faculty assignment,
enrollment history, confirmed withdrawal, weekly timetable creation/edit/deactivation,
and appropriate read-only views. Record selectors support real server-side search
and pagination instead of silently using only the first page. Dashboard batch
counts use /api/batches/stats. No production data is mocked.

## Database and files

New tables: courses, batches, batch_teacher_assignments, batch_enrollments,
batch_schedules. Existing Students, Teachers and Users are referenced by foreign
keys, never recreated. No data reset, DROP, TRUNCATE or cascading deletion is used.
Current development ddl-auto=update creates these tables automatically. For a
manual schema deployment use database/step5-schema.sql against eduflow; it contains
additive CREATE TABLE IF NOT EXISTS statements and does not modify existing tables.
Do not apply both as competing migration systems in production.

New entities/enums, DTOs, five repositories, CourseService, BatchService,
BatchEnrollmentService, BatchTimetableService, AcademicAccess, CourseController,
BatchController and AcademicConflictException form the backend. TeacherRepository
adds a row-lock query; global advice maps conflicts. Frontend additions live in
the existing components/pages/services folders. Tests cover API contracts,
permissions, historical eligibility, concurrent capacity and teacher conflicts,
forms, filters, rosters and withdrawal confirmation.

## Run and verify

Use the same DB_URL, DB_USERNAME, DB_PASSWORD and JWT_SECRET as before. No new
credentials are needed. Start backend normally; run npm.cmd run dev in frontend.
Log in with an existing ADMIN, add a Course, then create its Batch. Assign an
existing Teacher, enroll an existing Student, and add two adjacent weekly classes.
Try an overlapping class and a full batch to see conflict messages. Withdraw a
student and confirm the old enrollment date remains. Check TEACHER/STUDENT scopes
using their linked accounts. Your Rohan account remains STUDENT until you explicitly
provision another role; this module never promotes existing accounts.

Backend: `mvnw.cmd test`. Package: `mvnw.cmd -DskipTests package` after tests pass
(stop the running JAR first on Windows). Frontend: `npm.cmd test` and `npm.cmd run build`.

The optional real-MySQL test is enabled with EDUFLOW_MYSQL_TEST=true and run using
`mvnw.cmd -Dtest=AcademicMySqlTests test`, with the existing DB/JWT environment.
It uses UUID-tagged new fixtures within a rollback transaction; existing records
are never deleted. Schema creation is additive and remains after rollback.
Other backend tests use H2; frontend component tests use mock responses only in
test files. Attendance remains intentionally unimplemented pending confirmation.

## Executed verification

- 65 backend tests passed on H2, including concurrency and all existing modules.
- The opt-in real MySQL test passed separately (one transactional test).
- 35 frontend tests passed and the production build succeeded.
- Backend packaging succeeded. The machine's installed JDK 22 compiled with
  Java 21 release targeting; a native JDK 21 runtime was not available here.
- A real browser ran through Vite → Spring Boot → MySQL, creating a course and
  batch, assigning a teacher, enrolling a student, saving a weekly class and
  withdrawing the student. No API responses were intercepted or mocked in this
  run. Desktop/tablet/mobile widths of 1440/768/390 were checked. Only this run's
  UUID-tagged temporary QA records were removed afterward; existing records
  and roles were untouched. Existing account login and scoped batch access
  were also checked against the running app.
