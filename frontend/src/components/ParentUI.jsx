import { Card, EmptyState, Input, Select } from "./UI";
import { Field } from "./FinanceUI";
export {
  useFinanceLoad as useParentLoad,
  FinanceError as ParentError,
  Field,
  inr,
} from "./FinanceUI";
export { examTime as parentTime, pct } from "./ExamUI";
export const childKey = (user) =>
  "eduflow.parent.child." + (user.id ?? user.userId ?? user.email);
export const parentTabs = [
  ["Overview", ""],
  ["Attendance", "attendance"],
  ["Fees", "fees"],
  ["Exam results", "results"],
  ["Assignments", "assignments"],
  ["Academic progress", "progress"],
  ["Learning resources", "resources"],
];
export function ParentHeading({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Parent portal</p>
        <h1 className="page-title">{title}</h1>
        {subtitle && (
          <p className="mt-2 max-w-xl text-sm text-slate-500">{subtitle}</p>
        )}
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function ParentDenied() {
  return (
    <Card>
      <EmptyState
        title="Parent portal access restricted"
        description="Sign in with your parent account to view verified linked children."
      />
    </Card>
  );
}
export function ParentMetrics({ items }) {
  return (
    <div className="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      {items.map(([title, value]) => (
        <Card key={title} className="p-5">
          <p className="text-xs text-slate-500">{title}</p>
          <p className="mt-3 break-words text-2xl font-semibold tabular-nums">
            {value ?? "No data"}
          </p>
        </Card>
      ))}
    </div>
  );
}
export function ParentDates({ from, to, onFrom, onTo }) {
  return (
    <>
      <Field label="From date">
        <Input
          aria-label="From date"
          type="date"
          value={from}
          onChange={(e) => onFrom(e.target.value)}
        />
      </Field>
      <Field label="To date">
        <Input
          aria-label="To date"
          type="date"
          value={to}
          onChange={(e) => onTo(e.target.value)}
        />
      </Field>
    </>
  );
}
export function ParentScope({ child, course, batch, onCourse, onBatch }) {
  const courses = Array.from(
    new Map(child.enrollments.map((e) => [e.courseId, e.courseName])).entries(),
  );
  const batches = Array.from(
    new Map(
      child.enrollments
        .filter((e) => !course || String(e.courseId) === course)
        .map((e) => [e.batchId, e.batchName]),
    ).entries(),
  );
  return (
    <>
      {onCourse && (
        <Field label="Course">
          <Select
            aria-label="Course"
            value={course}
            onChange={(e) => onCourse(e.target.value)}
          >
            <option value="">All courses</option>
            {courses.map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </Select>
        </Field>
      )}
      <Field label="Batch">
        <Select
          aria-label="Batch"
          value={batch}
          onChange={(e) => onBatch(e.target.value)}
        >
          <option value="">All batches</option>
          {batches.map(([id, name]) => (
            <option key={id} value={id}>
              {name}
            </option>
          ))}
        </Select>
      </Field>
    </>
  );
}
export function ProfileFields({ form, setForm, admin = false }) {
  const edit = (k, v) => setForm((f) => ({ ...f, [k]: v }));
  return (
    <div className="grid gap-5 md:grid-cols-2">
      {[
        ["Full name", "fullName", "text", 100, true],
        ["Phone", "phoneNumber", "tel", 16, true],
        ["Alternate phone", "alternatePhone", "tel", 16, false],
        ["Contact email", "email", "email", 254, false],
      ].map(([label, k, type, max, required]) => (
        <Field key={k} label={label + (required ? " *" : "")}>
          <Input
            type={type}
            maxLength={max}
            required={required}
            pattern={type === "tel" ? "[+]?[0-9]{7,15}" : undefined}
            value={form[k] || ""}
            onChange={(e) => edit(k, e.target.value)}
          />
        </Field>
      ))}
      <div className="md:col-span-2">
        <Field label="Address">
          <textarea
            className="input min-h-24"
            maxLength={500}
            value={form.address || ""}
            onChange={(e) => edit("address", e.target.value)}
          />
        </Field>
      </div>
      {admin && (
        <Field label="Profile status">
          <Select
            value={form.status}
            onChange={(e) => edit("status", e.target.value)}
          >
            <option>ACTIVE</option>
            <option>INACTIVE</option>
          </Select>
        </Field>
      )}
    </div>
  );
}
