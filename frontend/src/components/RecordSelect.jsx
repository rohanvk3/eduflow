import { useEffect, useId, useState } from "react";
import axios from "axios";
import { api, errorMessage } from "../api/client";
import { Button, Input, Select } from "./UI";

/** Server-side search plus pagination: never assumes the first page contains every record. */
export default function RecordSelect({
  endpoint,
  label,
  value,
  selectedLabel,
  onChange,
  required = true,
  status = "ACTIVE",
  valueKey = "id",
}) {
  const id = useId();
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reload, setReload] = useState(0);
  useEffect(() => {
    if (search === query) return;
    const timer = setTimeout(() => {
      setQuery(search);
      setPage(0);
    }, 250);
    return () => clearTimeout(timer);
  }, [search, query]);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    api
      .get(endpoint, {
        params: { search: query, page, size: 20, status: status || undefined },
        signal: controller.signal,
      })
      .then(({ data }) => {
        if (!controller.signal.aborted) setResult(data);
      })
      .catch((e) => {
        if (!axios.isCancel(e)) setError(errorMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [endpoint, query, page, reload, status]);
  const options = result?.content || [];
  return (
    <div>
      <label className="field-label" htmlFor={id}>
        {label}
        {required && " *"}
      </label>
      <Input
        className="mb-2 mt-2"
        aria-label={`Search ${label.toLowerCase()}`}
        value={search}
        maxLength={100}
        placeholder="Search available records…"
        onChange={(e) => setSearch(e.target.value)}
      />
      <Select
        id={id}
        required={required}
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value)}
        aria-busy={loading}
      >
        <option value="">{loading ? "Loading…" : "Select a record"}</option>
        {value &&
          !options.some((o) => String(o[valueKey]) === String(value)) && (
            <option value={value}>
              {selectedLabel || `Selected #${value}`}
            </option>
          )}
        {options.map((o) => (
          <option key={o.id} value={o[valueKey]}>
            {o.fullName || o.name} ·{" "}
            {o.admissionNumber || o.employeeId || o.code || o.email || "#" + o.id}
          </option>
        ))}
      </Select>
      {error && (
        <p role="alert" className="mt-2 text-xs text-red-600">
          {error}
          <Button variant="ghost" onClick={() => setReload((r) => r + 1)}>
            Retry
          </Button>
        </p>
      )}
      <div className="mt-2 flex items-center gap-2 text-[10px] text-slate-500">
        <Button
          variant="ghost"
          disabled={loading || page === 0}
          onClick={() => setPage((p) => p - 1)}
        >
          Previous options
        </Button>
        <span>
          {page + 1} / {Math.max(1, result?.totalPages || 0)}
        </span>
        <Button
          variant="ghost"
          disabled={loading || page + 1 >= (result?.totalPages || 0)}
          onClick={() => setPage((p) => p + 1)}
        >
          More options
        </Button>
      </div>
    </div>
  );
}
