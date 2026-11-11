import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  FileText,
  Image,
  Presentation,
  Sheet,
  Link2,
  Library,
  UploadCloud,
  Download,
  Eye,
} from "lucide-react";
import { Button, Card, EmptyState, Select, Modal, ErrorNotice } from "./UI";
import { useFinanceLoad, FinanceError, Field } from "./FinanceUI";
import {
  getResourceOptions,
  getResourcePolicy,
  downloadMaterial,
  previewMaterial,
  resourceError,
  saveDownload,
} from "../services/resourceService";
export {
  useFinanceLoad as useResourceLoad,
  FinanceError as ResourceError,
  Field,
};
export const staffRole = (role) => ["ADMIN", "TEACHER"].includes(role);
export const resourceTime = (value) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: "Asia/Kolkata",
      }).format(
        new Date(
          value.endsWith("Z") || /[+-]\d\d:\d\d$/.test(value)
            ? value
            : value + "+05:30",
        ),
      )
    : "—";
export const fileSize = (n) =>
  n == null
    ? "No file"
    : n < 1024
      ? n + " B"
      : n < 1048576
        ? (n / 1024).toFixed(1) + " KB"
        : (n / 1048576).toFixed(1) + " MB";
export function ResourceHeading({
  title = "Study Materials",
  subtitle = "Access and organize learning resources across courses and batches.",
  children,
}) {
  return (
    <div className="mb-7 flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="eyebrow mb-2">Learning library</p>
        <h1 className="page-title">{title}</h1>
        <p className="mt-2 max-w-xl text-sm text-slate-500">{subtitle}</p>
      </div>
      <div className="flex flex-wrap gap-2">{children}</div>
    </div>
  );
}
export function ResourceDenied() {
  return (
    <Card>
      <EmptyState
        title="Resource access restricted"
        description="Learning resources are available to administrators, assigned teachers, and enrolled students."
      />
    </Card>
  );
}
export function ResourceIcon({ material }) {
  const mime = material.file?.contentType || "";
  const label =
    material.resourceType === "LINK"
      ? "Link"
      : material.resourceType === "IMAGE"
        ? "Image"
        : mime === "application/pdf"
          ? "PDF"
          : mime.includes("wordprocessing")
            ? "Word"
            : mime.includes("presentation")
              ? "Slides"
              : mime.includes("spreadsheet")
                ? "Excel"
                : "Text";
  const Icon =
    material.resourceType === "LINK"
      ? Link2
      : material.resourceType === "IMAGE"
        ? Image
        : material.resourceType === "PRESENTATION"
          ? Presentation
          : mime.includes("spreadsheet")
            ? Sheet
            : FileText;
  return (
    <span
      title={label}
      className="inline-flex h-14 w-14 shrink-0 flex-col items-center justify-center gap-1 rounded-xl bg-indigo-50 text-indigo-600"
    >
      <Icon size={21} aria-hidden="true" />
      <span className="text-[9px] font-semibold">{label}</span>
    </span>
  );
}
export function ScopeFields({
  courseId,
  batchId,
  onCourse,
  onBatch,
  teacher = false,
  required = false,
}) {
  const s = useFinanceLoad(getResourceOptions, []);
  return (
    <>
      <FinanceError state={s} />
      <Field label={"Course" + (required ? " *" : "")}>
        <Select
          aria-label="Course"
          required={required}
          value={courseId || ""}
          onChange={(e) => onCourse(e.target.value)}
        >
          <option value="">All courses</option>
          {s.data?.courses.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </Select>
      </Field>
      <Field label={"Batch" + (teacher && required ? " *" : "")}>
        <Select
          aria-label="Batch"
          value={batchId || ""}
          required={teacher && required}
          onChange={(e) => onBatch(e.target.value)}
        >
          <option value="">
            {teacher && required
              ? "Select assigned batch"
              : "Course-wide / all batches"}
          </option>
          {s.data?.batches
            .filter((b) => !courseId || String(b.courseId) === String(courseId))
            .map((b) => (
              <option key={b.id} value={b.id}>
                {b.name}
              </option>
            ))}
        </Select>
      </Field>
    </>
  );
}
export function ResourceFilePicker({
  file,
  onChange,
  disabled = false,
  required = false,
}) {
  const s = useFinanceLoad(getResourcePolicy, []),
    [error, setError] = useState("");
  const accept = s.data?.extensions || [
    "pdf",
    "docx",
    "pptx",
    "xlsx",
    "txt",
    "png",
    "jpg",
    "jpeg",
  ];
  const choose = (f) => {
    setError("");
    if (!f) return;
    if (!accept.includes(f.name.split(".").pop().toLowerCase())) {
      setError("Choose a supported learning document or image.");
      return;
    }
    if (!s.data) {
      setError("Wait for the upload policy to load.");
      return;
    }
    if (f.size > s.data.maxFileBytes) {
      setError("File exceeds " + fileSize(s.data.maxFileBytes));
      return;
    }
    onChange(f);
  };
  return (
    <div>
      <FinanceError state={s} />
      <label
        className="block cursor-pointer rounded-xl border-2 border-dashed border-slate-200 bg-slate-50 p-6 text-center"
        onDragOver={(e) => e.preventDefault()}
        onDrop={(e) => {
          e.preventDefault();
          if (!disabled) choose(e.dataTransfer.files[0]);
        }}
      >
        <UploadCloud className="mx-auto mb-3 text-indigo-500" size={26} />
        <span className="block text-sm font-medium">
          Drop a file here or browse
        </span>
        <span className="my-2 block text-xs text-slate-500">
          {accept.join(", ").toUpperCase()} ·{" "}
          {s.data
            ? fileSize(s.data.maxFileBytes) + " maximum"
            : "Loading upload policy"}
        </span>
        <input
          className="mx-auto block max-w-full text-xs"
          aria-label="Resource file"
          type="file"
          required={required && !file}
          disabled={disabled || !s.data}
          accept={accept.map((e) => "." + e).join(",")}
          onChange={(e) => {
            choose(e.target.files[0]);
            e.target.value = "";
          }}
        />
      </label>
      {file && (
        <p className="mt-2 break-all text-xs">
          {file.name} · {fileSize(file.size)}{" "}
          <Button
            variant="secondary"
            disabled={disabled}
            onClick={() => onChange(null)}
          >
            Remove
          </Button>
        </p>
      )}
      <ErrorNotice message={error} />
    </div>
  );
}
export function MaterialActions({ material }) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [preview, setPreview] = useState(null);
  useEffect(
    () => () => {
      if (preview) URL.revokeObjectURL(preview);
    },
    [preview],
  );
  const run = async (inline) => {
    setBusy(true);
    setError("");
    try {
      const r = await (inline
        ? previewMaterial(material.id)
        : downloadMaterial(material.id));
      if (inline) setPreview(URL.createObjectURL(r.data));
      else saveDownload(r.data, material.file?.originalFileName || "resource");
    } catch (e) {
      setError(await resourceError(e));
    } finally {
      setBusy(false);
    }
  };
  if (material.resourceType === "LINK")
    return (
      <a
        href={material.externalUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="button-secondary"
      >
        Open resource <Link2 size={14} />
      </a>
    );
  const canPreview =
    material.file?.contentType === "application/pdf" ||
    material.file?.contentType?.startsWith("image/");
  return (
    <div>
      <div className="flex flex-wrap gap-2">
        {canPreview && (
          <Button variant="secondary" disabled={busy} onClick={() => run(true)}>
            <Eye size={14} />
            Preview
          </Button>
        )}
        <Button
          variant="secondary"
          disabled={busy || !material.file}
          onClick={() => run(false)}
        >
          <Download size={14} />
          Download
        </Button>
      </div>
      <ErrorNotice message={error} />
      {preview && (
        <Modal
          title={"Preview: " + material.title}
          onClose={() => setPreview(null)}
        >
          <div className="p-4">
            {material.file.contentType.startsWith("image/") ? (
              <img
                alt={material.title}
                src={preview}
                className="mx-auto max-h-[65vh] max-w-full"
              />
            ) : (
              <iframe
                title={material.title + " PDF preview"}
                src={preview}
                className="h-[65vh] w-full rounded-lg"
              />
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
export function ResourceCard({ material: m }) {
  return (
    <Card className="flex min-w-0 flex-col gap-4 p-5 transition-shadow hover:shadow-md">
      <div className="flex justify-between gap-3">
        <ResourceIcon material={m} />
        <span className="text-[10px] font-medium text-slate-500">
          {m.visibility}
        </span>
      </div>
      <div className="flex-1">
        <Link
          className="break-words text-sm font-semibold text-slate-900 hover:text-indigo-600"
          to={`/resources/materials/${m.id}`}
        >
          {m.title}
        </Link>
        <p className="mt-2 text-xs text-slate-500">
          {m.courseName} · {m.batchName || "Course-wide"}
        </p>
        <p className="mt-2 line-clamp-2 text-xs leading-5 text-slate-500">
          {m.description || m.categoryName}
        </p>
      </div>
      <div className="border-t border-slate-100 pt-3 text-[11px] text-slate-500">
        <p>
          {m.uploadedBy} · {resourceTime(m.createdAt)}
        </p>
        <p className="mt-1">
          {fileSize(m.file?.fileSize)} · Version {m.currentVersion}
        </p>
      </div>
      <MaterialActions material={m} />
    </Card>
  );
}
