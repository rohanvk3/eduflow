import { useState } from "react";
import { Link, useParams, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  ErrorNotice,
  Loading,
  Table,
} from "../components/UI";
import {
  ResourceHeading,
  ResourceDenied,
  ResourceError,
  ResourceIcon,
  MaterialActions,
  ResourceFilePicker,
  Field,
  useResourceLoad,
  staffRole,
  fileSize,
  resourceTime,
} from "../components/ResourceUI";
import {
  getMaterial,
  getMaterialVersions,
  uploadMaterialVersion,
  publishMaterial,
  archiveMaterial,
  downloadMaterial,
  saveDownload,
  resourceError,
} from "../services/resourceService";
export default function ResourceDetailsPage() {
  const { id } = useParams(),
    { user } = useAuth(),
    location = useLocation(),
    allowed = staffRole(user.role) || user.role === "STUDENT",
    s = useResourceLoad((signal) => getMaterial(id, signal), [id], allowed);
  if (!allowed) return <ResourceDenied />;
  if (s.loading) return <Loading label="Loading resource…" />;
  if (s.error) return <ResourceError state={s} />;
  return (
    s.data && (
      <Details
        key={s.data.id + "-" + s.data.version}
        m={s.data}
        user={user}
        reload={s.reload}
        message={location.state?.message}
      />
    )
  );
}
function Details({ m, user, reload, message }) {
  const manage =
      user.role === "ADMIN" || (user.role === "TEACHER" && !!m.batchId),
    [file, setFile] = useState(null),
    [notes, setNotes] = useState(""),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [progress, setProgress] = useState(0);
  const history = useResourceLoad(
    (signal) => getMaterialVersions(m.id, signal),
    [m.id, m.version],
    manage,
  );
  const run = async (action, confirmation) => {
    if (busy) return;
    if (confirmation && !window.confirm(confirmation)) return;
    setBusy(true);
    setError("");
    try {
      await action();
      reload();
    } catch (e) {
      setError(await resourceError(e));
    } finally {
      setBusy(false);
    }
  };
  const downloadable = async (v) => {
    setBusy(true);
    setError("");
    try {
      const r = await downloadMaterial(m.id, v.id);
      saveDownload(r.data, v.originalFileName);
    } catch (e) {
      setError(await resourceError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <ResourceHeading
        title={m.title}
        subtitle={m.courseName + " · " + (m.batchName || "Course-wide")}
      >
        <Link className="button-secondary" to="/resources">
          Back to library
        </Link>
        {manage && m.visibility === "DRAFT" && (
          <Link
            className="button-secondary"
            to={`/resources/materials/${m.id}/edit`}
          >
            Edit metadata
          </Link>
        )}
        {manage &&
          m.visibility !== "ARCHIVED" &&
          (m.visibility === "DRAFT" ||
            m.currentVersion !== m.publishedVersion) && (
            <Button
              disabled={busy}
              onClick={() =>
                run(
                  () => publishMaterial(m.id, m.version),
                  "Publish the current material version to eligible students?",
                )
              }
            >
              Publish{m.visibility === "PUBLISHED" ? " latest version" : ""}
            </Button>
          )}
        {manage && m.visibility === "PUBLISHED" && (
          <Button
            variant="secondary"
            disabled={busy}
            onClick={() =>
              run(
                () => archiveMaterial(m.id, m.version),
                "Archive this material? Students will lose access; version history will be preserved.",
              )
            }
          >
            Archive
          </Button>
        )}
      </ResourceHeading>
      <ErrorNotice message={error} />
      {message && (
        <p role="status" className="mb-4 text-sm text-emerald-600">
          {message}
        </p>
      )}
      <div className="grid gap-5 lg:grid-cols-[2fr_1fr]">
        <Card className="min-w-0 p-6">
          <div className="mb-5 flex items-center gap-4">
            <ResourceIcon material={m} />
            <div>
              <p className="text-xs font-semibold text-indigo-600">
                {m.categoryName}
              </p>
              <p className="mt-1 text-xs text-slate-500">
                {m.visibility} · Version {m.currentVersion}
              </p>
            </div>
          </div>
          <p className="whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
            {m.description || "No description provided."}
          </p>
          <div className="mt-6">
            <MaterialActions material={m} />
          </div>
          {m.file &&
            !["application/pdf", "image/png", "image/jpeg"].includes(
              m.file.contentType,
            ) && (
              <p className="mt-3 text-xs text-slate-500">
                This document is available as an authenticated download. Inline
                preview is supported for PDF and images.
              </p>
            )}
        </Card>
        <Card className="p-6">
          <h2 className="mb-5 text-sm font-semibold">Resource information</h2>
          <dl className="space-y-4 text-xs">
            {[
              ["Course", m.courseName],
              ["Batch", m.batchName || "Course-wide"],
              ["Folder", m.folderName || "Library root"],
              ["Uploaded by", m.uploadedBy],
              ["Uploaded", resourceTime(m.createdAt)],
              ["File", m.file?.originalFileName || "External link"],
              ["Size", fileSize(m.file?.fileSize)],
              ["Published version", m.publishedVersion ?? "Not published"],
            ].map(([label, value]) => (
              <div key={label}>
                <dt className="text-slate-500">{label}</dt>
                <dd className="mt-1 break-words font-medium">{value}</dd>
              </div>
            ))}
          </dl>
        </Card>
      </div>
      {manage && m.resourceType !== "LINK" && m.visibility !== "ARCHIVED" && (
        <Card className="mt-5 p-6">
          <h2 className="text-sm font-semibold">Upload a new version</h2>
          <p className="mb-5 mt-2 text-xs text-slate-500">
            Existing files remain in history. Students continue to receive the
            published version until you publish the replacement.
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (file)
                run(() =>
                  uploadMaterialVersion(m.id, m.version, file, notes, (e) =>
                    setProgress(
                      e.total ? Math.round((e.loaded / e.total) * 100) : 0,
                    ),
                  ),
                );
            }}
          >
            <ResourceFilePicker
              file={file}
              onChange={setFile}
              required
              disabled={busy}
            />
            <div className="my-4">
              <Field label="Change notes">
                <Input
                  maxLength={2000}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                />
              </Field>
            </div>
            {busy && (
              <progress
                aria-label="Version upload progress"
                max={100}
                value={progress}
              />
            )}
            <Button type="submit" disabled={busy || !file}>
              {busy ? "Uploading…" : "Save new version"}
            </Button>
          </form>
        </Card>
      )}
      {manage && (
        <Card className="mt-5 p-5">
          <h2 className="mb-4 text-sm font-semibold">Version history</h2>
          <ResourceError state={history} />
          {history.loading ? (
            <Loading label="Loading versions…" />
          ) : history.data?.length ? (
            <Table label="Material versions">
              <thead>
                <tr>
                  <th>Version</th>
                  <th>File</th>
                  <th>Uploaded by</th>
                  <th>Notes</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {history.data.map((v) => (
                  <tr key={v.id}>
                    <td>
                      v{v.versionNumber}
                      {v.versionNumber === m.publishedVersion
                        ? " · Published"
                        : ""}
                    </td>
                    <td>
                      {v.originalFileName}
                      <p className="mt-1 text-xs text-slate-500">
                        {fileSize(v.fileSize)}
                      </p>
                    </td>
                    <td>
                      {v.uploadedBy}
                      <p className="mt-1 text-xs">
                        {resourceTime(v.createdAt)}
                      </p>
                    </td>
                    <td className="max-w-xs break-words">
                      {v.changeNotes || "—"}
                    </td>
                    <td>
                      <Button
                        variant="secondary"
                        disabled={busy}
                        onClick={() => downloadable(v)}
                      >
                        Download v{v.versionNumber}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          ) : (
            <p className="text-xs text-slate-500">
              {m.resourceType === "LINK"
                ? "External links do not have binary versions."
                : "Upload the first file to get started."}
            </p>
          )}
        </Card>
      )}
    </>
  );
}
