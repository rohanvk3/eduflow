import { useState } from "react";
import { Link } from "react-router-dom";
import { api, errorMessage } from "../api/client";
import { Card, Input, Button, ErrorNotice } from "../components/UI";
import { Field } from "../components/FinanceUI";
export default function AccountRegistrationPage() {
  const [form, setForm] = useState({ fullName: "", email: "", password: "" }),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [done, setDone] = useState(false);
  return (
    <main className="grid min-h-screen place-items-center p-5">
      <Card className="w-full max-w-md space-y-5 p-7">
        <h1 className="text-2xl font-semibold">Student account registration</h1>
        <p className="text-sm text-slate-500">
          Registration creates a STUDENT login. Institute staff manage verified
          profile linking and any permitted role changes.
        </p>
        <ErrorNotice message={error} />
        {done ? (
          <p role="status">Account created. Sign in to continue.</p>
        ) : (
          <form
            className="space-y-5"
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              try {
                await api.post("/auth/register", form);
                setForm({ fullName: "", email: "", password: "" });
                setDone(true);
              } catch (ex) {
                setError(errorMessage(ex));
              } finally {
                setBusy(false);
              }
            }}
          >
            {[
              ["fullName", "Full name", "text"],
              ["email", "Email", "email"],
              ["password", "Password", "password"],
            ].map(([k, label, type]) => (
              <Field label={label} key={k}>
                <Input
                  required
                  type={type}
                  autoComplete={
                    k === "password"
                      ? "new-password"
                      : k === "email"
                        ? "email"
                        : "name"
                  }
                  minLength={k === "password" ? 8 : undefined}
                  maxLength={
                    k === "password" ? 72 : k === "fullName" ? 100 : 254
                  }
                  value={form[k]}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, [k]: e.target.value }))
                  }
                />
              </Field>
            ))}
            <Button type="submit" disabled={busy}>
              {busy ? "Creating…" : "Create student account"}
            </Button>
          </form>
        )}
        <Link className="inline-block text-sm text-indigo-600" to="/login">
          Back to sign in
        </Link>
      </Card>
    </main>
  );
}
