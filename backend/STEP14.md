# Step 14 — Administration and hardening

## Prioritized audit plan

1. Critical: add live account status and JWT security-version checks. Keep Bearer
   transport, BCrypt, existing claim issuer and authorization checks.
2. Critical: serialize admin account changes; prohibit self-lockout, last-admin
   removal, and role changes incompatible with existing linked profiles.
3. Add singleton institute settings with private validated logo storage and
   settings-backed report headers. Existing modules currently use IST/INR, so
   changing their financial currency/timezone requires a separate migration.
4. Add hashed single-use recovery tokens and administrator-assisted handoff; no
   email provider exists, so do not claim email delivery.
5. Append safe audit events transactionally to sensitive mutations, centralize
   request IDs/error metadata and suppress internal database exception text.
6. Build connected admin/account pages using existing shared UI, routing and JWT.
7. Run API, security, regression, UI/build and real-database checks. Record actual
   outcomes and remaining operational limitations below.

Initial findings: no user status/revocation fields; JWT filter did not check
UserDetails enabled/locked flags; static institute headers; no general admin
management or password recovery. Existing ownership checks, private randomized
file storage, upload signature validation, transactional finance, notification
outbox/coordinator locks, and bounded reports are retained. Existing domain audit
tables remain intact. Global errors needed metadata and a safe unexpected-error
fallback. Existing package is com.eduflow.backend, reused without restructuring.

## Implemented administration

- Singleton institute settings, validated contacts/address/site, academic dates,
  branding and private PNG/JPEG logo replacement. Logo maximum 1 MiB, dimensions
  2000×2000, extension/signature plus ImageIO decoding; SVG is not accepted.
  Randomized storage uses the existing AssignmentFileStorage abstraction. Failed
  transactions clean up the new file; replaced files are removed after commit.
  No storage keys or filesystem paths appear in settings/branding responses.
- Settings-backed institute names in fee receipts and advanced PDF reports, plus
  sidebar branding. `/api/institute/branding` exposes name/code/logo presence only
  to authenticated users; logo downloads require authentication too.
- Control Center uses actual profile/account/course/batch counts, recent audit
  events, configuration descriptions, and missing-profile-link observations.
  These are not invented service-health measurements or a full integrity scan.
- Paginated user directory with name/email search, role/status filters, detail
  dialog, activate/deactivate/lock/unlock, guarded single-role transitions and
  verified recovery handoff. Existing Student registration and Parent profile
  workflows create accounts; ordinary user-management APIs never accept passwords.
- All roles can edit their own account display name and change their password.
  Login email/role/status are read-only there. Existing linked Student/Teacher/
  Parent profile editors retain their separate permitted contact fields.
- Dedicated admin audit viewer with search, action/actor/entity/outcome/date
  filters, pagination and event detail dialog.
- Existing router/sidebar/dialog/table/loading/focus conventions are reused;
  new routes are lazy-loaded, role-aware and responsive. Profile/security links,
  meaningful breadcrumbs, actual institution branding, registration/recovery and
  success/error feedback replace disconnected administration flows.

## Security and recovery policy

Users gain ACTIVE/INACTIVE/LOCKED status, securityVersion (default 0), and an
optional recovery-request timestamp. Existing accounts default to ACTIVE and
version 0; no password, role or profile relationship is rewritten by migration.

The existing JWT architecture stays intact. Login issues an `sv` claim. Requests
perform a live indexed UserDetails lookup for role, enabled/locked status and
securityVersion. Missing `sv` is compatible with pre-change version-0 tokens.
Status changes, role changes, password changes and completed recovery increment
the version; old access tokens then receive 401. Unlock/reactivation does not
restore old tokens. New login uses the current version. Login and password writes
use row locks; security-version checks after waiting refresh the locked row to
avoid lost updates. There are no refresh tokens or per-device session records.
Ordinary logout keeps the existing client-side token removal policy. In-flight
requests already authenticated can finish; revocation applies to subsequent checks.

Admin status/role mutations serialize on the singleton institute row and lock the
target User. They recheck the actor's active ADMIN state after acquiring the lock.
Self-deactivation, self-locking and self-role changes are refused. At least one
active ADMIN must remain. A role incompatible with an existing linked Student,
Teacher or ParentProfile is refused; profiles are never silently unlinked or roles
changed automatically. The application retains one Role enum per user.

Password changes verify the current BCrypt password, require a different password
with at least 12 characters, a letter and a number, and no more than 72 UTF-8 bytes.
Successful changes sign out all existing tokens, including the current browser.
Existing public registration's contract (STUDENT role, minimum 8 characters) is
preserved; the new registration page uses that supported API.

There is no email provider. Recovery therefore works as follows:

1. An unauthenticated recovery request always returns the same assistance message
   for existing/missing accounts. It neither sends nor claims to send an email,
   and never returns a reset token. A known account's request flag becomes visible
   only to admins. Requests are limited to one per minute per hashed remote client
   address in MySQL; repeated requests keep the same generic response.
2. An ADMIN verifies identity outside the application and explicitly confirms
   private handoff. The protected issuance endpoint returns one 256-bit random,
   single-use token, valid for 15 minutes. Issuance is limited to once per minute
   per account. Only SHA-256 hashes are persisted. Disabled/locked accounts must
   be explicitly reactivated/unlocked before recovery.
3. The dialog constructs a private `/recover-account#token` link in memory. The
   fragment is not sent in HTTP requests and is removed from browser history on
   consumption. Tokens are not stored in local/session storage or audit text.
4. Confirmation validates hash, expiry, unused state, active account and security
   version under locks, hashes the new password, marks the token used and revokes
   existing access tokens and other outstanding recovery tokens. The response is
   204; no credential or JWT is returned. The user then signs in normally.

Account reset/role operations do not delete financial, academic or profile data.
There is no new MFA, email transport or claim of automated identity verification.
An authorized admin must protect the private handoff; it is a bearer recovery
credential and must not be pasted into messages, tickets, logs or screenshots.

## Audit, errors and configuration

New audit records capture actor ID/role, action, entity type/ID, safe description,
outcome, UTC timestamp and generated request ID. No passwords, hashes, JWTs,
recovery tokens, payment references, request bodies, IP addresses or user agents
are recorded in audit payloads. Actions cover:

- Public User creation and parent profile/account creation.
- Account status/role, profile/password and recovery issuance/completion changes.
- Parent link creation/verification/revocation (existing domain audit retained).
- Successful payment recording and explicit voiding (existing financial audit retained).
- Exam result publication and assignment grading/corrections (existing history retained).
- Institute configuration/logo changes and rejected sensitive admin requests.

Success records commit in the business transaction, so an audit persistence failure
rolls back the sensitive operation. Failures for controller-handled admin mutations
use a separate transaction and store only a generic rejection event. Requests
rejected by the security filter before controller handling are not persisted as
admin events. Scheduler actions without a current principal have no actor ID.
Old domain history is preserved; general audit events begin at this implementation,
not as fabricated backfilled activity.

AuditLog is Hibernate-immutable and has no create/update/delete REST endpoints.
Only ADMIN can read it. Application permissions make the log append-only; a DB
administrator/root can still modify storage externally. This is not cryptographic
WORM storage. Retention policy: no automatic audit purge; retain until the institute
approves an archival/retention policy. Expired/used reset hashes and throttle rows
are also retained by default; any maintenance purge needs an explicit operational
policy. No audit deletion was performed during implementation.

Existing error fields `status`, `message`, `errors` remain. Additive metadata:
`timestamp`, `errorCode`, `path`, `requestId`. Request IDs are server-generated
UUIDs, also returned in X-Request-ID; client IDs are not trusted. Unexpected errors
return a generic 500, with only exception class/request ID logged. Invalid data
query usage no longer returns ORM/SQL query text. Existing validation, conflict,
403/401, 404, 413 and stale/concurrent change handling remains compatible.

Optional property: `app.cors.allowed-origins` (environment
`APP_CORS_ALLOWED_ORIGINS`), comma-separated explicit HTTP/HTTPS origins, default
`http://localhost:5173`. Wildcards, user info, paths, fragments and queries fail
configuration validation. JWT remains in Authorization headers, not auth cookies;
stateless sessions and the existing CSRF policy are retained. No protected API was
made public; only the two recovery POST routes join the existing auth/public allowlist.
Existing JWT secret checks/environment configuration and DB credentials are untouched.

Current business modules use Asia/Kolkata and INR. Settings validate standard
timezone IDs and explicitly reject a different zone/currency rather than display
inconsistent balances or dates. Supporting another accounting currency/timezone
requires a deliberate data/logic migration, not a cosmetic setting change.
Uploads still use `app.assignments.storage-dir` and existing resource limits;
no Windows path is hardcoded in new application code.

## REST endpoints

| Method | Endpoint | Access |
| --- | --- | --- |
| GET | `/api/admin/control-center` | ADMIN |
| GET, PUT | `/api/admin/institute-settings` | ADMIN |
| POST | `/api/admin/institute-settings/logo` (multipart `file`) | ADMIN |
| GET | `/api/admin/users` | ADMIN |
| GET | `/api/admin/users/{id}` | ADMIN |
| PATCH | `/api/admin/users/{id}/status` (`status`) | ADMIN |
| PATCH | `/api/admin/users/{id}/roles` (`role`) | ADMIN |
| POST | `/api/admin/users/{id}/password-reset` | ADMIN, verified private handoff |
| GET | `/api/admin/audit-logs` | ADMIN |
| GET | `/api/admin/audit-logs/{id}` | ADMIN |
| GET, PUT | `/api/settings/profile` (`fullName`) | Own authenticated account |
| PUT | `/api/settings/security/password` (`currentPassword`, `newPassword`) | Own account |
| GET | `/api/institute/branding`, `/api/institute/logo` | Authenticated branding only |
| POST | `/api/auth/password-reset/request` (`email`) | Public, generic response |
| POST | `/api/auth/password-reset/confirm` (`token`, `newPassword`) | Public token possession |

Users filter `search`, `role`, `status`, `page`, `size`. Audit filters `search`,
`action`, `actorUserId`, `entityType`, `outcome`, `fromDate`, `toDate`, `page`,
`size`; date policy reuses the inclusive institute-local 366-day bounded range.
Settings PUT includes the current response `version`; stale writes return 409.
Settings/Account/Audit DTOs never expose JPA graphs, storage keys or password hashes.

## Database and files

`database/step14-administration-schema.sql` is additive and rerunnable on the
selected existing MySQL database. It checks information_schema before adding
`users.status`, `users.security_version`, `users.reset_requested_at`, then creates
`institute_settings`, `audit_logs`, `password_reset_tokens`, `recovery_throttles`.
The application initializes settings id=1 only if absent. No destructive migration,
drop/truncate, duplicate-record cleanup or change to application.properties occurs.
Existing local Hibernate update can create the same additions. Existing unique
constraints for enrollment, attempts, submissions, links, payments and notification
deduplication, transaction boundaries and DECIMAL finance fields are retained.

Created backend files under `src/main/java/com/eduflow/backend`:

- entity: AccountStatus, InstituteSettings, AuditLog, PasswordResetToken, RecoveryThrottle.
- repository: AdministrationRepository, AuditLogRepository.
- dto: AdminDtos.
- service: InstituteSettingsService, AdministrationService, AccountSettingsService,
  AuditRecorder.
- controller: AdministrationController, AccountSettingsController.
- config: RequestContextFilter, SensitiveFailureAdvice.
- security: AccountPrincipal.

Modified backend: User, UserRepository, JwtService, JwtAuthenticationFilter,
SecurityConfig, AuthService, CorsConfig, ApiError, GlobalExceptionHandler,
NotificationOutbox, FinanceService, ParentManagementService, FinanceReceiptService,
ReportExportService. Existing business behavior is retained except the explicit
account security checks and transactional audit hooks.

Created frontend: services/adminService.js, components/InstituteBrand.jsx,
pages/AdministrationPage.jsx, pages/AccountRecoveryPage.jsx,
pages/AccountRegistrationPage.jsx, test/Administration.test.jsx.
Modified: App.jsx, components/Layout.jsx, pages/LoginPage.jsx, auth/AuthContext.jsx,
api/client.js. Public auth requests omit stale session headers, while all protected
requests continue to use the existing JWT interceptor.
Backend tests: AdministrationTests, AdministrationMySqlTests. No new dependency.

## Manual checklist

1. ADMIN: open `/admin/control-center`. Compare real counts and recent activity.
   Open settings; edit name/contact/year; test stale version, bad URL/timezone and
   invalid logo. Upload a valid small PNG/JPEG and check branding and PDF headers.
2. Open `/admin/users`, search/filter/page and view a user. Try self-lock/role changes
   and changing a linked teacher's role; expect 409. Deactivate or lock a test
   account; its old token and login must fail. Reactivate it; old token must still
   fail while a fresh login succeeds. Do not perform these writes on real accounts
   merely to test the implementation.
3. Change your own display name. Try wrong current password, weak new password,
   then valid change on a test account. Verify sign-out and old JWT rejection.
4. Request recovery for known/unknown emails: same message, no token/email claim.
   Admin verifies a test user and issues a private link. Open it, confirm its URL
   fragment disappears, reset, sign in, then reject token reuse/expiry. Do not
   copy a real recovery credential into QA logs or screenshots.
5. Open Audit Logs; test filters, pages/details and safe request IDs. Confirm
   account, institute, parent verification, payment void, result publication and
   grading events appear after real supported workflows. Ordinary roles cannot
   read audit logs or mutate institute/users.
6. Recheck student/parent/teacher IDOR, files, reports, notifications and existing
   module workflows. Check mobile drawer, keyboard dialog close/focus return,
   profile dropdown, light/dark mode, and 390/768/1440 widths.

## Verification and operational limits

Executed verification (10–11 November 2026):

- Full Maven suite: 323 discovered, 304 passed, 19 optional MySQL cases skipped;
  zero failures/errors (`target/step14-all-tests.log`). This includes the existing
  finance, attendance, exams, assignments, resources, parents, notification and
  analytics regression suites.
- Final focused administration/authentication checks: 58 passed, including
  missing-route/missing-file/corrupt-image error handling
  (`target/step14-final-tests.log`).
- Additional opt-in real MySQL AdministrationMySqlTests: 2 passed, rollback-only
  account/settings fixtures. Verified real status revocation, recovery, audit and
  settings-backed PDF header (`target/step14-mysql-tests.log`).
- Frontend: 177 tests passed across 15 files; production build passed
  (`target/step14-frontend-tests.log`, `target/step14-build.log`).
- Maven package succeeded (`target/step14-package.log`). A prior package attempt
  hit the running JAR's Windows file lock; stopping the verified backend process
  before rebuilding resolved it. Source release remains Java21; installed test
  runtime is JDK22, so no native JDK21 runtime test is claimed.
- The additive schema script was executed twice against the existing MySQL
  schema successfully (`target/step14-migration-check.log`); no reset/drop/truncate
  statements or existing business-record deletions were involved.
- A final authentication-only rerun passed 25 tests after preserving the existing
  provider's unknown-account dummy-password timing mitigation alongside row locking
  (`target/step14-auth-final.log`). The final JAR package succeeded afterward.
- Live Edge through Vite/Spring/MySQL checked control center, institute settings,
  users and user dialog, audit viewer, profile, security, public recovery and logout
  at widths 390/768/1440 with no browser exceptions. This found and fixed mobile
  password-action overflow. Screenshots/log: `target/step14-*.png`,
  `target/step14-browser.log`. These are read-only browser checks; mutations are
  verified with automated API/MySQL fixtures and component tests.

On the first initialization only, `app.institute.name` seeds the singleton name
(default EduFlow). Subsequent database settings take precedence; startup never
overwrites an existing record with a configuration default.

Tests use rollback fixtures; no real account passwords/statuses are changed for QA.
This is application hardening, not an external penetration-test or accessibility
certification. No email/MFA provider, per-device sessions, cryptographic audit vault,
malware scanner or historical-audit backfill was introduced. Existing private file
validation is retained; production transport, database privileges, backups, proxy
rate limiting and operational retention remain deployment responsibilities.
