import { useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { Grid2X2, List, FolderOpen, Plus } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Input,
  Select,
  Button,
  Pagination,
  EmptyState,
  Table,
  Skeleton,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import ResourceDashboard from "../components/ResourceDashboard";
import {
  ResourceHeading,
  ResourceDenied,
  ResourceCard,
  ResourceError,
  ScopeFields,
  useResourceLoad,
  staffRole,
  MaterialActions,
  resourceTime,
} from "../components/ResourceUI";
import { getMaterials } from "../services/resourceService";
export default function ResourcesPage({ own = false }) {
  const { user } = useAuth(),
    staff = staffRole(user.role),
    allowed = staff || user.role === "STUDENT",
    [params, setParams] = useSearchParams();
  const [search, setSearch] = useState(""),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [course, setCourse] = useState(""),
    [batch, setBatch] = useState(""),
    [category, setCategory] = useState(""),
    [type, setType] = useState(""),
    [visibility, setVisibility] = useState(""),
    [grid, setGrid] = useState(true),
    [mine, setMine] = useState(own && staff);
  const folder = params.get("folderId");
  const state = useResourceLoad(
    (signal) =>
      getMaterials(
        {
          search,
          page,
          size,
          courseId: course || undefined,
          batchId: batch || undefined,
          categoryId: category || undefined,
          folderId: folder || undefined,
          resourceType: type || undefined,
          visibility: visibility || undefined,
          uploadedBy: mine ? (user.id ?? user.userId) : undefined,
        },
        signal,
      ),
    [
      search,
      page,
      size,
      course,
      batch,
      category,
      type,
      visibility,
      folder,
      mine,
    ],
    allowed,
  );
  const change = (setter) => (value) => {
    setter(value);
    setPage(0);
  };
  if (!allowed) return <ResourceDenied />;
  return (
    <>
      <ResourceHeading title={own ? "My Resources" : "Study Materials"}>
        <Link className="button-secondary" to="/resources/folders">
          <FolderOpen size={16} />
          Browse folders
        </Link>
        {staff && (
          <Link className="button-primary" to="/resources/new">
            <Plus size={16} />
            Upload material
          </Link>
        )}
      </ResourceHeading>
      <ResourceDashboard compact />
      <Card className="mb-5 p-5">
        <div className="grid gap-4 md:grid-cols-3">
          <label className="field-label">
            Search
            <Input
              className="mt-2"
              aria-label="Search resources"
              placeholder="Find notes, sheets, references…"
              maxLength={100}
              value={search}
              onChange={(e) => change(setSearch)(e.target.value)}
            />
          </label>
          <ScopeFields
            courseId={course}
            batchId={batch}
            onCourse={(value) => {
              change(setCourse)(value);
              setBatch("");
            }}
            onBatch={change(setBatch)}
          />
          <RecordSelect
            label="Category"
            endpoint="/resources/categories"
            value={category}
            onChange={change(setCategory)}
            required={false}
          />
          <label className="field-label">
            Resource type
            <Select
              className="mt-2"
              aria-label="Resource type"
              value={type}
              onChange={(e) => change(setType)(e.target.value)}
            >
              <option value="">All types</option>
              {["DOCUMENT", "PRESENTATION", "IMAGE", "LINK"].map((t) => (
                <option key={t}>{t}</option>
              ))}
            </Select>
          </label>
          {staff && (
            <label className="field-label">
              Visibility
              <Select
                className="mt-2"
                aria-label="Visibility"
                value={visibility}
                onChange={(e) => change(setVisibility)(e.target.value)}
              >
                <option value="">All states</option>
                {["DRAFT", "PUBLISHED", "ARCHIVED"].map((t) => (
                  <option key={t}>{t}</option>
                ))}
              </Select>
            </label>
          )}
        </div>
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
          {folder ? (
            <p className="text-xs">
              Showing folder #{folder}{" "}
              <Button variant="secondary" onClick={() => setParams({})}>
                Clear folder
              </Button>
            </p>
          ) : (
            <span className="text-xs text-slate-500">
              Only resources available to your account are shown.
            </span>
          )}
          {staff && (
            <label className="flex gap-2 text-xs">
              <input
                type="checkbox"
                checked={mine}
                onChange={(e) => change(setMine)(e.target.checked)}
              />
              Uploaded by me
            </label>
          )}
          <div className="flex gap-1">
            <Button
              aria-label="Grid view"
              aria-pressed={grid}
              variant="secondary"
              onClick={() => setGrid(true)}
            >
              <Grid2X2 size={16} />
            </Button>
            <Button
              aria-label="List view"
              aria-pressed={!grid}
              variant="secondary"
              onClick={() => setGrid(false)}
            >
              <List size={16} />
            </Button>
          </div>
        </div>
      </Card>
      <ResourceError state={state} />
      {state.loading ? (
        <div className="grid gap-4 md:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-64" />
          ))}
        </div>
      ) : !state.error && !state.data?.content.length ? (
        <Card>
          <EmptyState
            title="No learning resources found"
            description={
              staff
                ? "Upload a material or adjust your filters."
                : "Published materials for your active enrollments will appear here."
            }
          />
        </Card>
      ) : (
        state.data &&
        (grid ? (
          <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
            {state.data.content.map((m) => (
              <ResourceCard key={m.id} material={m} />
            ))}
          </div>
        ) : (
          <Card>
            <Table label="Study materials">
              <thead>
                <tr>
                  <th>Resource</th>
                  <th>Course / batch</th>
                  <th>Uploader</th>
                  <th>State</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {state.data.content.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <Link
                        className="font-semibold text-indigo-600"
                        to={`/resources/materials/${m.id}`}
                      >
                        {m.title}
                      </Link>
                      <p className="mt-1 text-xs text-slate-500">
                        {m.categoryName}
                      </p>
                    </td>
                    <td>
                      {m.courseName}
                      <p className="text-xs">{m.batchName || "Course-wide"}</p>
                    </td>
                    <td>
                      {m.uploadedBy}
                      <p className="text-xs">{resourceTime(m.createdAt)}</p>
                    </td>
                    <td>{m.visibility}</td>
                    <td>
                      <MaterialActions material={m} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </Card>
        ))
      )}
      <div className="mt-5">
        <Pagination
          noun="resources"
          page={page}
          size={size}
          total={state.data?.totalElements}
          totalPages={state.data?.totalPages}
          onPageChange={setPage}
          onSizeChange={change(setSize)}
          loading={state.loading}
        />
      </div>
    </>
  );
}
