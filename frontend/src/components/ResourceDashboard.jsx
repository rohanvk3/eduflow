import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { Card, Skeleton } from "./UI";
import { useResourceLoad, ResourceError, staffRole } from "./ResourceUI";
import { getResourceStats } from "../services/resourceService";
export default function ResourceDashboard({ compact = false }) {
  const { user } = useAuth(),
    allowed = staffRole(user.role) || user.role === "STUDENT",
    [rev, setRev] = useState(0);
  const s = useResourceLoad(getResourceStats, [rev], allowed);
  useEffect(() => {
    if (!allowed) return;
    const refresh = () => setRev((v) => v + 1);
    const timer = setInterval(refresh, 30000);
    window.addEventListener("eduflow:resources-changed", refresh);
    window.addEventListener("focus", refresh);
    return () => {
      clearInterval(timer);
      window.removeEventListener("eduflow:resources-changed", refresh);
      window.removeEventListener("focus", refresh);
    };
  }, [allowed]);
  if (!allowed) return null;
  return (
    <Card className="mb-6 p-5">
      <div className="mb-4 flex justify-between gap-3">
        <h2 className="text-sm font-semibold">Learning resources</h2>
        <Link
          className="text-xs font-medium text-indigo-600"
          to={user.role === "STUDENT" ? "/my-resources" : "/resources"}
        >
          Open library
        </Link>
      </div>
      <ResourceError state={s} />
      {s.loading ? (
        <Skeleton className="h-20" />
      ) : (
        s.data && (
          <>
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              {[
                [
                  user.role === "STUDENT"
                    ? "Available materials"
                    : "Total materials",
                  s.data.totalMaterials,
                ],
                ["Published", s.data.publishedMaterials],
                ...(user.role === "STUDENT"
                  ? []
                  : [
                      ["Drafts", s.data.draftMaterials],
                      ["Uploaded by you", s.data.uploadedByMe],
                    ]),
              ].map(([label, n]) => (
                <div key={label}>
                  <p className="text-xs text-slate-500">{label}</p>
                  <p className="mt-2 text-2xl font-semibold tabular-nums">
                    {n}
                  </p>
                </div>
              ))}
            </div>
            {!compact && (
              <div className="mt-5 grid gap-6 border-t border-slate-100 pt-5 md:grid-cols-2">
                <div>
                  <h3 className="mb-3 text-xs font-semibold">Recent uploads</h3>
                  {s.data.recent.length ? (
                    s.data.recent.map((m) => (
                      <Link
                        key={m.id}
                        className="mb-3 block text-xs text-indigo-600"
                        to={`/resources/materials/${m.id}`}
                      >
                        {m.title}
                        <span className="ml-2 text-slate-500">
                          {m.batchName || m.courseName}
                        </span>
                      </Link>
                    ))
                  ) : (
                    <p className="text-xs text-slate-500">
                      No materials available yet.
                    </p>
                  )}
                </div>
                <div>
                  <h3 className="mb-3 text-xs font-semibold">
                    Resources by course
                  </h3>
                  {s.data.courses.map((c) => (
                    <p
                      key={c.id}
                      className="mb-2 flex justify-between gap-3 text-xs"
                    >
                      <span>{c.name}</span>
                      <strong>{c.count}</strong>
                    </p>
                  ))}
                  <h3 className="mb-3 mt-4 text-xs font-semibold">
                    Categories
                  </h3>
                  {s.data.categories.map((c) => (
                    <span
                      key={c.id}
                      className="mr-2 inline-block rounded-md bg-slate-50 px-2 py-1 text-xs text-slate-500"
                    >
                      {c.name} · {c.count}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </>
        )
      )}
    </Card>
  );
}
