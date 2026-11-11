# Teacher Management — Step 4

Uses the actual existing `com.eduflow.backend` layers. Authentication, student
APIs, database credentials and application.properties are preserved.

## API

Send `Authorization: Bearer <token>` on every request.

| Method | Endpoint | Role |
| --- | --- | --- |
| POST | /api/teachers | ADMIN |
| GET | /api/teachers | ADMIN |
| GET | /api/teachers/stats | ADMIN |
| GET | /api/teachers/me | TEACHER |
| GET | /api/teachers/{id} | ADMIN |
| PUT | /api/teachers/{id} | ADMIN |
| PATCH | /api/teachers/{id}/deactivate | ADMIN |
| PATCH | /api/teachers/{id}/activate | ADMIN |

POST returns 201 plus Location; other successes return 200. Existing JSON error
format returns 400 for validation, 401 for missing authentication, 403 for denied
roles, 404 for missing records/profiles and 409 for conflicts. No DELETE API.

List example: `/api/teachers?page=0&size=10&search=math&status=ACTIVE&sort=fullName,asc`.
Size is 1–100; search is limited to 100 characters and covers name, employee ID,
email and specialization. Wildcards are literal. Sort fields: fullName,
employeeId, email, joiningDate, createdAt, experienceYears, status; direction asc
or desc. Default createdAt,desc with ID tie-breaker. Pagination uses the existing
content/page/size/totalElements/totalPages format. Stats return total/active/inactive.

## Example request

```json
{
  "employeeId": "EMP-001",
  "fullName": "Teacher Full Name",
  "email": "teacher@example.com",
  "phone": "+919876543210",
  "qualification": "M.Sc.",
  "specialization": "Mathematics",
  "subjects": ["Algebra", "Calculus"],
  "experienceYears": 5,
  "joiningDate": "2025-01-01",
  "salary": "50000.00",
  "userId": null
}
```

Employee ID, fullName and email are required on POST and PUT. IDs are manually
assigned (letters/digits/underscore/hyphen), normalized uppercase. Emails are
lowercase and unique among teachers, including inactive records. A teacher
email may match a User email; this never links accounts automatically.
Non-empty phones require 7–15 digits, optionally starting with +. Birth date
must be past, joining date not future or before birth. Experience is 0–100;
salary non-negative with at most ten integer and two fractional digits.
Subjects permit 30 nonblank strings of up to 100 characters.

PUT null/omitted optional fields preserve existing values. Empty subjects clears
subjects; empty optional text clears text. Blank dates/numeric fields in the UI
preserve them. Null userId preserves a link. Clearing salary/dates and unlinking
accounts are not exposed. The UI preserves all loaded optional values.

Only ADMIN can link an existing TEACHER user; a user may link to one teacher.
Roles never change automatically. The self-profile resolves identity from JWT
security context and uses a separate DTO without salary or internal userId.
Deactivation retains the record and login link; reactivation is supported.

## Database and files

The existing development ddl-auto=update creates `teachers` and `teacher_subjects`.
Employee ID, email and linked user are constrained unique. Subjects are a JPA
element collection with batched loading; salary is DECIMAL(12,2); timestamps
use LocalDateTime callbacks. Existing StudentStatus ACTIVE/INACTIVE is reused.
No existing credentials, tables, or records are replaced. Production migration
tooling remains outside this step.

Backend additions: Teacher, TeacherRepository, TeacherService, TeacherController,
TeacherRequest, TeacherAdminResponse, TeacherProfileResponse, TeacherStatistics,
TeacherConflictException and TeacherManagementTests. Exception advice adds 409.
Frontend additions: services/teacherService.js, TeachersPage, TeacherFormPage,
TeacherDetailsPage and component tests. Sidebar/routes, profile menu, TEACHER
login destination, dashboard teacher statistics and shared pagination are integrated.

## Manual checks

1. Open http://localhost:5173 and log in with an existing ADMIN. Your previously
   created Rohan account remains STUDENT; it is not silently promoted. A database
   owner must explicitly provision development ADMIN/TEACHER accounts. Public
   registration continues to assign STUDENT only.
2. Open Teachers, add a record, view details, edit, then search/filter/sort/page.
3. Deactivate and reactivate. Try duplicate ID/email (409), negative salary or
   future joining date (400). No record is physically deleted.
4. Link an existing TEACHER user ID. Log in as that user to see `/teachers/me`
   without salary/admin controls. Missing links show a helpful empty state.
5. Verify dashboard active/total teachers and unchanged student metrics.

Run `mvnw.cmd test` and `mvnw.cmd package` (stop a running JAR first on Windows).
Run `npm.cmd test` and `npm.cmd run build` in frontend. Backend tests use H2;
component/browser tests mock API responses. MySQL schema startup and health are
checked separately; browser tests are not live MySQL CRUD verification.

Courses, Batches, Attendance, Fees and payroll are not implemented in this step.
Earlier README references to teacher tools being unavailable are superseded by
this module and its real statistics API.
