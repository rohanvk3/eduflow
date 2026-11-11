# Step 12 — Notifications and announcements

This module reuses the existing JWT identity, layered packages, MySQL database,
Axios client, router, responsive UI, and dark mode. No dependencies, credentials,
or main `application.properties` were changed. It adds in-app delivery only.

## Features and policies

- Own paginated inbox, real unread count, read/read-all, soft dismissal, type/read/
  search/date filters, and six saved preferences.
- Plain-text draft editor, preview, immediate/scheduled publication, immutable
  published content, archive, audience filters, and staff delivery progress.
- Bell with recent five items and 60-second polling; notification center;
  settings; admin/teacher/student/parent dashboard panels.
- Admin manages ALL, ROLE, COURSE, BATCH notices. Teachers manage only BATCH
  notices for currently assigned batches, including other authorized staff notices
  within those batches. Students and parents cannot manage announcements.
- ALL/ROLE non-parent recipients are existing accounts of the intended roles.
  Parent accounts additionally need an ACTIVE ParentProfile and a VERIFIED link.
  COURSE/BATCH recipients are current active enrolled students, assigned teachers,
  and verified guardians of eligible students. Multiple children/memberships yield
  one notification per user/event. Pending/revoked links never qualify.
- Publication snapshots recipient identifiers. Ordinary readers must be in that
  snapshot **and** currently authorized. Joining later does not grant access to
  old announcements. Muting announcements suppresses delivery, not authorized
  browsing. Archiving hides content from ordinary recipients; staff retains it.
- A scheduled notice whose teacher lost assignment authority stays SCHEDULED,
  with a staff-visible `publicationIssue`. An admin can archive and recreate it.
- Notifications use generic messages; they contain no marks, payment amounts,
  transaction references, or private feedback. Linked pages enforce their own
  existing authorization. Action URLs are allowlisted internal routes, never
  external redirects or URLs containing tokens.
- DELETE soft-dismisses only the recipient's copy. The deduplication tombstone,
  shared announcement, and delivery history remain. There is no retention purge.

## REST endpoints

All require the existing `Authorization: Bearer <token>` header.

| Method | Endpoint | Scope |
| --- | --- | --- |
| GET | `/api/notifications` | Own inbox |
| GET | `/api/notifications/unread-count` | Own `{count}` |
| PATCH | `/api/notifications/{id}/read` | Own copy |
| PATCH | `/api/notifications/read-all` | Own visible unread copies |
| DELETE | `/api/notifications/{id}` | Own copy, 204 |
| GET, PUT | `/api/notifications/preferences` | Own preferences |
| POST | `/api/announcements` | Admin / assigned teacher, 201 |
| GET | `/api/announcements` | Authorized management or recipient scope |
| GET | `/api/announcements/me` | Intended, currently authorized published notices |
| GET | `/api/announcements/overview` | Staff aggregate delivery counts |
| GET, PUT | `/api/announcements/{id}` | Authorized read / draft management |
| POST | `/api/announcements/{id}/publish` | Staff immediate/scheduled publication |
| POST | `/api/announcements/{id}/archive` | Staff archive |

Inbox parameters: `type`, `readStatus=ALL|READ|UNREAD`, `search`, `fromDate`,
`toDate` (institute-local dates), `page=0`, `size=10`. Announcement parameters:
`search`, `audienceType`, `status`, `courseId`, `batchId`, `createdByMe=false`,
`page`, `size`, `sort=createdAt,desc`. Safe sort properties are id, title,
createdAt, publishAt, publishedAt, status. Both return the existing `PageResponse`.
Validation/permission/stale-version errors reuse existing global error responses.

Example admin draft (substitute an existing batch ID):

```json
{
  "title": "Revision class update",
  "content": "Please review the published worksheet before the next class.",
  "audienceType": "BATCH",
  "batchId": 12
}
```

Audience fields are mutually exclusive: ALL has no target; ROLE uses targetRole;
COURSE uses courseId; BATCH uses batchId. Optional `publishAt` is a future UTC
ISO instant. The UI labels the institute timezone and converts IST input to UTC.
POST publish/archive with `{"version": 0}` using the **current response version**.
PUT draft also includes that version. Published content cannot be edited.

PUT preferences sends all six booleans:

```json
{
  "announcementsEnabled": true,
  "examNotificationsEnabled": true,
  "assignmentNotificationsEnabled": true,
  "feeNotificationsEnabled": true,
  "materialNotificationsEnabled": true,
  "attendanceNotificationsEnabled": false
}
```

These are the defaults. Attendance is opt-in to limit volume. GENERAL account
updates bypass optional preferences. Preferences are applied at delivery; turning
a setting on later does not replay already skipped deliveries.

## Automatic triggers

| Source transition | Recipients / idempotency |
| --- | --- |
| Exam published | Eligible students and verified parents; exam ID |
| Results published | Students with published results and verified parents; exam ID |
| Assignment published | Snapshot-eligible students and verified parents; assignment ID |
| Assignment graded / audited correction | Owner and verified parents; submission + grade-history ID |
| Successful verified payment recorded | Account student and verified parents; payment ID; replay creates no new event |
| Material published | Eligible course/batch students only; material + published version |
| Attendance finalized | Verified parents only, preference opt-in; session ID |

Exam eligibility reuses historical enrollment dates; assignments reuse published
recipient snapshots. Material notifications follow current published-version
access. Attendance uses finalized records. Voided payments, settled installments,
archived/unpublished resources, withdrawn scope, and revoked relationships are
rechecked before delivery. Notification snapshots contain no entire entity graph.
These hooks apply to future transitions; old publications are not backfilled.

## Reliability and scheduler configuration

The source transition, outbox job, and paged recipient snapshot commit in the same
database transaction. Delivery consumes bounded batches with progress and inbox
copies committed atomically. Unique event keys, unique job/user targets, and
unique recipient/deduplication keys prevent replay, restart, and multi-instance
duplicates. Five database coordinator rows use pessimistic transaction locks to
serialize delivery and each reminder scanner. Persistent keyset cursors prevent
large scans from always restarting at the first rows. Source-state checks happen
again at delivery; removed recipients are safely skipped.

The shared Spring scheduler has two threads so notification work can run alongside
existing exam expiry. No queue, Redis, or external delivery service is used.

| Spring property | Default |
| --- | --- |
| `app.notifications.scheduler-enabled` | true |
| `app.notifications.initial-delay-ms` | 60000 |
| `app.notifications.poll-ms` | 60000 |
| `app.notifications.batch-size` | 100 (1–500) |
| `app.notifications.delivery-batches-per-tick` | 10 (1–50) |
| `app.notifications.assignment-reminder-hours` | 24 (1–720) |
| `app.notifications.exam-reminder-hours` | 24 (1–720) |
| `app.notifications.fee-reminder-days` | 3 (0–30) |

Use environment variables such as `APP_NOTIFICATIONS_POLL_MS` or JVM `-D` Spring
properties; do not put secrets in source. Appointments/deadlines use UTC instants;
fee dates/week boundaries use Asia/Kolkata. Exam and assignment reminders are once
per exam start/assignment deadline, inside their configured upcoming window.
Only unsubmitted assignments qualify. Fee reminders require a positive outstanding
successful-allocation balance and ACTIVE account. Upcoming reminders are once per
installment/due date, overdue reminders once per Monday-based week. Stale overdue
backlog from earlier weeks is skipped. Repeated scans/restarts do not redeliver.
Failures log the exception class without sensitive record contents and retry.

Delivery is eventually consistent: wait at least one tick. Very large backlogs
can require several ticks; configure throughput for deployment scale. The full
audience snapshot is atomic and paged in memory, so very large institute-wide
publications still hold a write transaction while saving target identifiers.
Preferences/scope suppression are terminal skipped outcomes, visible in counts.
There is no automatic backfill, retention cleanup, email/SMS/push, or WebSocket.

## Database and changed files

Apply `database/step12-notifications-schema.sql` to the selected existing schema
with the normal MySQL client if schema changes are managed manually. It uses only
additive CREATE TABLE IF NOT EXISTS and operational INSERT IGNORE statements.
It creates `notifications`, `notification_preferences`, `announcements`,
`notification_jobs`, `notification_targets`, `notification_coordinators`.
Existing local Hibernate update configuration can also create them on startup;
it was not replaced. Back up normally before applying deployment schema changes.
Target user/student IDs intentionally have no FK: a removed recipient must not
erase outbox history. Inbox/preferences/source authors retain their User FKs.
All new table timestamps are UTC, and no previous module table is altered.

Backend additions (under `src/main/java/com/eduflow/backend`):

- entity: the six tables above, NotificationType, AnnouncementAudience,
  AnnouncementStatus.
- repository: Notification, NotificationPreference, Announcement,
  NotificationJob, NotificationTarget repositories.
- dto: NotificationDtos.
- config: NotificationSettings, NotificationClockConfig.
- service: NotificationAudienceService, NotificationService, NotificationOutbox,
  NotificationCoordination, NotificationDeliveryService, AnnouncementPublication,
  AnnouncementService, NotificationReminderService, NotificationScheduler.
- controller: NotificationController, AnnouncementController.
- Modified event producers only: ExamService, ExamResultService,
  AssignmentService, SubmissionService, FinanceService, StudyMaterialService,
  AttendanceSessionService. Authentication and Student Management code preserved.

Frontend additions (under `frontend/src`):

- services: notificationService.js, announcementService.js.
- components: NotificationUI.jsx, NotificationBell.jsx, NotificationDashboard.jsx.
- pages: NotificationsPage.jsx, NotificationPreferencesPage.jsx,
  AnnouncementsPage.jsx, AnnouncementFormPage.jsx, AnnouncementDetailsPage.jsx.
- Modified: App.jsx routes, components/Layout.jsx sidebar/topbar,
  pages/DashboardPage.jsx and ParentPortalPage.jsx panels.
- Tests: Notifications.test.jsx; existing Dashboard/Parents tests isolate the new
  panels. Backend tests: NotificationManagementTests, NotificationConcurrencyTests,
  NotificationAutomaticEventTests, NotificationMySqlTests. Test-only
  `src/test/resources/application.yml` disables background work; tests explicitly
  invoke workers and use a controlled UTC clock.

## Verification and manual workflow

Run `./mvnw.cmd test`, and from frontend `npm test` and `npm run build`.
The opt-in real MySQL test uses existing environment credentials:
`EDUFLOW_MYSQL_TEST=true ./mvnw.cmd -Dtest=NotificationMySqlTests test`
(set the variable using your shell). Its UUID-tagged fixtures are rolled back.
No database reset or permanent demo accounts are required.

1. Log in as ADMIN. Open Announcements, create a batch draft, and preview it.
   Check that an unrelated student cannot read the draft.
2. Publish with the current version. The response initially shows queued audience
   size. Wait for the next scheduler tick, refresh delivery progress, then log in
   as an eligible student/assigned teacher/verified parent to see the bell/inbox.
3. Test unread/type/date filters, mark one/read-all, dismiss, and verify unread
   count updates. Another user's notification ID must return 403.
4. Schedule a second notice one or two minutes ahead (UI IST). Confirm it remains
   invisible to recipients before the time, then publishes once. A teacher cannot
   select ALL/ROLE/COURSE or an unrelated batch. Archive hides recipient content.
5. Disable a preference, publish a new matching event, and confirm no copy arrives
   while the authorized announcement remains browsable. Enable attendance alerts
   as a verified parent, finalize a real eligible attendance session, and check it.
6. Exercise normal exam/result/assignment/payment/material publication workflows.
   Notifications must link to existing authorized pages and not show confidential
   values. Retrying idempotent payment recording must not duplicate delivery.
7. Use an unpaid installment within three days, an unsubmitted assignment/exam
   within 24 hours, and an overdue installment to test real reminders. Run another
   scheduler tick/restart and confirm no duplicate copies in that window.
8. Revoke a parent link or teacher assignment; new delivery and linked content
   access must stop. Check mobile/tablet/desktop, dark mode, keyboard dismissal,
   login/logout, and existing module navigation.

Executed on 10 November 2026:

- Full Maven suite: 257 discovered, 242 passed, 15 optional MySQL cases skipped,
  zero failures/errors (`target/step12-all-tests.log`).
- Additional opt-in NotificationMySqlTests: 2 passed, zero failures/errors,
  against existing MySQL 8.0.46 (`target/step12-mysql-tests.log`). Fixtures rolled
  back; operational coordination rows retained.
- Maven package: BUILD SUCCESS; source/target release 21, installed runtime JDK22
  (`target/step12-package.log`). No JDK21 runtime execution was claimed.
- Frontend Vitest: 155 passed across 13 files; production Vite build passed
  (`target/step12-frontend-tests.log`, `target/step12-build.log`).
- Live Edge through Vite/Spring/MySQL: existing admin and student inboxes,
  bell/popover, preferences load, announcement directory/editor/preview,
  dashboard, existing Students/Assignments/Resources navigation, and logout.
  No browser runtime exceptions. Pages fit widths 390, 768, 1440. Existing theme
  controls verified both light and dark. Screenshots in `target/step12-*.png`.
- Running app restarted with the completed module: backend health UP on 8080,
  frontend HTTP200 on 5173. No credentials or main properties changed.

H2 tests exercise scope, APIs, event producers, atomic rollback, preferences,
controlled-time reminders, and simultaneous workers; opt-in MySQL verifies real
dialect queries and API flow. These are not a large multi-instance load test.
No real parent browser account existed, so parent behavior is covered by automated
H2/MySQL fixtures. Browser QA did not create permanent notices/accounts or change
preferences: creation, publication, event writes, and read mutations are covered
by automated API/component tests rather than a live browser write scenario.
