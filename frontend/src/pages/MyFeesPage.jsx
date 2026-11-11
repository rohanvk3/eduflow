import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { Card, EmptyState, Pagination, TableSkeleton } from "../components/UI";
import {
  AccountTable,
  FinanceDenied,
  FinanceHeading,
  FinanceError,
  useFinanceLoad,
} from "../components/FinanceUI";
import { getMyFees } from "../services/financeService";
export default function MyFeesPage() {
  const { user } = useAuth(),
    allowed = user.role === "STUDENT";
  const [page, setPage] = useState(0),
    [size, setSize] = useState(10);
  const state = useFinanceLoad(
    (signal) => getMyFees({ page, size }, signal),
    [page, size],
    allowed,
  );
  if (!allowed) return <FinanceDenied />;
  return (
    <>
      <FinanceHeading
        title="My Fees"
        subtitle="View your agreed fees, outstanding dues, installments, and receipts."
      />
      <FinanceError state={state} />
      <Card className="overflow-hidden">
        {state.loading ? (
          <TableSkeleton />
        ) : state.data?.linked === false ? (
          <EmptyState
            title="Your student profile is not linked"
            description="Ask your institute administrator to link your login to your student record. No fee data is available for this account."
          />
        ) : (
          <AccountTable rows={state.data?.accounts.content} />
        )}
        {state.data?.linked && (
          <Pagination
            noun="fee accounts"
            page={page}
            size={size}
            total={state.data.accounts.totalElements}
            totalPages={state.data.accounts.totalPages}
            onPageChange={setPage}
            onSizeChange={(v) => {
              setSize(v);
              setPage(0);
            }}
          />
        )}
      </Card>
      <p className="mt-5 text-xs leading-6 text-slate-500">
        Open a fee account to see paid amounts, upcoming and overdue
        installments, payment history, and downloadable receipts. Contact the
        institute for payment verification.
      </p>
    </>
  );
}
