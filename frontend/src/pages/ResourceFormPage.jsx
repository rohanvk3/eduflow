import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  Input,
  Select,
  ErrorNotice,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ResourceHeading,
  ResourceDenied,
  ResourceError,
  ScopeFields,
  ResourceFilePicker,
  Field,
  useResourceLoad,
  staffRole,
} from "../components/ResourceUI";
import {
  getMaterial,
  saveMaterial,
  publishMaterial,
  resourceError,
} from "../services/resourceService";
export default function ResourceFormPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = staffRole(user.role),
    s = useResourceLoad(
      (signal) =>
        id ? getMaterial(id, signal) : Promise.resolve({ data: {} }),
      [id],
      allowed,
    );
  if (!allowed) return <ResourceDenied />;
  if (s.loading) return <Loading label="Loading material…" />;
  if (s.error) return <ResourceError state={s} />;
  return s.data && <Form key={id || "new"} initial={s.data} user={user} />;
}
function Form({ initial, user }) {
  const nav = useNavigate(),
    [form, setForm] = useState({
      title: "",
      description: "",
      courseId: "",
      batchId: "",
      categoryId: "",
      folderId: "",
      resourceType: "DOCUMENT",
      externalUrl: "",
      ...initial,
    }),
    [file, setFile] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [progress, setProgress] = useState(0),
    [saved, setSaved] = useState(initial.id ? initial : null);
  const edit = (key, value) => setForm((f) => ({ ...f, [key]: value }));
  const frozen = initial.id && initial.visibility !== "DRAFT";
  const submit = async (event, publish = false) => {
    event.preventDefault();
    if (busy) return;
    if (
      publish &&
      !window.confirm("Publish this material to eligible students?")
    )
      return;
    setBusy(true);
    setError("");
    try {
      const input = {
        ...form,
        courseId: Number(form.courseId),
        batchId: form.batchId ? Number(form.batchId) : null,
        categoryId: Number(form.categoryId),
        folderId: form.folderId ? Number(form.folderId) : null,
        version: saved?.version,
      };
      let m = (
        await saveMaterial(saved?.id, input, saved ? null : file, (e) =>
          setProgress(e.total ? Math.round((e.loaded / e.total) * 100) : 0),
        )
      ).data;
      setSaved(m);
      if (publish) m = (await publishMaterial(m.id, m.version)).data;
      nav("/resources/materials/" + m.id, {
        state: { message: publish ? "Material published" : "Draft saved" },
      });
    } catch (e) {
      setError(await resourceError(e));
    } finally {
      setBusy(false);
    }
  };
  if (frozen)
    return (
      <Card className="p-6">
        <p>
          Published metadata is preserved. Upload a replacement from the
          material details page.
        </p>
        <Link
          to={"/resources/materials/" + initial.id}
          className="button-secondary mt-4"
        >
          Back to material
        </Link>
      </Card>
    );
  return (
    <>
      <ResourceHeading
        title={initial.id ? "Edit material" : "Upload material"}
        subtitle="Save a private draft, then publish when it is ready."
      />
      <form onSubmit={(e) => submit(e, false)}>
        <ErrorNotice message={error} />
        {saved && !initial.id && (
          <p role="status" className="mb-4 text-sm">
            Draft #{saved.id} is saved. You can retry publication without
            creating a duplicate.
          </p>
        )}
        <Card className="p-6">
          <div className="grid gap-5 md:grid-cols-2">
            <Field label="Title *">
              <Input
                required
                maxLength={180}
                value={form.title}
                onChange={(e) => edit("title", e.target.value)}
              />
            </Field>
            <Field label="Resource type *">
              <Select
                aria-label="Resource type"
                value={form.resourceType}
                onChange={(e) => {
                  edit("resourceType", e.target.value);
                  setFile(null);
                }}
              >
                {["DOCUMENT", "PRESENTATION", "IMAGE", "LINK"].map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </Select>
            </Field>
            <ScopeFields
              courseId={form.courseId}
              batchId={form.batchId}
              required
              teacher={user.role === "TEACHER"}
              onCourse={(value) =>
                setForm((f) => ({
                  ...f,
                  courseId: value,
                  batchId: "",
                  folderId: "",
                }))
              }
              onBatch={(value) =>
                setForm((f) => ({ ...f, batchId: value, folderId: "" }))
              }
            />
            <RecordSelect
              label="Category"
              endpoint="/resources/categories"
              value={form.categoryId}
              selectedLabel={initial.categoryName}
              onChange={(v) => edit("categoryId", v)}
            />
            <RecordSelect
              label="Folder"
              endpoint={
                "/resources/folders?courseId=" +
                (form.courseId || 0) +
                (form.batchId
                  ? "&batchId=" + form.batchId
                  : "&courseWideOnly=true")
              }
              value={form.folderId}
              selectedLabel={initial.folderName}
              onChange={(v) => edit("folderId", v)}
              required={false}
            />
            <div className="md:col-span-2">
              <Field label="Description">
                <textarea
                  className="input min-h-32"
                  maxLength={10000}
                  value={form.description || ""}
                  onChange={(e) => edit("description", e.target.value)}
                />
              </Field>
            </div>
            <div className="md:col-span-2">
              {form.resourceType === "LINK" ? (
                <Field label="External URL *">
                  <Input
                    type="url"
                    required
                    maxLength={2000}
                    placeholder="https://…"
                    value={form.externalUrl || ""}
                    onChange={(e) => edit("externalUrl", e.target.value)}
                  />
                </Field>
              ) : initial.id ? (
                <p className="text-sm text-slate-500">
                  Use the material details page to upload or replace files.
                  Existing versions are preserved.
                </p>
              ) : (
                <ResourceFilePicker
                  file={file}
                  onChange={setFile}
                  disabled={busy || !!saved}
                  required
                />
              )}
            </div>
          </div>
          {busy && file && (
            <progress
              aria-label="Upload progress"
              value={progress}
              max={100}
              className="mt-4 w-full accent-indigo-600"
            />
          )}
        </Card>
        <div className="mt-5 flex flex-wrap gap-3">
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save draft"}
          </Button>
          <Button
            variant="secondary"
            type="submit"
            disabled={busy}
            onClick={(e) => {
              if (e.currentTarget.form.reportValidity()) submit(e, true);
              else e.preventDefault();
            }}
          >
            Save and publish
          </Button>
          <Link
            className="button-secondary"
            to={saved ? "/resources/materials/" + saved.id : "/resources"}
          >
            Cancel
          </Link>
        </div>
      </form>
    </>
  );
}
