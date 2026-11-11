import { useEffect, useState } from "react";
import {
  GraduationCap,
  ArrowRight,
  Users,
  Layers,
  Check,
  Eye,
  EyeOff,
} from "lucide-react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import { Button, ErrorNotice, Input } from "../components/UI";
import ThemeToggle from "../components/ThemeToggle";

export default function LoginPage() {
  const { user, login, notice } = useAuth();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    document.title = "Sign in · EduFlow";
  }, []);
  if (user)
    return (
      <Navigate
        to={
          user.role === "PARENT"
            ? "/parent/dashboard"
            : user.role === "TEACHER"
              ? "/teachers/me"
              : "/students"
        }
        replace
      />
    );
  const submit = async (event) => {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(email, password);
    } catch (failure) {
      setError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };
  return (
    <div className="relative grid min-h-dvh lg:grid-cols-2">
      <div className="absolute right-4 top-4 z-10">
        <ThemeToggle />
      </div>
      <div className="relative hidden flex-col justify-between border-r border-indigo-100 bg-indigo-50/60 p-12 lg:flex xl:p-16">
        <div className="flex items-center gap-2.5">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary text-white">
            <GraduationCap size={24} />
          </span>
          <span className="text-2xl font-semibold tracking-tight">
            EduFlow<span className="text-indigo-600">.</span>
          </span>
        </div>
        <div className="max-w-md">
          <p className="mb-5 text-xs font-semibold uppercase tracking-[0.18em] text-indigo-500">
            A better way to run your institute
          </p>
          <h2 className="text-5xl font-semibold leading-[1.15] tracking-[-0.055em] text-slate-900">
            Less admin.
            <br />
            More possibility.
          </h2>
          <p className="mt-6 max-w-sm text-sm leading-7 text-slate-500">
            Bring your student records together in a focused workspace. Spend
            less time managing details and more time helping students grow.
          </p>
          <div className="mt-10 space-y-4">
            {[
              [Users, "Every student, in one place"],
              [Layers, "A workspace that grows with you"],
              [Check, "Clear roles. Confident access."],
            ].map(([Icon, label]) => (
              <div
                key={label}
                className="flex items-center gap-3 text-xs font-medium text-slate-600"
              >
                <span className="flex h-7 w-7 items-center justify-center rounded-lg border border-indigo-100 bg-surface text-indigo-500">
                  <Icon size={14} />
                </span>
                {label}
              </div>
            ))}
          </div>
        </div>
        <p className="text-xs text-slate-400">
          EduFlow · Designed around education.
        </p>
      </div>
      <div className="flex min-w-0 flex-col items-center justify-center bg-surface px-6 py-12">
        <div className="mb-12 flex items-center gap-2 lg:hidden">
          <GraduationCap className="text-indigo-600" size={28} />
          <span className="text-2xl font-semibold tracking-tight">
            EduFlow.
          </span>
        </div>
        <div className="w-full max-w-[360px]">
          <p className="eyebrow mb-3">Your institute workspace</p>
          <h1 className="text-[30px] font-semibold tracking-[-0.04em]">
            Welcome back
          </h1>
          <p className="mb-8 mt-2 text-sm text-slate-500">
            Enter your details to sign in to EduFlow.
          </p>
          {notice?.startsWith("Password changed") && !error ? (
            <p
              role="status"
              className="mb-5 rounded-lg bg-emerald-50 p-4 text-sm text-emerald-800"
            >
              {notice}
            </p>
          ) : (
            <ErrorNotice message={error || notice} />
          )}
          <form onSubmit={submit} className="space-y-5">
            <label className="field-label" htmlFor="login-email">
              Email address
              <Input
                id="login-email"
                className="mt-2"
                type="email"
                placeholder="you@institute.com"
                autoComplete="username"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
                maxLength={254}
                disabled={busy}
              />
            </label>
            <div>
              <label className="field-label" htmlFor="login-password">
                Password
              </label>
              <div className="relative mt-2">
                <Input
                  id="login-password"
                  className="pr-11"
                  type={visible ? "text" : "password"}
                  placeholder="Enter your password"
                  autoComplete="current-password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  required
                  maxLength={72}
                  disabled={busy}
                />
                <Button
                  variant="icon"
                  className="absolute right-1 top-0.5"
                  aria-label={visible ? "Hide password" : "Show password"}
                  aria-pressed={visible}
                  onClick={() => setVisible(!visible)}
                >
                  {visible ? <EyeOff size={16} /> : <Eye size={16} />}
                </Button>
              </div>
            </div>
            <Button
              type="submit"
              disabled={busy}
              className="!mt-7 w-full !py-3"
            >
              {busy ? "Signing in…" : "Sign in"}
              <ArrowRight size={16} />
            </Button>
          </form>
          <p className="mt-7 text-center text-xs leading-6 text-slate-400">
            <a className="text-indigo-600" href="/recover-account">
              Forgot your password?
            </a>{" "}
            · Need access? Contact your institute administrator.
          </p>
        </div>
      </div>
    </div>
  );
}
