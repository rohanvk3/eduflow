import { useState } from "react";
import { Link } from "react-router-dom";
import { Card, Input, Button, ErrorNotice } from "../components/UI";
import { Field } from "../components/FinanceUI";
import { requestRecovery, confirmRecovery } from "../services/adminService";
import { notificationError } from "../services/notificationService";
export default function AccountRecoveryPage() {
  const [token] = useState(() => {
      const value = location.hash.slice(1);
      if (value) history.replaceState(null, "", location.pathname);
      return value;
    }),
    [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [message, setMessage] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [done, setDone] = useState(false);
  return (
    <main className="grid min-h-screen place-items-center bg-slate-50 p-5">
      <Card className="w-full max-w-md space-y-5 p-7">
        <h1 className="text-2xl font-semibold">Account Recovery</h1>
        <p className="text-sm leading-6 text-slate-500">
          {token
            ? "Choose a new password using your single-use recovery link."
            : "Contact institute administration for verified recovery. This installation does not send password reset emails."}
        </p>
        <ErrorNotice message={error} />
        {message && (
          <p role="status" className="text-sm leading-6 text-indigo-600">
            {message}
          </p>
        )}
        {!done && (
          <form
            className="space-y-5"
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                if (token) {
                  await confirmRecovery({ token, newPassword: password });
                  setPassword("");
                  setMessage(
                    "Password reset completed. Existing sessions are revoked. Sign in with your new password.",
                  );
                  setDone(true);
                } else {
                  const r = await requestRecovery(email);
                  setMessage(r.data.message);
                  setDone(true);
                }
              } catch (ex) {
                setError(await notificationError(ex));
              } finally {
                setBusy(false);
              }
            }}
          >
            {token ? (
              <Field label="New password">
                <Input
                  type="password"
                  minLength={12}
                  maxLength={72}
                  autoComplete="new-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </Field>
            ) : (
              <Field label="Account email">
                <Input
                  type="email"
                  required
                  maxLength={254}
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </Field>
            )}
            <Button type="submit" disabled={busy}>
              {busy
                ? "Processing…"
                : token
                  ? "Reset password"
                  : "Request verified assistance"}
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
