import { Link } from "react-router-dom";
import { Card } from "./UI";
import { FinanceStats, FinanceError, useFinanceLoad } from "./FinanceUI";
import { getFinanceDashboard } from "../services/financeService";
export default function FinanceDashboard() {
  const state = useFinanceLoad(
    (signal) => getFinanceDashboard({ size: 1 }, signal),
    [],
  );
  return (
    <Card className="mb-6 p-5">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold">Finance overview</h2>
          <p className="mt-1 text-xs text-slate-500">
            Collections: trailing 12 calendar months through today · Dues:
            lifetime balances · INR
          </p>
        </div>
        <Link className="button-secondary" to="/finance">
          Open finance
        </Link>
      </div>
      <FinanceError state={state} />
      {!state.error && (
        <FinanceStats data={state.data} loading={state.loading} />
      )}
    </Card>
  );
}
