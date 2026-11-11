import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Select,
  Modal,
  Table,
  Pagination,
  EmptyState,
  ErrorNotice,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ParentHeading,
  ParentDenied,
  ParentError,
  useParentLoad,
  Field,
} from "../components/ParentUI";
import {
  getParents,
  createParent,
  parentError,
} from "../services/parentService";
export default function AdminParentsPage() {
  const { user } = useAuth(),
    allowed = user.role === "ADMIN",
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [create, setCreate] = useState(false);
  const s = useParentLoad(
    (signal) =>
      getParents({ page, size, search, status: status || undefined }, signal),
    [page, size, search, status],
    allowed,
  );
  if (!allowed) return <ParentDenied />;
  return (
    <>
      <ParentHeading
        title="Parent Management"
        subtitle="Manage parent accounts and verify access to student records."
      >
        <Button onClick={() => setCreate(true)}>Create parent profile</Button>
      </ParentHeading>
      <Card className="mb-5 grid gap-4 p-5 md:grid-cols-2">
        <Input
          aria-label="Search parents"
          placeholder="Search by name, login email or phone…"
          maxLength={100}
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
        <Select
          aria-label="Parent status"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All profiles</option>
          <option>ACTIVE</option>
          <option>INACTIVE</option>
        </Select>
      </Card>
      <ParentError state={s} />
      <Card>
        {s.loading ? (
          <Loading label="Loading parents…" />
        ) : s.data?.content.length ? (
          <Table label="Parent directory">
            <thead>
              <tr>
                <th>Name</th>
                <th>Login email</th>
                <th>Phone</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {s.data.content.map((p) => (
                <tr key={p.id}>
                  <td className="font-medium">{p.fullName}</td>
                  <td>{p.loginEmail}</td>
                  <td>{p.phoneNumber}</td>
                  <td>{p.status}</td>
                  <td>
                    <Link
                      className="button-secondary"
                      to={"/admin/parents/" + p.id}
                    >
                      View profile
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !s.error && (
            <EmptyState
              title="No parent profiles found"
              description="Create a parent account or attach a profile to an existing PARENT login."
            />
          )
        )}
        <Pagination
          noun="parents"
          page={page}
          size={size}
          total={s.data?.totalElements}
          totalPages={s.data?.totalPages}
          onPageChange={setPage}
          onSizeChange={(v) => {
            setSize(v);
            setPage(0);
          }}
          loading={s.loading}
        />
      </Card>
      {create && <CreateParent onClose={() => setCreate(false)} />}
    </>
  );
}
function CreateParent({ onClose }) {
  const nav = useNavigate(),
    [existing, setExisting] = useState(false),
    [form, setForm] = useState({
      fullName: "",
      email: "",
      password: "",
      phoneNumber: "",
      alternatePhone: "",
      address: "",
      userId: "",
    }),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const edit = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  const save = async (e) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      const payload = {
        phoneNumber: form.phoneNumber,
        alternatePhone: form.alternatePhone || null,
        address: form.address || null,
        ...(existing
          ? { userId: Number(form.userId) }
          : {
              fullName: form.fullName,
              email: form.email,
              password: form.password,
            }),
      };
      const r = await createParent(payload);
      nav("/admin/parents/" + r.data.id);
    } catch (ex) {
      setError(await parentError(ex));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Modal title="Create parent profile" busy={busy} onClose={onClose}>
      <form className="space-y-5 p-6" onSubmit={save}>
        <ErrorNotice message={error} />
        <label className="flex items-center gap-2 text-xs">
          <input
            type="checkbox"
            checked={existing}
            onChange={(e) => setExisting(e.target.checked)}
          />
          Use an existing PARENT account
        </label>
        {existing ? (
          <RecordSelect
            label="Parent login"
            endpoint="/parents/candidates/users"
            status=""
            value={form.userId}
            onChange={(v) => edit("userId", v)}
          />
        ) : (
          <>
            <Field label="Full name *">
              <Input
                required
                maxLength={100}
                value={form.fullName}
                onChange={(e) => edit("fullName", e.target.value)}
              />
            </Field>
            <Field label="Login email *">
              <Input
                type="email"
                required
                maxLength={254}
                value={form.email}
                onChange={(e) => edit("email", e.target.value)}
              />
            </Field>
            <Field label="Initial password *">
              <Input
                type="password"
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                value={form.password}
                onChange={(e) => edit("password", e.target.value)}
              />
            </Field>
            <p className="text-xs text-slate-500">
              Creates a PARENT login only. Share the initial credentials
              privately with the parent.
            </p>
          </>
        )}
        <Field label="Phone *">
          <Input
            type="tel"
            required
            pattern="[+]?[0-9]{7,15}"
            maxLength={16}
            value={form.phoneNumber}
            onChange={(e) => edit("phoneNumber", e.target.value)}
          />
        </Field>
        <Field label="Alternate phone">
          <Input
            type="tel"
            pattern="[+]?[0-9]{7,15}"
            maxLength={16}
            value={form.alternatePhone}
            onChange={(e) => edit("alternatePhone", e.target.value)}
          />
        </Field>
        <Field label="Address">
          <textarea
            className="input"
            maxLength={500}
            value={form.address}
            onChange={(e) => edit("address", e.target.value)}
          />
        </Field>
        <Button type="submit" disabled={busy}>
          {busy ? "Creating…" : "Create profile"}
        </Button>
      </form>
    </Modal>
  );
}
