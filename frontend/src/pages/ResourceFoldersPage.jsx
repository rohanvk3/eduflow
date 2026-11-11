import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { FolderOpen, ChevronRight } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Pagination,
  EmptyState,
  ErrorNotice,
  Modal,
  Loading,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  ResourceHeading,
  ResourceDenied,
  ResourceError,
  useResourceLoad,
  ScopeFields,
  Field,
  staffRole,
} from "../components/ResourceUI";
import {
  getResourceFolders,
  getResourceFolder,
  saveResourceFolder,
  deleteResourceFolder,
} from "../services/resourceFolderService";
import { resourceError } from "../services/resourceService";
export default function ResourceFoldersPage() {
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT",
    [params, setParams] = useSearchParams(),
    parentId = params.get("folderId");
  const [course, setCourse] = useState(""),
    [batch, setBatch] = useState(""),
    [search, setSearch] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [edit, setEdit] = useState(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const current = useResourceLoad(
    async (signal) => {
      if (!parentId) return { data: [] };
      const trail = [];
      let id = parentId;
      for (let n = 0; id && n < 20; n++) {
        const f = (await getResourceFolder(id, signal)).data;
        trail.unshift(f);
        id = f.parentFolderId;
      }
      return { data: trail };
    },
    [parentId],
    allowed,
  );
  const parent = current.data?.at(-1);
  const s = useResourceLoad(
    (signal) =>
      getResourceFolders(
        {
          page,
          size,
          search,
          rootOnly: !parentId,
          parentFolderId: parentId || undefined,
          courseId: course || undefined,
          batchId: batch || undefined,
        },
        signal,
      ),
    [page, size, search, parentId, course, batch],
    allowed,
  );
  if (!allowed) return <ResourceDenied />;
  const move = (id) => {
    setParams(id ? { folderId: id } : {});
    setPage(0);
    setCourse("");
    setBatch("");
  };
  const save = async (e) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await saveResourceFolder(edit.id, {
        name: edit.name,
        description: edit.description,
        courseId: Number(edit.courseId),
        batchId: edit.batchId ? Number(edit.batchId) : null,
        parentFolderId: edit.parentFolderId
          ? Number(edit.parentFolderId)
          : null,
      });
      setEdit(null);
      s.reload();
      current.reload();
    } catch (ex) {
      setError(await resourceError(ex));
    } finally {
      setBusy(false);
    }
  };
  const remove = async (f) => {
    if (!window.confirm("Delete empty folder " + f.name + "?")) return;
    setBusy(true);
    setError("");
    try {
      await deleteResourceFolder(f.id);
      s.reload();
    } catch (ex) {
      setError(await resourceError(ex));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <ResourceHeading
        title="Resource Folders"
        subtitle="Browse course and batch libraries without widening access."
      >
        <Link className="button-secondary" to="/resources">
          Open library
        </Link>
        {staff && (!parent || user.role === "ADMIN" || parent.batchId) && (
          <Button
            onClick={() => {
              setError("");
              setEdit({
                name: "",
                description: "",
                courseId: parent?.courseId || course,
                batchId: parent?.batchId || batch,
                parentFolderId: parentId || "",
              });
            }}
          >
            Create folder
          </Button>
        )}
      </ResourceHeading>
      <nav
        aria-label="Folder breadcrumb"
        className="mb-5 flex flex-wrap items-center gap-2 text-xs"
      >
        <Button variant="secondary" onClick={() => move(null)}>
          Library folders
        </Button>
        {current.data?.map((f) => (
          <span key={f.id} className="flex items-center gap-2">
            <ChevronRight size={14} />
            <Button variant="secondary" onClick={() => move(f.id)}>
              {f.name}
            </Button>
          </span>
        ))}
      </nav>
      <ResourceError state={current} />
      <Card className="mb-5 grid gap-4 p-5 md:grid-cols-3">
        <Input
          aria-label="Search folders"
          placeholder="Search folders…"
          maxLength={100}
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
        <ScopeFields
          courseId={course}
          batchId={batch}
          onCourse={(v) => {
            setCourse(v);
            setBatch("");
            setPage(0);
          }}
          onBatch={(v) => {
            setBatch(v);
            setPage(0);
          }}
        />
      </Card>
      <ResourceError state={s} />
      {!edit && <ErrorNotice message={error} />}
      {parent && (
        <Link
          className="button-secondary mb-5"
          to={"/resources?folderId=" + parent.id}
        >
          View materials in {parent.name} ({parent.resourceCount})
        </Link>
      )}
      {s.loading ? (
        <Loading label="Loading folders…" />
      ) : !s.data?.content.length ? (
        <Card>
          <EmptyState
            title="No folders here"
            description="Materials can also be stored directly in the library."
          />
        </Card>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {s.data.content.map((f) => (
            <Card key={f.id} className="p-5">
              <FolderOpen className="mb-4 text-indigo-500" size={28} />
              <button
                className="text-left text-sm font-semibold hover:text-indigo-600"
                onClick={() => move(f.id)}
              >
                {f.name}
              </button>
              <p className="mt-2 text-xs text-slate-500">
                {f.courseName} · {f.batchName || "Course-wide"}
              </p>
              <p className="mt-2 line-clamp-2 text-xs text-slate-500">
                {f.description}
              </p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Link
                  className="button-secondary"
                  to={"/resources?folderId=" + f.id}
                >
                  {f.resourceCount} resources
                </Link>
                {staff && (user.role === "ADMIN" || f.batchId) && (
                  <>
                    <Button
                      variant="secondary"
                      disabled={busy}
                      onClick={() => {
                        setError("");
                        setEdit({
                          ...f,
                          parentFolderId: f.parentFolderId || "",
                        });
                      }}
                    >
                      Rename / move
                    </Button>
                    <Button
                      variant="secondary"
                      disabled={busy}
                      onClick={() => remove(f)}
                    >
                      Delete
                    </Button>
                  </>
                )}
              </div>
            </Card>
          ))}
        </div>
      )}
      <Pagination
        noun="folders"
        page={page}
        size={size}
        total={s.data?.totalElements}
        totalPages={s.data?.totalPages}
        onPageChange={setPage}
        onSizeChange={(v) => {
          setSize(v);
          setPage(0);
        }}
        loading={s.loading}
      />
      {edit && (
        <Modal
          title={edit.id ? "Edit folder" : "Create folder"}
          busy={busy}
          onClose={() => setEdit(null)}
        >
          <form className="space-y-5 p-6" onSubmit={save}>
            <ErrorNotice message={error} />
            <Field label="Folder name *">
              <Input
                required
                maxLength={150}
                value={edit.name}
                onChange={(e) => setEdit({ ...edit, name: e.target.value })}
              />
            </Field>
            <Field label="Description">
              <textarea
                className="input"
                maxLength={2000}
                value={edit.description || ""}
                onChange={(e) =>
                  setEdit({ ...edit, description: e.target.value })
                }
              />
            </Field>
            {edit.id ? (
              <p className="text-xs text-slate-500">
                Course and batch scope are preserved when renaming or moving a
                folder.
              </p>
            ) : (
              <ScopeFields
                required
                teacher={user.role === "TEACHER"}
                courseId={edit.courseId}
                batchId={edit.batchId}
                onCourse={(v) =>
                  setEdit({
                    ...edit,
                    courseId: v,
                    batchId: "",
                    parentFolderId: "",
                  })
                }
                onBatch={(v) =>
                  setEdit({ ...edit, batchId: v, parentFolderId: "" })
                }
              />
            )}
            <RecordSelect
              label="Parent folder"
              endpoint={
                "/resources/folders?courseId=" +
                (edit.courseId || 0) +
                (edit.batchId
                  ? "&batchId=" + edit.batchId
                  : "&courseWideOnly=true")
              }
              required={false}
              value={edit.parentFolderId}
              selectedLabel={edit.parentFolderName}
              onChange={(v) => setEdit({ ...edit, parentFolderId: v })}
            />
            <p className="text-xs text-slate-500">
              Parent folders must share the exact course and batch scope.
              Nonempty folders cannot be deleted.
            </p>
            <Button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save folder"}
            </Button>
          </form>
        </Modal>
      )}
    </>
  );
}
