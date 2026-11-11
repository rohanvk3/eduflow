import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { Button, Card, ErrorNotice, Loading, Toast } from "../components/UI";
import {
  ParentHeading,
  ParentDenied,
  ParentError,
  useParentLoad,
  ProfileFields,
} from "../components/ParentUI";
import {
  getParentProfile,
  updateParentProfile,
  parentError,
} from "../services/parentService";
export default function ParentProfilePage() {
  const auth = useAuth(),
    allowed = auth.user.role === "PARENT",
    s = useParentLoad(getParentProfile, [], allowed);
  if (!allowed) return <ParentDenied />;
  if (s.loading) return <Loading label="Loading parent profile…" />;
  if (s.error)
    return (
      <>
        <ParentHeading title="My Profile" />
        <ParentError state={s} />
        <p className="text-sm text-slate-500">
          If your profile has not been created, contact institute
          administration.
        </p>
      </>
    );
  return (
    s.data && (
      <Profile
        key={s.data.version}
        initial={s.data}
        refreshUser={auth.refreshUser}
      />
    )
  );
}
function Profile({ initial, refreshUser }) {
  const [form, setForm] = useState(initial),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const save = async (e) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      const r = await updateParentProfile({
        version: form.version,
        fullName: form.fullName,
        phoneNumber: form.phoneNumber,
        alternatePhone: form.alternatePhone || null,
        email: form.email || null,
        address: form.address || null,
      });
      setForm(r.data);
      if (refreshUser) await refreshUser();
      setMessage("Profile updated");
    } catch (ex) {
      setError(await parentError(ex));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <ParentHeading
        title="My Profile"
        subtitle="Keep your personal contact information up to date."
      />
      <Card className="mb-5 p-5">
        <p className="text-xs text-slate-500">Login email</p>
        <p className="mt-1 text-sm font-medium">{initial.loginEmail}</p>
        <p className="mt-2 text-xs text-slate-500">
          Contact email changes do not change your login address. Account
          security and student links are managed separately by the institute.
        </p>
        <p className="mt-3 text-xs">Profile status: {initial.status}</p>
      </Card>
      <form onSubmit={save}>
        <ErrorNotice message={error} />
        <Card className="p-6">
          <ProfileFields form={form} setForm={setForm} />
          <Button
            className="mt-5"
            type="submit"
            disabled={busy || initial.status !== "ACTIVE"}
          >
            {busy ? "Saving…" : "Save profile"}
          </Button>
        </Card>
      </form>
      <Toast message={message} onDismiss={() => setMessage("")} />
    </>
  );
}
