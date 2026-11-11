# Step 11 — Parent Portal & Verified Parent–Student Linking

The module uses the existing JWT, MySQL, Student, enrollment, attendance, finance, exam, assignment, and resource implementations. No credentials, application.properties, infrastructure, or dependencies were changed.

## Implemented behavior

- ADMIN creates a parent profile either for an existing PARENT User or together with a new BCrypt-protected PARENT login. Existing account roles/passwords are never silently changed. Public registration still creates STUDENT users only.
- Profile names and login email reuse User. Optional contact email is stored only when different from the login email; phone/address belong to ParentProfile.
- PARENT can edit permitted personal/contact fields. Changing contact email does not change login email, password, role, profile status, or student links. The existing application has no verified login-email-change workflow, so this module preserves login identity.
- ADMIN creates a PENDING relationship with a required reason, then explicitly verifies it with a recorded verification basis. Phone/email/admission-number matching does not grant access.
- Only an ACTIVE profile belonging to a PARENT User with a VERIFIED relationship grants child access.
- Every child endpoint checks the relationship using JWT identity and the database. Child switching and session storage are navigation conveniences, never authorization.
- Revocation blocks future child requests after commit. Previously delivered data/files cannot be recalled; portal screens recheck every 30 seconds and on focus, and clear data when access is denied.
- Multiple children and multiple guardians are supported. One parent/student pair has one persistent relationship row. A revoked pair may be explicitly returned to PENDING; its earlier audit events remain.
- At most one VERIFIED primary guardian per student is allowed. Verification locks the student row, serializing this check across guardians. Replace an existing primary through explicit revocation and verification.
- All relationship creation, verification, revocation and re-link events retain actor, timestamp, reason, previous/new status, relationship type, and primary flag. Audit access is ADMIN only.
- Parent views are read-only for student records. No attendance editing, payment recording/voiding, exam attempts/marks changes, assignment submissions/grading, or resource-management permissions are granted.

## Academic and financial integration

Attendance reuses AttendanceReportRepository and its finalized-session policy. PRESENT and LATE count as attended, ABSENT contributes to the denominator, and EXCUSED does not. A zero denominator returns null. Valid historical finalized records remain visible after later withdrawal. Parent responses omit staff attendance remarks.

Fees reuse FinanceService's account/installment/payment calculations and FinanceReceiptService's PDF renderer through narrowly scoped parent read methods. Overall totals aggregate the same persisted fee agreements, successful payments, and successful allocations. VOIDED payments do not reduce balances. Withdrawal does not erase agreements/payments. All amounts remain BigDecimal/INR.

The nested receipt endpoint checks both the verified link and the payment's exact student. A guardian of two children cannot use child A's URL to download child B's payment. Receipts use stored data and current balances. Voided receipts are clearly labeled; internal void-audit reasons and payment notes are excluded from parent output.

Results include only the child's published results, with historical enrollment eligibility matching the exam's eligibility date. Percentage/grade reuse ExamResultService. Pass/fail uses passingMarks, independently of the grade letter. No answer keys, peer rankings, private exam notes, attempts, or other students' marks are returned.

Assignments follow Step 9's recipient snapshot. Published assignments are visible, along with CLOSED assignments where the child already submitted work, preserving the existing historical-view rule. Drafts and unrelated assignments are hidden. Feedback/marks are returned only for GRADED work; Step 9 publishes grades immediately. Resubmissions are not double-counted.

Resource access is **metadata-only**. Published resources must match the child's current ACTIVE enrollment and course/batch scope, following Step 10 eligibility dates. Draft/archived resources, storage keys, binary URLs, and external-link targets are omitted. Existing resource preview/download endpoints continue to reject PARENT; no storage authorization is bypassed.

## Dashboard, filters and progress

The parent workspace provides greeting, searchable/paginated child selection, profile summary, enrollment and actual batch status, attendance, lifetime fee balance, upcoming/available published exams, pending homework, recent published results, recent activity, and unpaid installments from the five most recent fee accounts. The Fees directory pages through all accounts.

Academic range defaults to the last 30 institute-local days, inclusive, with a maximum range of 366 days:

- Attendance: session date.
- Results and exam trends: exam start date.
- Assignment progress: assignment due date.
- Assignment directory: all visible homework by default; supplying dates applies the selected due-date range.
- Outstanding fees: current lifetime balances, unaffected by academic date controls.
- Upcoming/available exams and pending-assignment dashboard counts: all currently applicable open records.

Progress includes attendance trends, published exam percentage trends/average, assignment completion and grading trends, course-wise summaries, and recent published results. Overall/course aggregates include all matching records. Trend series are bounded to 100 exams and 100 assignments; the UI states this limit. Narrow the date range for detail.

No-data percentages are shown as “No data,” not zero. Calendar dates without finalized attendance do not imply absence. Multiple sessions on a date are counted. Activity dates derived from class dates are displayed without invented recording times.

## REST endpoints

Base path: /api/parents.

| Method | Path | Access / purpose |
|---|---|---|
| POST | / | ADMIN: create profile/new parent login or attach existing PARENT user |
| GET | / | ADMIN: paginated/searchable/status-filtered parent directory |
| GET / PUT | /{id} | ADMIN: read/update profile, contact fields and status |
| GET | /candidates/users | ADMIN: paginated PARENT logins without profiles |
| GET / PUT | /me | PARENT: own profile / permitted contact fields |
| POST / GET | /links | ADMIN: pending link / paginated relationship directory |
| POST | /links/{id}/verify | ADMIN: verify pending link with reason/version |
| POST | /links/{id}/revoke | ADMIN: revoke pending/verified link with reason/version |
| GET | /links/{id}/history | ADMIN: paginated immutable relationship audit |
| GET | /me/children | PARENT: searchable/paginated verified children |
| GET | /dashboard | PARENT: selected child dashboard; defaults to first verified child |
| GET | /children/{studentId} | PARENT: verified child profile/enrollment summary |
| GET | /children/{studentId}/dashboard | PARENT: selected child dashboard |
| GET | /children/{studentId}/attendance | PARENT: finalized totals, batch summaries, calendar trend, history |
| GET | /children/{studentId}/fees | PARENT: lifetime totals and paginated account details |
| GET | /children/{studentId}/payments/{paymentId}/receipt | PARENT: ownership-checked PDF |
| GET | /children/{studentId}/results | PARENT: published results only |
| GET | /children/{studentId}/assignments | PARENT: eligible homework/submission/feedback summary |
| GET | /children/{studentId}/resources | PARENT: eligible published resource metadata only |
| GET | /children/{studentId}/progress | PARENT: academic trends and course summaries |

Page sizes are 1–100. Parents cannot read the administrator relationship directory or candidate searches. Invalid input returns 400; missing authentication 401; role/ownership failures 403; missing administrator-selected records 404; duplicate/stale/conflicting transitions 409.

Common filters:

- Parent directory: page, size, search, status.
- Links: parentId, studentId, status, page, size.
- Children: search, page, size.
- Dashboard: studentId (optional), fromDate, toDate.
- Attendance: batchId, fromDate, toDate, page, size, batchPage.
- Results: courseId, batchId, fromDate, toDate, page, size.
- Assignments: search, batchId, status=PENDING/SUBMITTED/LATE/GRADED, fromDate, toDate, page, size.
- Resources: search, courseId, batchId, page, size.
- Progress: fromDate, toDate.

### API examples

Use the existing POST /api/auth/login and Bearer-token authorization in Postman. Use real IDs from the existing database.

ADMIN creates a parent account/profile:

    POST /api/parents
    {"fullName":"Parent name","email":"parent@example.com","password":"a-private-initial-password","phoneNumber":"9876543210"}

Alternatively, attach an existing PARENT account without altering its password:

    {"userId":123,"phoneNumber":"9876543210"}

Create a pending link:

    POST /api/parents/links
    {"parentId":10,"studentId":20,"relationshipType":"GUARDIAN","primaryGuardian":true,"reason":"Guardian documents received"}

Verify only after institute identity/relationship/consent checks:

    POST /api/parents/links/{id}/verify
    {"version":0,"reason":"Identity and signed guardian consent verified"}

Use the returned current version for later transitions:

    POST /api/parents/links/{id}/revoke
    {"version":1,"reason":"Guardian consent withdrawn"}

On 409, reload before retrying. Re-linking a revoked pair uses POST /links and returns it to PENDING with the original audit preserved.

## Frontend routes

- /parent/dashboard
- /parent/children
- /parent/children/:studentId
- /parent/children/:studentId/attendance
- /parent/children/:studentId/fees
- /parent/children/:studentId/results
- /parent/children/:studentId/assignments
- /parent/children/:studentId/progress
- /parent/children/:studentId/resources
- /parent/profile
- /admin/parents
- /admin/parents/:id

Sidebar shortcuts /parent/attendance, /parent/fees, /parent/results, /parent/assignments, /parent/progress and /parent/resources resolve the selected child through the same server checks. Selected-child storage is keyed by the authenticated user and is never trusted as proof of access. Invalid/revoked selections offer a reset.

PARENT login and /dashboard redirect to the parent dashboard. Other role destinations/navigation are preserved. Parent profile updates refresh the existing auth context's displayed name without changing JWT identity.

The UI reuses existing cards, forms, buttons, paginated record selectors, tables, modals, alerts, themes and Axios/JWT interceptors. Recharts provides responsive actual-data charts. Loading, retry, validation, empty states and confirmation/reason dialogs are included.

## Database changes and deployment

database/step11-parents-schema.sql creates only:

- parent_profiles: unique User relationship and personal contact fields.
- parent_student_links: unique parent/student pair, status, verification actor/time, optimistic version, access index.
- parent_link_audits: immutable relationship transition records.

Existing User/Student and academic/financial tables are not duplicated or reset. Foreign keys restrict accidental history deletion; no cascading deletes or parent/link delete APIs were added.

The existing local ddl-auto=update setting adds the tables on startup and was not changed. Where automatic schema updates are disabled, select the existing database and apply the additive SQL before starting the new build. No file-storage changes are necessary; receipt generation and resource security reuse existing implementations.

## File inventory

Created backend files under src/main/java/com/eduflow/backend:

- entity: ParentProfile.java, ParentStudentLink.java, ParentLinkAudit.java, RelationshipType.java, ParentLinkStatus.java.
- repository: ParentProfileRepository.java, ParentStudentLinkRepository.java, ParentLinkAuditRepository.java.
- dto/ParentDtos.java.
- service: ParentAccess.java, ParentManagementService.java, ParentPortalService.java.
- controller/ParentController.java.

Modified backend:

- entity/User.java: controlled full-name update, reused by parent profile edits.
- entity/ResourceRecordBase.java: explicit touch helper so name-only aggregate edits advance the profile version.
- repository/UserRepository.java: Specifications for administrator PARENT-account candidate queries.
- service/FinanceService.java: parent-only account/payment read adapters with link and ownership checks.
- service/FinanceReceiptService.java: shared renderer with guarded parent receipts and private-note suppression.

Created frontend files:

- services/parentService.js, parentDashboardService.js.
- components/ParentUI.jsx, ParentCharts.jsx, ParentAttendanceCalendar.jsx.
- pages/ParentPortalPage.jsx, ParentChildrenPage.jsx, ParentProfilePage.jsx, AdminParentsPage.jsx, AdminParentDetailsPage.jsx.
- test/Parents.test.jsx.

Modified frontend:

- App.jsx: routes.
- components/Layout.jsx: role-specific navigation and profile link.
- auth/AuthContext.jsx: refresh existing authenticated user's display details.
- pages/LoginPage.jsx, DashboardPage.jsx: PARENT redirect.
- components/RecordSelect.jsx and pages/AcademicCatalogPage.jsx: prevent an initial empty-search debounce from resetting pagination, an existing issue exposed by the expanded regression suite. Parent login choices also display email to distinguish accounts with the same name.

Other additions: ParentPortalTests.java, ParentMySqlTests.java, schema script, this guide.

## Manual verification

1. Login as ADMIN. Open Parent Management and create a new PARENT login/profile or choose an existing PARENT user. Verify a STUDENT/TEACHER account cannot be silently promoted through this form.
2. Create a pending relationship using an existing student. Login as that parent: no child data should be visible.
3. Return to ADMIN, verify institute identity/relationship/consent evidence, then confirm verification with a meaningful reason. Parent refresh should now show the child.
4. Add a second child and another guardian. Switch children and check each section. A second verified primary guardian must be rejected unless the previous primary is explicitly revoked.
5. Check finalized attendance, selected dates, calendar and batch/history pagination against the existing attendance module.
6. Compare fee totals with Finance. Download a real receipt, and try another student's payment ID under the current child URL: expect 403. Voided payments must not reduce balances.
7. Confirm unpublished exam marks stay hidden; published scores/grade/pass status match the existing exam module.
8. Check pending/submitted/late/graded homework and feedback. Parents must not have submission, grading or editing actions.
9. Resource metadata must match current enrollment. Generic resource previews/downloads must continue to reject PARENT.
10. Update personal contact fields. Login email, role and links must remain unchanged; the displayed name should refresh.
11. Revoke a relationship. Future child requests, including receipts, must fail immediately after commit. Check the audit, then deliberately re-link and confirm it remains pending until re-verified.
12. Deactivate the parent profile and verify child access stops. Check mobile/tablet/desktop layouts, light/dark theme and existing login/logout/student/finance/exam/assignment/resource navigation.

## Verification results

Executed on 10 November 2026:

| Check | Result |
|---|---|
| Full Maven regression | 204 discovered: **191 passed**, 13 opt-in MySQL tests skipped; no failures/errors |
| ParentPortalTests | **20 passed**, included in the full regression total |
| Opt-in ParentMySqlTests | **2 passed**: the parent login/link/finance/receipt/result/revocation flow plus the inherited academic integration test |
| Frontend suite | **134 passed** across 12 files, including 22 parent tests |
| Frontend production build | **Passed** |
| Maven package | **Passed** |
| Live read-only Edge check | Admin parent directory, creation modal, existing navigation/logout passed at 390/768/1440 widths without horizontal page overflow |
| Running services | Backend health UP on 8080; Vite HTTP 200 on 5173 |

The name-only profile update regression is covered: it advances the aggregate version and rejects stale writes. The initial pagination debounce race found in the existing academic/record selectors was fixed and the full frontend suite rechecked.

Reproduce with:

- .\mvnw.cmd test — isolated H2 regression suite.
- Existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET and EDUFLOW_MYSQL_TEST=true, then .\mvnw.cmd -Dtest=ParentMySqlTests test — new UUID-tagged fixtures inside rollback transactions; existing records are not edited.
- frontend npm test and npm run build.

The MySQL parent flow creates a parent login/profile, logs in with BCrypt credentials, verifies a link, reads real attendance/fees/results/progress, downloads a receipt, revokes access and confirms denial. Fixtures roll back.

Live browser checks are read-only. No existing PARENT login was available, so no production parent account or relationship was created for browser testing. Parent UI workflows are covered by component tests and real backend HTTP/MySQL tests; live parent-browser workflows require the manual steps above.

Intentional limits: resources are metadata-only for parents; contact email does not change login identity; verification is an audited administrator procedure, not automated identity proofing; no invitations, password-reset flow, online payment gateway, peer rankings or parent write access were added. Charts are bounded while aggregate totals remain complete. Updates use polling/focus refresh rather than WebSockets. Java 22 was the installed test runtime; the project retains Java 21 compilation, and separate Java 21 runtime execution was not performed.
