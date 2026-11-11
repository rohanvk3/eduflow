# API reference

Base path `/api`. Send `Authorization: Bearer <token>` except public health/login/registration/recovery endpoints. Send JSON content type for JSON mutations. Files use multipart form data. Pagination/filter contracts vary by module; consult each module reference below and its controller DTOs before integrating.

| Area | Reference (routes, request and response contracts, authorization) |
|---|---|
| Authentication, students | [Backend reference](../backend/README.md) |
| Teachers | [Teacher reference](../backend/TEACHERS.md) |
| Courses, batches, enrollments, schedules | [Step 5](../backend/STEP5.md) |
| Attendance sessions, marking, reports | [Step 6](../backend/STEP6.md) |
| Fee structures, accounts, payments, receipts, exports | [Step 7](../backend/STEP7.md) |
| Exams, attempts, gradebooks, published results | [Step 8](../backend/STEP8.md) |
| Assignments, submissions, attachments, grading | [Step 9](../backend/STEP9.md) |
| Resources, categories, versions, downloads | [Step 10](../backend/STEP10.md) |
| Parent profiles and verified child portals | [Step 11](../backend/STEP11.md) |
| Announcements, notifications, preferences | [Step 12](../backend/STEP12.md) |
| Analytics, report preview/CSV/PDF | [Step 13](../backend/STEP13.md) |
| Administration, audit, settings and recovery | [Step 14](../backend/STEP14.md) |

`GET /api/health` is public process liveness only. `GET /api/health/readiness` requires ADMIN, checks a database connection, returns only `{"status":"UP"}` (200) or `{"status":"DOWN"}` (503). It exposes no configuration. Other roles receive 403 and anonymous requests receive 401. No actuator environment endpoint is installed.

Common errors: 400 invalid input, 401 invalid/expired/revoked token, 403 role or ownership rejection, 404 missing resource, 409 stale version/duplicate/conflicting change, 413 upload limit, 500 internal/storage failure. Errors include a request ID, also returned as `X-Request-ID`. Preserve it for support; do not log tokens or student payloads. Clients must handle these errors without retrying non-idempotent mutations blindly.
