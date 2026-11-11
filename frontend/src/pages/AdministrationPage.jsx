import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Select,
  Modal,
  EmptyState,
  ErrorNotice,
  Toast,
} from "../components/UI";
import { Field, useFinanceLoad, FinanceError } from "../components/FinanceUI";
import { notificationError } from "../services/notificationService";
import {
  getAdmin,
  saveInstitute,
  uploadLogo,
  changeUserStatus,
  changeUserRole,
  issueRecovery,
  getProfile,
  saveProfile,
  changePassword,
} from "../services/adminService";
const roles = ["ADMIN", "TEACHER", "STUDENT", "PARENT"],
  statuses = ["ACTIVE", "INACTIVE", "LOCKED"];
const nice = (s) => s.replace(/([a-z])([A-Z])/g, "$1 $2").replace(/([A-Za-z])(\d)/g,"$1 $2").replaceAll("_", " ").replace(/\b\w/g,c=>c.toUpperCase()).replace(/\bId\b/g,'ID');
function Heading({ title, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-3">
      <div>
        <p className="eyebrow">Workspace settings</p>
        <h1 className="page-title">{title}</h1>
      </div>
      {children}
    </div>
  );
}
function DataTable({ rows, onSelect }) {
  if (!rows?.length) return <EmptyState title="No records found" />;
  const keys = Object.keys(rows[0]).filter(
    (k) => !["description", "requestId"].includes(k),
  );
  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-sm">
        <thead>
          <tr>
            {keys.map((k) => (
              <th
                scope="col"
                className="whitespace-nowrap border-b p-3 text-xs capitalize text-slate-500"
                key={k}
              >
                {nice(k)}
              </th>
            ))}
            <th scope="col" className="p-3">
              Details
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr className="border-b border-slate-100" key={r.id || i}>
              {keys.map((k) => (
                <td className="whitespace-nowrap p-3" key={k}>
                  {r[k] == null ? "—" : String(r[k])}
                </td>
              ))}
              <td className="p-3">
                <Button variant="secondary" onClick={() => onSelect(r)}>
                  View
                </Button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function Pages({ state, page, setPage }) {
  return (
    state.data && (
      <div className="my-5 flex gap-4">
        <Button
          variant="secondary"
          disabled={!page}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous
        </Button>
        <span className="self-center text-sm">
          Page {page + 1} of {Math.max(1, state.data.totalPages)}
        </span>
        <Button
          variant="secondary"
          disabled={page + 1 >= state.data.totalPages}
          onClick={() => setPage((p) => p + 1)}
        >
          Next
        </Button>
      </div>
    )
  );
}
export default function AdministrationPage({ kind = "control-center" }) {
  const { user } = useAuth(),
    [revision, setRevision] = useState(0),
    [success, setSuccess] = useState(""),
    [page, setPage] = useState(0),
    [filters, setFilters] = useState({}),
    [selected, setSelected] = useState(null),
    { id } = useParams();
  const allowed = user.role === "ADMIN",
    state = useFinanceLoad(
      (signal) =>
        getAdmin(
          kind,
          kind === "users" || kind === "audit-logs"
            ? { ...filters, page, size: 20 }
            : undefined,
          signal,
        ),
      [kind, revision, page, JSON.stringify(filters)],
      allowed,
    );
  const edit = (k, v) => {
    setFilters((f) => ({ ...f, [k]: v || undefined }));
    setPage(0);
  };
  if (!allowed) return <EmptyState title="Administrator access required" />;
  const title = {
    "control-center": "Admin Control Center",
    "institute-settings": "Institute Settings",
    users: "User Management",
    "audit-logs": "Audit Logs",
  }[kind];
  return (
    <>
      <Toast message={success} onDismiss={() => setSuccess("")} />
      <Heading title={title}>
        <Button onClick={() => setRevision((v) => v + 1)}>Refresh</Button>
      </Heading>
      <nav className="mb-5 flex flex-wrap gap-3 text-sm">
        {[
          ["Control Center", "control-center"],
          ["Institute Settings", "institute-settings"],
          ["User Management", "users"],
          ["Audit Logs", "audit-logs"],
        ].map(([name, path]) => (
          <Link className="button-secondary" to={"/admin/" + path} key={path}>
            {name}
          </Link>
        ))}
      </nav>
      <FinanceError state={state} />
      {state.loading && (
        <Card className="animate-pulse p-10" role="status">
          Loading administration…
        </Card>
      )}
      {(kind === "users" || kind === "audit-logs") && (
        <Card className="mb-5 grid gap-4 p-5 sm:grid-cols-2 xl:grid-cols-4">
          <Field label="Search">
            <Input
              type="search"
              value={filters.search || ""}
              onChange={(e) => edit("search", e.target.value)}
            />
          </Field>
          {kind === "users" ? (
            <>
              <Field label="Role">
                <Select
                  value={filters.role || ""}
                  onChange={(e) => edit("role", e.target.value)}
                >
                  <option value="">All roles</option>
                  {roles.map((r) => (
                    <option key={r}>{r}</option>
                  ))}
                </Select>
              </Field>
              <Field label="Account status">
                <Select
                  value={filters.status || ""}
                  onChange={(e) => edit("status", e.target.value)}
                >
                  <option value="">All statuses</option>
                  {statuses.map((r) => (
                    <option key={r}>{r}</option>
                  ))}
                </Select>
              </Field>
            </>
          ) : (
            <>
              {[
                "action",
                "actorUserId",
                "entityType",
                "outcome",
                "fromDate",
                "toDate",
              ].map((k) => (
                <Field label={nice(k)} key={k}>
                  <Input
                    type={
                      k.includes("Date")
                        ? "date"
                        : k === "actorUserId"
                          ? "number"
                          : "text"
                    }
                    value={filters[k] || ""}
                    onChange={(e) => edit(k, e.target.value)}
                  />
                </Field>
              ))}
            </>
          )}
        </Card>
      )}
      {state.data && kind === "control-center" && (
        <>
          <Card className="mb-5 p-6">
            <h2 className="text-xl font-semibold">
              {state.data.institute.instituteName}
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              {state.data.institute.instituteCode} · Single-institute
              administration
            </p>
          </Card>
          <div className="mb-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {Object.entries(state.data.counts).map(([k, v]) => (
              <Card className="p-5" key={k}>
                <p className="text-sm capitalize text-slate-500">{nice(k)}</p>
                <p className="mt-2 text-3xl font-semibold">{v}</p>
              </Card>
            ))}
          </div>
          <div className="mb-5 grid gap-5 lg:grid-cols-2">
            <Card className="p-5">
              <h2 className="mb-3 font-semibold">Security configuration</h2>
              {state.data.security.map((s) => (
                <p className="mb-3 text-sm text-slate-500" key={s}>
                  {s}
                </p>
              ))}
            </Card>
            <Card className="p-5">
              <h2 className="mb-3 font-semibold">
                Data integrity observations
              </h2>
              {state.data.integrityWarnings.length ? (
                state.data.integrityWarnings.map((s) => (
                  <p className="mb-3 text-sm text-amber-700" key={s}>
                    {s}
                  </p>
                ))
              ) : (
                <p className="text-sm text-slate-500">
                  No missing-account links found by this check. This is not a
                  full database consistency scan.
                </p>
              )}
            </Card>
          </div>
          <Card className="p-5">
            <h2 className="mb-4 font-semibold">
              Recent administrative activity
            </h2>
            <DataTable
              rows={state.data.recentAuditEvents}
              onSelect={setSelected}
            />
          </Card>
        </>
      )}
      {state.data && kind === "institute-settings" && (
        <InstituteForm
          key={state.data.version}
          initial={state.data}
          reload={() => setRevision((v) => v + 1)}
          notify={setSuccess}
        />
      )}
      {state.data && (kind === "users" || kind === "audit-logs") && (
        <>
          <Card className="p-4">
            <DataTable rows={state.data.content} onSelect={setSelected} />
          </Card>
          <Pages state={state} page={page} setPage={setPage} />
          {kind === "users" && (
            <Card className="p-5 text-sm text-slate-500">
              Create accounts through the supported workflows:{" "}
              <Link className="text-indigo-600" to="/register">
                Student registration
              </Link>{" "}
              ·{" "}
              <Link className="text-indigo-600" to="/admin/parents">
                Parent profiles
              </Link>
              . Existing linked profiles constrain role transitions. User
              Management never sets plaintext passwords.
            </Card>
          )}
        </>
      )}
      {selected &&
        (kind === "users" ? (
          <UserActions
            user={selected}
            onClose={() => setSelected(null)}
            onChange={() => {
              setSelected(null);
              setRevision((v) => v + 1);
            }}
          />
        ) : (
          <Modal title="Audit event details" onClose={() => setSelected(null)}>
            <dl className="grid gap-3 p-6 text-sm">
              {Object.entries(selected).map(([k, v]) => (
                <div key={k}>
                  <dt className="font-medium capitalize">{nice(k)}</dt>
                  <dd className="mt-1 break-words text-slate-500">
                    {v == null ? "—" : String(v)}
                  </dd>
                </div>
              ))}
            </dl>
          </Modal>
        ))}
    </>
  );
}
function InstituteForm({ initial, reload, notify }) {
  const [form, setForm] = useState(initial),
    [file, setFile] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const fields = [
    "instituteName",
    "instituteCode",
    "email",
    "phoneNumber",
    "alternatePhone",
    "addressLine1",
    "addressLine2",
    "city",
    "state",
    "postalCode",
    "country",
    "website",
    "timezone",
    "currencyCode",
    "academicYearStart",
    "academicYearEnd",
  ];
  return (
    <>
      <ErrorNotice message={error} />

      <form
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError("");
          try {
            const cleaned = {
              ...form,
              academicYearStart: form.academicYearStart || null,
              academicYearEnd: form.academicYearEnd || null,
            };
            await saveInstitute(cleaned);
            setMessage("Institute settings saved");
            notify("Institute settings saved");
            reload();
          } catch (ex) {
            setError(await notificationError(ex));
          } finally {
            setBusy(false);
          }
        }}
      >
        <Card className="grid gap-5 p-6 md:grid-cols-2">
          {fields.map((k) => (
            <Field
              label={
                nice(k) +
                ([
                  "instituteName",
                  "instituteCode",
                  "timezone",
                  "currencyCode",
                ].includes(k)
                  ? " *"
                  : "")
              }
              key={k}
            >
              <Input
                required={[
                  "instituteName",
                  "instituteCode",
                  "timezone",
                  "currencyCode",
                ].includes(k)}
                type={
                  k === "email"
                    ? "email"
                    : k === "website"
                      ? "url"
                      : k.startsWith("academicYear")
                        ? "date"
                        : "text"
                }
                maxLength={
                  k === "instituteName"
                    ? 150
                    : k === "instituteCode"
                      ? 30
                      : k === "currencyCode"
                        ? 3
                        : k.includes("Phone") || k === "phoneNumber"
                          ? 16
                          : k === "email"
                            ? 254
                            : 250
                }
                value={form[k] || ""}
                onChange={(e) =>
                  setForm((f) => ({ ...f, [k]: e.target.value }))
                }
              />
            </Field>
          ))}
          <p className="text-xs text-slate-500 md:col-span-2">
            Existing records use Asia/Kolkata and INR. Changing the institute's
            accounting timezone or currency needs a separate migration.
          </p>
        </Card>
        <div className="my-5 flex gap-3">
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save settings"}
          </Button>
          <Button
            variant="secondary"
            onClick={() => setForm(initial)}
            disabled={busy}
          >
            Cancel changes
          </Button>
        </div>
      </form>
      <Card className="p-6">
        <h2 className="mb-3 font-semibold">Institute logo</h2>
        <p className="mb-3 text-xs text-slate-500">
          PNG or JPEG, at most 1 MiB and 2000 × 2000 pixels. Stored privately.
        </p>
        <input
          aria-label="Institute logo file"
          type="file"
          accept="image/png,image/jpeg"
          onChange={(e) => setFile(e.target.files?.[0] || null)}
        />
        <Button
          className="mt-4"
          disabled={!file || busy}
          onClick={async () => {
            setBusy(true);
            setError("");
            try {
              await uploadLogo(file);
              setMessage("Logo updated");
              notify("Logo updated");
              reload();
            } catch (e) {
              setError(await notificationError(e));
            } finally {
              setBusy(false);
            }
          }}
        >
          Upload logo
        </Button>
      </Card>
    </>
  );
}
function UserActions({ user, onClose, onChange }) {
  const { user: viewer } = useAuth();
  const self = String(viewer.id ?? viewer.userId) === String(user.id);
  const [role, setRole] = useState(user.role),
    [status, setStatus] = useState(user.status),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [recovery, setRecovery] = useState(null),
    [copied, setCopied] = useState(false);
  const mutate = async (kind) => {
    if (
      !window.confirm(
        kind === "recovery"
          ? "Confirm you verified the account holder’s identity and will deliver this link privately. No email will be sent."
          : "Apply this account change? Existing sessions will be revoked.",
      )
    )
      return;
    setBusy(true);
    setError("");
    try {
      if (kind === "status") await changeUserStatus(user.id, status);
      else if (kind === "role") await changeUserRole(user.id, role);
      else {
        const r = await issueRecovery(user.id);
        setRecovery(r.data);
        return;
      }
      onChange();
    } catch (e) {
      setError(await notificationError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal title={"Manage " + user.fullName} onClose={onClose}>
      <div className="space-y-5 p-6">
        <ErrorNotice message={error} />
        <p className="text-sm text-slate-500">
          {user.email} · User #{user.id}
        </p>
        <Field label="Account status">
          <Select
            disabled={busy || self}
            value={status}
            onChange={(e) => setStatus(e.target.value)}
          >
            {statuses.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        </Field>
        <Button
          disabled={busy || self || status === user.status}
          onClick={() => mutate("status")}
        >
          Apply status
        </Button>
        <Field label="Account role">
          <Select
            disabled={busy || self}
            value={role}
            onChange={(e) => setRole(e.target.value)}
          >
            {roles.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </Select>
        </Field>
        <Button
          disabled={busy || self || role === user.role}
          onClick={() => mutate("role")}
        >
          Apply role
        </Button>
        <div className="border-t pt-5">
          <h3 className="font-semibold">Verified account recovery</h3>
          <p className="my-3 text-xs leading-6 text-slate-500">
            Never ask for the user's password. Verify identity outside this
            application before issuing a 15-minute single-use link. Close this
            dialog after private delivery.
          </p>
          <Button
            disabled={busy || !!recovery}
            onClick={() => mutate("recovery")}
          >
            Issue recovery link
          </Button>
          {recovery && (
            <div className="mt-4 space-y-3">
              <p className="text-xs text-amber-700">{recovery.instruction}</p>
              <Input
                readOnly
                aria-label="Single-use recovery link"
                value={
                  location.origin + "/recover-account#" + recovery.resetToken
                }
              />
              <p className="text-xs">
                Expires: {new Date(recovery.expiresAt).toLocaleString()}
              </p>
              <Button
                variant="secondary"
                onClick={async () => {
                  try {
                    await navigator.clipboard.writeText(
                      location.origin +
                        "/recover-account#" +
                        recovery.resetToken,
                    );
                    setCopied(true);
                  } catch {
                    setError(
                      "Clipboard unavailable. Select and copy the link manually.",
                    );
                  }
                }}
              >
                {copied ? "Copied" : "Copy private link"}
              </Button>
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
}
export function AccountSettingsPage({ security = false }) {
  const { logout, refreshUser } = useAuth(),
    s = useFinanceLoad(getProfile, []),
    [name, setName] = useState(null),
    [password, setPassword] = useState({
      currentPassword: "",
      newPassword: "",
    }),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  return (
    <>
      <Heading title={security ? "Security Settings" : "My Profile"} />
      <FinanceError state={s} />
      <ErrorNotice message={error} />
      <Toast message={message} onDismiss={() => setMessage("")} />
      {s.loading && (
        <Card className="p-10" role="status">
          Loading account…
        </Card>
      )}
      {s.data && (
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError("");
            try {
              if (security) {
                await changePassword(password);
                logout(
                  "Password changed successfully. Sign in with your new password.",
                );
              } else {
                await saveProfile(name ?? s.data.fullName);
                await refreshUser();
                setMessage("Profile updated");
              }
            } catch (ex) {
              setError(await notificationError(ex));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Card className="max-w-2xl space-y-5 p-6">
            <p className="text-sm text-slate-500">
              {s.data.email} · {s.data.role} · {s.data.status}
            </p>
            {security ? (
              <>
                <Field label="Current password">
                  <Input
                    type="password"
                    autoComplete="current-password"
                    required
                    value={password.currentPassword}
                    onChange={(e) =>
                      setPassword((p) => ({
                        ...p,
                        currentPassword: e.target.value,
                      }))
                    }
                  />
                </Field>
                <Field label="New password">
                  <Input
                    type="password"
                    autoComplete="new-password"
                    minLength={12}
                    maxLength={72}
                    required
                    value={password.newPassword}
                    onChange={(e) =>
                      setPassword((p) => ({
                        ...p,
                        newPassword: e.target.value,
                      }))
                    }
                  />
                </Field>
                <p className="text-xs leading-6 text-slate-500">
                  Use at least 12 characters with a letter and a number (maximum
                  72 UTF-8 bytes). A successful change signs out every existing
                  session, including this one. Sign in again with the new
                  password.
                </p>
              </>
            ) : (
              <Field label="Full name">
                <Input
                  required
                  maxLength={100}
                  value={name ?? s.data.fullName}
                  onChange={(e) => setName(e.target.value)}
                />
              </Field>
            )}
            <div className="flex flex-wrap gap-3">
              <Button type="submit" disabled={busy}>
                {busy
                  ? "Saving…"
                  : security
                    ? "Change password and sign out"
                    : "Save profile"}
              </Button>
              <Button
                variant="secondary"
                onClick={() => {
                  setName(null);
                  setPassword({ currentPassword: "", newPassword: "" });
                }}
              >
                Cancel changes
              </Button>
            </div>
            <Link
              className="inline-block text-sm text-indigo-600"
              to={security ? "/settings/profile" : "/settings/security"}
            >
              {security ? "My Profile" : "Security Settings"}
            </Link>
          </Card>
        </form>
      )}
    </>
  );
}
