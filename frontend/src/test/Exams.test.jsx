import { beforeEach, afterEach, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import ExamsPage from "../pages/ExamsPage";
import ExamFormPage from "../pages/ExamFormPage";
import ExamGradebookPage from "../pages/ExamGradebookPage";
import ExamAnalyticsPage from "../pages/ExamAnalyticsPage";
import MyResultsPage from "../pages/MyResultsPage";
import QuestionBuilder from "../components/QuestionBuilder";
import ExamDashboard from "../components/ExamDashboard";
import { AttemptRunner } from "../pages/ExamTakePage";
const { service, question, attempt, result, auth } = vi.hoisted(() => ({
  service: {
    getExams: vi.fn(),
    getExam: vi.fn(),
    saveExam: vi.fn(),
    getExamAnalytics: vi.fn(),
  },
  question: {
    getQuestions: vi.fn(),
    saveQuestion: vi.fn(),
    removeQuestion: vi.fn(),
    reorderQuestions: vi.fn(),
  },
  attempt: {
    saveAnswers: vi.fn(),
    submitAttempt: vi.fn(),
    getAttempt: vi.fn(),
    startAttempt: vi.fn(),
  },
  result: {
    getGradebook: vi.fn(),
    saveGradebook: vi.fn(),
    publishResults: vi.fn(),
    getMyResults: vi.fn(),
    getOwnResult: vi.fn(),
  },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/examService", () => service);
vi.mock("../services/questionService", () => question);
vi.mock("../services/examAttemptService", () => attempt);
vi.mock("../services/resultService", () => result);
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange }) => (
    <label>
      {label}
      <input
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
vi.mock("../components/ExamTrend", () => ({
  default: () => <p>Performance trend</p>,
}));
vi.mock("../components/ExamDistribution", () => ({
  default: () => <p>Distribution</p>,
}));
const exam = {
  id: 1,
  examCode: "EX-1",
  title: "Mathematics Test",
  batchId: 2,
  batchName: "Morning",
  courseName: "Math",
  examType: "ONLINE_MCQ",
  durationMinutes: 10,
  totalMarks: 10,
  passingMarks: 5,
  startDateTime: "2026-11-09T09:00:00Z",
  endDateTime: "2026-11-09T10:00:00Z",
  status: "DRAFT",
  version: 0,
};
const page = {
  content: [exam],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};
const studentQuestion = {
  id: 10,
  questionText: "What is two plus two?",
  marks: 10,
  options: [
    { id: 20, optionText: "Four" },
    { id: 21, optionText: "Five" },
  ],
};
const active = {
  id: 5,
  examId: 1,
  title: exam.title,
  status: "IN_PROGRESS",
  version: 0,
  serverNow: "2026-11-09T09:00:00Z",
  expiresAt: "2026-11-09T09:10:00Z",
  questions: [studentQuestion],
  answers: [],
};
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { role: "ADMIN" };
  service.getExams.mockResolvedValue({ data: page });
  question.getQuestions.mockResolvedValue({ data: [] });
  service.getExam.mockResolvedValue({ data: exam });
});
afterEach(() => vi.useRealTimers());
const mount = (element) => render(<MemoryRouter>{element}</MemoryRouter>);
it("loads the exam directory and sends search filters to the backend", async () => {
  mount(<ExamsPage />);
  expect(await screen.findByText(exam.title)).toBeInTheDocument();
  await userEvent.type(
    screen.getByPlaceholderText("Exam title or code…"),
    "Math",
  );
  await waitFor(() =>
    expect(service.getExams).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "Math", page: 0 }),
      expect.any(AbortSignal),
    ),
  );
});
it("students have no exam create control and parents do not request exams", async () => {
  auth.user = { role: "STUDENT" };
  const view = mount(<ExamsPage />);
  await screen.findByText(exam.title);
  expect(screen.queryByText("Create Exam")).not.toBeInTheDocument();
  view.unmount();
  service.getExams.mockClear();
  auth.user = { role: "PARENT" };
  mount(<ExamsPage />);
  expect(screen.getByText("Exam access restricted")).toBeInTheDocument();
  expect(service.getExams).not.toHaveBeenCalled();
});
it("creates an exam with IST converted to UTC", async () => {
  service.saveExam.mockResolvedValue({ data: exam });
  render(
    <MemoryRouter initialEntries={["/exams/new"]}>
      <Routes>
        <Route path="/exams/new" element={<ExamFormPage />} />
        <Route path="/exams/1" element={<p>Saved exam</p>} />
      </Routes>
    </MemoryRouter>,
  );
  for (const [label, value] of [
    ["Exam Code *", "MATH-1"],
    ["Exam Title *", "Math"],
    ["Batch", "2"],
    ["Start date & time (IST) *", "2026-11-09T14:30"],
    ["End date & time (IST) *", "2026-11-09T15:30"],
    ["Total marks *", "10"],
    ["Passing marks *", "5"],
  ])
    fireEvent.change(screen.getByLabelText(label), { target: { value } });
  await userEvent.click(screen.getByText("Save draft exam"));
  expect(await screen.findByText("Saved exam")).toBeInTheDocument();
  expect(service.saveExam).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({
      startDateTime: "2026-11-09T09:00:00.000Z",
      batchId: 2,
    }),
  );
});
it("question builder saves one selected correct option and real text", async () => {
  question.saveQuestion.mockResolvedValue({});
  const done = vi.fn();
  mount(<QuestionBuilder exam={exam} onChange={done} />);
  await userEvent.click(screen.getByText("Add question"));
  await userEvent.type(
    screen.getByLabelText("Question text *"),
    "Choose an answer",
  );
  fireEvent.change(screen.getByLabelText("Question marks *"), {
    target: { value: "10" },
  });
  await userEvent.type(screen.getByLabelText("Option 1 text"), "One");
  await userEvent.type(screen.getByLabelText("Option 2 text"), "Two");
  await userEvent.click(screen.getByLabelText("Option 2 is correct"));
  await userEvent.click(screen.getByText("Save question"));
  await waitFor(() => expect(done).toHaveBeenCalled());
  const input = question.saveQuestion.mock.calls[0][2];
  expect(input.options.filter((x) => x.correct)).toEqual([
    { optionText: "Two", correct: true },
  ]);
});
it("loads saved selections and persists each answer using the server version", async () => {
  auth.user = { role: "STUDENT" };
  attempt.saveAnswers.mockResolvedValue({
    data: {
      ...active,
      version: 1,
      answers: [{ questionId: 10, selectedOptionId: 21 }],
    },
  });
  mount(
    <AttemptRunner
      initial={{
        ...active,
        answers: [{ questionId: 10, selectedOptionId: 20 }],
      }}
    />,
  );
  expect(screen.getByLabelText("Four")).toBeChecked();
  await userEvent.click(screen.getByLabelText("Five"));
  await waitFor(() =>
    expect(attempt.saveAnswers).toHaveBeenCalledWith(5, {
      version: 0,
      answers: [{ questionId: 10, selectedOptionId: 21 }],
    }),
  );
  expect(await screen.findByText(/All answers saved/)).toBeInTheDocument();
});
it("shows a save error with retry rather than claiming answers were saved", async () => {
  attempt.saveAnswers.mockRejectedValue({
    response: { status: 409, data: { message: "Attempt changed", errors: {} } },
  });
  mount(<AttemptRunner initial={active} />);
  await userEvent.click(screen.getByLabelText("Four"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Attempt changed");
  expect(screen.getByText("Retry save")).toBeInTheDocument();
});
it("submits only after confirmation and hides unpublished scores", async () => {
  attempt.submitAttempt.mockResolvedValue({
    data: { ...active, status: "SUBMITTED" },
  });
  mount(<AttemptRunner initial={active} />);
  await userEvent.click(screen.getByText("Submit Exam"));
  expect(attempt.submitAttempt).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole("button", { name: "Submit exam" }));
  expect(
    await screen.findByText("Your examination has been submitted."),
  ).toBeInTheDocument();
  expect(screen.queryByText("Grade")).not.toBeInTheDocument();
});
it("requests server finalization at the deadline", async () => {
  vi.useFakeTimers();
  attempt.submitAttempt.mockResolvedValue({
    data: { ...active, status: "AUTO_SUBMITTED" },
  });
  mount(
    <AttemptRunner
      initial={{ ...active, expiresAt: "2026-11-09T09:00:01Z" }}
    />,
  );
  await act(async () => {
    await vi.advanceTimersByTimeAsync(1500);
  });
  expect(attempt.submitAttempt).toHaveBeenCalledWith(5);
  expect(
    screen.getByText("Your examination has been submitted."),
  ).toBeInTheDocument();
});
it("offline gradebook bulk save sends validated draft marks", async () => {
  result.getGradebook.mockResolvedValue({
    data: {
      exam: { ...exam, examType: "OFFLINE", status: "PUBLISHED" },
      students: [
        {
          studentId: 3,
          studentName: "Asha",
          admissionNumber: "EDU-3",
          obtainedMarks: null,
          remarks: "",
        },
      ],
    },
  });
  result.saveGradebook.mockResolvedValue({});
  render(
    <MemoryRouter initialEntries={["/exams/1/gradebook"]}>
      <Routes>
        <Route path="/exams/:id/gradebook" element={<ExamGradebookPage />} />
      </Routes>
    </MemoryRouter>,
  );
  fireEvent.change(await screen.findByLabelText("Marks for Asha"), {
    target: { value: "8" },
  });
  await userEvent.click(screen.getByText("Bulk Save Draft"));
  await waitFor(() =>
    expect(result.saveGradebook).toHaveBeenCalledWith(1, {
      version: 0,
      results: [{ studentId: 3, obtainedMarks: "8", remarks: "" }],
    }),
  );
});
it("result publication is an explicit server operation", async () => {
  result.getGradebook.mockResolvedValue({
    data: {
      exam: { ...exam, examType: "OFFLINE", status: "PUBLISHED" },
      students: [],
    },
  });
  result.publishResults.mockResolvedValue({});
  render(
    <MemoryRouter initialEntries={["/exams/1/gradebook"]}>
      <Routes>
        <Route path="/exams/:id/gradebook" element={<ExamGradebookPage />} />
      </Routes>
    </MemoryRouter>,
  );
  await userEvent.click(await screen.findByText("Publish Results"));
  expect(result.publishResults).not.toHaveBeenCalled();
  await userEvent.click(
    screen.getByRole("button", { name: "Publish results" }),
  );
  await waitFor(() => expect(result.publishResults).toHaveBeenCalledWith(1, 0));
});
it("student pending result screen does not show marks or answer key", async () => {
  auth.user = { role: "STUDENT" };
  result.getOwnResult.mockResolvedValue({
    data: { published: false, result: null, review: [] },
  });
  render(
    <MemoryRouter initialEntries={["/exams/1/result"]}>
      <Routes>
        <Route path="/exams/:id/result" element={<MyResultsPage detail />} />
      </Routes>
    </MemoryRouter>,
  );
  expect(
    await screen.findByText("Results pending publication"),
  ).toBeInTheDocument();
  expect(screen.queryByText("Correct answer")).not.toBeInTheDocument();
});
it("analytics displays server-calculated publication metrics", async () => {
  service.getExamAnalytics.mockResolvedValue({
    data: {
      totalExams: 7,
      publishedExams: 3,
      completedExams: 2,
      upcomingExams: 1,
      awaitingResults: 2,
      scores: { average: 75, passRate: 80, count: 10 },
      batches: [],
      courses: [],
      distribution: [],
      trend: [],
    },
  });
  mount(<ExamAnalyticsPage />);
  expect(await screen.findByText("75.00%")).toBeInTheDocument();
  expect(await screen.findByText("80.00%")).toBeInTheDocument();
});
it("student dashboard shows own recent published scores and performance trend", async () => {
  auth.user = { role: "STUDENT" };
  service.getExamAnalytics.mockResolvedValue({
    data: {
      totalExams: 3,
      upcomingExams: 1,
      completedExams: 1,
      scores: { average: 80, passRate: 100 },
      upcoming: [],
      needsGrading: [],
      trend: [
        {
          id: 7,
          examId: 1,
          examTitle: "My published test",
          percentage: 80,
          grade: "A",
        },
      ],
    },
  });
  mount(<ExamDashboard />);
  const link = await screen.findByRole("link", { name: "My published test" });
  expect(link).toHaveAttribute("href", "/exams/1/result");
  expect(await screen.findByText("Performance trend")).toBeInTheDocument();
});
