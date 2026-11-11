import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Button,
  Input,
  Select,
  Table,
  Pagination,
  EmptyState,
  ErrorNotice,
  Modal,
  Loading,
} from "../components/UI";
import {
  ResourceHeading,
  ResourceDenied,
  ResourceError,
  useResourceLoad,
  Field,
} from "../components/ResourceUI";
import {
  getResourceCategories,
  saveResourceCategory,
  deactivateResourceCategory,
} from "../services/resourceCategoryService";
import { resourceError } from "../services/resourceService";
export default function ResourceCategoriesPage() {
  const { user } = useAuth(),
    allowed = user.role === "ADMIN",
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [edit, setEdit] = useState(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  const s = useResourceLoad(
    (signal) =>
      getResourceCategories(
        { page, size, search, status: status || undefined },
        signal,
      ),
    [page, size, search, status],
    allowed,
  );
  if (!allowed) return <ResourceDenied />;
  const save = async (e) => {
    e.preventDefault();
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await saveResourceCategory(edit.id, {
        name: edit.name,
        description: edit.description,
        icon: edit.icon,
      });
      setEdit(null);
      s.reload();
    } catch (ex) {
      setError(await resourceError(ex));
    } finally {
      setBusy(false);
    }
  };
  const deactivate = async (c) => {
    if (
      !window.confirm(
        "Deactivate " + c.name + "? Existing materials remain accessible.",
      )
    )
      return;
    setBusy(true);
    setError("");
    try {
      await deactivateResourceCategory(c.id);
      s.reload();
    } catch (e) {
      setError(await resourceError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <>
      <ResourceHeading
        title="Resource Categories"
        subtitle="Organize learning materials with consistent, reusable categories."
      >
        <Button
          onClick={() => {
            setEdit({ name: "", description: "", icon: "" });
            setError("");
          }}
        >
          Add category
        </Button>
      </ResourceHeading>
      <Card className="mb-5 grid gap-4 p-5 sm:grid-cols-2">
        <Input
          aria-label="Search categories"
          placeholder="Search categories…"
          value={search}
          maxLength={100}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
        <Select
          aria-label="Category status"
          value={status}
          onChange={(e) => {
            setStatus(e.target.value);
            setPage(0);
          }}
        >
          <option value="">All categories</option>
          <option>ACTIVE</option>
          <option>INACTIVE</option>
        </Select>
      </Card>
      <ResourceError state={s} />
      {!edit && <ErrorNotice message={error} />}
      <Card>
        {s.loading ? (
          <Loading label="Loading categories…" />
        ) : s.data?.content.length ? (
          <Table label="Resource categories">
            <thead>
              <tr>
                <th>Category</th>
                <th>Description</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {s.data.content.map((c) => (
                <tr key={c.id}>
                  <td className="font-medium">{c.name}</td>
                  <td>{c.description || "—"}</td>
                  <td>{c.status}</td>
                  <td>
                    <div className="flex gap-2">
                      <Button
                        variant="secondary"
                        disabled={busy}
                        onClick={() => {
                          setEdit(c);
                          setError("");
                        }}
                      >
                        Edit
                      </Button>
                      {c.status === "ACTIVE" && (
                        <Button
                          variant="secondary"
                          disabled={busy}
                          onClick={() => deactivate(c)}
                        >
                          Deactivate
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          <EmptyState
            title="No categories"
            description="Create the first category before uploading materials."
          />
        )}
        <Pagination
          noun="categories"
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
      </Card>
      {edit && (
        <Modal
          title={edit.id ? "Edit category" : "Add category"}
          busy={busy}
          onClose={() => setEdit(null)}
        >
          <form className="space-y-5 p-6" onSubmit={save}>
            <ErrorNotice message={error} />
            <Field label="Name *">
              <Input
                required
                maxLength={100}
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
            <Field label="Icon label (optional)">
              <Input
                maxLength={40}
                value={edit.icon || ""}
                onChange={(e) => setEdit({ ...edit, icon: e.target.value })}
              />
            </Field>
            <Button type="submit" disabled={busy}>
              {busy ? "Saving…" : "Save category"}
            </Button>
          </form>
        </Modal>
      )}
    </>
  );
}
