import { test, expect, request } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const read = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
const state = read(read(path.resolve('../.verification/current.json')).stateFile);
const f = read(path.join(state.directory, 'fixtures.json'));
const base = 'http://127.0.0.1:18081/api';
const clients = {};
const tag = crypto.randomUUID().slice(0, 8);
let createdStudent, course, batch, enrollment, material, category, firstImage, secondImage;
test.describe.configure({ mode: 'serial' });
async function api(actor, method, route, data, status = 200) {
  const r = await clients[actor].fetch(base + route, { method, data });
  expect(r.status(), `${method} ${route}: ${await r.text()}`).toBe(status);
  return status === 204 ? null : r.json();
}
async function login(page, actor) {
  await page.goto('/login'); await page.getByLabel('Email address').fill(actor + '@e2e.invalid');
  await page.getByLabel('Password', { exact: true }).fill(f.password);
  await page.getByRole('button', { name: /Sign in/i }).click(); await expect(page).not.toHaveURL(/\/login$/);
}
test.beforeAll(async () => {
  for (const actor of ['admin', 'teacher', 'student', 'other', 'parent']) {
    const r = await request.newContext();
    const auth = await r.post(base + '/auth/login', { data: { email: actor + '@e2e.invalid', password: f.password } });
    expect(auth.ok()).toBeTruthy(); const { token } = await auth.json(); await r.dispose();
    clients[actor] = await request.newContext({ extraHTTPHeaders: { Authorization: `Bearer ${token}` } });
  }
});
test.afterAll(async () => { for (const c of Object.values(clients)) await c.dispose(); });

test('ADMIN creates teacher, student, course, batch, enrollment and timetable, then reviews audit logs', async ({ page }) => {
  const settings = await api('admin', 'GET', '/admin/institute-settings');
  await api('admin', 'PUT', '/admin/institute-settings', { ...settings, instituteName: 'EduFlow Synthetic Acceptance' });
  const faculty = await api('admin', 'POST', '/teachers', { employeeId: `QA-${tag}`, fullName: 'Acceptance Faculty', email: `${tag}@faculty.invalid`, phone: '9876543210', joiningDate: f.today }, 201);
  createdStudent = await api('admin', 'POST', '/students', { fullName: 'Acceptance Sibling', email: `${tag}@student.invalid`, phone: '9876543210', dateOfBirth: '2008-01-01', address: 'Synthetic fixture', guardianName: 'Acceptance Guardian', guardianPhone: '9876543211', admissionDate: f.today }, 201);
  course = await api('admin', 'POST', '/courses', { code: `QA-${tag}`, name: 'Acceptance Chemistry', durationWeeks: 12 }, 201);
  const end = new Date(f.today + 'T12:00:00Z'); end.setMonth(end.getMonth() + 2);
  batch = await api('admin', 'POST', '/batches', { code: `QA-${tag}`, name: 'Acceptance Batch', courseId: course.id, capacity: 10, startDate: f.today, endDate: end.toISOString().slice(0, 10), status: 'ACTIVE' }, 201);
  await api('admin', 'POST', `/batches/${batch.id}/teachers`, { teacherId: faculty.id }, 201);
  enrollment = await api('admin', 'POST', `/batches/${batch.id}/enrollments`, { studentId: createdStudent.id, enrollmentDate: f.today }, 201);
  const guardian = await api('admin', 'POST', '/parents', { fullName: 'Acceptance New Guardian', email: `${tag}@guardian.invalid`, password: f.password, phoneNumber: '9876543212' }, 201);
  const guardianLink = await api('admin', 'POST', '/parents/links', { parentId: guardian.id, studentId: createdStudent.id, relationshipType: 'GUARDIAN', primaryGuardian: false, reason: 'Synthetic admin parent creation' }, 201);
  await api('admin', 'POST', `/parents/links/${guardianLink.id}/verify`, { version: guardianLink.version, reason: 'Synthetic identity verification' });
  await api('admin', 'POST', `/batches/${batch.id}/schedules`, { teacherId: faculty.id, dayOfWeek: 'MONDAY', startTime: '14:00', endTime: '15:00', room: 'QA Lab', active: true }, 201);
  await login(page, 'admin');
  for (const route of [`/teachers/${faculty.id}`, `/students/${createdStudent.id}`, `/courses/${course.id}`, `/batches/${batch.id}`, '/admin/institute-settings', '/admin/audit-logs']) {
    await page.goto(route); await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  }
  expect((await api('admin', 'GET', '/admin/audit-logs')).totalElements).toBeGreaterThan(0);
  await api('teacher', 'GET', `/batches/${batch.id}`, undefined, 403);
});

test('TEACHER and STUDENT view assigned/enrolled timetable and academic progress', async ({ page }) => {
  if (!(await api('admin', 'GET', `/batches/${f.batchId}/schedules`)).some(s => s.room === 'E2E Room'))
    await api('admin', 'POST', `/batches/${f.batchId}/schedules`, { teacherId: f.teacherId, dayOfWeek: 'TUESDAY', startTime: '11:00', endTime: '12:00', room: 'E2E Room', active: true }, 201);
  for (const actor of ['teacher', 'student']) {
    await login(page, actor); await page.goto(`/batches/${f.batchId}`);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    expect((await api(actor, 'GET', `/batches/${f.batchId}/schedules`)).some(row => row.room === 'E2E Room')).toBe(true);
    await page.getByRole('button', { name: 'Logout', exact: true }).click();
  }
  await login(page, 'student');
  for (const route of ['/batches/my-enrollments', '/attendance', '/my-results', '/my-progress']) {
    await page.goto(route); await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  }
});

test('ADMIN verifies a second parent child link and PARENT switches between linked children', async ({ page }) => {
  const parent = await api('parent', 'GET', '/parents/me');
  const link = await api('admin', 'POST', '/parents/links', { parentId: parent.id, studentId: createdStudent.id, relationshipType: 'GUARDIAN', primaryGuardian: false, reason: 'Synthetic second child verification' }, 201);
  await api('parent', 'GET', `/parents/children/${createdStudent.id}/fees`, undefined, 403);
  await api('admin', 'POST', `/parents/links/${link.id}/verify`, { version: link.version, reason: 'Synthetic verification complete' });
  // The earlier revocation scenario may have revoked the original child; explicitly reverify only that synthetic link.
  const original = (await api('admin', 'GET', `/parents/links?studentId=${f.studentId}`)).content.find(x => x.parentId === parent.id);
  if (original.status !== 'VERIFIED') {
    const pending = original.status === 'REVOKED' ? await api('admin', 'POST', '/parents/links', { parentId: parent.id, studentId: f.studentId, relationshipType: 'GUARDIAN', primaryGuardian: false, reason: 'Synthetic new verification request' }, 201) : original;
    await api('admin', 'POST', `/parents/links/${pending.id}/verify`, { version: pending.version, reason: 'Synthetic multi-child acceptance' });
  }
  await login(page, 'parent'); await page.goto('/parent/dashboard');
  const selector = page.getByRole('combobox', { name: 'Select child', exact: true });
  await selector.selectOption(String(createdStudent.id)); await expect(page).toHaveURL(new RegExp(`studentId=${createdStudent.id}`));
  await expect(page.getByText('Acceptance Sibling', { exact: true }).first()).toBeVisible();
  await selector.selectOption(String(f.studentId)); await expect(page).toHaveURL(new RegExp(`studentId=${f.studentId}`));
  await expect(page.getByText('E2E-ONE', { exact: true }).first()).toBeVisible();
});

test('TEACHER uploads and publishes a real study resource through the browser form', async ({ page }) => {
  category = await api('admin', 'POST', '/resources/categories', { name: `Acceptance Notes ${tag}`, description: 'Synthetic category' }, 201);
  await login(page, 'teacher'); firstImage = await page.screenshot();
  await page.goto('/resources/new');
  await page.getByLabel('Title *', { exact: true }).fill('Acceptance Versioned Notes');
  await page.getByRole('combobox', { name: 'Resource type', exact: true }).selectOption('IMAGE');
  await expect(page.getByRole('combobox', { name: 'Course', exact: true }).locator(`option[value="${f.courseId}"]`)).toHaveCount(1);
  await page.getByRole('combobox', { name: 'Course', exact: true }).selectOption(String(f.courseId));
  await page.getByRole('combobox', { name: 'Batch', exact: true }).selectOption(String(f.batchId));
  await page.getByRole('combobox', { name: /^Category/ }).selectOption(String(category.id));
  await page.getByLabel('Resource file').setInputFiles({ name: 'notes-v1.png', mimeType: 'image/png', buffer: firstImage });
  const created = page.waitForResponse(r => r.url().endsWith('/api/resources/materials') && r.request().method() === 'POST');
  await page.getByRole('button', { name: /Save draft/i }).click();
  const response = await created; expect(response.status()).toBe(201); material = await response.json();
  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Publish', exact: true }).click();
  await expect.poll(async () => (await api('teacher', 'GET', `/resources/materials/${material.id}`)).visibility).toBe('PUBLISHED');
  await api('other', 'GET', `/resources/materials/${material.id}`, undefined, 403);
});

test('STUDENT previews/downloads authorized resources, while staff preserves and publishes replacements', async ({ page }) => {
  await login(page, 'student'); await page.goto(`/resources/materials/${material.id}`);
  const download = page.waitForEvent('download'); await page.getByRole('button', { name: 'Download', exact: true }).click();
  expect(fs.readFileSync(await (await download).path()).equals(firstImage)).toBe(true);
  const preview = page.waitForResponse(r => r.url().endsWith(`/materials/${material.id}/preview`));
  await page.getByRole('button', { name: 'Preview', exact: true }).click(); expect((await preview).status()).toBe(200);
  await page.goto('/my-resources'); await expect(page.getByText('Acceptance Versioned Notes', { exact: true }).first()).toBeVisible();
  await page.getByRole('button', { name: 'Logout', exact: true }).click();
  await login(page, 'teacher'); await page.goto(`/resources/materials/${material.id}`); secondImage = await page.screenshot();
  await page.getByLabel('Resource file').setInputFiles({ name: 'notes-v2.png', mimeType: 'image/png', buffer: secondImage });
  await page.getByLabel('Change notes').fill('Synthetic replacement; previous version retained');
  await page.getByRole('button', { name: 'Save new version', exact: true }).click();
  await expect.poll(async () => (await api('teacher', 'GET', `/resources/materials/${material.id}`)).currentVersion).toBe(2);
  const before = await clients.student.get(base + `/resources/materials/${material.id}/download`);
  expect((await before.body()).equals(firstImage)).toBe(true);
  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Publish latest version', exact: true }).click();
  await expect.poll(async () => (await api('teacher', 'GET', `/resources/materials/${material.id}`)).publishedVersion).toBe(2);
  const versions = await api('teacher', 'GET', `/resources/materials/${material.id}/versions`); expect(versions).toHaveLength(2);
  const old = await clients.teacher.get(base + `/resources/materials/${material.id}/versions/${versions.find(v => v.versionNumber === 1).id}/download`);
  expect((await old.body()).equals(firstImage)).toBe(true);
  const current = await clients.student.get(base + `/resources/materials/${material.id}/download`); expect((await current.body()).equals(secondImage)).toBe(true);
});

test('resource validation rejects executable/oversized files and archive immediately removes student access', async ({ page }) => {
  material = await api('teacher', 'GET', `/resources/materials/${material.id}`);
  for (const file of [{ name: 'unsafe.exe', mimeType: 'application/octet-stream', buffer: Buffer.from('MZ') }, { name: 'oversized.png', mimeType: 'image/png', buffer: Buffer.alloc(10485761) }]) {
    const r = await clients.teacher.post(base + `/resources/materials/${material.id}/versions?version=${material.version}`, { multipart: { file } });
    expect([400, 413]).toContain(r.status());
  }
  expect(await api('teacher', 'GET', `/resources/materials/${material.id}/versions`)).toHaveLength(2);
  await login(page, 'teacher'); await page.goto(`/resources/materials/${material.id}`); page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Archive', exact: true }).click();
  await expect.poll(async () => (await api('teacher', 'GET', `/resources/materials/${material.id}`)).visibility).toBe('ARCHIVED');
  await api('student', 'GET', `/resources/materials/${material.id}`, undefined, 403);
});

test('financial full payment, replay, overpayment and void reconcile with revenue exports', async ({ page }) => {
  const structure = await api('admin', 'POST', '/fees/structures', { courseId: course.id, name: 'Acceptance Fees', tuitionFee: 1000, admissionFee: 0, materialFee: 0, otherFee: 0, effectiveFrom: f.today, status: 'ACTIVE' }, 201);
  let ledger = await api('admin', 'POST', '/fees/accounts', { enrollmentId: enrollment.id, feeStructureId: structure.id, discountAmount: 0, agreementDate: f.today }, 201);
  ledger = await api('admin', 'POST', `/fees/accounts/${ledger.account.id}/installments`, { version: ledger.account.version, installments: [{ installmentNumber: 1, amount: 1000, dueDate: f.today }] });
  const payload = { feeAccountId: ledger.account.id, amount: 1000, paymentDate: f.today, paymentMethod: 'CASH', idempotencyKey: crypto.randomUUID(), allocations: [{ installmentId: ledger.installments[0].id, amount: 1000 }] };
  const payment = await api('admin', 'POST', '/fees/payments', payload, 201);
  expect((await api('admin', 'POST', '/fees/payments', payload)).id).toBe(payment.id);
  await api('admin', 'POST', '/fees/payments', { ...payload, idempotencyKey: crypto.randomUUID() }, 409);
  expect(Number((await api('admin', 'GET', `/fees/accounts/${ledger.account.id}`)).account.outstandingAmount)).toBe(0);
  await api('admin', 'POST', `/fees/payments/${payment.id}/void`, { reason: 'Synthetic acceptance reversal' });
  ledger = await api('admin', 'GET', `/fees/accounts/${ledger.account.id}`); expect(Number(ledger.account.paidAmount)).toBe(0); expect(Number(ledger.account.outstandingAmount)).toBe(1000);
  const report = await api('admin', 'GET', `/fees/reports?courseId=${course.id}`); expect(Number(report.totalCollections)).toBe(0); expect(Number(report.outstandingBalance)).toBe(1000);
  await login(page, 'admin'); await page.goto(`/finance/accounts/${ledger.account.id}`); await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
});

test('STUDENT opens own fee installments/history and downloads receipt without financial write controls', async ({ page }) => {
  const fees = await api('student', 'GET', '/fees/me');
  const own = fees.accounts.content[0];
  await login(page, 'student'); await page.goto('/my-fees');
  await page.getByRole('link', { name: /View account/i }).first().click();
  await expect(page).toHaveURL(new RegExp(`/finance/accounts/${own.id}$`));
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Record payment', exact: true })).toHaveCount(0);
  const file = page.waitForEvent('download'); await page.getByRole('button', { name: /Download receipt/i }).first().click();
  expect(fs.readFileSync(await (await file).path()).subarray(0,4).toString()).toBe('%PDF');
  await page.reload(); await expect(page).toHaveURL(new RegExp(`/finance/accounts/${own.id}$`));
});

test('offline marks reject excessive scores and become visible only after publication', async ({ page }) => {
  let exam = await api('teacher', 'POST', '/exams', { examCode: `OFF-${tag}`, title: 'Acceptance Offline Assessment', batchId: f.batchId, examType: 'OFFLINE', totalMarks: 20, passingMarks: 10, startDateTime: new Date(Date.now() - 60000).toISOString(), endDateTime: new Date(Date.now() + 5000).toISOString() }, 201);
  exam = await api('teacher', 'POST', `/exams/${exam.id}/publish`, { version: exam.version });
  await expect.poll(() => Date.now(), { intervals: [500] }).toBeGreaterThan(Date.parse(exam.endDateTime));
  await api('teacher', 'PUT', `/exams/${exam.id}/offline-results`, { version: exam.version, results: [{ studentId: f.studentId, obtainedMarks: 21 }] }, 400);
  await api('teacher', 'PUT', `/exams/${exam.id}/offline-results`, { version: exam.version, results: [{ studentId: f.studentId, obtainedMarks: 18 }] });
  const hidden = await api('student', 'GET', `/exams/${exam.id}/result`); expect(hidden.published).toBe(false);
  exam = await api('teacher', 'GET', `/exams/${exam.id}`); await api('teacher', 'POST', `/exams/${exam.id}/results/publish`, { version: exam.version });
  await login(page, 'student'); await page.goto('/my-results'); await expect(page.getByText('Acceptance Offline Assessment', { exact: true })).toBeVisible();
});

test('account password recovery, deactivation and role changes revoke existing JWTs', async ({ page }) => {
  const administrator = await api('admin', 'GET', '/auth/me');
  await api('admin', 'PATCH', `/admin/users/${administrator.id}/status`, { status: 'INACTIVE' }, 409);
  await api('admin', 'PATCH', `/admin/users/${administrator.id}/roles`, { role: 'STUDENT' }, 409);
  const email = `${crypto.randomUUID()}@security.invalid`; const visitor = await request.newContext();
  try {
    const registered = await visitor.post(base + '/auth/register', { data: { fullName: 'Acceptance Recovery', email, password: f.password } }); expect(registered.status()).toBe(201); const user = await registered.json();
    const signIn = async password => { const r = await visitor.post(base + '/auth/login', { data: { email, password } }); expect(r.status()).toBe(200); return (await r.json()).token; };
    let token = await signIn(f.password);
    const changed = await visitor.put(base + '/settings/security/password', { headers: { Authorization: `Bearer ${token}` }, data: { currentPassword: f.password, newPassword: 'ChangedAcceptance123!' } }); expect(changed.status()).toBe(204);
    expect((await visitor.get(base + '/auth/me', { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(401);
    await visitor.post(base + '/auth/password-reset/request', { data: { email } });
    const recovery = await api('admin', 'POST', `/admin/users/${user.id}/password-reset`);
    expect((await visitor.post(base + '/auth/password-reset/confirm', { data: { token: recovery.resetToken, newPassword: 'RecoveredAcceptance123!' } })).status()).toBe(204);
    token = await signIn('RecoveredAcceptance123!'); await api('admin', 'PATCH', `/admin/users/${user.id}/status`, { status: 'INACTIVE' });
    expect((await visitor.get(base + '/auth/me', { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(401);
    await api('admin', 'PATCH', `/admin/users/${user.id}/status`, { status: 'ACTIVE' }); token = await signIn('RecoveredAcceptance123!');
    await api('admin', 'PATCH', `/admin/users/${user.id}/roles`, { role: 'TEACHER' });
    expect((await visitor.get(base + '/auth/me', { headers: { Authorization: `Bearer ${token}` } })).status()).toBe(401);
  } finally { await visitor.dispose(); }
  await login(page, 'admin'); await page.goto('/admin/users'); await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
});

test('assignment resubmission preserves history and real deadlines enforce late policy', async () => {
  const deadline = new Date(Date.now() + 8000).toISOString();
  const create = async (title, allowLateSubmission) => {
    const a = await api('teacher', 'POST', '/assignments', { title, batchId: f.batchId, maxMarks: 10, dueDateTime: deadline, allowLateSubmission }, 201);
    return api('teacher', 'POST', `/assignments/${a.id}/publish`, { version: a.version });
  };
  const revision = await create('Acceptance revisions', true);
  const blocked = await create('Acceptance strict deadline', false);
  const late = await create('Acceptance permitted late work', true);
  let submission = await api('student', 'POST', `/assignments/${revision.id}/submissions`, { submissionText: 'Original synthetic working' }, 201);
  submission = await api('student', 'PUT', `/assignments/submissions/${submission.id}`, { version: submission.version, submissionText: 'Revised synthetic working' });
  const history = await api('student', 'GET', `/assignments/submissions/${submission.id}/history`);
  expect(history.revisions.some(r => r.submissionText === 'Original synthetic working')).toBe(true);
  await expect.poll(() => Date.now(), { intervals: [500] }).toBeGreaterThan(Date.parse(deadline));
  await api('student', 'PUT', `/assignments/submissions/${submission.id}`, { version: submission.version, submissionText: 'After cutoff' }, 409);
  await api('student', 'POST', `/assignments/${blocked.id}/submissions`, { submissionText: 'Rejected late' }, 409);
  expect((await api('student', 'POST', `/assignments/${late.id}/submissions`, { submissionText: 'Accepted late' }, 201)).status).toBe('LATE');
});

test('server exam cutoff defeats client duration manipulation and duplicate starts', async () => {
  let e = await api('teacher', 'POST', '/exams', { examCode: `CUT-${tag}`, title: 'Acceptance Server Cutoff', batchId: f.batchId, examType: 'ONLINE_MCQ', durationMinutes: 1, totalMarks: 1, passingMarks: 1, startDateTime: new Date(Date.now() - 60000).toISOString(), endDateTime: new Date(Date.now() + 8000).toISOString() }, 201);
  await api('teacher', 'POST', `/exams/${e.id}/questions`, { version: e.version, questionText: 'Server-owned duration?', marks: 1, displayOrder: 1, options: [{ optionText: 'Yes', correct: true }, { optionText: 'No', correct: false }] }, 201);
  e = await api('teacher', 'GET', `/exams/${e.id}`); await api('teacher', 'POST', `/exams/${e.id}/publish`, { version: e.version });
  const attempt = await api('student', 'POST', `/exams/${e.id}/attempts/start`, { durationMinutes: 99999, expiresAt: '2099-01-01T00:00:00Z' });
  expect(Date.parse(attempt.expiresAt)).toBeLessThanOrEqual(Date.parse(e.endDateTime));
  expect(JSON.stringify(attempt)).not.toMatch(/"correct"|"awardedMarks"|"obtainedMarks"/);
  expect((await api('student', 'POST', `/exams/${e.id}/attempts/start`)).id).toBe(attempt.id);
  await api('other', 'GET', `/exams/attempts/${attempt.id}`, undefined, 403);
  await expect.poll(async () => (await api('student', 'GET', `/exams/attempts/${attempt.id}`)).status, { intervals: [500] }).toBe('AUTO_SUBMITTED');
});

test('teacher announcements are restricted to assigned batches and role pages render at three viewports', async ({ page }) => {
  test.setTimeout(180000);
  const notice = await api('teacher', 'POST', '/announcements', { title: 'Acceptance Batch Notice', content: 'Synthetic assigned-batch announcement', audienceType: 'BATCH', batchId: f.batchId }, 201);
  await api('teacher', 'POST', `/announcements/${notice.id}/publish`, { version: notice.version });
  await api('teacher', 'POST', '/announcements', { title: 'Forbidden batch', content: 'Synthetic access assertion', audienceType: 'BATCH', batchId: batch.id }, 403);
  await api('other', 'GET', `/announcements/${notice.id}`, undefined, 403);
  for (const [actor, routes] of Object.entries({ admin: ['/students', '/teachers', '/courses', '/batches', '/finance', '/reports', '/admin/audit-logs'], teacher: ['/teachers/me', '/assignments', '/exams', '/resources', '/analytics/batches'], student: ['/my-assignments', '/my-fees', '/attendance', '/my-progress', '/my-resources', '/notifications', '/settings/notifications', '/announcements'], parent: ['/parent/dashboard', '/parent/children', '/parent/fees', '/parent/attendance', '/parent/results', '/notifications', '/settings/notifications', '/announcements'] })) {
    await login(page, actor);
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 768, height: 1024 }, { width: 390, height: 844 }]) {
      await page.setViewportSize(viewport);
      for (const route of routes) { await page.goto(route); await expect(page.getByRole('heading', { level: 1 })).toBeVisible(); await page.waitForLoadState('networkidle'); await expect(page.getByRole('alert')).toHaveCount(0); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${actor} ${route} ${viewport.width}`).toBe(true); }
    }
    await page.setViewportSize({ width: 1440, height: 1000 }); await page.getByRole('button', { name: 'Logout', exact: true }).click();
  }
  const parent = await api('parent', 'GET', '/parents/me');
  const original = (await api('admin', 'GET', `/parents/links?studentId=${f.studentId}`)).content.find(x => x.parentId === parent.id);
  await api('admin', 'POST', `/parents/links/${original.id}/revoke`, { version: original.version, reason: 'Synthetic final recovery checkpoint' });
  await api('parent', 'GET', `/parents/children/${f.studentId}/fees`, undefined, 403);
});
