import {
  BrowserRouter,
  Navigate,
  Route,
  Routes,
} from "react-router-dom";
import { lazy, Suspense } from "react";
import { AuthProvider } from "./auth/AuthContext";
import RequireAuth from "./auth/RequireAuth";
import Layout from "./components/Layout";
import { Loading } from "./components/UI";
import LoginPage from "./pages/LoginPage";
const StudentsPage = lazy(() => import("./pages/StudentsPage"));
const StudentDetailsPage = lazy(() => import("./pages/StudentDetailsPage"));
const TeachersPage = lazy(() => import("./pages/TeachersPage"));
const TeacherFormPage = lazy(() => import("./pages/TeacherFormPage"));
const TeacherDetailsPage = lazy(() => import("./pages/TeacherDetailsPage"));
const AcademicCatalogPage = lazy(() => import("./pages/AcademicCatalogPage"));
const CourseDetailsPage = lazy(() => import("./pages/CourseDetailsPage"));
const BatchDetailsPage = lazy(() => import("./pages/BatchDetailsPage"));
const MyEnrollmentsPage = lazy(() => import("./pages/MyEnrollmentsPage"));
const AttendancePage = lazy(() => import("./pages/AttendancePage"));
const AttendanceSessionPage = lazy(
  () => import("./pages/AttendanceSessionPage"),
);
const AttendanceReportsPage = lazy(
  () => import("./pages/AttendanceReportsPage"),
);
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const AnalyticsPage = lazy(() => import("./pages/AnalyticsPage"));
const AdministrationPage = lazy(() => import("./pages/AdministrationPage"));
const AccountSettingsPage = lazy(() =>
  import("./pages/AdministrationPage").then((m) => ({
    default: m.AccountSettingsPage,
  })),
);
const AccountRecoveryPage = lazy(() => import("./pages/AccountRecoveryPage"));
const AccountRegistrationPage = lazy(
  () => import("./pages/AccountRegistrationPage"),
);
const FinancePage = lazy(() => import("./pages/FinancePage"));
const FeeStructuresPage = lazy(() => import("./pages/FeeStructuresPage"));
const FeeStructureFormPage = lazy(() => import("./pages/FeeStructureFormPage"));
const FeeAccountsPage = lazy(() => import("./pages/FeeAccountsPage"));
const FeeAccountDetailsPage = lazy(
  () => import("./pages/FeeAccountDetailsPage"),
);
const FinancePaymentsPage = lazy(() => import("./pages/FinancePaymentsPage"));
const MyFeesPage = lazy(() => import("./pages/MyFeesPage"));
const ExamsPage = lazy(() => import("./pages/ExamsPage"));
const ExamFormPage = lazy(() => import("./pages/ExamFormPage"));
const ExamDetailsPage = lazy(() => import("./pages/ExamDetailsPage"));
const ExamTakePage = lazy(() => import("./pages/ExamTakePage"));
const ExamGradebookPage = lazy(() => import("./pages/ExamGradebookPage"));
const ExamResultsPage = lazy(() => import("./pages/ExamResultsPage"));
const MyResultsPage = lazy(() => import("./pages/MyResultsPage"));
const ExamAnalyticsPage = lazy(() => import("./pages/ExamAnalyticsPage"));
const AssignmentsPage = lazy(() => import("./pages/AssignmentsPage"));
const AssignmentFormPage = lazy(() => import("./pages/AssignmentFormPage"));
const AssignmentDetailsPage = lazy(
  () => import("./pages/AssignmentDetailsPage"),
);
const AssignmentSubmitPage = lazy(() => import("./pages/AssignmentSubmitPage"));
const AssignmentReviewPage = lazy(() => import("./pages/AssignmentReviewPage"));
const SubmissionDetailsPage = lazy(
  () => import("./pages/SubmissionDetailsPage"),
);
const MyAssignmentsPage = lazy(() => import("./pages/MyAssignmentsPage"));
const ResourcesPage = lazy(() => import("./pages/ResourcesPage"));
const ResourceFormPage = lazy(() => import("./pages/ResourceFormPage"));
const ResourceDetailsPage = lazy(() => import("./pages/ResourceDetailsPage"));
const ResourceCategoriesPage = lazy(
  () => import("./pages/ResourceCategoriesPage"),
);
const ResourceFoldersPage = lazy(() => import("./pages/ResourceFoldersPage"));
const ParentPortalPage = lazy(() => import("./pages/ParentPortalPage"));
const ParentChildrenPage = lazy(() => import("./pages/ParentChildrenPage"));
const ParentProfilePage = lazy(() => import("./pages/ParentProfilePage"));
const AdminParentsPage = lazy(() => import("./pages/AdminParentsPage"));
const AdminParentDetailsPage = lazy(
  () => import("./pages/AdminParentDetailsPage"),
);
const NotificationsPage = lazy(() => import("./pages/NotificationsPage"));
const NotificationPreferencesPage = lazy(
  () => import("./pages/NotificationPreferencesPage"),
);
const AnnouncementsPage = lazy(() => import("./pages/AnnouncementsPage"));
const AnnouncementFormPage = lazy(() => import("./pages/AnnouncementFormPage"));
const AnnouncementDetailsPage = lazy(
  () => import("./pages/AnnouncementDetailsPage"),
);

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/recover-account"
            element={
              <Suspense fallback={<Loading />}>
                <AccountRecoveryPage />
              </Suspense>
            }
          />
          <Route
            path="/register"
            element={
              <Suspense fallback={<Loading />}>
                <AccountRegistrationPage />
              </Suspense>
            }
          />
          <Route element={<RequireAuth />}>
            <Route element={<Layout />}>
              <Route
                path="/notifications"
                element={
                  <Suspense fallback={<Loading />}>
                    <NotificationsPage />
                  </Suspense>
                }
              />
              <Route
                path="/settings/notifications"
                element={
                  <Suspense fallback={<Loading />}>
                    <NotificationPreferencesPage />
                  </Suspense>
                }
              />
              <Route
                path="/announcements"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnnouncementsPage />
                  </Suspense>
                }
              />
              <Route
                path="/announcements/new"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnnouncementFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/announcements/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnnouncementDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/announcements/:id/edit"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnnouncementFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/parent/dashboard"
                element={
                  <Suspense fallback={<Loading />}>
                    <ParentPortalPage />
                  </Suspense>
                }
              />
              <Route
                path="/parent/children"
                element={
                  <Suspense fallback={<Loading />}>
                    <ParentChildrenPage />
                  </Suspense>
                }
              />
              <Route
                path="/parent/children/:studentId"
                element={
                  <Suspense fallback={<Loading />}>
                    <ParentPortalPage mode="overview" />
                  </Suspense>
                }
              />
              {[
                "attendance",
                "fees",
                "results",
                "assignments",
                "progress",
                "resources",
              ].map((mode) => (
                <Route
                  key={"parent-" + mode}
                  path={"/parent/" + mode}
                  element={
                    <Suspense fallback={<Loading />}>
                      <ParentPortalPage key={mode} mode={mode} />
                    </Suspense>
                  }
                />
              ))}
              {[
                "attendance",
                "fees",
                "results",
                "assignments",
                "progress",
                "resources",
              ].map((mode) => (
                <Route
                  key={"child-" + mode}
                  path={"/parent/children/:studentId/" + mode}
                  element={
                    <Suspense fallback={<Loading />}>
                      <ParentPortalPage key={mode} mode={mode} />
                    </Suspense>
                  }
                />
              ))}
              <Route
                path="/parent/profile"
                element={
                  <Suspense fallback={<Loading />}>
                    <ParentProfilePage />
                  </Suspense>
                }
              />
              <Route
                path="/admin/parents"
                element={
                  <Suspense fallback={<Loading />}>
                    <AdminParentsPage />
                  </Suspense>
                }
              />
              <Route
                path="/admin/parents/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <AdminParentDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/resources"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourcesPage />
                  </Suspense>
                }
              />
              <Route
                path="/my-resources"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourcesPage own />
                  </Suspense>
                }
              />
              <Route
                path="/resources/new"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourceFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/resources/materials/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourceDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/resources/materials/:id/edit"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourceFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/resources/categories"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourceCategoriesPage />
                  </Suspense>
                }
              />
              <Route
                path="/resources/folders"
                element={
                  <Suspense fallback={<Loading />}>
                    <ResourceFoldersPage />
                  </Suspense>
                }
              />
              <Route
                path="/dashboard"
                element={
                  <Suspense fallback={<Loading label="Loading dashboard…" />}>
                    <DashboardPage />
                  </Suspense>
                }
              />
              <Route path="/students" element={<StudentsPage />} />
              <Route
                path="/assignments"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentsPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/new"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/submissions/:submissionId"
                element={
                  <Suspense fallback={<Loading />}>
                    <SubmissionDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/:id/edit"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/:id/submit"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentSubmitPage />
                  </Suspense>
                }
              />
              <Route
                path="/assignments/:id/submissions"
                element={
                  <Suspense fallback={<Loading />}>
                    <AssignmentReviewPage />
                  </Suspense>
                }
              />
              <Route
                path="/my-assignments"
                element={
                  <Suspense fallback={<Loading />}>
                    <MyAssignmentsPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamsPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/new"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/analytics"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamAnalyticsPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id/edit"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id/take"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamTakePage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id/gradebook"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamGradebookPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id/results"
                element={
                  <Suspense fallback={<Loading />}>
                    <ExamResultsPage />
                  </Suspense>
                }
              />
              <Route
                path="/exams/:id/result"
                element={
                  <Suspense fallback={<Loading />}>
                    <MyResultsPage detail />
                  </Suspense>
                }
              />
              <Route
                path="/my-results"
                element={
                  <Suspense fallback={<Loading />}>
                    <MyResultsPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance"
                element={
                  <Suspense fallback={<Loading />}>
                    <FinancePage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/reports"
                element={
                  <Suspense fallback={<Loading />}>
                    <FinancePage reports />
                  </Suspense>
                }
              />
              <Route
                path="/finance/structures"
                element={
                  <Suspense fallback={<Loading />}>
                    <FeeStructuresPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/structures/new"
                element={
                  <Suspense fallback={<Loading />}>
                    <FeeStructureFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/structures/:id/edit"
                element={
                  <Suspense fallback={<Loading />}>
                    <FeeStructureFormPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/accounts"
                element={
                  <Suspense fallback={<Loading />}>
                    <FeeAccountsPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/accounts/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <FeeAccountDetailsPage />
                  </Suspense>
                }
              />
              <Route
                path="/finance/payments"
                element={
                  <Suspense fallback={<Loading />}>
                    <FinancePaymentsPage />
                  </Suspense>
                }
              />
              <Route
                path="/my-fees"
                element={
                  <Suspense fallback={<Loading />}>
                    <MyFeesPage />
                  </Suspense>
                }
              />
              <Route
                path="/attendance"
                element={
                  <Suspense fallback={<Loading />}>
                    <AttendancePage />
                  </Suspense>
                }
              />
              <Route
                path="/attendance/sessions/:id"
                element={
                  <Suspense fallback={<Loading />}>
                    <AttendanceSessionPage />
                  </Suspense>
                }
              />
              <Route
                path="/attendance/reports"
                element={
                  <Suspense fallback={<Loading />}>
                    <AttendanceReportsPage />
                  </Suspense>
                }
              />
              <Route
                path="/courses"
                element={<AcademicCatalogPage key="courses" kind="courses" />}
              />
              <Route path="/courses/:id" element={<CourseDetailsPage />} />
              <Route
                path="/batches"
                element={<AcademicCatalogPage key="batches" kind="batches" />}
              />
              <Route
                path="/batches/my-enrollments"
                element={<MyEnrollmentsPage />}
              />
              <Route path="/batches/:id" element={<BatchDetailsPage />} />
              <Route path="/teachers" element={<TeachersPage />} />
              <Route path="/teachers/new" element={<TeacherFormPage />} />
              <Route path="/teachers/me" element={<TeacherDetailsPage own />} />
              <Route path="/teachers/:id" element={<TeacherDetailsPage />} />
              <Route path="/teachers/:id/edit" element={<TeacherFormPage />} />
              <Route path="/students/me" element={<StudentDetailsPage own />} />
              <Route path="/students/:id" element={<StudentDetailsPage />} />
              {[
                "control-center",
                "institute-settings",
                "users",
                "audit-logs",
              ].map((kind) => (
                <Route
                  key={kind}
                  path={"/admin/" + kind}
                  element={
                    <Suspense fallback={<Loading />}>
                      <AdministrationPage key={kind} kind={kind} />
                    </Suspense>
                  }
                />
              ))}
              <Route
                path="/settings/profile"
                element={
                  <Suspense fallback={<Loading />}>
                    <AccountSettingsPage key="profile" />
                  </Suspense>
                }
              />
              <Route
                path="/settings/security"
                element={
                  <Suspense fallback={<Loading />}>
                    <AccountSettingsPage key="security" security />
                  </Suspense>
                }
              />
              {[
                "overview",
                "finance",
                "attendance",
                "academics",
                "batches",
                "teachers",
              ].map((kind) => (
                <Route
                  key={kind}
                  path={"/analytics" + (kind === "overview" ? "" : "/" + kind)}
                  element={
                    <Suspense fallback={<Loading />}>
                      <AnalyticsPage key={kind} kind={kind} />
                    </Suspense>
                  }
                />
              ))}
              {["batches", "teachers"].map((kind) => (
                <Route
                  key={kind}
                  path={"/analytics/" + kind + "/:id"}
                  element={
                    <Suspense fallback={<Loading />}>
                      <AnalyticsPage key={kind + "-detail"} kind={kind} />
                    </Suspense>
                  }
                />
              ))}
              <Route
                path="/reports"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnalyticsPage kind="reports" />
                  </Suspense>
                }
              />
              <Route
                path="/my-progress"
                element={
                  <Suspense fallback={<Loading />}>
                    <AnalyticsPage kind="progress" />
                  </Suspense>
                }
              />
            </Route>
          </Route>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
