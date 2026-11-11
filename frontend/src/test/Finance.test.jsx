import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import {
  RecordPaymentForm,
  InstallmentScheduleForm,
  VoidPaymentDialog,
} from "../components/FeeAccountActions";
import { ReceiptButton, cents, decimal } from "../components/FinanceUI";
import FeeAccountsPage from "../pages/FeeAccountsPage";
import FeeAccountDetailsPage from "../pages/FeeAccountDetailsPage";
import MyFeesPage from "../pages/MyFeesPage";
import FinancePage from "../pages/FinancePage";
import { localDate } from "../components/AttendanceUI";
const { service, auth } = vi.hoisted(() => ({
  service: {
    getFeeAccounts: vi.fn(),
    getFeeAccount: vi.fn(),
    getMyFees: vi.fn(),
    createInstallments: vi.fn(),
    recordPayment: vi.fn(),
    voidPayment: vi.fn(),
    downloadReceipt: vi.fn(),
    saveDownload: vi.fn(),
    getFinanceDashboard: vi.fn(),
    getFinanceReport: vi.fn(),
    exportFinanceReport: vi.fn(),
    financeError: vi.fn(async () => "Payment failed"),
  },
  auth: { user: { role: "ADMIN" } },
}));
vi.mock("../services/financeService", () => service);
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../components/RecordSelect", () => ({ default: () => null }));
vi.mock("../components/FinanceCharts", () => ({
  default: () => <p>Collection charts</p>,
}));
const a = {
  id: 10,
  studentName: "Asha",
  admissionNumber: "EDU-1",
  courseName: "Math",
  batchName: "Morning",
  finalPayableAmount: 1000,
  originalAmount: 1000,
  discountAmount: 0,
  paidAmount: 0,
  outstandingAmount: 1000,
  overdueAmount: 0,
  agreementDate: "2026-01-01",
  status: "ACTIVE",
  paymentStatus: "PENDING",
  version: 0,
};
const installments = [
  {
    id: 20,
    installmentNumber: 1,
    amount: 1000,
    outstandingAmount: 1000,
    paidAmount: 0,
    dueDate: localDate(),
    status: "PENDING",
  },
];
const empty = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  page: 0,
  size: 10,
};
beforeEach(() => {
  vi.clearAllMocks();
  sessionStorage.clear();
  auth.user = { role: "ADMIN" };
  service.getFeeAccounts.mockResolvedValue({
    data: { ...empty, content: [a], totalElements: 1 },
  });
  service.getFeeAccount.mockResolvedValue({
    data: { account: a, installments, payments: [] },
  });
  service.getMyFees.mockResolvedValue({
    data: { linked: true, accounts: { ...empty, content: [a] } },
  });
});
const mount = (element) => render(<MemoryRouter>{element}</MemoryRouter>);
it("preserves API conflict messages when field errors are empty", async () => {
  const actual = await vi.importActual("../services/financeService");
  expect(
    await actual.financeError({
      response: {
        status: 409,
        data: { message: "Payment exceeds outstanding balance", errors: {} },
      },
    }),
  ).toBe("Payment exceeds outstanding balance");
});
it("uses exact decimal cents for client validation", () => {
  expect(cents("0.10") + cents("0.20")).toBe(30n);
  expect(decimal(30n)).toBe("0.30");
  expect(cents("1.001")).toBeNull();
});
it("loads fee accounts with real service search and pagination", async () => {
  mount(<FeeAccountsPage />);
  expect(await screen.findByText("Asha")).toBeInTheDocument();
  await userEvent.type(
    screen.getByPlaceholderText("Name or admission number…"),
    "Asha",
  );
  await waitFor(() =>
    expect(service.getFeeAccounts).toHaveBeenLastCalledWith(
      expect.objectContaining({ search: "Asha", page: 0 }),
      expect.any(AbortSignal),
    ),
  );
});
it("requires payment review and reuses the same key after a failed retry", async () => {
  service.recordPayment
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValueOnce({ data: { receiptNumber: "EF-123" } });
  const done = vi.fn();
  mount(
    <RecordPaymentForm
      account={a}
      installments={installments}
      onClose={() => {}}
      onSuccess={done}
    />,
  );
  fireEvent.change(screen.getByLabelText("Payment amount (INR) *"), {
    target: { value: "100" },
  });
  fireEvent.change(screen.getByLabelText(/Installment 1/), {
    target: { value: "100" },
  });
  await userEvent.click(screen.getByText("Review payment"));
  expect(service.recordPayment).not.toHaveBeenCalled();
  await userEvent.click(screen.getByText("Confirm payment"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Payment failed");
  expect(done).not.toHaveBeenCalled();
  const first = service.recordPayment.mock.calls[0][0].idempotencyKey;
  await userEvent.click(screen.getByText("Confirm payment"));
  await waitFor(() => expect(done).toHaveBeenCalled());
  expect(service.recordPayment.mock.calls[1][0].idempotencyKey).toBe(first);
});
it("rejects mismatched allocations before submission", async () => {
  mount(
    <RecordPaymentForm
      account={a}
      installments={installments}
      onClose={() => {}}
      onSuccess={() => {}}
    />,
  );
  fireEvent.change(screen.getByLabelText("Payment amount (INR) *"), {
    target: { value: "100" },
  });
  await userEvent.click(screen.getByText("Review payment"));
  expect(await screen.findByRole("alert")).toHaveTextContent("add up");
  expect(service.recordPayment).not.toHaveBeenCalled();
});
it("creates the complete installment schedule with a version", async () => {
  service.createInstallments.mockResolvedValue({});
  const done = vi.fn();
  mount(
    <InstallmentScheduleForm account={a} onClose={() => {}} onSuccess={done} />,
  );
  await userEvent.click(screen.getByText("Save schedule"));
  await waitFor(() => expect(done).toHaveBeenCalled());
  expect(service.createInstallments).toHaveBeenCalledWith(
    10,
    expect.objectContaining({
      version: 0,
      installments: [
        expect.objectContaining({ amount: "1000", installmentNumber: 1 }),
      ],
    }),
  );
});
it("student fee account details have receipts but no financial write controls", async () => {
  auth.user = { role: "STUDENT" };
  render(
    <MemoryRouter initialEntries={["/finance/accounts/10"]}>
      <Routes>
        <Route
          path="/finance/accounts/:id"
          element={<FeeAccountDetailsPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
  expect(await screen.findByText("Agreed fee breakdown")).toBeInTheDocument();
  expect(screen.queryByText("Record payment")).not.toBeInTheDocument();
  expect(screen.queryByText("Edit due date")).not.toBeInTheDocument();
});
it("unlinked students receive a helpful portal state", async () => {
  auth.user = { role: "STUDENT" };
  service.getMyFees.mockResolvedValue({
    data: { linked: false, accounts: empty },
  });
  mount(<MyFeesPage />);
  expect(
    await screen.findByText("Your student profile is not linked"),
  ).toBeInTheDocument();
});
it("teachers do not load financial data", () => {
  auth.user = { role: "TEACHER" };
  mount(<FeeAccountsPage />);
  expect(screen.getByText("Finance access restricted")).toBeInTheDocument();
  expect(service.getFeeAccounts).not.toHaveBeenCalled();
});
it("downloads receipts as authenticated service blobs", async () => {
  const blob = new Blob(["pdf"]);
  service.downloadReceipt.mockResolvedValue({ data: blob });
  mount(
    <ReceiptButton
      payment={{ id: 1, receiptNumber: "EF-1", status: "SUCCESS" }}
    />,
  );
  await userEvent.click(screen.getByText("Receipt"));
  await waitFor(() =>
    expect(service.saveDownload).toHaveBeenCalledWith(blob, "EF-1.pdf"),
  );
});
it("voiding needs a mandatory reason and sends it to the API", async () => {
  service.voidPayment.mockResolvedValue({});
  const done = vi.fn();
  mount(
    <VoidPaymentDialog
      payment={{ id: 1, studentName: "Asha", amount: 100 }}
      onClose={() => {}}
      onSuccess={done}
    />,
  );
  expect(screen.getByRole("button", { name: "Void payment" })).toBeDisabled();
  await userEvent.type(
    screen.getByLabelText("Mandatory reason *"),
    "Wrong account",
  );
  await userEvent.click(screen.getByRole("button", { name: "Void payment" }));
  await waitFor(() =>
    expect(service.voidPayment).toHaveBeenCalledWith(1, "Wrong account"),
  );
});
it("finance dashboard and filtered report export use backend totals", async () => {
  const data = {
    totalCollections: 1000,
    outstandingBalance: 500,
    overdueAmount: 100,
    monthCollections: 1000,
    totalAgreedFees: 1500,
    todayCollections: 1000,
    lifetimeCollections: 1000,
    fromDate: "2026-01-01",
    toDate: localDate(),
    payments: empty,
    courseCollections: [],
    batchOutstanding: [],
  };
  service.getFinanceReport.mockResolvedValue({ data });
  const blob = new Blob(["csv"]);
  service.exportFinanceReport.mockResolvedValue({ data: blob });
  mount(<FinancePage reports />);
  expect(await screen.findByText("Total collections")).toBeInTheDocument();
  await userEvent.click(screen.getByText("Export CSV"));
  await waitFor(() =>
    expect(service.saveDownload).toHaveBeenCalledWith(
      blob,
      "eduflow-finance.csv",
    ),
  );
});
