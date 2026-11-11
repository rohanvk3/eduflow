import { useState } from "react";
import {
  Button,
  Card,
  EmptyState,
  ErrorNotice,
  Input,
  Modal,
  TableSkeleton,
} from "./UI";
import { Field, useExamLoad, ExamError, examError } from "./ExamUI";
import {
  getQuestions,
  saveQuestion,
  removeQuestion,
  reorderQuestions,
} from "../services/questionService";
export default function QuestionBuilder({ exam, onChange }) {
  const state = useExamLoad(
    (signal) => getQuestions(exam.id, signal),
    [exam.id, exam.version],
  );
  const [editing, setEditing] = useState(null),
    [adding, setAdding] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [preview, setPreview] = useState(false);
  const draft = exam.status === "DRAFT";
  const run = async (operation) => {
    if (busy) return;
    setBusy(true);
    setError("");
    try {
      await operation();
      onChange();
    } catch (e) {
      setError(await examError(e));
    } finally {
      setBusy(false);
    }
  };
  return (
    <Card className="mb-6 p-5">
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-semibold">Question bank</h2>
          <p className="mt-1 text-xs text-slate-500">
            Required total: {exam.totalMarks} marks · {state.data?.length || 0}{" "}
            questions
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={() => setPreview(!preview)}>
            {preview ? "Back to builder" : "Preview exam"}
          </Button>
          {draft && (
            <Button onClick={() => setAdding(true)}>Add question</Button>
          )}
        </div>
      </div>
      <ExamError state={state} />
      <ErrorNotice message={error} />
      {state.loading ? (
        <TableSkeleton />
      ) : !state.data?.length ? (
        <EmptyState
          title="No questions yet"
          description="Add questions and one correct option per question before publication."
        />
      ) : (
        <ol className="space-y-4">
          {state.data.map((q, index) => (
            <li key={q.id} className="rounded-xl border border-slate-200 p-4">
              <div className="flex items-start justify-between gap-3">
                <p className="whitespace-pre-wrap break-words text-sm font-medium">
                  {index + 1}. {q.questionText}
                </p>
                <span className="shrink-0 text-xs text-slate-500">
                  {q.marks} marks
                </span>
              </div>
              <ul className="mt-4 space-y-2">
                {q.options.map((o) => (
                  <li
                    key={o.id}
                    className={`rounded-lg border px-3 py-2 text-sm ${!preview && o.correct ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-slate-200 text-slate-600"}`}
                  >
                    {o.optionText}
                    {!preview && o.correct && " ✓ Correct"}
                  </li>
                ))}
              </ul>
              {draft && !preview && (
                <div className="mt-4 flex flex-wrap gap-2">
                  <Button
                    variant="ghost"
                    disabled={busy}
                    onClick={() => setEditing(q)}
                  >
                    Edit
                  </Button>
                  <Button
                    variant="ghost"
                    disabled={busy || index === 0}
                    onClick={() =>
                      run(() => {
                        const ids = state.data.map((x) => x.id);
                        [ids[index - 1], ids[index]] = [
                          ids[index],
                          ids[index - 1],
                        ];
                        return reorderQuestions(exam.id, {
                          version: exam.version,
                          questionIds: ids,
                        });
                      })
                    }
                  >
                    Move up
                  </Button>
                  <Button
                    variant="ghost"
                    disabled={busy || index === state.data.length - 1}
                    onClick={() =>
                      run(() => {
                        const ids = state.data.map((x) => x.id);
                        [ids[index + 1], ids[index]] = [
                          ids[index],
                          ids[index + 1],
                        ];
                        return reorderQuestions(exam.id, {
                          version: exam.version,
                          questionIds: ids,
                        });
                      })
                    }
                  >
                    Move down
                  </Button>
                  <Button
                    variant="ghost"
                    disabled={busy}
                    onClick={() => {
                      if (window.confirm("Remove this draft question?"))
                        run(() => removeQuestion(exam.id, q.id, exam.version));
                    }}
                  >
                    Remove
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ol>
      )}
      {(adding || editing) && (
        <QuestionForm
          exam={exam}
          initial={editing}
          nextOrder={
            Math.max(0, ...(state.data || []).map((q) => q.displayOrder)) + 1
          }
          onClose={() => {
            setAdding(false);
            setEditing(null);
          }}
          onSuccess={() => {
            setAdding(false);
            setEditing(null);
            onChange();
          }}
        />
      )}
    </Card>
  );
}
function QuestionForm({ exam, initial, nextOrder, onClose, onSuccess }) {
  const [text, setText] = useState(initial?.questionText || ""),
    [marks, setMarks] = useState(initial?.marks || ""),
    [opts, setOpts] = useState(
      initial?.options.map((o) => ({
        optionText: o.optionText,
        correct: o.correct,
      })) || [
        { optionText: "", correct: true },
        { optionText: "", correct: false },
      ],
    ),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  return (
    <Modal
      title={initial ? "Edit question" : "Add question"}
      busy={busy}
      onClose={onClose}
    >
      <form
        className="space-y-5 p-6"
        onSubmit={async (e) => {
          e.preventDefault();
          if (busy) return;
          setBusy(true);
          setError("");
          try {
            await saveQuestion(exam.id, initial?.id, {
              questionText: text,
              marks,
              displayOrder: initial?.displayOrder || nextOrder,
              options: opts,
              version: exam.version,
            });
            onSuccess();
          } catch (e) {
            setError(await examError(e));
          } finally {
            setBusy(false);
          }
        }}
      >
        <ErrorNotice message={error} />
        <fieldset disabled={busy} className="space-y-5">
          <Field label="Question text *">
            <textarea
              className="input min-h-24"
              required
              maxLength={6000}
              value={text}
              onChange={(e) => setText(e.target.value)}
            />
          </Field>
          <Field label="Question marks *">
            <Input
              type="number"
              min="0.01"
              max="99999999.99"
              step="0.01"
              required
              value={marks}
              onChange={(e) => setMarks(e.target.value)}
            />
          </Field>
          <p className="text-xs text-slate-500">
            Select exactly one correct option.
          </p>
          {opts.map((o, i) => (
            <div key={i} className="flex items-center gap-3">
              <input
                type="radio"
                name="correct-option"
                aria-label={`Option ${i + 1} is correct`}
                checked={o.correct}
                onChange={() =>
                  setOpts(opts.map((x, n) => ({ ...x, correct: n === i })))
                }
              />
              <Input
                aria-label={`Option ${i + 1} text`}
                required
                maxLength={2000}
                value={o.optionText}
                onChange={(e) =>
                  setOpts(
                    opts.map((x, n) =>
                      n === i ? { ...x, optionText: e.target.value } : x,
                    ),
                  )
                }
              />
              <Button
                variant="ghost"
                disabled={opts.length <= 2}
                onClick={() => {
                  const next = opts.filter((_, n) => n !== i);
                  if (!next.some((x) => x.correct)) next[0].correct = true;
                  setOpts(next);
                }}
              >
                Remove
              </Button>
            </div>
          ))}
          <Button
            variant="secondary"
            disabled={opts.length >= 10}
            onClick={() =>
              setOpts([...opts, { optionText: "", correct: false }])
            }
          >
            Add option
          </Button>
        </fieldset>
        <div className="flex justify-end gap-3">
          <Button variant="secondary" disabled={busy} onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save question"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
