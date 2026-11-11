import { useState } from "react";
import { Link } from "react-router-dom";
import { Plus, Megaphone } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import {
  Card,
  Input,
  Select,
  Table,
  Badge,
  Pagination,
  EmptyState,
  Skeleton,
} from "../components/UI";
import RecordSelect from "../components/RecordSelect";
import {
  NoticeHeading,
  NoticeError,
  useNoticeLoad,
} from "../components/NotificationUI";
import { parentTime } from "../components/ParentUI";
import { getAnnouncements } from "../services/announcementService";
export default function AnnouncementsPage() {
  const { user } = useAuth(),
    staff = ["ADMIN", "TEACHER"].includes(user.role),
    [page, setPage] = useState(0),
    [size, setSize] = useState(10),
    [search, setSearch] = useState(""),
    [audience, setAudience] = useState(""),
    [status, setStatus] = useState(""),
    [course, setCourse] = useState(""),
    [batch, setBatch] = useState("");
  const s = useNoticeLoad(
    (signal) =>
      getAnnouncements(
        {
          page,
          size,
          search,
          audienceType: audience || undefined,
          status: status || undefined,
          courseId: course || undefined,
          batchId: batch || undefined,
        },
        signal,
      ),
    [page, size, search, audience, status, course, batch],
  );
  const filter = (setter) => (value) => {
    setter(value);
    setPage(0);
  };
  return (
    <>
      <NoticeHeading
        title="Announcements"
        subtitle={
          staff
            ? "Publish clear, timely updates to authorized institute audiences."
            : "Published notices for your current authorized audience."
        }
      >
        {staff && (
          <Link className="button-primary" to="/announcements/new">
            <Plus size={16} />
            Create announcement
          </Link>
        )}
      </NoticeHeading>
      <Card className="mb-5 grid gap-4 p-5 md:grid-cols-3">
        <Input
          aria-label="Search announcements"
          placeholder="Search announcements…"
          maxLength={100}
          value={search}
          onChange={(e) => filter(setSearch)(e.target.value)}
        />
        <Select
          aria-label="Audience filter"
          value={audience}
          onChange={(e) => filter(setAudience)(e.target.value)}
        >
          <option value="">All audiences</option>
          {["ALL", "ROLE", "COURSE", "BATCH"].map((a) => (
            <option key={a}>{a}</option>
          ))}
        </Select>
        {staff && (
          <Select
            aria-label="Announcement status"
            value={status}
            onChange={(e) => filter(setStatus)(e.target.value)}
          >
            <option value="">All states</option>
            {["DRAFT", "SCHEDULED", "PUBLISHED", "ARCHIVED"].map((a) => (
              <option key={a}>{a}</option>
            ))}
          </Select>
        )}
        {staff && (
          <>
            <RecordSelect
              endpoint="/courses"
              label="Course"
              status=""
              required={false}
              value={course}
              onChange={filter(setCourse)}
            />
            <RecordSelect
              endpoint="/batches"
              label="Batch"
              status=""
              required={false}
              value={batch}
              onChange={filter(setBatch)}
            />
          </>
        )}
      </Card>
      <NoticeError state={s} />
      <Card>
        {s.loading ? (
          <div className="space-y-4 p-5">
            <Skeleton className="h-24" />
            <Skeleton className="h-24" />
          </div>
        ) : s.data?.content.length ? (
          <Table label="Announcements">
            <thead>
              <tr>
                <th>Announcement</th>
                <th>Audience</th>
                <th>Author</th>
                <th>Publication</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {s.data.content.map((a) => (
                <tr key={a.id}>
                  <td>
                    <Link
                      className="font-semibold text-indigo-600"
                      to={"/announcements/" + a.id}
                    >
                      {a.title}
                    </Link>
                    <p className="mt-1 line-clamp-2 max-w-md text-xs leading-5 text-slate-500">
                      {a.content}
                    </p>
                  </td>
                  <td>
                    {a.batchName ||
                      a.courseName ||
                      a.targetRole ||
                      "Institute-wide"}
                    <p className="mt-1 text-xs text-slate-500">
                      {a.audienceType}
                    </p>
                  </td>
                  <td>{a.createdBy}</td>
                  <td>
                    {a.status === "SCHEDULED"
                      ? parentTime(a.publishAt)
                      : a.publishedAt
                        ? parentTime(a.publishedAt)
                        : "Not published"}
                  </td>
                  <td>
                    <Badge
                      tone={
                        a.status === "PUBLISHED"
                          ? "success"
                          : a.status === "SCHEDULED"
                            ? "indigo"
                            : "neutral"
                      }
                    >
                      {a.status}
                    </Badge>
                  </td>
                  <td>
                    <Link
                      className="button-secondary"
                      to={"/announcements/" + a.id}
                    >
                      View
                    </Link>
                    {a.canManage && a.status === "DRAFT" && (
                      <Link
                        className="button-secondary ml-2"
                        to={"/announcements/" + a.id + "/edit"}
                      >
                        Edit
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !s.error && (
            <EmptyState
              icon={Megaphone}
              title="No announcements found"
              description={
                staff
                  ? "Create a draft or adjust your filters."
                  : "Relevant published announcements will appear here."
              }
            />
          )
        )}
        <Pagination
          noun="announcements"
          page={page}
          size={size}
          total={s.data?.totalElements}
          totalPages={s.data?.totalPages}
          onPageChange={setPage}
          onSizeChange={filter(setSize)}
          loading={s.loading}
        />
      </Card>
    </>
  );
}
