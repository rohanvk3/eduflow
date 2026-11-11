import { test, expect, request } from "@playwright/test";
import fs from "node:fs";
import path from "node:path";

const json = file => JSON.parse(fs.readFileSync(file, "utf8").replace(/^\uFEFF/, ""));
const pointer = json(path.resolve("../.verification/current.json"));
const state = json(pointer.stateFile);
const f = json(path.join(state.directory, "fixtures.json"));
const actors = {};
let account, session, assignment, exam, paymentId;
test.describe.configure({ mode: "serial" });

async function api(actor, method, url, data) {
  const response = await actors[actor].fetch("http://127.0.0.1:18081/api" + url, { method, data });
  expect(response.ok(), `${method} ${url}: ${response.status()} ${await response.text()}`).toBeTruthy();
  return response.status() === 204 ? null : response.json();
}
async function login(page, actor) {
  await page.goto("/login");
  await page.getByLabel("Email address").fill(actor + "@e2e.invalid");
  await page.getByLabel("Password", { exact: true }).fill(f.password);
  await page.getByRole("button", { name: /Sign in/i }).click();
  await expect(page).not.toHaveURL(/\/login$/);
}
async function browserGet(page, url) {
  return page.evaluate(async url => {
    const r = await fetch("/api" + url, { headers: { Authorization: "Bearer " + sessionStorage.getItem("eduflow.token") } });
    return { status: r.status, body: await r.json() };
  }, url);
}

test.beforeAll(async () => {
  for (const name of ["admin", "teacher", "student", "other", "parent", "pending"]) {
    const client = await request.newContext();
    const response = await client.post("http://127.0.0.1:18081/api/auth/login", { data: { email: name + "@e2e.invalid", password: f.password } });
    expect(response.ok()).toBeTruthy();
    const { token } = await response.json();
    await client.dispose();
    actors[name] = await request.newContext({ extraHTTPHeaders: { Authorization: "Bearer " + token } });
  }
});
test.afterAll(async () => { for (const client of Object.values(actors)) await client.dispose(); });

test("authentication rejects invalid credentials and protects deep links", async ({ page }) => {
  await page.goto("/finance/payments"); await expect(page).toHaveURL(/\/login$/);
  await page.getByLabel("Email address").fill("admin@e2e.invalid");
  await page.getByLabel("Password", { exact: true }).fill("InvalidPassword123!");
  await page.getByRole("button", { name: /Sign in/i }).click();
  await expect(page.getByText("Invalid email or password")).toBeVisible();
});

test("ADMIN configures a fee agreement, records a partial payment and downloads receipt/report", async ({ page }) => {
  const structure = await api("admin", "POST", "/fees/structures", { courseId: f.courseId, name: "E2E Tuition", tuitionFee: 1000, admissionFee: 0, materialFee: 0, otherFee: 0, effectiveFrom: f.today, status: "ACTIVE" });
  account = await api("admin", "POST", "/fees/accounts", { enrollmentId: f.enrollmentId, feeStructureId: structure.id, discountAmount: 0, agreementDate: f.today });
  account = await api("admin", "POST", `/fees/accounts/${account.account.id}/installments`, { version: account.account.version, installments: [{ installmentNumber: 1, amount: 1000, dueDate: f.today }] });
  await login(page, "admin");
  const image = await page.screenshot();
  fs.writeFileSync(path.join(state.directory, "synthetic-upload.png"), image);
  const logo = await actors.admin.post("http://127.0.0.1:18081/api/admin/institute-settings/logo", { multipart: { file: { name: "synthetic-logo.png", mimeType: "image/png", buffer: image } } });
  expect(logo.ok()).toBeTruthy();
  await page.goto(`/finance/accounts/${account.account.id}`);
  await page.getByRole("button", { name: "Record payment", exact: true }).click();
  await page.getByLabel("Payment amount (INR) *", { exact: true }).fill("300");
  await page.getByLabel(/Installment 1.*Outstanding/).fill("300");
  await page.getByRole("button", { name: "Review payment", exact: true }).click();
  const paid = page.waitForResponse(r => r.url().endsWith("/api/fees/payments") && r.request().method() === "POST");
  await page.getByRole("button", { name: "Confirm payment", exact: true }).click();
  const response = await paid; expect(response.status()).toBe(201); paymentId = (await response.json()).id;
  const ledger = await api("admin", "GET", `/fees/accounts/${account.account.id}`);
  expect(Number(ledger.account.paidAmount)).toBe(300); expect(Number(ledger.account.outstandingAmount)).toBe(700);
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: /receipt/i }).first().click();
  const receipt = await download; expect(receipt.suggestedFilename()).toMatch(/\.pdf$/);
  expect(fs.readFileSync(await receipt.path()).subarray(0, 4).toString()).toBe("%PDF");
  await page.goto("/reports");
  await expect(page.getByRole("button", { name: "Export CSV" })).toBeEnabled();
  const reportDownload = page.waitForEvent("download"); await page.getByRole("button", { name: "Export CSV" }).click();
  const report = await reportDownload; expect(fs.readFileSync(await report.path(), "utf8")).toContain("Report");
  const pdfDownload = page.waitForEvent("download"); await page.getByRole("button", { name: "Export PDF" }).click();
  const pdf = await pdfDownload; expect(fs.readFileSync(await pdf.path()).subarray(0,4).toString()).toBe("%PDF");
  await page.goto("/analytics"); await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
});

test("TEACHER marks/finalizes attendance and publishes homework and online exam", async ({ page }) => {
  session = await api("teacher", "POST", "/attendance/sessions", { batchId: f.batchId, sessionDate: f.today, startTime: "09:00", endTime: "10:00", topic: "E2E attendance" });
  await login(page, "teacher"); await page.goto(`/attendance/sessions/${session.session.id}`);
  await page.getByLabel("Bulk attendance status").selectOption("PRESENT");
  await page.getByRole("button", { name: /Apply to/i }).click();
  await page.getByRole("button", { name: "Save Draft" }).click();
  await expect(page.getByRole("button", { name: "Finalize Attendance", exact: true })).toBeEnabled();
  await page.getByRole("button", { name: "Finalize Attendance", exact: true }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Finalize attendance", exact: true }).click();
  await expect(page.getByText("Finalized roster preserved")).toBeVisible();
  assignment = await api("teacher", "POST", "/assignments", { title: "E2E Algebra Homework", description: "Solve 2+2", batchId: f.batchId, maxMarks: 10, dueDateTime: new Date(Date.now() + 86400000).toISOString(), allowLateSubmission: false });
  assignment = await api("teacher", "POST", `/assignments/${assignment.id}/publish`, { version: assignment.version });
  exam = await api("teacher", "POST", "/exams", { examCode: "E2E-EXAM", title: "E2E Online Assessment", batchId: f.batchId, examType: "ONLINE_MCQ", durationMinutes: 1, totalMarks: 10, passingMarks: 5, startDateTime: new Date(Date.now() - 60000).toISOString(), endDateTime: new Date(Date.now() + 60000).toISOString() });
  await api("teacher", "POST", `/exams/${exam.id}/questions`, { version: exam.version, questionText: "What is 2+2?", marks: 10, displayOrder: 1, options: [{ optionText: "4", correct: true }, { optionText: "5", correct: false }] });
  exam = await api("teacher", "GET", `/exams/${exam.id}`);
  exam = await api("teacher", "POST", `/exams/${exam.id}/publish`, { version: exam.version });
  await page.goto("/assignments"); await expect(page.getByRole("cell", { name: /E2E Algebra Homework/ })).toBeVisible();
  await page.goto("/analytics/batches"); await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  const denied = await actors.teacher.get(`http://127.0.0.1:18081/api/students/${f.otherStudentId}`); expect(denied.status()).toBe(403);
});

test("STUDENT submits homework and attempts online exam through actual browser forms", async ({ page }) => {
  await login(page, "student");
  await page.goto(`/assignments/${assignment.id}/submit`);
  await page.getByLabel("Your response").fill("2+2=4. Synthetic browser submission.");
  await page.locator('input[type="file"]').setInputFiles(path.join(state.directory, "synthetic-upload.png"));
  await page.getByRole("button", { name: "Submit Homework", exact: true }).click();
  await expect(page.getByText(/Homework submitted successfully/)).toBeVisible();
  await page.goto(`/exams/${exam.id}/take`);
  await page.getByRole("button", { name: "Start / Resume exam" }).click();
  await expect(page.getByRole("timer")).toBeVisible();
  await page.getByRole("radio", { name: "4", exact: true }).check();
  await expect(page.getByText("All answers saved")).toBeVisible();
  await page.getByRole("button", { name: "Submit Exam", exact: true }).click();
  await page.getByRole("dialog").getByRole("button", { name: "Submit exam", exact: true }).click();
  await expect(page.getByText("Your saved answers have been evaluated.", { exact: false })).toBeVisible();
  await page.goto("/my-fees"); await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
  expect((await browserGet(page, `/students/${f.otherStudentId}`)).status).toBe(403);
  await page.goto("/admin/users"); await expect(page).toHaveURL(/\/dashboard$/);
  await page.reload(); await expect(page).toHaveURL(/\/dashboard$/);
});

test("TEACHER grades homework and publishes completed exam results", async ({ page }) => {
  const submissions = await api("teacher", "GET", `/assignments/${assignment.id}/submissions`);
  const submission = submissions.content[0];
  await login(page, "teacher"); await page.goto(`/assignments/submissions/${submission.id}`);
  await page.getByLabel(/Marks \(maximum 10\)/).fill("9");
  await page.getByLabel("Feedback", { exact: true }).fill("Verified browser feedback");
  await page.getByRole("button", { name: "Publish grade & feedback" }).click();
  await expect(page.getByText("Verified browser feedback", { exact: true }).first()).toBeVisible();
  await expect.poll(() => Date.now(), { timeout: 70000, intervals: [1000] }).toBeGreaterThan(Date.parse(exam.endDateTime));
  const current = await api("teacher", "GET", `/exams/${exam.id}`);
  await api("teacher", "POST", `/exams/${exam.id}/results/publish`, { version: current.version });
  await page.goto(`/exams/${exam.id}/results`); await expect(page.getByText("E2E-ONE", { exact: true }).first()).toBeVisible();
});

test("PARENT views verified child records and pending/unrelated access is rejected", async ({ page }) => {
  await login(page, "parent"); await page.goto("/parent/children");
  await expect(page.getByText("E2E-ONE", { exact: true }).first()).toBeVisible();
  for (const section of ["attendance", "fees", "results", "assignments", "progress"]) {
    await page.goto(`/parent/children/${f.studentId}/${section}`);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    expect((await browserGet(page, `/parents/children/${f.studentId}/${section}`)).status).toBe(200);
  }
  expect((await browserGet(page, `/parents/children/${f.otherStudentId}/fees`)).status).toBe(403);
  const pending = await actors.pending.get(`http://127.0.0.1:18081/api/parents/children/${f.studentId}/fees`); expect(pending.status()).toBe(403);
  const modify = await actors.parent.post("http://127.0.0.1:18081/api/fees/payments", { data: {} }); expect(modify.status()).toBeGreaterThanOrEqual(400);
  const receipt = await actors.parent.get(`http://127.0.0.1:18081/api/parents/children/${f.studentId}/payments/${paymentId}/receipt`); expect(receipt.status()).toBe(200); expect((await receipt.body()).subarray(0,4).toString()).toBe("%PDF");
});

test("responsive authenticated navigation renders on tablet and mobile", async ({ page }) => {
  await login(page, "student");
  for (const viewport of [{ width: 768, height: 1024 }, { width: 390, height: 844 }]) {
    await page.setViewportSize(viewport); await page.goto("/dashboard");
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
});

test("revoking a parent link immediately blocks access and logout protects routes", async ({ page }) => {
  await login(page, "parent");
  const links = await api("admin", "GET", `/parents/links?studentId=${f.studentId}`);
  const link = links.content.find(row => row.status === "VERIFIED");
  await api("admin", "POST", `/parents/links/${link.id}/revoke`, { version: link.version, reason: "Synthetic browser revocation verification" });
  expect((await browserGet(page, `/parents/children/${f.studentId}/fees`)).status).toBe(403);
  await page.getByRole("button", { name: "Logout", exact: true }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto("/parent/children"); await expect(page).toHaveURL(/\/login$/);
});
