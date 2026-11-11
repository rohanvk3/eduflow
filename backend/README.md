# EduFlow

Coaching Institute Management System backend using Java 21, Spring Boot 3.x,
Maven, MySQL 8+, Spring Data JPA, and Spring Security.

Step 6 Attendance is implemented. See [STEP6.md](STEP6.md) for routes, API
contracts, authorization, calculation/eligibility rules, schema and verification.
Earlier references to Attendance being unavailable are superseded by Step 6.

Step 5 is implemented: Courses, Batches, Teacher Assignments, Student Enrollment
History and Weekly Timetables. See [STEP5.md](STEP5.md) for API contracts, role
scope, schema, testing and manual verification. Earlier references below to
Courses/Batches being unavailable are superseded by Step 5. Attendance remains
unimplemented pending confirmation.

## Run locally

Install JDK 21 and MySQL 8+ and point `JAVA_HOME` at your JDK 21 installation.
Create an empty MySQL database named `eduflow`. Set the following environment
variables in the same PowerShell session used to start the application:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/eduflow'
$env:DB_USERNAME = 'your_mysql_username'
$env:DB_PASSWORD = 'your_mysql_password'

# Generate a random signing secret (32 bytes, Base64 encoded).
$keyBytes = New-Object byte[] 32
$generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generator.GetBytes($keyBytes)
$generator.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
$env:JWT_EXPIRATION_SECONDS = '3600'

.\mvnw.cmd spring-boot:run
```

Keep the generated secret private and reuse the same value across restarts if
existing tokens should remain valid. Never commit real credentials or secrets.
Startup fails if `JWT_SECRET` is missing, malformed Base64, or shorter than
32 decoded bytes. Tokens use HS256, an issuer of `eduflow`, and a default lifetime
of one hour. The configurable lifetime must be positive.

Hibernate `ddl-auto=update` creates/updates the `users` table for local development.
Use this setting against your development database. Schema migrations can be
introduced before production deployment.

## API and Postman

Create a Postman environment variable named `baseUrl` with value
`http://localhost:8080`. Use **No Auth** on registration, login, and health.
For requests with JSON bodies, choose **Body > raw > JSON**.

### Health

`GET {{baseUrl}}/api/health`

Returns `200 OK` and `{"status":"UP"}` without authentication. This is a simple
application response rather than a database probe.

### Register

`POST {{baseUrl}}/api/auth/register`

```json
{
  "fullName": "Asha Sharma",
  "email": "asha@example.com",
  "password": "StudyStrong123!"
}
```

Returns `201 Created`:

```json
{
  "id": 1,
  "fullName": "Asha Sharma",
  "email": "asha@example.com",
  "role": "STUDENT",
  "createdAt": "2026-11-08T08:00:00Z"
}
```

IDs and timestamps above are examples. Passwords need at least eight characters
and must fit within BCrypt's 72 UTF-8 byte limit. Names are required and limited
to 100 characters. Email addresses must be valid, at most 254 characters, and
are stored in lowercase. Additional JSON fields such as `role` are ignored;
public registration always assigns `STUDENT`. No public role-changing API exists.

### Login

`POST {{baseUrl}}/api/auth/login`

```json
{
  "email": "asha@example.com",
  "password": "StudyStrong123!"
}
```

Returns `200 OK`:

```json
{
  "token": "<JWT access token>",
  "userId": 1,
  "fullName": "Asha Sharma",
  "email": "asha@example.com",
  "role": "STUDENT"
}
```

Save the returned token into a Postman environment variable named `token`.
You can automate that in the login request's **Scripts > Post-response** tab:

```javascript
pm.test("Login succeeded", () => pm.response.to.have.status(200));
if (pm.response.code === 200) {
  pm.environment.set("token", pm.response.json().token);
}
```

### Current user

`GET {{baseUrl}}/api/auth/me`

Choose **Authorization > Bearer Token**, enter `{{token}}`, and send the request.
This sends `Authorization: Bearer <JWT access token>` and returns the same safe
user fields as registration. Passwords and hashes are never included.

All other endpoints require a valid Bearer token. HTTP Basic and server sessions
are disabled. CSRF is disabled because authentication uses explicitly supplied
Authorization headers, not cookies. React CORS permits `http://localhost:5173`.

### Error checks

| Action | Expected result |
| --- | --- |
| Register the same email again (including different casing) | 409 |
| Register with blank name, invalid email, or short password | 400 |
| Login with incorrect password or unknown email | 401, same generic message |
| Call `/api/auth/me` with No Auth | 401 |
| Call `/api/auth/me` with a malformed or modified token | 401 |
| Call `/api/auth/me` with an expired token | 401, `JWT has expired` |
| Call a method requiring a different role | 403 |

To try expiry, set `JWT_EXPIRATION_SECONDS` to `5`, restart using the same
`JWT_SECRET`, log in, wait more than five seconds, and call `/api/auth/me`.
Set the lifetime back to `3600` afterward and log in again.

Errors have a common JSON shape, with field messages for validation failures:

```json
{
  "status": 401,
  "message": "Invalid email or password",
  "errors": {}
}
```

## Roles and architecture

Roles are `ADMIN`, `TEACHER`, `STUDENT`, and `PARENT`. User roles are loaded from
the database for each authenticated request. A deleted user cannot continue
using an existing token. Method authorization is enabled; future service or
controller methods can use `@PreAuthorize("hasRole('ADMIN')")`, for example.
Student management endpoints are described below. Authentication integration tests
also use a test-only admin endpoint to verify authorization across all four roles.

Packages under `com.eduflow.backend`: `controller`, `service`, `repository`,
`entity`, `dto`, `security`, `config`, and `exception`.

## Build and test

```powershell
.\mvnw.cmd clean verify
```

Tests use a test-only H2 database in MySQL compatibility mode, generated schema,
and a test-only signing key. They do not need MySQL credentials. Tests cover
registration and password hashing, role injection attempts, duplicate emails,
validation, login, token validation/expiry, stateless access, method role checks,
and CORS. They do not verify a live MySQL connection. H2 is not packaged in the
application JAR.

## Student management (Step 3)

The local React/Vite frontend is in `frontend/`. It uses Tailwind CSS, Axios,
and the existing JWT login API; no student data is embedded in the product.
The UI follows the [Tailwind Vite integration](https://tailwindcss.com/docs/installation/using-vite).

Start the backend with the database and JWT environment variables above. Then,
in a second terminal:

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Open `http://localhost:5173`. Vite forwards `/api` to `http://localhost:8080`.
For another backend address, copy `.env.example` to `.env.local` and change
`VITE_API_BASE_URL`. The backend CORS policy allows `http://localhost:5173`.

Sign in using an existing account. No default admin or password is created.
To prepare a local admin, register an account using the existing auth API, then
have the database owner explicitly promote that account in the development DB:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-admin-email@example.com';
```

Use your real registered email in that SQL. Public registration continues to
create STUDENT users only. An admin sees the Students directory with search,
status filters, pagination, add, details, edit, and deactivate confirmation.
A STUDENT sees their linked profile; a missing link produces an explanatory
message. TEACHER and PARENT accounts have no student-record access in this step.

The frontend stores its JWT in `sessionStorage` for the current browser tab,
adds `Authorization: Bearer ...` to Axios requests, and returns to sign-in on
401 responses. Signing out removes the token from that tab. All permissions
are also enforced by the backend.

### Student API

Use an admin Bearer token in Postman for these endpoints:

| Method | Endpoint | Result |
| --- | --- | --- |
| POST | `/api/students` | 201; new student and Location header |
| GET | `/api/students?search=asha&page=0&size=10&status=ACTIVE` | 200; paginated results |
| GET | `/api/students/{id}` | 200; admin or that student's linked user |
| PUT | `/api/students/{id}` | 200; replace editable details, admin only |
| PATCH | `/api/students/{id}/deactivate` | 200; mark inactive, admin only |
| GET | `/api/students/me` | 200; authenticated STUDENT's linked profile |

`search` matches name, email, admission number, or phone without case sensitivity;
wildcards are treated literally. Search is limited to 100 characters. `status`
is optional. Page numbers start at zero and page sizes range from 1 to 100.
Results are ordered by admission date descending, then ID descending.
The list response is `{ "content": [...], "page": 0, "size": 10,
"totalElements": 1, "totalPages": 1 }`.

Example create/update JSON:

```json
{
  "fullName": "Asha Sharma",
  "email": "asha@example.com",
  "phone": "+919876543210",
  "dateOfBirth": "2005-03-14",
  "gender": "Female",
  "address": "24 Park Road, Pune",
  "guardianName": "Meera Sharma",
  "guardianPhone": "+919876543211",
  "admissionDate": "2026-11-08",
  "userId": null
}
```

Use an admission date that is not in the future. `gender` and `userId` are
optional; other fields are required. Birth date must be in the past and cannot
follow admission date. Phone numbers contain 7–15 digits with an optional
leading `+`; spaces and dashes are not accepted.

Admission numbers are generated on the server as `EDU-` followed by a full
random UUID (without dashes), and protected by a database unique constraint.
They cannot be chosen or changed through a request. New records are ACTIVE.
Deactivation is idempotent, retains all data and account links, and does not
disable the linked login. Reactivation is not exposed in this step.

### Email and account-link rules

- Student admission emails are normalized to lowercase and unique within the
  `students` table, including inactive records. Conflicts return 409.
- An admission email may match an existing User email. Student contact details
  and login credentials are separate; editing a student does not edit a User.
- Matching emails never link accounts automatically. Only an admin can supply
  `userId` to explicitly link an existing STUDENT user. A user can be linked to
  at most one student. Invalid roles return 400, missing users 404, and a user
  already linked elsewhere 409.
- PUT replaces all editable fields: omit `userId` or send null to remove a link.
  The edit form preserves the current link unless an admin clears the field.
- STUDENT access is resolved from the authenticated user, never a supplied
  user ID. They cannot list, create, update, deactivate, or view another profile.

Invalid input returns 400 with field errors, anonymous requests return 401,
denied operations return 403, missing records return 404, and uniqueness
conflicts return 409. Responses contain DTOs, never JPA entities or passwords.

### Student verification

```powershell
.\mvnw.cmd clean verify
cd frontend
npm.cmd test
npm.cmd run build
```

Backend tests include database uniqueness, validation, admin operations,
ownership checks, non-admin write rejection, search, filters, and pagination,
alongside the existing authentication suite. Frontend tests cover API-backed
tables, search and pagination, forms and validation errors, deactivation
confirmation, role-specific controls, Bearer headers, and 401 handling.
Backend tests use H2; frontend component tests mock HTTP responses using
fixtures kept under `src/test`. A live MySQL connection is not part of these tests.

## Frontend dashboard redesign

The UI uses a light indigo/slate theme, locally bundled Inter, Tailwind CSS v4,
Lucide icons, and Recharts. `/dashboard` is a new protected route; `/login`,
`/students`, `/students/me`, and `/students/:id` retain their existing behavior.
Sign-in still opens Students. The root URL now opens the Dashboard.
Authentication, sessionStorage JWT handling, and student API endpoints are unchanged.

`components/UI.jsx` provides shared Button, Input, Select, Modal, Card, Badge,
Table, Pagination, Skeleton, EmptyState, Toast, and ConfirmDialog components.
The sidebar collapses on desktop and uses a modal navigation drawer on mobile.
The profile dropdown includes account details and logout. Global search searches
student records for administrators, with Ctrl/Command+K available on wider screens.
Notifications and unfinished modules are visibly disabled.

The dashboard gets student totals and recent admissions from the existing
paginated student API. It does not invent teacher, batch, fee, attendance, or
schedule data. The revenue chart has an explicit unavailable state with no
fabricated points; it is ready to accept real monthly `{ month, amount }` records.
Non-admin roles receive a role-appropriate workspace without institute-wide data.

No backend changes were made for this redesign. To activate the unavailable
dashboard sections, future backend work needs role-aware aggregates for active
teachers/batches and fees, monthly revenue history, attendance summaries, upcoming
classes, and a notification feed. These are proposed capabilities, not endpoints
that this frontend currently calls.

Run `npm.cmd test` and `npm.cmd run build` from `frontend/` for verification.
Browser checks covered login, sidebar/global navigation, real form submissions
against mocked API responses, add/edit/deactivate, logout, and widths of 390,
768, and 1440 pixels. Browser screenshots contain test fixtures, not production data.
