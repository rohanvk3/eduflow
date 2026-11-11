# Step 6 — Student Attendance

Session-based attendance is integrated with existing Users, Students, Teachers,
Courses, Batches, assignments, enrollment history and weekly schedules in
`com.eduflow.backend`. Credentials and application.properties are unchanged.

## Endpoints

All endpoints use the existing Bearer JWT. There is no public attendance API.

| Method | Endpoint | Access / result |
| --- | --- | --- |
| POST | /api/attendance/sessions | ADMIN or assigned TEACHER; 201 and Location |
| GET | /api/attendance/sessions | Scoped, paginated session list |
| GET | /api/attendance/sessions/{id} | Staff register and eligible students |
| PUT | /api/attendance/sessions/{id}/records | Transactional draft replacement |
| POST | /api/attendance/sessions/{id}/finalize | Lock a complete draft |
| GET | /api/attendance/batches/{batchId}/register | Scoped paginated class register index |
| GET | /api/attendance/batches/{batchId}/summary | Assigned-batch or ADMIN summary |
| GET | /api/attendance/students/{studentId}/summary | ADMIN, scoped TEACHER, or that STUDENT |
| GET | /api/attendance/me | STUDENT's personal summary/history |
| GET | /api/attendance/overview | ADMIN/TEACHER totals, draft count, recent activity |
| GET | /api/attendance/reports | ADMIN report |
| GET | /api/attendance/reports/export | ADMIN CSV export, same report filters |

Lists use existing content/page/size/totalElements/totalPages format. Session
filters: batchId, fromDate, toDate, status=DRAFT/FINALIZED, page, size, sort.
Sort fields: sessionDate, startTime, createdAt, status; asc/desc with ID tie-break.
Default is sessionDate,desc. Size is 1–100; page is 0–100000. The batch register
index lists separate class occurrences; open each session for its student register.

Reports accept courseId, batchId, studentId, fromDate, toDate, page, size and
batchPage. Filters combine with AND. page controls student summaries; batchPage
controls batch summaries independently. Personal attendance uses page for history
and batchPage for batch summaries. The default date range is the last 30 days
including today. All date ranges must be ordered and at most 366 days.

400: invalid input/range/status or ineligible/duplicate student entries.
401: missing/invalid JWT. 403: role/ownership violation. 404: missing resource.
409: duplicate occurrence, stale version, incomplete/stale roster, finalized
write attempt or concurrent change. Existing JSON error conventions are reused.

## Creation and occurrence identity

```json
{
  "batchId": 1,
  "sessionDate": "2026-11-08",
  "scheduleId": null,
  "startTime": "09:00",
  "endTime": "10:00",
  "topic": "Algebra"
}
```

Use IDs/dates from your own institute data. Times use Asia/Kolkata with minute
precision. Start/end must both be supplied or both omitted. An untimed manual
session is permitted once per batch/date; timed sessions are distinct by start.
Scheduled sessions derive their times from the active weekly schedule and the
date must match its weekday. Client time overrides must match the schedule.
Batch dates are inclusive. Ordinary creation for COMPLETED/CANCELLED batches
is rejected. A created session is always DRAFT; markedBy is the authenticated
User, never a request field. Sessions are not generated automatically.

The unique occurrence is `(batch_id, session_date, occurrence_key)`, where the
key is normalized HH:mm or UNTIMED. This applies across manual and scheduled
requests, so a different schedule ID cannot duplicate the same class start.
Multiple different start times on the same day are supported. A changed end
time does not create a second instance of the same start-time occurrence.

## Marking and finalization

GET a session first and retain its `session.version`.

```json
{
  "version": 0,
  "records": [
    { "studentId": 10, "status": "PRESENT", "remarks": "" },
    { "studentId": 11, "status": "LATE", "remarks": "Arrived late" }
  ]
}
```

PUT is a full replacement of the desired draft marks, not a partial upsert.
Omitted eligible students become NOT_MARKED, never ABSENT. Empty records clears
draft marks. NOT_MARKED is an API/UI absence of a record and cannot be persisted
as a status. Valid statuses are PRESENT, ABSENT, LATE and EXCUSED; remarks are
optional and limited to 500 characters. Max 10000 entries matches batch capacity.
All IDs, eligibility and duplicates are validated before deleting or writing
anything. A failure rolls back the whole operation.

The save response includes the new version. Finalize with:

```json
{ "version": 1 }
```

Finalization also supports an empty body for API clients; the UI sends the version
to detect stale state. Every currently eligible student must have exactly one
mark, with no stale extra student marks. Future dates cannot finalize. An empty
eligible roster may finalize, but has no percentage and contributes zero eligible
student records. Session/class count still includes that finalized class.

Writes acquire the batch lock before the session lock at READ_COMMITTED, also
serializing eligibility changes from Step 5. Session optimistic versions protect
draft edits. Concurrent finalize/edit requests cannot overwrite finalized marks.
Finalized data is read-only for ADMIN and TEACHER. An audited correction operation
is intentionally not provided; there is no silent admin override or reopen route.
Existing drafts may be completed for a batch that later became terminal; new
sessions for terminal batches remain forbidden.

## Eligibility and history

Eligibility uses `enrollmentDate <= sessionDate`, exclusive withdrawal date,
and inclusive completion date from Step 5. It does not filter by today's Student
status or current EnrollmentStatus. Re-enrollment periods are preserved and
deduplicated to a student ID. Sessions before enrollment cannot add that student.

Draft rosters track current date-effective eligibility. If an enrollment changes,
old draft marks may be stale; GET reports staleRecords and finalization rejects
them until the current register is reviewed and re-saved.

Finalization freezes the validated roster through its attendance records. Later
withdrawal, even on the same day after finalization, does not erase a valid
finalized mark. Finalized views and reports use this snapshot, not a fresh query
that would retroactively remove historical attendance. Unfinalized later classes
continue to use the updated enrollment dates.

## Calculations

Only FINALIZED records count. PRESENT and LATE are attended; ABSENT contributes
to the denominator; EXCUSED contributes to neither numerator nor denominator.

`percentage = (present + late) / (present + late + absent) * 100`

Round HALF_UP to two decimals. A zero denominator returns null and the UI says
No data. Drafts, NOT_MARKED and days without sessions are never absences.
`totalEligibleFinalizedSessions` is the number of eligible student-session records
(including excused); `finalizedSessions` counts distinct classes. Overall student
percentages are weighted by records, not averaged percentages from different batches.
All charts, groups, dashboards and CSV use the same SQL aggregation policy.
Date trends contain only dates with finalized records; missing dates are not
filled with zeros. Excused-only points have no percentage.

## Authorization

ADMIN manages sessions across batches and owns institute reports/exports.
TEACHER can create, mark and finalize only actively assigned batches. Lists,
batch summaries and student summaries are restricted to those assigned batches.
An assignment to one batch does not expose a student's records from other batches.
STUDENT can use /me and their own /students/{id}/summary, but cannot view staff
session rosters, another student's summary, reports or export. Current identity
comes from JWT/security context, not any supplied userId. PARENT gets no access
without a verified parent-child relationship (none exists in this project).

## UI

Routes: /attendance, /attendance/sessions/:id, /attendance/reports. ADMIN/TEACHER
get the scoped session workspace; STUDENT gets personal history. The sidebar
and role-aware dashboards link to real attendance. Staff overview includes batch
selection, date range, draft filter, session pagination, recent finalizations and
first-five batch insights; teacher batch shortcuts are themselves scoped.

Marking shows admission numbers, eligible students, explicit status/remarks,
visible-row bulk marking, versioned draft save and confirmed finalization.
No student is automatically marked present on load. Save a dirty register before
finalizing. Finalized controls are read-only. Unsaved changes are guarded on links,
history navigation, refresh/close, global search and logout.

The responsive calendar shows each actual session on its date and permits multiple
classes per day. Select a batch first. Calendar month is independent of the list's
date-range controls. Months with over 100 sessions offer Load more, with loaded
counts shown explicitly; empty cells do not imply absence.

Personal history and admin reports include date filters, batch summaries, history
or student summaries, and real Recharts date trends. CSV uses the exact report
filters, quotes/escapes every field and prefixes formula-risk text. Export is
bounded to 10000 students; larger exports require narrower filters. Only needed
IDs/names/admission numbers and attendance totals are exported, not salaries,
emails, passwords or guardian information. No PDF or payroll feature was added.

## Database and files

Adds attendance_sessions and attendance_records with foreign keys to existing
batch, schedule, user and student rows. Adds occurrence uniqueness, unique
(session_id,student_id), session-date/batch/student indexes and a session version.
No existing table is dropped, truncated or reset. Credentials and properties
remain unchanged. The existing development ddl-auto=update creates the schema.
For manual additive deployment, see database/step6-attendance-schema.sql; apply
after Step 5 against the existing database. CREATE IF NOT EXISTS leaves existing
tables intact and is not a replacement for versioned production migrations.

Backend additions: AttendanceSession/AttendanceRecord and their enums,
AttendanceDtos, session/record/report repositories, AttendanceScope,
AttendanceSessionService, AttendanceReportService, AttendanceController,
AttendanceConflictException, attendance tests. Existing enrollment repository
adds an eligible-count query; exception advice adds attendance conflicts.

Frontend additions: attendanceService, attendance pages, session form, calendar,
shared attendance stats/tables/trend, dashboard widget and unsaved-change guard.
App/sidebar/dashboard integration and optional selector status filtering reuse
the existing auth context, Axios instance, React Router and Tailwind design.

## Manual checks

1. Sign in as ADMIN at http://localhost:5173. Choose Attendance → Create session.
2. Choose an existing eligible batch and schedule, matching date, and optional
   topic. Or create a manual session with its own start/end times.
3. Verify students start Not marked. Choose statuses, save a draft, reload and
   verify choices persist. Try Finalize with missing entries: it stays disabled
   in the UI and is rejected by the API.
4. Save all choices and finalize. Verify read-only register, personal history,
   percentage and report. A later withdrawal should not remove finalized history.
5. Open Reports, combine filters and export CSV. Compare counts with the UI.
6. Log in as an assigned TEACHER and an unassigned TEACHER to check scope.
   Log in as linked STUDENT to verify own history only. Unlinked students receive
   a helpful empty state. Your existing Rohan account is not promoted automatically.

Backend tests: mvnw.cmd test. Frontend: npm.cmd test and npm.cmd run build.
Opt-in MySQL tests: set EDUFLOW_MYSQL_TEST=true with the existing DB/JWT environment,
then run mvnw.cmd -Dtest=AttendanceMySqlTests test. They use rollback transactions
and UUID-tagged fixtures, without resetting existing records. Stop a running JAR
before packaging on Windows. Tests in normal runs use H2; MySQL checks are run
separately. Native Java 21 execution is limited by this machine's installed JDK 22;
the build still compiles with release 21.

## Executed results

- 83 backend tests passed in the normal H2 run; the opt-in MySQL test was skipped
  there. Attendance tests cover stale versions, transactional replacement,
  concurrent edit/finalization, eligibility, uniqueness, history and role scope.
- Two opt-in MySQL tests passed separately: prerequisite round-trip and attendance
  draft/finalization/report/history/CSV. Fixture transactions were rolled back.
- 47 frontend component tests passed, including marking, finalization, read-only
  controls, drafts, calendar, student history, filters and export.
- Backend packaging and frontend production build succeeded.
- Actual browser verification ran through Vite → Spring Boot → MySQL without
  mocked API responses: prerequisite setup, scheduled session, NOT_MARKED initial
  roster, draft save, finalization/read-only, same-day withdrawal history,
  calendar, CSV file download, student ownership, teacher scope and logout.
  Responsive checks used 1440/768/390 pixels. Only this run's UUID-tagged temporary
  QA fixtures were removed; existing records and user roles were not modified.

Known bounds are intentional: no automatic session generation, no correction or
reopen operation, no PARENT access, no PDF export, 366-day report windows, and
10000-student CSV exports. There are no silent finalized edits.
