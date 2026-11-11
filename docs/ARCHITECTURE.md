# Architecture

The Vite SPA uses React Router, centralized Axios (`frontend/src/api/client.js`), sessionStorage bearer tokens and role-aware navigation. Major modules load lazily. Page/service modules call `/api`; development Vite proxies that prefix and production Nginx must forward it unchanged.

The executable Spring Boot JAR uses package `com.eduflow.backend` (preserved to avoid breaking scanning/imports). Controllers validate DTOs; transactional services enforce roles, ownership, teacher batch assignments and verified parent relationships. Spring Data repositories persist to MySQL. Responses use DTOs rather than serializing password-bearing user entities.

JWT authentication reloads accounts and checks their security version on every authenticated request, so account locking, deactivation and password/role changes can revoke tokens. Financial amounts use BigDecimal. Account locks, optimistic versions and uniqueness constraints protect concurrent mutations. Exams use server time and snapshot questions; notifications use persistent events and deduplication keys.

Private attachments, study material versions and institute logos share configurable filesystem storage behind authorized APIs. Database records and files must be backed up together. No static upload mapping should be configured.

Two scheduler threads handle exam expiry and notification work. Run a single application instance until database coordination and concurrency are verified on MySQL under expected load. Scheduler failures retry and log their exception type without message payloads.

The application currently supports INR and Asia/Kolkata academic/financial dates. These are enforced in institute settings. Do not change the JVM timezone expecting stored dates to migrate. See limitations in the release report.
