import { useEffect, useRef } from "react";
import {
  AlertCircle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Inbox,
  LoaderCircle,
  X,
} from "lucide-react";

export function Button({
  variant = "primary",
  className = "",
  type = "button",
  children,
  ...props
}) {
  return (
    <button type={type} className={`button-${variant} ${className}`} {...props}>
      {children}
    </button>
  );
}

export function Input({ className = "", ...props }) {
  return <input className={`input ${className}`} {...props} />;
}

export function Select({ className = "", children, ...props }) {
  return (
    <select className={`input ${className}`} {...props}>
      {children}
    </select>
  );
}

export function Card({
  className = "",
  children,
  as: Tag = "section",
  ...props
}) {
  return (
    <Tag className={`card ${className}`} {...props}>
      {children}
    </Tag>
  );
}

export function Badge({ children, tone = "neutral", className = "" }) {
  const tones = {
    neutral: "bg-slate-100 text-slate-600",
    indigo: "bg-indigo-50 text-indigo-700",
    success: "bg-emerald-50 text-emerald-700",
    warning: "bg-amber-50 text-amber-700",
  };
  return (
    <span
      className={`inline-flex items-center gap-1.5 whitespace-nowrap rounded-md px-2 py-1 text-[11px] font-medium ${tones[tone] || tones.neutral} ${className}`}
    >
      {children}
    </span>
  );
}

export function StatusBadge({ status }) {
  return (
    <Badge tone={status === "ACTIVE" ? "success" : "neutral"}>
      <span
        className={`h-1.5 w-1.5 rounded-full ${status === "ACTIVE" ? "bg-emerald-500" : "bg-slate-400"}`}
      />
      {status === "ACTIVE" ? "Active" : "Inactive"}
    </Badge>
  );
}

export function Avatar({ name, size = "md", className = "" }) {
  const sizes = {
    sm: "h-8 w-8 text-[11px]",
    md: "h-10 w-10 text-xs",
    lg: "h-20 w-20 text-2xl",
  };
  return (
    <span
      aria-hidden="true"
      className={`inline-flex shrink-0 items-center justify-center rounded-xl border border-indigo-100 bg-indigo-50 font-semibold text-indigo-600 ${sizes[size]} ${className}`}
    >
      {initials(name)}
    </span>
  );
}

export function Table({ children, label = "Student records", className = "" }) {
  return (
    <div
      className="relative overflow-x-auto focus-visible:outline-offset-[-2px]"
      role="region"
      aria-label={label}
      tabIndex={0}
    >
      <table className={`data-table ${className}`}>{children}</table>
    </div>
  );
}

export function Pagination({
  noun = "students",
  page,
  size,
  total = 0,
  totalPages = 0,
  loading,
  onPageChange,
  onSizeChange,
}) {
  return (
    <nav
      aria-label="Table pagination"
      className="flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 px-5 py-4 text-xs text-slate-500"
    >
      <span>
        {total > 0 ? (
          <>
            Showing{" "}
            <strong className="font-medium text-slate-700">
              {page * size + 1}–{Math.min((page + 1) * size, total)}
            </strong>{" "}
            of <strong className="font-medium text-slate-700">{total}</strong>{" "}
            {noun}
          </>
        ) : (
          `No ${noun} to display`
        )}
      </span>
      <div className="flex flex-wrap items-center gap-3">
        <label className="hidden items-center gap-2 sm:flex">
          Rows per page
          <Select
            aria-label="Rows per page"
            className="!w-16 !py-1.5 !text-xs"
            value={size}
            onChange={(e) => onSizeChange(Number(e.target.value))}
          >
            <option>10</option>
            <option>20</option>
            <option>50</option>
          </Select>
        </label>
        <Button
          variant="icon"
          aria-label="Previous page"
          disabled={loading || page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft size={16} />
        </Button>
        <span className="tabular-nums">
          Page {page + 1} of {Math.max(1, totalPages)}
        </span>
        <Button
          variant="icon"
          aria-label="Next page"
          disabled={loading || page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight size={16} />
        </Button>
      </div>
    </nav>
  );
}

export function Skeleton({ className = "" }) {
  return (
    <span
      aria-hidden="true"
      className={`block rounded-md bg-slate-100 ${className}`}
    />
  );
}

export function TableSkeleton({ rows = 5 }) {
  return (
    <div role="status" className="space-y-5 px-5 py-6">
      <span className="sr-only">Loading students…</span>
      {Array.from({ length: rows }, (_, i) => (
        <div key={i} className="flex items-center gap-4">
          <Skeleton className="h-10 w-10 rounded-xl" />
          <div className="flex-1 space-y-2">
            <Skeleton className="h-3 w-36" />
            <Skeleton className="h-2.5 w-48 max-w-full" />
          </div>
          <Skeleton className="hidden h-3 w-24 sm:block" />
          <Skeleton className="h-6 w-16" />
        </div>
      ))}
    </div>
  );
}

export function EmptyState({
  icon: Icon = Inbox,
  title,
  description,
  action,
  compact = false,
}) {
  return (
    <div
      className={`flex flex-col items-center px-5 text-center ${compact ? "py-8" : "py-14"}`}
    >
      <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl border border-slate-200 bg-surface text-slate-400 shadow-xs">
        <Icon size={21} />
      </span>
      <h3 className="text-sm font-semibold text-slate-800">{title}</h3>
      {description && (
        <p className="mt-2 max-w-xs text-xs leading-5 text-slate-500">
          {description}
        </p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function Loading({ label = "Loading students…" }) {
  return (
    <div
      role="status"
      className="flex items-center justify-center gap-3 py-20 text-sm text-slate-500"
    >
      <LoaderCircle className="motion-safe:animate-spin" size={18} />
      {label}
    </div>
  );
}

export function ErrorNotice({ message }) {
  return message ? (
    <div
      role="alert"
      className="mb-5 flex gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm leading-6 text-red-800"
    >
      <AlertCircle className="mt-1 shrink-0" size={17} />
      {message}
    </div>
  ) : null;
}

export function Toast({ message, onDismiss }) {
  if (!message) return null;
  return (
    <div
      role="status"
      className="fixed bottom-5 right-5 z-40 flex max-w-[calc(100vw-2.5rem)] items-start gap-3 rounded-xl border border-slate-200 bg-surface px-4 py-3.5 text-sm text-slate-700 shadow-lg"
    >
      <CheckCircle2 size={19} className="mt-0.5 shrink-0 text-emerald-600" />
      <p className="max-w-sm leading-6">{message}</p>
      {onDismiss && (
        <Button
          variant="icon"
          className="!-m-1 !ml-1"
          aria-label="Dismiss notification"
          onClick={onDismiss}
        >
          <X size={16} />
        </Button>
      )}
    </div>
  );
}

export function Modal({ title, description, children, onClose, busy = false }) {
  const ref = useRef(null);
  useEffect(() => {
    const dialog = ref.current;
    const opener = document.activeElement;
    if (!dialog.open) dialog.showModal();
    return () => {
      if (dialog.open && dialog.close) dialog.close();
      opener?.focus();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
      aria-label={title}
      className="app-modal"
    >
      <div className="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-5">
        <div>
          <h2 className="text-lg font-semibold tracking-tight text-slate-900">
            {title}
          </h2>
          {description && (
            <p className="mt-1 text-xs text-slate-500">{description}</p>
          )}
        </div>
        <Button
          variant="icon"
          aria-label="Close dialog"
          disabled={busy}
          onClick={onClose}
        >
          <X size={18} />
        </Button>
      </div>
      {children}
    </dialog>
  );
}

export function ConfirmDialog({
  title,
  children,
  error,
  busy,
  onClose,
  onConfirm,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
}) {
  return (
    <Modal title={title} busy={busy} onClose={onClose}>
      <div className="px-6 py-5">
        <ErrorNotice message={error} />
        <div className="text-sm leading-6 text-slate-600">{children}</div>
      </div>
      <div className="flex flex-wrap justify-end gap-3 border-t border-slate-100 bg-slate-50/50 px-6 py-4">
        <Button variant="secondary" disabled={busy} autoFocus onClick={onClose}>
          {cancelLabel}
        </Button>
        <Button variant="danger" disabled={busy} onClick={onConfirm}>
          {busy ? "Working…" : confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}

export function initials(name = "") {
  return (
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase() || "EF"
  );
}
export function dateLabel(value) {
  return value
    ? new Intl.DateTimeFormat("en-IN", { dateStyle: "medium" }).format(
        new Date(`${value}T00:00:00`),
      )
    : "—";
}
