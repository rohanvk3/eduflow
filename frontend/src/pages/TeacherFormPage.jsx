import { useEffect, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import {
  ArrowLeft,
  Save,
  UserRoundCheck,
  BookOpen,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { errorMessage } from "../api/client";
import {
  getTeacherById,
  createTeacher,
  updateTeacher,
} from "../services/teacherService";
import { Button, Card, ErrorNotice, Input, Loading } from "../components/UI";

const blank = {
  fullName: "",
  employeeId: "",
  email: "",
  phone: "",
  gender: "",
  dateOfBirth: "",
  qualification: "",
  specialization: "",
  subjects: "",
  experienceYears: "",
  joiningDate: "",
  address: "",
  salary: "",
  userId: "",
};

export default function TeacherFormPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [values, setValues] = useState(blank);
  const [loading, setLoading] = useState(!!id);
  const [busy, setBusy] = useState(false);
  const submitting = useRef(false);
  const [error, setError] = useState("");
  const [errors, setErrors] = useState({});
  const [reload, setReload] = useState(0);
  useEffect(() => {
    if (!id || user.role !== "ADMIN") return;
    const controller = new AbortController();
    setLoading(true);
    setError("");
    getTeacherById(id, controller.signal)
      .then(({ data }) => {
        if (!controller.signal.aborted)
          setValues(
            Object.fromEntries(
              Object.keys(blank).map((key) => [
                key,
                key === "subjects"
                  ? (data.subjects || []).join(", ")
                  : (data[key] ?? ""),
              ]),
            ),
          );
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [id, user.role, reload]);
  if (user.role !== "ADMIN")
    return (
      <ErrorNotice message="Only administrators can manage teacher records." />
    );
  const change = (e) => {
    setValues((v) => ({ ...v, [e.target.name]: e.target.value }));
    setErrors((v) => ({ ...v, [e.target.name]: undefined }));
  };
  const submit = async (e) => {
    e.preventDefault();
    if (submitting.current) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    setErrors({});
    const payload = {
      ...values,
      subjects: values.subjects
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean),
      experienceYears:
        values.experienceYears === "" ? null : Number(values.experienceYears),
      salary: values.salary === "" ? null : values.salary,
      userId: values.userId === "" ? null : Number(values.userId),
      dateOfBirth: values.dateOfBirth || null,
      joiningDate: values.joiningDate || null,
    };
    try {
      const { data } = id
        ? await updateTeacher(id, payload)
        : await createTeacher(payload);
      navigate(id ? `/teachers/${data.id}` : "/teachers", {
        state: {
          notice: id
            ? "Teacher details updated."
            : "Teacher added successfully.",
        },
      });
    } catch (failure) {
      setError(errorMessage(failure));
      setErrors(failure.response?.data?.errors || {});
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  };
  const field = (name, label, props = {}) => (
    <label className="field-label" key={name} htmlFor={name}>
      {label}
      {props.required && <span className="ml-1 text-indigo-500">*</span>}
      <Input
        id={name}
        name={name}
        className="mt-2"
        value={values[name]}
        onChange={change}
        aria-invalid={!!errors[name]}
        aria-describedby={errors[name] ? `${name}-error` : undefined}
        {...props}
      />
      {errors[name] && (
        <span id={`${name}-error`} className="mt-1 block text-xs text-red-600">
          {errors[name]}
        </span>
      )}
    </label>
  );
  const section = (title, Icon, children) => (
    <Card className="p-6">
      <h2 className="form-section">
        <Icon size={16} className="text-indigo-500" />
        {title}
      </h2>
      <div className="grid gap-5 md:grid-cols-2">{children}</div>
    </Card>
  );
  const today = new Date().toLocaleDateString("en-CA");
  return (
    <>
      <Link
        to={id ? `/teachers/${id}` : "/teachers"}
        className="mb-5 inline-flex items-center gap-2 text-xs text-slate-500"
      >
        <ArrowLeft size={14} />
        Back to teachers
      </Link>
      <h1 className="page-title">{id ? "Edit teacher" : "Add Teacher"}</h1>
      <p className="mb-7 mt-2 text-sm text-slate-500">
        Build a complete teaching profile for your institute.
      </p>
      <ErrorNotice message={error} />
      {loading ? (
        <Loading label="Loading teacher…" />
      ) : id && error && !values.employeeId ? (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Try again
        </Button>
      ) : (
        <form onSubmit={submit}>
          <fieldset disabled={busy} className="space-y-6">
            {section(
              "Personal information",
              UserRoundCheck,
              <>
                {field("fullName", "Full Name", {
                  required: true,
                  maxLength: 100,
                })}
                {field("employeeId", "Employee ID", {
                  required: true,
                  maxLength: 50,
                  pattern: "[A-Za-z0-9_-]+",
                })}
                {field("email", "Email", {
                  required: true,
                  type: "email",
                  maxLength: 254,
                })}
                {field("phone", "Phone", {
                  type: "tel",
                  pattern: "\\+?[0-9]{7,15}",
                  maxLength: 16,
                })}
                {field("gender", "Gender", { maxLength: 30 })}
                {field("dateOfBirth", "Date of Birth", {
                  type: "date",
                  max: today,
                })}
                <label className="field-label md:col-span-2" htmlFor="address">
                  Address
                  <textarea
                    className="input mt-2"
                    id="address"
                    name="address"
                    rows={2}
                    maxLength={500}
                    value={values.address}
                    onChange={change}
                    aria-invalid={!!errors.address}
                  />
                  {errors.address && (
                    <span className="text-xs text-red-600">
                      {errors.address}
                    </span>
                  )}
                </label>
              </>,
            )}
            {section(
              "Teaching profile",
              BookOpen,
              <>
                {field("qualification", "Qualification", { maxLength: 200 })}
                {field("specialization", "Specialization", { maxLength: 200 })}
                {field("subjects", "Subjects (comma separated)")}
                {field("experienceYears", "Experience in Years", {
                  type: "number",
                  min: 0,
                  max: 100,
                  step: 1,
                })}
                {field("joiningDate", "Joining Date", {
                  type: "date",
                  max: today,
                })}
              </>,
            )}
            {section(
              "Administration · Admin only",
              ShieldCheck,
              <>
                {field("salary", "Salary (INR)", {
                  type: "number",
                  min: 0,
                  max: "9999999999.99",
                  step: "0.01",
                })}
                {field("userId", "Linked TEACHER user ID (optional)", {
                  type: "number",
                  min: 1,
                  step: 1,
                })}
                <p className="text-xs leading-5 text-slate-500 md:col-span-2">
                  Link only an existing TEACHER login account. This never
                  changes its role. Blank optional numeric/date fields preserve
                  existing values on edit; clear Subjects to remove all
                  subjects.
                </p>
              </>,
            )}
          </fieldset>
          <div className="mt-6 flex justify-end gap-3">
            <Link
              className={`button-secondary ${busy ? "pointer-events-none opacity-50" : ""}`}
              aria-disabled={busy}
              to={id ? `/teachers/${id}` : "/teachers"}
            >
              Cancel
            </Link>
            <Button type="submit" disabled={busy || loading}>
              <Save size={15} />
              {busy ? "Saving…" : "Save teacher"}
            </Button>
          </div>
        </form>
      )}
    </>
  );
}
