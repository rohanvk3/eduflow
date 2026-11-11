import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import axios from "axios";
import { useAuth } from "../auth/AuthContext";
import { getCourse } from "../services/academicService";
import { errorMessage } from "../api/client";
import {
  Badge,
  Button,
  Card,
  ErrorNotice,
  Loading,
  Modal,
  Toast,
} from "../components/UI";
import AcademicForm from "../components/AcademicForm";
export default function CourseDetailsPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [course, setCourse] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [reload, setReload] = useState(0);
  const allowed = ["ADMIN", "TEACHER", "STUDENT"].includes(user.role);
  useEffect(() => {
    if (!allowed) return;
    const c = new AbortController();
    setLoading(true);
    setError("");
    getCourse(id, c.signal)
      .then(({ data }) => {
        if (!c.signal.aborted) setCourse(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    return () => c.abort();
  }, [id, allowed, reload]);
  if (!allowed) return <ErrorNotice message="Course access denied." />;
  return (
    <>
      <Link className="mb-5 inline-block text-xs text-slate-500" to="/courses">
        ← Back to courses
      </Link>
      <ErrorNotice message={error} />
      {error && (
        <Button variant="secondary" onClick={() => setReload((r) => r + 1)}>
          Try again
        </Button>
      )}
      {loading ? (
        <Loading label="Loading course…" />
      ) : (
        !error &&
        course && (
          <Card className="p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-3">
                  <h1 className="page-title">{course.name}</h1>
                  <Badge>{course.status}</Badge>
                </div>
                <p className="mt-3 font-mono text-xs text-slate-500">
                  {course.code} · {course.durationWeeks} weeks
                </p>
              </div>
              {user.role === "ADMIN" && (
                <Button variant="secondary" onClick={() => setEditing(true)}>
                  Edit course
                </Button>
              )}
            </div>
            <h2 className="mt-8 text-sm font-semibold">About this course</h2>
            <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-7 text-slate-600">
              {course.description || "No description provided."}
            </p>
          </Card>
        )
      )}
      {editing && (
        <Modal
          title="Edit course"
          busy={busy}
          onClose={() => setEditing(false)}
        >
          <AcademicForm
            kind="courses"
            record={course}
            onBusyChange={setBusy}
            onClose={() => setEditing(false)}
            onSaved={(data) => {
              setCourse(data);
              setEditing(false);
              setNotice("Course saved.");
            }}
          />
        </Modal>
      )}
      <Toast message={notice} onDismiss={() => setNotice("")} />
    </>
  );
}
