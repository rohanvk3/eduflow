# EduFlow

Coaching institute ERP with ADMIN, TEACHER, STUDENT and PARENT workspaces. Includes student/teacher records, courses, batches, enrollment history, timetable, attendance, fees, payment receipts, exams, assignments, private study resources, verified parent links, notifications, analytics and administration.

Java 21, Spring Boot 3.5, Maven Wrapper, MySQL 8, Spring Security/JWT and JPA serve a React 19/Vite/Tailwind frontend. This is one backend process and a static frontend; no container infrastructure is required.

## Local setup

Install JDK 21, MySQL 8 and Node 24 with npm. Create a separate empty development database and a restricted database user. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a private `JWT_SECRET` containing at least 32 random bytes encoded as Base64. Do not commit these values. Example secret generation: `openssl rand -base64 32`.

```sh
cd backend
bash mvnw spring-boot:run
# In another terminal
cd frontend
npm ci
npm run dev
```

On Windows use `mvnw.cmd`. Default local schema behavior is Hibernate update; use only a development database. The frontend runs at localhost:5173 and proxies `/api` to localhost:8080. Copy `frontend/.env.example` to `.env.local` only if changing the API base URL. Vite variables are public. See [database setup](docs/DATABASE.md) before starting against existing data.

Public registration creates STUDENT accounts only. Privileged account provisioning requires a trusted administrator. There are no shipped default passwords. Initial administrator provisioning must follow [security guidance](docs/SECURITY.md).

## Verification and builds

```sh
cd backend
bash mvnw clean test
bash mvnw clean package
cd ../frontend
npm ci
npm run lint
npm test
npm run build
```

Tests default to isolated H2, never application MySQL. MySQL tests require explicit opt-in and separate test variables. Production builds yield `backend/target/backend-0.0.1-SNAPSHOT.jar` and `frontend/dist/`.

Read [architecture](docs/ARCHITECTURE.md), [API](docs/API.md), [database](docs/DATABASE.md), [testing](docs/TESTING.md), [security](docs/SECURITY.md), [deployment and recovery](docs/DEPLOYMENT.md), and the evidence-based [release report](docs/FINAL_READINESS_REPORT.md). No production deployment has been performed.

For real-browser tests and the executable isolated MySQL/file recovery rehearsal, follow [verification setup](docs/VERIFICATION.md). `npm run test:e2e` requires that test-only backend; it never targets the development API. Portable JDK 21 download/checksum verification and session-only activation are documented there; no global Java installation change is needed.
