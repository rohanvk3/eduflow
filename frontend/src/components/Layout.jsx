import { useEffect, useRef, useState } from "react";
import {
  NavLink,
  Link,
  Outlet,
  useLocation,
  useNavigate,
} from "react-router-dom";
import {
  GraduationCap,
  LayoutDashboard,
  Users,
  UserRoundCheck,
  BookOpen,
  Layers,
  CalendarCheck,
  Wallet,
  ClipboardCheck,
  FileText,
  Library,
  ChartNoAxesCombined,
  Settings,
  LogOut,
  PanelLeftClose,
  PanelLeftOpen,
  Menu,
  X,
  Search,
  Bell,
  ChevronRight,
  ChevronDown,
  Building2,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { Avatar, Badge, Button, Input } from "./UI";
import ThemeToggle from "./ThemeToggle";
import NotificationBell from "./NotificationBell";
import InstituteBrand from "./InstituteBrand";

function Logo({ compact = false }) {
  return (
    <Link
      to="/dashboard"
      aria-label="EduFlow dashboard"
      className="flex min-h-10 min-w-0 max-w-full items-center gap-2.5 rounded-lg"
    >
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary text-white shadow-xs">
        <GraduationCap size={22} strokeWidth={1.8} />
      </span>
      {!compact && (
        <span className="flex min-w-0 items-center gap-2 overflow-hidden text-xl font-semibold tracking-[-0.045em] text-slate-900">
          <InstituteBrand />
        </span>
      )}
    </Link>
  );
}

export default function Layout() {
  const { user, logout } = useAuth();
  const safeLogout = () => {
    if (
      window.dispatchEvent(
        new Event("eduflow:before-navigation", { cancelable: true }),
      )
    )
      logout();
  };
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem("eduflow.sidebar") === "collapsed",
  );
  const [mobileOpen, setMobileOpen] = useState(false);
  const [search, setSearch] = useState("");
  const searchRef = useRef(null);
  const drawerRef = useRef(null);
  const profileRef = useRef(null);
  const admin = user.role === "ADMIN";
  const studentAccess = admin || user.role === "STUDENT";
  const isDashboard = pathname === "/dashboard";
  const detail = pathname.startsWith("/students/");
  const current =
    pathname === "/settings/profile"
      ? "My Profile"
      : pathname === "/settings/security"
        ? "Security Settings"
        : [
              "/admin/control-center",
              "/admin/institute-settings",
              "/admin/users",
              "/admin/audit-logs",
            ].includes(pathname)
          ? {
              "/admin/control-center": "Control Center",
              "/admin/institute-settings": "Institute Settings",
              "/admin/users": "User Management",
              "/admin/audit-logs": "Audit Logs",
            }[pathname]
          : pathname.startsWith("/analytics")
            ? "Analytics"
            : pathname === "/reports"
              ? "Reports"
              : pathname === "/my-progress"
                ? "My Progress"
                : pathname.startsWith("/announcements")
                  ? "Announcements"
                  : pathname === "/notifications"
                    ? "Notifications"
                    : pathname === "/settings/notifications"
                      ? "Notification Preferences"
                      : pathname.startsWith("/parent/")
                        ? "Parent Portal"
                        : pathname.startsWith("/admin/parents")
                          ? "Parent Management"
                          : pathname.startsWith("/resources") ||
                              pathname === "/my-resources"
                            ? "Study Materials"
                            : pathname.startsWith("/assignments")
                              ? "Assignments"
                              : pathname === "/my-assignments"
                                ? "My Assignments"
                                : pathname.startsWith("/exams")
                                  ? "Exams"
                                  : pathname === "/my-results"
                                    ? "My Results"
                                    : pathname.startsWith("/finance")
                                      ? "Finance"
                                      : pathname === "/my-fees"
                                        ? "My Fees"
                                        : isDashboard
                                          ? "Overview"
                                          : pathname.startsWith("/attendance")
                                            ? "Attendance"
                                            : pathname.startsWith("/courses")
                                              ? "Courses"
                                              : pathname.startsWith("/batches")
                                                ? "Batches"
                                                : pathname.startsWith(
                                                      "/teachers",
                                                    )
                                                  ? pathname === "/teachers/me"
                                                    ? "My teaching profile"
                                                    : "Teachers"
                                                  : detail
                                                    ? pathname.endsWith("/me")
                                                      ? "My profile"
                                                      : "Student profile"
                                                    : "Students";

  useEffect(() => {
    localStorage.setItem(
      "eduflow.sidebar",
      collapsed ? "collapsed" : "expanded",
    );
  }, [collapsed]);
  useEffect(() => {
    document.title = `${current} · EduFlow`;
    setMobileOpen(false);
    if (profileRef.current) profileRef.current.open = false;
  }, [pathname, current]);
  useEffect(() => {
    const keyboard = (event) => {
      if (
        (event.ctrlKey || event.metaKey) &&
        event.key.toLowerCase() === "k" &&
        admin
      ) {
        event.preventDefault();
        searchRef.current?.focus();
      }
      if (event.key === "Escape" && profileRef.current?.open) {
        profileRef.current.open = false;
        profileRef.current.querySelector("summary")?.focus();
      }
    };
    const outside = (event) => {
      if (profileRef.current && !profileRef.current.contains(event.target))
        profileRef.current.open = false;
    };
    window.addEventListener("keydown", keyboard);
    window.addEventListener("pointerdown", outside);
    return () => {
      window.removeEventListener("keydown", keyboard);
      window.removeEventListener("pointerdown", outside);
    };
  }, [admin]);
  useEffect(() => {
    if (!mobileOpen) return;
    const drawer = drawerRef.current;
    const opener = document.activeElement;
    drawer.showModal();
    return () => {
      if (drawer.open) drawer.close();
      opener?.focus();
    };
  }, [mobileOpen]);

  const links = (compact = false) => (
    <>
      <nav
        aria-label="Main navigation"
        className="flex-1 space-y-1 overflow-y-auto px-3 py-4"
      >
        {user.role === "PARENT" ? (
          <>
            {!compact && <p className="eyebrow px-3 pb-3">Family workspace</p>}
            {[
              ["Dashboard", "/parent/dashboard", LayoutDashboard],
              ["My Children", "/parent/children", Users],
              ["Attendance", "/parent/attendance", CalendarCheck],
              ["Fees", "/parent/fees", Wallet],
              ["Exam Results", "/parent/results", ClipboardCheck],
              ["Assignments", "/parent/assignments", FileText],
              ["Academic Progress", "/parent/progress", ChartNoAxesCombined],
              ["My Profile", "/parent/profile", ShieldCheck],
            ].map(([label, path, Icon]) => (
              <NavLink
                key={path}
                end
                title={label}
                to={path}
                onClick={() => setMobileOpen(false)}
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <Icon size={18} />
                {!compact && label}
              </NavLink>
            ))}
          </>
        ) : (
          <>
            {!compact && <p className="eyebrow px-3 pb-3">Workspace</p>}
            <NavLink
              to="/dashboard"
              onClick={() => setMobileOpen(false)}
              title="Dashboard"
              className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
            >
              <LayoutDashboard size={18} strokeWidth={1.8} />
              {!compact && "Dashboard"}
            </NavLink>
            {studentAccess ? (
              <NavLink
                to="/students"
                onClick={() => setMobileOpen(false)}
                title="Students"
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <Users size={18} strokeWidth={1.8} />
                {!compact && "Students"}
              </NavLink>
            ) : (
              <button
                disabled
                aria-label="Students"
                title="Student records are not available for your role"
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <Users size={18} />
                {!compact && "Students"}
              </button>
            )}
            {!compact && (
              <div className="px-3 pb-2 pt-6">
                <p className="eyebrow">Institute</p>
              </div>
            )}
            {(admin || user.role === "TEACHER") && (
              <NavLink
                to="/teachers"
                onClick={() => setMobileOpen(false)}
                title="Teachers"
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <UserRoundCheck size={18} />
                {!compact && "Teachers"}
              </NavLink>
            )}
            {["ADMIN", "TEACHER", "STUDENT"].includes(user.role) && (
              <>
                <NavLink
                  title="Courses"
                  to="/courses"
                  onClick={() => setMobileOpen(false)}
                  className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                >
                  <BookOpen size={18} />
                  {!compact && "Courses"}
                </NavLink>
                <NavLink
                  title="Batches"
                  to="/batches"
                  onClick={() => setMobileOpen(false)}
                  className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                >
                  <Layers size={18} />
                  {!compact && "Batches"}
                </NavLink>
              </>
            )}
            {["ADMIN", "TEACHER", "STUDENT"].includes(user.role) && (
              <NavLink
                title="Attendance"
                to="/attendance"
                onClick={() => setMobileOpen(false)}
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <CalendarCheck size={18} />
                {!compact && "Attendance"}
              </NavLink>
            )}
            {["ADMIN", "TEACHER", "STUDENT"].includes(user.role) && (
              <>
                <NavLink
                  title="Exams"
                  to="/exams"
                  onClick={() => setMobileOpen(false)}
                  className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                >
                  <ClipboardCheck size={18} />
                  {!compact && "Exams"}
                </NavLink>
                <NavLink
                  title="Assignments"
                  to="/assignments"
                  onClick={() => setMobileOpen(false)}
                  className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                >
                  <FileText size={18} />
                  {!compact && "Assignments"}
                </NavLink>
                {user.role === "STUDENT" && (
                  <NavLink
                    title="My Assignments"
                    to="/my-assignments"
                    onClick={() => setMobileOpen(false)}
                    className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                  >
                    <FileText size={18} />
                    {!compact && "My Assignments"}
                  </NavLink>
                )}
                {user.role === "STUDENT" && (
                  <NavLink
                    title="My Results"
                    to="/my-results"
                    onClick={() => setMobileOpen(false)}
                    className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                  >
                    <ChartNoAxesCombined size={18} />
                    {!compact && "My Results"}
                  </NavLink>
                )}
              </>
            )}
            {admin && (
              <>
                {!compact && <p className="eyebrow px-3 pb-2 pt-6">Finance</p>}
                {[
                  ["Finance Overview", "/finance", Wallet],
                  ["Fee Structures", "/finance/structures", BookOpen],
                  ["Fee Accounts", "/finance/accounts", Users],
                  ["Payments", "/finance/payments", Wallet],
                  ["Reports", "/finance/reports", ChartNoAxesCombined],
                ].map(([label, path, Icon]) => (
                  <NavLink
                    key={path}
                    to={path}
                    end
                    title={label}
                    onClick={() => setMobileOpen(false)}
                    className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                  >
                    <Icon size={18} />
                    {!compact && label}
                  </NavLink>
                ))}
              </>
            )}
            {user.role === "STUDENT" && (
              <NavLink
                title="My Fees"
                to="/my-fees"
                onClick={() => setMobileOpen(false)}
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <Wallet size={18} />
                {!compact && "My Fees"}
              </NavLink>
            )}
            {["ADMIN", "TEACHER", "STUDENT"].includes(user.role) &&
              [
                ...(user.role === "STUDENT"
                  ? [["My Resources", "/my-resources"]]
                  : [["Study Materials", "/resources"]]),
                ...(user.role === "ADMIN"
                  ? [
                      ["Resource Categories", "/resources/categories"],
                      ["Resource Folders", "/resources/folders"],
                    ]
                  : []),
                ...(user.role === "TEACHER"
                  ? [
                      ["Upload Material", "/resources/new"],
                      ["My Resources", "/my-resources"],
                      ["Resource Folders", "/resources/folders"],
                    ]
                  : []),
              ].map(([label, path]) => (
                <NavLink
                  key={path}
                  end
                  to={path}
                  title={label}
                  onClick={() => setMobileOpen(false)}
                  className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
                >
                  <Library size={18} />
                  {!compact && label}
                </NavLink>
              ))}
            {admin && (
              <NavLink
                title="Parent Management"
                to="/admin/parents"
                onClick={() => setMobileOpen(false)}
                className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              >
                <Users size={18} />
                {!compact && "Parent Management"}
              </NavLink>
            )}
          </>
        )}
        {["ADMIN", "TEACHER"].includes(user.role) &&
          [
            ["Analytics", "/analytics"],
            ...(admin ? [["Financial Analytics", "/analytics/finance"]] : []),
            ["Attendance Analytics", "/analytics/attendance"],
            ["Academic Analytics", "/analytics/academics"],
            ["Batch Analytics", "/analytics/batches"],
            ["Teacher Analytics", "/analytics/teachers"],
            ["Reports Center", "/reports"],
          ].map(([title, to]) => (
            <NavLink
              key={to}
              to={to}
              title={title}
              onClick={() => setMobileOpen(false)}
              className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
            >
              <ChartNoAxesCombined size={18} />
              {!compact && title}
            </NavLink>
          ))}
        {user.role === "STUDENT" && (
          <NavLink
            to="/my-progress"
            title="My Progress"
            className="sidebar-link"
          >
            <ChartNoAxesCombined size={18} />
            {!compact && "My Progress"}
          </NavLink>
        )}
        {admin &&
          [
            ["Control Center", "/admin/control-center"],
            ["Institute Settings", "/admin/institute-settings"],
            ["User Management", "/admin/users"],
            ["Audit Logs", "/admin/audit-logs"],
          ].map(([title, to]) => (
            <NavLink
              key={to}
              to={to}
              title={title}
              className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
              onClick={() => setMobileOpen(false)}
            >
              <ShieldCheck size={18} />
              {!compact && title}
            </NavLink>
          ))}
        {[
          ["My Profile", "/settings/profile"],
          ["Security Settings", "/settings/security"],
        ].map(([title, to]) => (
          <NavLink
            key={to}
            to={to}
            title={title}
            className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
            onClick={() => setMobileOpen(false)}
          >
            <Settings size={18} />
            {!compact && title}
          </NavLink>
        ))}
        <NavLink
          to="/announcements"
          title="Announcements"
          onClick={() => setMobileOpen(false)}
          className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
        >
          <Bell size={18} />
          {!compact && "Announcements"}
        </NavLink>
        <NavLink
          to="/notifications"
          title="Notifications"
          onClick={() => setMobileOpen(false)}
          className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
        >
          <Bell size={18} />
          {!compact && "Notifications"}
        </NavLink>
      </nav>
      <div className="space-y-1 border-t border-slate-100 px-3 py-3">
        <NavLink
          to="/settings/notifications"
          aria-label="Notification Preferences"
          title="Notification Preferences"
          className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
        >
          <Settings size={18} />
          {!compact && "Notification Preferences"}
        </NavLink>
        <button
          onClick={safeLogout}
          aria-label="Logout"
          title="Logout"
          className={`sidebar-link ${compact ? "justify-center !px-0" : ""}`}
        >
          <LogOut size={18} />
          {!compact && "Logout"}
        </button>
      </div>
      {!compact && (
        <div className="mx-4 mb-4 flex items-center gap-2 rounded-lg border border-slate-100 bg-slate-50 px-3 py-2.5 text-[11px] text-slate-500">
          <ShieldCheck size={14} className="text-indigo-500" />
          <span className="capitalize">
            {user.role.toLowerCase()} workspace
          </span>
        </div>
      )}
    </>
  );

  return (
    <div
      className="min-h-screen bg-slate-50"
      style={{ "--sidebar-width": collapsed ? "76px" : "236px" }}
    >
      <a
        href="#main-content"
        className="sr-only fixed left-4 top-4 z-[100] rounded-lg bg-primary p-3 text-sm text-white focus:not-sr-only"
      >
        Skip to content
      </a>
      <aside
        aria-label="Workspace sidebar"
        className="fixed inset-y-0 left-0 z-30 hidden w-[var(--sidebar-width)] flex-col border-r border-slate-200/80 bg-surface transition-[width] duration-150 lg:flex"
      >
        <div
          className={`flex h-[76px] shrink-0 items-center ${collapsed ? "justify-center" : "px-6"}`}
        >
          <Logo compact={collapsed} />
        </div>
        {!collapsed && (
          <div className="mx-4 mb-1 flex items-center gap-2.5 rounded-lg border border-slate-200/80 px-3 py-2.5">
            <span className="flex h-7 w-7 items-center justify-center rounded-md bg-slate-100 text-slate-500">
              <Building2 size={15} />
            </span>
            <div>
              <p className="text-xs font-medium text-slate-700">
                Institute workspace
              </p>
              <p className="mt-0.5 text-[10px] text-slate-400">
                Education, organized.
              </p>
            </div>
          </div>
        )}
        {links(collapsed)}
        <div className="border-t border-slate-100 p-3">
          <Button
            variant="ghost"
            className="w-full"
            aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            aria-expanded={!collapsed}
            onClick={() => setCollapsed(!collapsed)}
          >
            {collapsed ? (
              <PanelLeftOpen size={17} />
            ) : (
              <>
                <PanelLeftClose size={17} />
                <span>Collapse sidebar</span>
              </>
            )}
          </Button>
        </div>
      </aside>
      {mobileOpen && (
        <dialog
          ref={drawerRef}
          onCancel={(event) => {
            event.preventDefault();
            setMobileOpen(false);
          }}
          aria-label="Navigation"
          className="fixed inset-y-0 left-0 m-0 h-dvh max-h-none w-[min(288px,90vw)] max-w-none border-0 bg-surface p-0 text-slate-900 shadow-xl backdrop:bg-slate-900/35"
        >
          <div className="flex h-full flex-col">
            <div className="flex h-[76px] shrink-0 items-center justify-between border-b border-slate-100 px-5">
              <Logo />
              <Button
                variant="icon"
                aria-label="Close navigation"
                onClick={() => setMobileOpen(false)}
              >
                <X size={18} />
              </Button>
            </div>
            {links()}
          </div>
        </dialog>
      )}
      <div className="min-w-0 transition-[padding] duration-150 lg:pl-[var(--sidebar-width)]">
        <header className="sticky top-0 z-20 flex min-h-[76px] items-center justify-between gap-3 border-b border-slate-200/80 bg-surface px-4 sm:px-7 lg:px-8">
          <div className="flex min-w-0 items-center gap-3">
            <Button
              variant="icon"
              className="lg:hidden"
              aria-label="Open navigation"
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen(true)}
            >
              <Menu size={20} />
            </Button>
            <nav
              aria-label="Breadcrumb"
              className="flex min-w-0 items-center gap-2 text-xs"
            >
              <Link
                to="/dashboard"
                className="hidden text-slate-400 hover:text-indigo-600 sm:block"
              >
                Workspace
              </Link>
              <ChevronRight
                size={13}
                className="hidden text-slate-300 sm:block"
              />
              {detail && (
                <>
                  <Link
                    to="/students"
                    className="text-slate-500 hover:text-indigo-600"
                  >
                    Students
                  </Link>
                  <ChevronRight size={13} className="text-slate-300" />
                </>
              )}
              <span
                aria-current="page"
                className="truncate font-medium text-slate-700"
              >
                {current}
              </span>
            </nav>
          </div>
          <div className="flex shrink-0 items-center gap-2 sm:gap-4">
            <form
              onSubmit={(event) => {
                event.preventDefault();
                if (admin && search.trim()) {
                  if (
                    !window.dispatchEvent(
                      new Event("eduflow:before-navigation", {
                        cancelable: true,
                      }),
                    )
                  )
                    return;
                  navigate(
                    `/students?search=${encodeURIComponent(search.trim())}`,
                  );
                  setSearch("");
                  searchRef.current?.blur();
                }
              }}
              className="relative hidden md:block"
            >
              <Search
                size={15}
                className="pointer-events-none absolute left-3 top-3 text-slate-400"
              />
              <Input
                ref={searchRef}
                aria-label="Search workspace"
                disabled={!admin}
                title={
                  admin
                    ? "Search students. Ctrl or Command + K"
                    : "Search is available to administrators"
                }
                className="!w-56 !bg-slate-50 !py-2.5 pl-9 pr-14 !text-xs"
                placeholder="Search students…"
                maxLength={100}
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              <kbd
                aria-hidden="true"
                className="pointer-events-none absolute right-2.5 top-2.5 rounded border border-slate-200 bg-surface px-1 text-[10px] leading-5 text-slate-400"
              >
                ⌘ K
              </kbd>
            </form>
            <ThemeToggle />
            <NotificationBell />
            <span className="h-7 w-px bg-slate-200" />
            <details ref={profileRef} className="relative">
              <summary
                aria-label="Open profile menu"
                className="flex list-none items-center gap-2 rounded-lg p-1 [&::-webkit-details-marker]:hidden"
              >
                <Avatar name={user.fullName} size="sm" />
                <ChevronDown size={13} className="text-slate-400" />
              </summary>
              <div className="absolute right-0 top-12 w-64 rounded-xl border border-slate-200 bg-surface p-2 shadow-lg">
                <div className="border-b border-slate-100 px-3 pb-3 pt-2">
                  <p className="truncate text-sm font-semibold">
                    {user.fullName}
                  </p>
                  <p className="mt-1 truncate text-xs text-slate-500">
                    {user.email}
                  </p>
                  <Badge className="mt-2" tone="indigo">
                    {user.role}
                  </Badge>
                </div>
                {user.role === "PARENT" && (
                  <Link className="sidebar-link mt-1" to="/parent/profile">
                    My parent profile
                  </Link>
                )}
                <Link className="sidebar-link mt-1" to="/settings/profile">
                  Account profile
                </Link>
                <Link className="sidebar-link mt-1" to="/settings/security">
                  Security settings
                </Link>
                {user.role === "STUDENT" && (
                  <Link className="sidebar-link mt-1" to="/students/me">
                    My student profile
                  </Link>
                )}
                {user.role === "TEACHER" && (
                  <Link className="sidebar-link mt-1" to="/teachers/me">
                    My teaching profile
                  </Link>
                )}
                <Button
                  variant="ghost"
                  className="mt-1 w-full !justify-start"
                  onClick={safeLogout}
                >
                  <LogOut size={15} />
                  Sign out
                </Button>
              </div>
            </details>
          </div>
        </header>
        <main
          id="main-content"
          tabIndex={-1}
          className="mx-auto max-w-[1600px] px-4 py-7 focus:outline-none sm:px-7 sm:py-8 lg:px-8"
        >
          <Outlet />
        </main>
        <footer className="flex flex-wrap items-center justify-between gap-2 px-4 pb-6 text-[10px] text-slate-400 sm:px-7 lg:px-8">
          <span>EduFlow · Institute management</span>
          <span>Built for better learning.</span>
        </footer>
      </div>
    </div>
  );
}
