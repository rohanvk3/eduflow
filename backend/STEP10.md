# Step 10 — Study Materials & Learning Resources

The resource library extends the existing Spring Boot/MySQL and React application. Authentication, students, teachers, academics, attendance, finance, exams, assignments, and dark mode are retained. No dependencies, credentials, or application.properties were changed.

## Features and access policy

- Case-insensitive category creation, editing, search, pagination, and deactivation. Names are stripped, whitespace is collapsed, Unicode is normalized with NFKC, and normalized names use Locale.ROOT lowercase. The database enforces a unique normalized name; its configured collation can impose additional accent equivalences. No sample categories are inserted.
- Course and batch folders with nested navigation, breadcrumbs, counts, rename/move, and safe empty-folder deletion. A parent must have the **exact same course and batch scope** as its child. Course/batch scope cannot be changed on an existing folder. Cycles are rejected and hierarchy writes are serialized through the course row.
- Materials support DOCUMENT (including Excel), PRESENTATION, IMAGE, and LINK. Categories are required; folders are optional.
- DRAFT materials are private to authorized staff. Publishing requires a valid file or validated HTTP/HTTPS link. Published metadata is frozen to preserve history. Archiving a published resource preserves its files and removes student access; archiving is terminal in V1.
- Replacements create immutable numbered versions. A material row is locked for upload/publication, and requests carry its optimistic version. Concurrent stale writers receive 409.
- A replacement uploaded to a published material is **staged**. Students still receive publishedVersion; explicit publication advances that pointer. Staff may download all versions within their management scope. Students cannot enumerate/download historical staff versions.
- ADMIN manages all resources, categories, and folders, including course-wide content.
- TEACHER manages only actively assigned batch scopes. Teachers can read related course-wide materials but cannot manage course-wide folders/materials. Category selection is permitted; category management is ADMIN only.
- STUDENT reads only PUBLISHED resources for a current ACTIVE enrollment. Enrollment must have begun, withdrawal must not have taken effect, and completion cannot predate the institute-local day. A course-wide material requires at least one qualifying enrollment in that course. This is current access, not the fixed publication roster used by assignments.
- PARENT receives no new access. All metadata, statistics, selectors, previews, files, and historical downloads enforce backend authorization.

## Storage and validation

Resources reuse Step 9's AssignmentFileStorage abstraction, LocalAssignmentFileStorage, AssignmentFiles transaction callbacks, and content validator. No second public storage directory or authentication mechanism was introduced.

Shared environment configuration:

| Variable | Meaning |
|---|---|
| APP_ASSIGNMENTS_STORAGE_DIR | Private storage root for both assignment files and resources. Default: user-home/.eduflow/assignment-files |
| APP_ASSIGNMENTS_MAX_FILE_BYTES | Maximum size per file. Default: 10485760 bytes (10 MiB); configurable from 1 KiB to 50 MiB |

Set environment variables before starting the backend. Keep the existing directory when restarting, and back it up together with the database. Changing the path without moving stored files makes existing files unavailable.

- PDF, DOCX, PPTX, XLSX, UTF-8 TXT, PNG, and JPEG are accepted. ZIP upload is intentionally unsupported.
- Filenames with traversal/separators/control characters are rejected. Storage keys are randomized UUIDs, never returned in DTOs.
- PDF content is parsed using the existing PDFBox dependency. Encrypted PDFs, JavaScript/actions, embedded files, and excessive complexity are rejected.
- Images require matching signatures and successful decoding with dimension/pixel bounds.
- OOXML containers require the correct primary document, content type, and package relationship. XML parsing disables external entities/DTDs. Macro/ActiveX/embedded executable content, external relationships, traversal entries, duplicate entries, and excessive ZIP expansion are rejected. This deliberately rejects some Office files with external links.
- TXT requires valid UTF-8 without binary control characters.
- Declared MIME must match verified content, or be omitted/application/octet-stream. Responses use the server-derived MIME.
- Rollback deletes newly written files. Historical versions are never deleted by the resource API.
- Downloads use attachment disposition; PDF/image previews use inline disposition. All binary responses have no-store, nosniff, and a sandbox content policy.
- React fetches private blobs through shared authenticated Axios, then revokes object URLs on preview close/unmount. JWTs are never put in URLs.
- External links must be absolute HTTP/HTTPS URLs without embedded credentials; frontend links use noopener noreferrer.

The validator/storage boundary is the extension point for a future antivirus service or object-storage adapter. No malware scanner or cloud adapter is claimed. After an abrupt process crash, orphan reconciliation is an operational task; transaction rollback handles normal failures.

## REST endpoints

All paths below are relative to /api/resources.

| Method | Path | Purpose |
|---|---|---|
| POST / GET | /categories | Create or paginated category directory |
| GET / PUT | /categories/{id} | Read or edit category |
| PATCH | /categories/{id}/deactivate | Deactivate without deleting referenced resources |
| POST / GET | /folders | Create or scoped folder directory |
| GET / PUT / DELETE | /folders/{id} | Read, rename/move, or delete an empty folder |
| POST / GET | /materials | Create draft or paginated library |
| GET / PUT | /materials/{id} | Read material or edit draft metadata |
| POST | /materials/{id}/publish | Publish draft/current staged version |
| POST | /materials/{id}/archive | Archive published material |
| GET | /materials/{id}/download | Authorized current/published file |
| GET | /materials/{id}/preview | Authorized PDF/image preview |
| GET / POST | /materials/{id}/versions | Staff history or upload a replacement |
| GET | /materials/{id}/versions/{versionId}/download | Staff historical download |
| GET | /me | Student's published materials |
| GET | /stats | Authorized counts, course/batch/category distributions, recent materials |
| GET | /options | Authorized course/batch choices, bounded to 1000 records |
| GET | /upload-policy | Shared server limit and accepted extensions |

Material directory filters: courseId, batchId, categoryId, folderId, resourceType, visibility, search, uploadedBy, page, size, sort. Sorting is allowlisted. Page sizes are 1–100.

Folder filters: courseId, batchId, parentFolderId, rootOnly, courseWideOnly, search, page, size. Folder pickers restrict the exact course/batch scope. Category directory supports search/status/page/size. Non-admin category choices are active only; existing materials retain inactive category labels.

### Example requests (Postman)

Use the existing login endpoint to get a JWT. Select Bearer Token authorization. Do not place the token in the URL.

1. ADMIN: POST /api/resources/categories

    {"name":"Lecture Notes","description":"Class notes"}

2. Authorized staff: POST /api/resources/materials with JSON for a link/draft:

    {"title":"Mechanics reference","description":"Chapter 1","courseId":1,"batchId":2,"categoryId":3,"resourceType":"LINK","externalUrl":"https://example.org/notes"}

Use actual existing IDs. A teacher must supply an assigned batch.

3. To create a file material atomically, POST multipart/form-data to /api/resources/materials:

   - data: JSON part with Content-Type application/json; same metadata with resourceType DOCUMENT/IMAGE/PRESENTATION and no externalUrl.
   - file: binary file part.

   Invalid uploads roll back material creation.

4. POST /api/resources/materials/{id}/publish with {"version": returnedMaterial.version}.

5. POST multipart/form-data to /api/resources/materials/{id}/versions?version={currentOptimisticVersion}, with file and optional changeNotes. Version numbers are assigned by the server.

6. Students GET /api/resources/materials/{id}/download continue to receive the previous published file until another explicit publish. Students cannot call the historical-version download API.

POST publish/archive and PUT metadata must use the latest returned optimistic version. On 409, reload first; do not blindly repeat an outdated operation.

## Frontend routes and behavior

- /resources — responsive grid/list library, filters, pagination, resource types, scoped statistics, preview and download.
- /resources/new — upload/drop file or link, scoped course/batch choices, category/folder selection, progress, save draft/publish, confirmation and validation.
- /resources/materials/:id — details, authenticated preview/download, staged replacement, explicit publication/archive, staff version history.
- /resources/materials/:id/edit — draft metadata.
- /resources/categories — administrator category management.
- /resources/folders — scoped folder explorer with nested breadcrumbs, create/rename/move, and deletion confirmation.
- /my-resources — student library; for teachers, defaults to their uploads within authorized scopes.
- Existing role dashboards include real resource totals, recent uploads, and course/category distributions. Counts refresh every 30 seconds, on focus, and after mutations.

## Database changes

database/step10-resources-schema.sql creates four tables only:

- resource_categories
- resource_folders
- study_materials
- study_material_versions

They reference the existing courses, batches, and users. A unique material/version-number pair protects history; material scope/folder indexes support browsing. There are no cascading history deletions.

The existing local ddl-auto=update setting creates the additive tables at startup and was not changed. For deployments with automatic schema changes disabled, select the existing database and run the supplied SQL before starting the new backend. Existing tables and rows are not reset.

## Created and modified files

Backend additions under src/main/java/com/eduflow/backend:

- entity: ResourceRecordBase, ResourceCategory, ResourceFolder, StudyMaterial, StudyMaterialVersion, ResourceType, ResourceVisibility.
- repository: ResourceCategoryRepository, ResourceFolderRepository, StudyMaterialRepository, StudyMaterialVersionRepository.
- dto/ResourceDtos.java.
- service/ResourceAccess.java, ResourceOrganizationService.java, StudyMaterialService.java.
- controller/ResourceController.java.

Modified backend file: service/AssignmentFileValidator.java extends the shared validator for resource formats; assignment uploads retain their original format allowlist.

Frontend additions under frontend/src:

- services/resourceService.js, resourceCategoryService.js, resourceFolderService.js.
- components/ResourceUI.jsx, ResourceDashboard.jsx.
- pages/ResourcesPage.jsx, ResourceFormPage.jsx, ResourceDetailsPage.jsx, ResourceCategoriesPage.jsx, ResourceFoldersPage.jsx.
- test/Resources.test.jsx.

Modified frontend: App.jsx, components/Layout.jsx, pages/DashboardPage.jsx, test/Dashboard.test.jsx. Step 9's chart test timeout was adjusted to allow its lazy Recharts import to complete.

Other additions: ResourceManagementTests.java, ResourceMySqlTests.java, additive schema script, this guide. No new npm/Maven dependency was needed.

## Manual verification

1. ADMIN: create a category. Try a differently cased duplicate and expect 409.
2. Create a course-wide folder and a separate batch folder using existing courses/batches. Create a child with the same scope. Rename it, try a cycle, and verify nonempty-folder deletion is rejected.
3. Upload a real PDF into a draft with the matching course/batch/category/folder. Verify progress, details and version 1. A renamed executable/fake PDF must fail.
4. Log in as an eligible student: the draft must be absent and its direct URL forbidden.
5. Publish as authorized staff. Student library/filters must now show it. Preview/download require JWT; changing the ID to another batch's material must fail.
6. Upload version 2. Verify staff sees both versions while students still download version 1. Publish again and verify students receive version 2.
7. Check image preview, UTF-8 TXT, DOCX/PPTX/XLSX downloads, and safe external links. Office files use information/download rather than an unsupported inline viewer.
8. Deactivate the category: existing published materials remain available. It cannot be selected for a new material.
9. Archive the material: it disappears from student views, but staff history remains.
10. TEACHER: unrelated batch IDs, course-wide management and category mutations must fail. PARENT access must fail.
11. Check grid/list, filters/pagination, dark/light themes, mobile layout, and existing login/logout/student/finance/exam/assignment screens.

## Verification and limits

Executed verification on 10 November 2026:

| Check | Result |
|---|---|
| Full Maven regression | 182 discovered; **171 passed**, 11 opt-in MySQL tests skipped; no failures/errors |
| ResourceManagementTests | **21 passed**, included in the regression total |
| Opt-in ResourceMySqlTests against MySQL 8.0.46 | **2 passed**, including the inherited academic integration test and the resource/version/publication/download flow |
| Step 9 opt-in AssignmentMySqlTests | **2 passed** at the Step 9 completion checkpoint |
| Frontend npm test | **112 passed** across 11 files, including 18 resource component tests |
| npm run build | **Passed** |
| Maven package | **Passed** |
| Live Edge smoke | Passed read-only library, form/upload policy, categories, folders, grid/list control, themes, existing module navigation and logout; student portal checked; 390/768/1440 widths without horizontal page overflow |

The installed Java runtime used for these checks is Java 22; the project retains its Java 21 compilation target. A separate Java 21 runtime execution was not performed.

Reproduce with:

- .\mvnw.cmd test — isolated H2 full regression tests.
- With existing DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET and EDUFLOW_MYSQL_TEST=true: .\mvnw.cmd -Dtest=ResourceMySqlTests test. The test uses new UUID-tagged fixtures inside rollback transactions and verifies file rollback cleanup.
- frontend: npm test and npm run build.

The live browser smoke test is read-only against Vite → Spring Boot → MySQL. Backend HTTP tests exercise writes with actual MySQL; frontend component tests exercise create/upload/preview/version UI interactions. Browser write workflows are not claimed as manually verified against production data.

V1 limits: no parent linking, malware-scanning service, cloud adapter, ZIP uploads, Office inline rendering, download tracking, resource deletion, or restoration of archived materials. None of these is exposed as a working placeholder. Course/batch selector payloads are bounded at 1000; staff libraries and resource/folder/category directories are paginated. Published metadata is intentionally immutable; create another draft if its scope/title/category must change. Archived/historical files require retention and backup.
