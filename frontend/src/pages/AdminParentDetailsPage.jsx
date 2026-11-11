import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  Input,
  Select,
  Table,
  Pagination,
  Modal,
  ErrorNotice,
  Loading,
  Toast,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ParentHeading,
  ParentDenied,
  ParentError,
  useParentLoad,
  ProfileFields,
  Field,
  parentTime,
} from "../components/ParentUI";
import {
  getParent,
  updateParent,
  getParentLinks,
  createParentLink,
  verifyParentLink,
  revokeParentLink,
  getParentLinkHistory,
  parentError,
} from "../services/parentService";
export default function AdminParentDetailsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = user.role === "ADMIN",
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [revision, setRevision] = useState(0),
    [modal, setModal] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState("");
  const s = useParentLoad(
      (signal) => getParent(id, signal),
      [id, revision],
      allowed,
    ),
    links = useParentLoad(
      (signal) => getParentLinks({ parentId: id, page, size }, signal),
      [id, page, size, revision],
      allowed,
    );
  if (!allowed) return <ParentDenied />;
  const complete = () => {
    setModal(null);
    setRevision((v) => v + 1);
    setMessage("Parent relationship updated");
  };
  return (
    <>
      <ParentHeading
        title={s.data?.fullName || "Parent Profile"}
        subtitle="Verify guardianship through institute checks before granting child-data access."
      >
        <Link className="button-secondary" to="/admin/parents">
          Parent directory
        </Link>
        <Button
          onClick={() => {
            setError("");
            setModal({
              type: "create",
              studentId: "",
              relationshipType: "GUARDIAN",
              primaryGuardian: false,
              reason: "",
            });
          }}
        >
          Create pending link
        </Button>
      </ParentHeading>
      <ParentError state={s} />
      {s.loading ? (
        <Loading label="Loading parent profile…" />
      ) : (
        s.data && (
          <ProfileEditor
            key={s.data.version}
            initial={s.data}
            onUpdated={() => {
              setRevision((v) => v + 1);
              setMessage("Profile updated");
            }}
          />
        )
      )}
      <Card className="mt-5">
        <div className="p-5">
          <h2 className="text-sm font-semibold">Student relationships</h2>
          <p className="mt-2 text-xs text-slate-500">
            Only verified relationships grant access. Revocation preserves the
            audit history.
          </p>
        </div>
        <ParentError state={links} />
        {links.loading ? (
          <Loading label="Loading relationships…" />
        ) : (
          <Table label="Parent student relationships">
            <thead>
              <tr>
                <th>Student</th>
                <th>Relationship</th>
                <th>Status</th>
                <th>Verified by</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {links.data?.content.map((l) => (
                <tr key={l.id}>
                  <td>
                    {l.studentName}
                    <p className="mt-1 text-xs text-slate-500">
                      {l.admissionNumber}
                    </p>
                  </td>
                  <td>
                    {l.relationshipType}
                    {l.primaryGuardian && (
                      <p className="mt-1 text-xs">Primary guardian</p>
                    )}
                  </td>
                  <td>{l.status}</td>
                  <td>
                    {l.verifiedBy || "Not verified"}
                    <p className="mt-1 text-xs text-slate-500">
                      {parentTime(l.verifiedAt)}
                    </p>
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-2">
                      {l.status === "PENDING" && (
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setError("");
                            setModal({ type: "verify", link: l, reason: "" });
                          }}
                        >
                          Verify
                        </Button>
                      )}
                      {l.status !== "REVOKED" && (
                        <Button
                          variant="secondary"
                          onClick={() => {
                            setError("");
                            setModal({ type: "revoke", link: l, reason: "" });
                          }}
                        >
                          Revoke
                        </Button>
                      )}
                      <Button
                        variant="secondary"
                        onClick={() => setModal({ type: "history", link: l })}
                      >
                        Audit history
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        )}
        {!links.loading && !links.error && !links.data?.content.length && (
          <p className="p-5 text-sm text-slate-500">
            No relationships yet. Create a pending link, then verify it after
            checking identity and guardian consent.
          </p>
        )}
        <Pagination
          noun="relationships"
          page={page}
          size={size}
          total={links.data?.totalElements}
          totalPages={links.data?.totalPages}
          onPageChange={setPage}
          onSizeChange={(v) => {
            setSize(v);
            setPage(0);
          }}
          loading={links.loading}
        />
      </Card>
      {modal && (
        <Modal
          title={
            modal.type === "create"
              ? "Create pending relationship"
              : modal.type === "history"
                ? "Relationship audit"
                : modal.type === "verify"
                  ? "Confirm relationship verification"
                  : "Confirm relationship revocation"
          }
          busy={busy}
          onClose={() => setModal(null)}
        >
          {modal.type === "history" ? (
            <LinkHistory id={modal.link.id} />
          ) : (
            <form
              className="space-y-5 p-6"
              onSubmit={async (e) => {
                e.preventDefault();
                if (busy) return;
                setBusy(true);
                setError("");
                try {
                  if (modal.type === "create")
                    await createParentLink({
                      parentId: Number(id),
                      studentId: Number(modal.studentId),
                      relationshipType: modal.relationshipType,
                      primaryGuardian: modal.primaryGuardian,
                      reason: modal.reason,
                    });
                  else
                    await (
                      modal.type === "verify"
                        ? verifyParentLink
                        : revokeParentLink
                    )(modal.link.id, {
                      version: modal.link.version,
                      reason: modal.reason,
                    });
                  complete();
                } catch (ex) {
                  setError(await parentError(ex));
                } finally {
                  setBusy(false);
                }
              }}
            >
              <ErrorNotice message={error} />
              {modal.type === "create" ? (
                <>
                  <RecordSelect
                    label="Student"
                    endpoint="/students"
                    status=""
                    value={modal.studentId}
                    onChange={(v) => setModal({ ...modal, studentId: v })}
                  />
                  <Field label="Relationship">
                    <Select
                      value={modal.relationshipType}
                      onChange={(e) =>
                        setModal({ ...modal, relationshipType: e.target.value })
                      }
                    >
                      {["FATHER", "MOTHER", "GUARDIAN", "OTHER"].map((s) => (
                        <option key={s}>{s}</option>
                      ))}
                    </Select>
                  </Field>
                  <label className="flex items-center gap-2 text-xs">
                    <input
                      type="checkbox"
                      checked={modal.primaryGuardian}
                      onChange={(e) =>
                        setModal({
                          ...modal,
                          primaryGuardian: e.target.checked,
                        })
                      }
                    />
                    Primary guardian
                  </label>
                  <p className="text-xs leading-6 text-slate-500">
                    Creates a pending link only. A revoked pair returns to
                    pending with its prior audit retained.
                  </p>
                </>
              ) : (
                <p className="text-sm leading-6 text-slate-600">
                  {modal.type === "verify"
                    ? "Confirm you have verified identity, relationship and guardian consent through institute procedures. Matching phone numbers or admission numbers alone are insufficient."
                    : "This immediately blocks future parent requests for this child after the change commits."}
                </p>
              )}
              <Field
                label={
                  modal.type === "verify" ? "Verification basis *" : "Reason *"
                }
              >
                <textarea
                  className="input min-h-24"
                  required
                  maxLength={1000}
                  value={modal.reason}
                  onChange={(e) =>
                    setModal({ ...modal, reason: e.target.value })
                  }
                />
              </Field>
              <Button type="submit" disabled={busy}>
                {busy
                  ? "Saving…"
                  : modal.type === "create"
                    ? "Create pending link"
                    : modal.type === "verify"
                      ? "Confirm verification"
                      : "Confirm revocation"}
              </Button>
            </form>
          )}
        </Modal>
      )}
      <Toast message={message} onDismiss={() => setMessage("")} />
    </>
  );
}
function ProfileEditor({ initial, onUpdated }) {
  const [form, setForm] = useState(initial),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (busy) return;
        setBusy(true);
        setError("");
        try {
          await updateParent(initial.id, {
            version: form.version,
            fullName: form.fullName,
            phoneNumber: form.phoneNumber,
            alternatePhone: form.alternatePhone || null,
            email: form.email || null,
            address: form.address || null,
            status: form.status,
          });
          onUpdated();
        } catch (ex) {
          setError(await parentError(ex));
        } finally {
          setBusy(false);
        }
      }}
    >
      <Card className="p-6">
        <p className="mb-5 text-xs text-slate-500">
          Login email: {initial.loginEmail}. Contact email updates preserve the
          login address.
        </p>
        <ErrorNotice message={error} />
        <ProfileFields form={form} setForm={setForm} admin />
        <Button className="mt-5" type="submit" disabled={busy}>
          {busy ? "Saving…" : "Save profile"}
        </Button>
      </Card>
    </form>
  );
}
function LinkHistory({ id }) {
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    s = useParentLoad(
      (signal) => getParentLinkHistory(id, { page, size }, signal),
      [id, page, size],
    );
  return (
    <div className="p-5">
      <ParentError state={s} />
      {s.loading ? (
        <Loading label="Loading relationship audit…" />
      ) : (
        s.data?.content.map((a) => (
          <div
            key={a.id}
            className="mb-4 rounded-lg border border-slate-200 p-4 text-xs"
          >
            <p className="font-medium">
              {a.previousStatus || "NEW"} → {a.newStatus}
            </p>
            <p className="mt-2 text-slate-500">
              {a.changedBy} · {parentTime(a.changedAt)} · {a.relationshipType}
              {a.primaryGuardian ? " · Primary" : ""}
            </p>
            <p className="mt-3 whitespace-pre-wrap break-words leading-6">
              {a.reason}
            </p>
          </div>
        ))
      )}
      <Pagination
        noun="audit events"
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
    </div>
  );
}
