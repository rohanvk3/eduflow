import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axios from "axios";
import {
  ArrowLeft,
  Pencil,
  UserRound,
  Phone,
  CalendarDays,
  ShieldCheck,
  Archive,
  Mail,
  Link2,
} from "lucide-react";
import { api, errorMessage } from "../api/client";
import { useAuth } from "../auth/AuthContext";
import {
  Avatar,
  Badge,
  Button,
  Card,
  ConfirmDialog,
  ErrorNotice,
  Modal,
  Skeleton,
  StatusBadge,
  Toast,
  dateLabel,
} from "../components/UI";
import StudentForm from "../components/StudentForm";

export default function StudentDetailsPage({ own = false }) {
  const { id } = useParams();
  const { user } = useAuth();
  const admin = user.role === "ADMIN";
  const [student, setStudent] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [success, setSuccess] = useState("");
  const [reload, setReload] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setStudent(null);
    api
      .get(own ? "/students/me" : `/students/${id}`, {
        signal: controller.signal,
      })
      .then(({ data }) => {
        if (!controller.signal.aborted) setStudent(data);
      })
      .catch((failure) => {
        if (!axios.isCancel(failure)) setError(errorMessage(failure));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, own, reload]);
  const deactivate = async () => {
    setBusy(true);
    setActionError("");
    try {
      const { data } = await api.patch(`/students/${student.id}/deactivate`);
      setStudent(data);
      setConfirming(false);
      setSuccess(
        "Student deactivated. The admission record has been retained.",
      );
    } catch (failure) {
      setActionError(errorMessage(failure));
    } finally {
      setBusy(false);
    }
  };
  const item = (label, value, full = false) => (
    <div key={label} className={full ? "sm:col-span-2" : ""}>
      <dt className="text-[11px] text-slate-500">{label}</dt>
      <dd className="mt-2 break-words text-[13px] font-medium leading-6 text-slate-800">
        {value || "—"}
      </dd>
    </div>
  );
  const section = (title, Icon, children) => (
    <Card>
      <div className="flex items-center gap-2.5 border-b border-slate-100 px-5 py-4">
        <Icon size={16} className="text-indigo-500" />
        <h2 className="text-sm font-semibold">{title}</h2>
      </div>
      <dl className="grid gap-x-8 gap-y-6 p-5 sm:grid-cols-2">{children}</dl>
    </Card>
  );

  return (
    <>
      {admin && (
        <Link
          to="/students"
          className="mb-5 inline-flex items-center gap-1.5 rounded text-xs font-medium text-slate-500 transition-colors hover:text-indigo-600"
        >
          <ArrowLeft size={14} />
          Back to students
        </Link>
      )}
      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="page-title">
            {own ? "My student profile" : "Student profile"}
          </h1>
          <p className="mt-2 text-[13px] text-slate-500">
            {own
              ? "Your admission details and contact information, in one place."
              : "A complete view of student and admission information."}
          </p>
        </div>
        {admin && student && (
          <div className="flex flex-wrap gap-2">
            <Button
              variant="secondary"
              onClick={() => {
                setEditing(true);
                setSuccess("");
              }}
            >
              <Pencil size={14} />
              Edit details
            </Button>
            {student.status === "ACTIVE" && (
              <Button
                variant="secondary"
                className="!text-red-600"
                onClick={() => {
                  setConfirming(true);
                  setActionError("");
                }}
              >
                <Archive size={14} />
                Deactivate
              </Button>
            )}
          </div>
        )}
      </div>
      <ErrorNotice message={error} />
      {error && (
        <Button
          variant="secondary"
          className="mb-5"
          onClick={() => setReload((r) => r + 1)}
        >
          Try again
        </Button>
      )}
      {loading ? (
        <div role="status" className="space-y-6">
          <span className="sr-only">Loading student profile…</span>
          <Card className="flex items-center gap-5 p-6">
            <Skeleton className="h-20 w-20 rounded-xl" />
            <div className="space-y-3">
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-3 w-64 max-w-full" />
            </div>
          </Card>
          <div className="grid gap-6 md:grid-cols-2">
            {[1, 2, 3, 4].map((key) => (
              <Card key={key} className="space-y-5 p-6">
                <Skeleton className="h-4 w-32" />
                <Skeleton className="h-3 w-2/3" />
                <Skeleton className="h-3 w-1/2" />
              </Card>
            ))}
          </div>
        </div>
      ) : (
        student && (
          <>
            <Card className="mb-6 overflow-hidden">
              <div className="flex flex-wrap items-center gap-5 p-6">
                <Avatar name={student.fullName} size="lg" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-3">
                    <h2 className="text-xl font-semibold tracking-tight">
                      {student.fullName}
                    </h2>
                    <StatusBadge status={student.status} />
                  </div>
                  <p className="mt-2 flex items-center gap-2 text-xs text-slate-500">
                    <Mail size={13} />
                    <span className="break-all">{student.email}</span>
                  </p>
                  <p className="mt-2 break-all font-mono text-[10px] text-slate-400">
                    {student.admissionNumber}
                  </p>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-x-8 gap-y-3 border-t border-slate-100 bg-slate-50/50 px-6 py-3.5">
                <span className="flex items-center gap-2 text-xs text-slate-500">
                  <CalendarDays size={14} />
                  Admitted {dateLabel(student.admissionDate)}
                </span>
                <span className="flex items-center gap-2 text-xs text-slate-500">
                  <Link2 size={14} />
                  {student.userId
                    ? "Login account linked"
                    : "No login account linked"}
                </span>
                <Badge className="sm:ml-auto">Student record</Badge>
              </div>
            </Card>
            <div className="grid gap-6 xl:grid-cols-2">
              {section(
                "Personal information",
                UserRound,
                <>
                  {item("Full name", student.fullName)}
                  {item("Date of birth", dateLabel(student.dateOfBirth))}
                  {item("Gender", student.gender)}
                  {item("Admission date", dateLabel(student.admissionDate))}
                </>,
              )}
              {section(
                "Contact information",
                Phone,
                <>
                  {item("Email address", student.email)}
                  {item("Phone number", student.phone)}
                  {item("Address", student.address, true)}
                </>,
              )}
              {section(
                "Guardian information",
                ShieldCheck,
                <>
                  {item("Guardian name", student.guardianName)}
                  {item("Guardian phone", student.guardianPhone)}
                </>,
              )}
              {section(
                "Record information",
                CalendarDays,
                <>
                  {item(
                    "Login account",
                    student.userId
                      ? `Linked user #${student.userId}`
                      : "No account linked",
                  )}
                  {item(
                    "Record status",
                    student.status === "ACTIVE" ? "Active" : "Inactive",
                  )}
                  {item(
                    "Created",
                    new Date(student.createdAt).toLocaleString(),
                  )}
                  {item(
                    "Last updated",
                    new Date(student.updatedAt).toLocaleString(),
                  )}
                </>,
              )}
            </div>
          </>
        )
      )}
      <Toast message={success} onDismiss={() => setSuccess("")} />
      {editing && admin && (
        <Modal
          title="Edit student details"
          description="Changes are saved to the existing admission record."
          busy={busy}
          onClose={() => setEditing(false)}
        >
          <StudentForm
            student={student}
            onBusyChange={setBusy}
            onClose={() => setEditing(false)}
            onSaved={(updated) => {
              setStudent(updated);
              setEditing(false);
              setSuccess("Student details updated successfully.");
            }}
          />
        </Modal>
      )}
      {confirming && admin && (
        <ConfirmDialog
          title="Deactivate this student?"
          busy={busy}
          error={actionError}
          onClose={() => setConfirming(false)}
          onConfirm={deactivate}
          cancelLabel="Keep active"
          confirmLabel="Deactivate student"
        >
          <p>
            <strong className="font-semibold text-slate-900">
              {student.fullName}
            </strong>{" "}
            will be marked inactive. Their details and admission history will
            remain available. This does not disable the linked login account.
          </p>
        </ConfirmDialog>
      )}
    </>
  );
}
