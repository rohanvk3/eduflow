import { useEffect, useState } from "react";
import { Link, useLocation, useParams } from "react-router-dom";
import axios from "axios";
import { ArrowLeft, Pencil, Power, UserRoundCheck } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getTeacherById,
  getMyTeacherProfile,
  activateTeacher,
  deactivateTeacher,
} from "../services/teacherService";
import {
  Avatar,
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorNotice,
  Loading,
  StatusBadge,
  Toast,
  dateLabel,
} from "../components/UI";

export default function TeacherDetailsPage({ own = false }) {
  const { id } = useParams();
  const { user } = useAuth();
  const location = useLocation();
  const allowed = own ? user.role === "TEACHER" : user.role === "ADMIN";
  const [teacher, setTeacher] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [missing, setMissing] = useState(false);
  const [reload, setReload] = useState(0);
  const [confirm, setConfirm] = useState(false);
  const [busy, setBusy] = useState(false);
  const [actionError, setActionError] = useState("");
  const [notice, setNotice] = useState(location.state?.notice || "");
  useEffect(() => {
    if (!allowed) return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setMissing(false);
    (own
      ? getMyTeacherProfile(controller.signal)
      : getTeacherById(id, controller.signal)
    )
      .then(({ data }) => {
        if (!controller.signal.aborted) setTeacher(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) {
          if (own && e.response?.status === 404) setMissing(true);
          else setError(errorMessage(e));
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [allowed, id, own, reload]);
  if (!allowed)
    return (
      <ErrorNotice message="You do not have permission to view this teacher profile." />
    );
  const toggle = async () => {
    if (busy) return;
    setBusy(true);
    setActionError("");
    try {
      const { data } = await (teacher.status === "ACTIVE"
        ? deactivateTeacher(teacher.id)
        : activateTeacher(teacher.id));
      setTeacher(data);
      setConfirm(false);
      setNotice("Teacher status updated.");
    } catch (e) {
      setActionError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  };
  const item = (label, value) => (
    <div key={label}>
      <dt className="text-xs text-slate-500">{label}</dt>
      <dd className="mt-2 break-words text-sm font-medium text-slate-800">
        {value ?? "—"}
      </dd>
    </div>
  );
  return (
    <>
      {!own && (
        <Link
          to="/teachers"
          className="mb-5 inline-flex items-center gap-2 text-xs text-slate-500"
        >
          <ArrowLeft size={14} />
          Back to teachers
        </Link>
      )}
      <div className="mb-6 flex flex-wrap justify-between gap-4">
        <h1 className="page-title">
          {own ? "My teaching profile" : "Teacher profile"}
        </h1>
        {!own && teacher && (
          <div className="flex gap-2">
            <Link
              className="button-secondary"
              to={`/teachers/${teacher.id}/edit`}
            >
              <Pencil size={15} />
              Edit teacher
            </Link>
            <Button
              variant="secondary"
              onClick={() => {
                setConfirm(true);
                setActionError("");
              }}
            >
              <Power size={15} />
              {teacher.status === "ACTIVE" ? "Deactivate" : "Activate"}
            </Button>
          </div>
        )}
      </div>
      <ErrorNotice message={error} />
      {error && (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Try again
        </Button>
      )}
      {loading ? (
        <Loading label="Loading teacher profile…" />
      ) : missing ? (
        <Card>
          <EmptyState
            icon={UserRoundCheck}
            title="Your teaching profile isn't linked yet"
            description="Contact your administrator to link your TEACHER account to your faculty record."
          />
        </Card>
      ) : (
        !error &&
        teacher && (
          <>
            <Card className="mb-6 flex flex-wrap items-center gap-5 p-6">
              <Avatar name={teacher.fullName} size="lg" />
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-xl font-semibold">{teacher.fullName}</h2>
                  <StatusBadge status={teacher.status} />
                </div>
                <p className="mt-2 font-mono text-xs text-slate-500">
                  {teacher.employeeId}
                </p>
                <p className="mt-2 text-sm text-slate-500">
                  {teacher.specialization || "Faculty member"}
                </p>
              </div>
            </Card>
            <div className="grid gap-6 lg:grid-cols-2">
              <Card className="p-6">
                <h2 className="form-section">Contact information</h2>
                <dl className="grid gap-6 sm:grid-cols-2">
                  {item("Email", teacher.email)}
                  {item("Phone", teacher.phone || "—")}
                  {item("Gender", teacher.gender || "—")}
                  {item("Date of birth", dateLabel(teacher.dateOfBirth))}
                  {item("Address", teacher.address || "—")}
                </dl>
              </Card>
              <Card className="p-6">
                <h2 className="form-section">Teaching profile</h2>
                <dl className="grid gap-6 sm:grid-cols-2">
                  {item("Qualification", teacher.qualification || "—")}
                  {item("Specialization", teacher.specialization || "—")}
                  {item(
                    "Experience",
                    teacher.experienceYears == null
                      ? "—"
                      : `${teacher.experienceYears} years`,
                  )}
                  {item("Joining date", dateLabel(teacher.joiningDate))}
                  <div className="sm:col-span-2">
                    <dt className="mb-2 text-xs text-slate-500">Subjects</dt>
                    <dd className="flex flex-wrap gap-2">
                      {teacher.subjects?.length
                        ? teacher.subjects.map((s) => (
                            <Badge tone="indigo" key={s}>
                              {s}
                            </Badge>
                          ))
                        : "—"}
                    </dd>
                  </div>
                </dl>
              </Card>
              {!own && user.role === "ADMIN" && (
                <Card className="p-6">
                  <h2 className="form-section">Administration · Admin only</h2>
                  <dl className="grid gap-6 sm:grid-cols-2">
                    {item(
                      "Salary (INR)",
                      teacher.salary == null
                        ? "Not set"
                        : new Intl.NumberFormat("en-IN", {
                            style: "currency",
                            currency: "INR",
                          }).format(teacher.salary),
                    )}
                    {item(
                      "Linked user",
                      teacher.userId ? `User #${teacher.userId}` : "Not linked",
                    )}
                  </dl>
                </Card>
              )}
            </div>
          </>
        )
      )}
      <Toast message={notice} onDismiss={() => setNotice("")} />
      {confirm && !own && (
        <ConfirmDialog
          title={`${teacher.status === "ACTIVE" ? "Deactivate" : "Activate"} teacher?`}
          busy={busy}
          error={actionError}
          onClose={() => setConfirm(false)}
          onConfirm={toggle}
          confirmLabel={
            teacher.status === "ACTIVE"
              ? "Deactivate teacher"
              : "Activate teacher"
          }
        >
          Update {teacher.fullName}'s status? The faculty record and linked
          login will be retained.
        </ConfirmDialog>
      )}
    </>
  );
}
