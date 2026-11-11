import { useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Avatar,
  Badge,
  Card,
  Input,
  Pagination,
  EmptyState,
  Skeleton,
} from "../components/UI";
import {
  ParentHeading,
  ParentDenied,
  ParentError,
  useParentLoad,
  childKey,
} from "../components/ParentUI";
import { getMyChildren } from "../services/parentService";
export default function ParentChildrenPage() {
  const { user } = useAuth(),
    allowed = user.role === "PARENT",
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [search, setSearch] = useState("");
  const s = useParentLoad(
    (signal) => getMyChildren({ page, size, search }, signal),
    [page, size, search],
    allowed,
  );
  if (!allowed) return <ParentDenied />;
  return (
    <>
      <ParentHeading
        title="My Children"
        subtitle="Only children with a currently verified relationship appear here."
      />
      <Card className="mb-5 p-5">
        <Input
          aria-label="Search my children"
          placeholder="Search by name or admission number…"
          maxLength={100}
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setPage(0);
          }}
        />
      </Card>
      <ParentError state={s} />
      {s.loading ? (
        <Skeleton className="h-64" />
      ) : s.data?.content.length ? (
        <div className="grid gap-5 md:grid-cols-2">
          {s.data.content.map((c) => (
            <Card className="p-5" key={c.id}>
              <div className="flex gap-4">
                <Avatar name={c.fullName} />
                <div>
                  <h2 className="font-semibold">{c.fullName}</h2>
                  <p className="mt-1 text-xs text-slate-500">
                    {c.admissionNumber} · {c.relationshipType}
                  </p>
                </div>
              </div>
              <div className="my-5 space-y-3">
                {c.enrollments.length ? (
                  c.enrollments.map((n) => (
                    <div
                      key={n.id}
                      className="flex flex-wrap justify-between gap-2 text-xs"
                    >
                      <span>
                        {n.courseName} · {n.batchName}
                        {n.batchStatus ? " · " + n.batchStatus : ""}
                      </span>
                      <Badge tone={n.active ? "success" : "neutral"}>
                        {n.status}
                      </Badge>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-slate-500">
                    No enrolled courses yet.
                  </p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                {[
                  ["Overview", ""],
                  ["Attendance", "/attendance"],
                  ["Fees", "/fees"],
                  ["Results", "/results"],
                  ["Assignments", "/assignments"],
                ].map(([label, path]) => (
                  <Link
                    key={label}
                    className="button-secondary"
                    to={"/parent/children/" + c.id + path}
                    onClick={() =>
                      sessionStorage.setItem(childKey(user), String(c.id))
                    }
                  >
                    {label}
                  </Link>
                ))}
              </div>
            </Card>
          ))}
        </div>
      ) : (
        !s.error && (
          <Card>
            <EmptyState
              title="No verified children linked"
              description="Contact institute administration to verify your parent–student relationship."
            />
          </Card>
        )
      )}
      <Pagination
        noun="children"
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
    </>
  );
}
