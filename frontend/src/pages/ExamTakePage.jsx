import { useEffect, useEffectEvent, useRef, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import {
  Button,
  Card,
  ConfirmDialog,
  ErrorNotice,
  Loading,
} from "../components/UI";
import {
  ExamHeading,
  ExamBadge,
  ExamDenied,
  ExamError,
  useExamLoad,
  examError,
  examTime,
} from "../components/ExamUI";
import { getExam } from "../services/examService";
import {
  startAttempt,
  getAttempt,
  saveAnswers,
  submitAttempt,
} from "../services/examAttemptService";
export default function ExamTakePage() {
  const { id } = useParams(),
    { user } = useAuth(),
    allowed = user.role === "STUDENT";
  const [params, setParams] = useSearchParams();
  const state = useExamLoad(
    (signal) =>
      params.get("attempt")
        ? getAttempt(params.get("attempt"), signal)
        : getExam(id, signal),
    [id, params.get("attempt")],
    allowed,
  );
  const [starting, setStarting] = useState(false),
    [error, setError] = useState("");
  if (!allowed) return <ExamDenied />;
  if (state.loading) return <Loading label="Opening exam…" />;
  if (state.error) return <ExamError state={state} />;
  if (!state.data) return null;
  if (params.get("attempt")) {
    if (String(state.data.examId) !== String(id))
      return (
        <ErrorNotice message="This attempt belongs to a different exam." />
      );
    return <AttemptRunner key={state.data.id} initial={state.data} />;
  }
  return (
    <>
      <ExamHeading
        title={state.data.title}
        subtitle="Online examination · One attempt only"
      />
      <Card className="max-w-2xl space-y-5 p-6">
        <ErrorNotice message={error} />
        <p className="text-sm leading-7 text-slate-600">
          Available {examTime(state.data.startDateTime)} to{" "}
          {examTime(state.data.endDateTime)}. Your timer starts when you begin
          and cannot be paused. Saved answers persist if you refresh or
          reconnect. Results remain hidden until publication.
        </p>
        <Button
          disabled={starting}
          onClick={async () => {
            setStarting(true);
            setError("");
            try {
              const { data } = await startAttempt(id);
              setParams({ attempt: String(data.id) }, { replace: true });
            } catch (e) {
              setError(await examError(e));
            } finally {
              setStarting(false);
            }
          }}
        >
          {starting ? "Opening…" : "Start / Resume exam"}
        </Button>
      </Card>
    </>
  );
}
export function AttemptRunner({ initial }) {
  const [attempt, setAttempt] = useState(initial),
    [selected, setSelected] = useState(
      Object.fromEntries(
        initial.answers.map((x) => [x.questionId, x.selectedOptionId]),
      ),
    ),
    [index, setIndex] = useState(0),
    [dirty, setDirty] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [confirm, setConfirm] = useState(false),
    [remaining, setRemaining] = useState(1),
    [clockTick, setClockTick] = useState(0);
  const lock = useRef(false),
    anchor = useRef({
      at: performance.now(),
      ms: new Date(initial.expiresAt) - new Date(initial.serverNow),
    }),
    retryAt = useRef(0);
  const active = attempt.status === "IN_PROGRESS";
  const apply = (data) => {
    setAttempt(data);
    anchor.current = {
      at: performance.now(),
      ms: new Date(data.expiresAt) - new Date(data.serverNow),
    };
  };
  const submit = async (auto = false) => {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      if (dirty && !auto) {
        const { data } = await saveAnswers(attempt.id, {
          version: attempt.version,
          answers: Object.entries(selected).map(([qid, oid]) => ({
            questionId: Number(qid),
            selectedOptionId: oid,
          })),
        });
        apply(data);
        setDirty(false);
      }
      const { data } = await submitAttempt(attempt.id);
      apply(data);
      setDirty(false);
      setConfirm(false);
    } catch (e) {
      if (e.response?.data?.status === "AUTO_SUBMITTED") {
        apply(e.response.data);
        setDirty(false);
      } else setError(await examError(e));
      retryAt.current = performance.now() + 3000;
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  useEffect(() => {
    const tick = () => {
      setClockTick(Math.floor(performance.now() / 1000));
      setRemaining(
        Math.max(
          0,
          Math.ceil(
            (anchor.current.ms - (performance.now() - anchor.current.at)) /
              1000,
          ),
        ),
      );
    };
    tick();
    const timer = setInterval(tick, 250);
    return () => clearInterval(timer);
  }, []);
  const submitOnExpiry = useEffectEvent(() => submit(true));
  useEffect(() => {
    if (
      active &&
      remaining === 0 &&
      !busy &&
      performance.now() >= retryAt.current
    )
      submitOnExpiry();
  }, [active, remaining, busy, clockTick]);
  useEffect(() => {
    if (!dirty) return;
    const warn = (e) => {
      e.preventDefault();
      e.returnValue = "";
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  const save = async (choices) => {
    if (lock.current) return;
    lock.current = true;
    setBusy(true);
    setError("");
    try {
      const { data } = await saveAnswers(attempt.id, {
        version: attempt.version,
        answers: Object.entries(choices).map(([qid, oid]) => ({
          questionId: Number(qid),
          selectedOptionId: oid,
        })),
      });
      apply(data);
      setDirty(false);
    } catch (e) {
      if (e.response?.data?.status === "AUTO_SUBMITTED") {
        apply(e.response.data);
        setDirty(false);
        setError(
          "The server deadline was reached. Late answers were not accepted.",
        );
      } else setError(await examError(e));
    } finally {
      lock.current = false;
      setBusy(false);
    }
  };
  if (!active)
    return (
      <>
        <ExamHeading
          title={attempt.title}
          subtitle="Your examination has been submitted."
        />
        <Card className="space-y-5 p-6">
          <ExamBadge status={attempt.status} />
          <p className="text-sm text-slate-600">
            Your saved answers have been evaluated. Results will be available
            after institute publication.
          </p>
          <Link
            className="button-primary"
            to={`/exams/${attempt.examId}/result`}
          >
            View result status
          </Link>
        </Card>
      </>
    );
  const q = attempt.questions[index],
    answered = Object.values(selected).filter((v) => v != null).length;
  return (
    <>
      <div className="sticky top-[76px] z-10 mb-6 flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-200 bg-surface p-5">
        <div>
          <h1 className="text-lg font-semibold">{attempt.title}</h1>
          <p className="mt-1 text-xs text-slate-500">
            {answered} of {attempt.questions.length} answered ·{" "}
            {busy ? "Saving…" : dirty ? "Unsaved changes" : "All answers saved"}
          </p>
        </div>
        <div
          role="timer"
          aria-label="Time remaining"
          className="font-mono text-xl font-semibold text-indigo-600"
        >
          {Math.floor(remaining / 60)}:{String(remaining % 60).padStart(2, "0")}
        </div>
      </div>
      <ErrorNotice message={error} />
      {dirty && (
        <Button
          variant="secondary"
          className="mb-5"
          disabled={busy || remaining === 0}
          onClick={() => save(selected)}
        >
          Retry save
        </Button>
      )}
      <div className="grid gap-6 lg:grid-cols-[1fr_240px]">
        <Card className="p-6">
          <p className="mb-4 text-xs text-slate-500">
            Question {index + 1} of {attempt.questions.length} · {q.marks} marks
          </p>
          <h2 className="whitespace-pre-wrap text-lg font-medium leading-8">
            {q.questionText}
          </h2>
          <fieldset
            disabled={busy || remaining === 0}
            className="mt-6 space-y-3"
          >
            <legend className="sr-only">Choose your answer</legend>
            {q.options.map((o) => (
              <label
                key={o.id}
                className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm ${selected[q.id] === o.id ? "border-indigo-200 bg-indigo-50" : "border-slate-200 hover:bg-slate-50"}`}
              >
                <input
                  className="mt-0.5"
                  type="radio"
                  name={`question-${q.id}`}
                  checked={selected[q.id] === o.id}
                  onChange={() => {
                    const next = { ...selected, [q.id]: o.id };
                    setSelected(next);
                    setDirty(true);
                    save(next);
                  }}
                />
                <span className="whitespace-pre-wrap break-words">
                  {o.optionText}
                </span>
              </label>
            ))}
            <Button
              variant="ghost"
              onClick={() => {
                const next = { ...selected, [q.id]: null };
                setSelected(next);
                setDirty(true);
                save(next);
              }}
            >
              Clear answer
            </Button>
          </fieldset>
          <div className="mt-7 flex justify-between gap-3">
            <Button
              variant="secondary"
              disabled={index === 0}
              onClick={() => setIndex(index - 1)}
            >
              Previous
            </Button>
            <Button
              variant="secondary"
              disabled={index === attempt.questions.length - 1}
              onClick={() => setIndex(index + 1)}
            >
              Next
            </Button>
          </div>
        </Card>
        <Card className="h-fit p-5">
          <h2 className="text-sm font-semibold">Question navigation</h2>
          <div className="mt-4 grid grid-cols-5 gap-2">
            {attempt.questions.map((item, i) => (
              <Button
                key={item.id}
                variant={i === index ? "primary" : "secondary"}
                aria-label={`Question ${i + 1}, ${selected[item.id] != null ? "answered" : "unanswered"}`}
                className={
                  selected[item.id] != null && i !== index
                    ? "!border-emerald-200 !bg-emerald-50 !text-emerald-700"
                    : ""
                }
                onClick={() => setIndex(i)}
              >
                {i + 1}
              </Button>
            ))}
          </div>
          <progress
            className="mt-5 w-full accent-indigo-600"
            max={attempt.questions.length}
            value={answered}
            aria-label="Answered questions"
          />
          <Button
            className="mt-5 w-full"
            disabled={busy || remaining === 0}
            onClick={() => setConfirm(true)}
          >
            Submit Exam
          </Button>
          <p className="mt-4 text-xs leading-6 text-slate-500">
            The server enforces the deadline even if you disconnect. Only saved
            answers can be evaluated.
          </p>
        </Card>
      </div>
      {confirm && (
        <ConfirmDialog
          title="Submit exam?"
          confirmLabel="Submit exam"
          busy={busy}
          error={error}
          onClose={() => setConfirm(false)}
          onConfirm={() => submit(false)}
        >
          You answered {answered} of {attempt.questions.length} questions.
          Submission is final; unanswered questions receive zero.
        </ConfirmDialog>
      )}
    </>
  );
}
