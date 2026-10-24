import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Question from "../components/Question";
import QuestionPalette from "../components/QuestionPalette";
import Timer from "../components/Timer";
import { apiGetExam, apiStartAttempt, apiSubmitAttempt } from "../services/api";
import {
  loadPersistedAnswers,
  savePersistedAnswers,
  clearPersistedAnswers,
} from "../services/examStorage";
import { useAuth } from "../context/AuthContext";
import {
  ChevronLeft,
  ChevronRight,
  Bookmark,
  Send,
  AlertCircle,
  FileCheck2,
  X,
  LogOut
} from "lucide-react";

// PRD §11–§14: timer, palette, review flags, submit confirmation, auto-submit.
export default function ExamPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();

  const [exam, setExam] = useState(null);
  const [attempt, setAttempt] = useState(null);
  const [current, setCurrent] = useState(0);
  const [answers, setAnswers] = useState({});
  const [marked, setMarked] = useState(new Set());
  const [visited, setVisited] = useState(new Set([0]));
  const [confirming, setConfirming] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const submittedRef = useRef(false);
  const attemptRef = useRef(null);
  const answersRef = useRef({});

  useEffect(() => {
    let isMounted = true;
    (async () => {
      try {
        const [ex, at] = await Promise.all([
          apiGetExam(id),
          apiStartAttempt(id, user?.id),
        ]);
        if (!isMounted) return;

        // Check if attempt is already completed/submitted
        if (at.status !== "in-progress") {
          clearPersistedAnswers(at.id);
          if (at.status === "submitted") {
            navigate(`/results/${at.id}`, { replace: true });
            return;
          }
        }

        setExam(ex);
        setAttempt(at);
        attemptRef.current = at;

        // Restore saved answers from sessionStorage for this attempt ID
        const savedAnswers = loadPersistedAnswers(at.id);
        if (savedAnswers && Object.keys(savedAnswers).length > 0) {
          setAnswers(savedAnswers);
          answersRef.current = savedAnswers;
        }
      } catch (err) {
        if (isMounted) {
          setError(err?.message || "Unable to load the examination. Please try again.");
        }
      }
    })();

    return () => {
      isMounted = false;
    };
  }, [id, user?.id, navigate]);

  const submit = useCallback(async () => {
    const at = attemptRef.current;
    if (submittedRef.current || !at) return;
    submittedRef.current = true;
    setSubmitting(true);
    setError("");

    // ALWAYS use the latest answers from answersRef (or sessionStorage fallback)
    let answersToSubmit = answersRef.current;
    if (!answersToSubmit || Object.keys(answersToSubmit).length === 0) {
      answersToSubmit = loadPersistedAnswers(at.id);
    }

    try {
      const result = await apiSubmitAttempt(at.id, answersToSubmit || {}, user?.id);
      // Clear locally saved answers only after confirmed successful submission
      clearPersistedAnswers(at.id);
      navigate(`/results/${result.id}`, { replace: true });
    } catch (err) {
      submittedRef.current = false;
      setError(err?.message || "Unable to submit your examination. Please try again.");
      setSubmitting(false);
    }
  }, [user, navigate]);

  // Handle safe exit back to student dashboard without submission
  const handleExit = () => {
    setShowExitModal(false);
    navigate("/dashboard");
  };

  // Close modals on Escape key
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") {
        if (showExitModal) setShowExitModal(false);
        if (confirming) setConfirming(false);
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [showExitModal, confirming]);

  // Keep latest submit in a ref for the timer callback.
  const submitRef = useRef(submit);
  useEffect(() => {
    submitRef.current = submit;
  }, [submit]);

  const counts = useMemo(() => {
    const answered = Object.keys(answers).length;
    const total = exam?.questions?.length ?? 0;
    return {
      answered,
      unanswered: Math.max(0, total - answered),
      reviewCount: marked.size,
    };
  }, [answers, exam, marked]);

  if (error && !exam) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-16 text-center">
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700 shadow-xs">
          <p className="font-semibold text-rose-800">Examination Error</p>
          <p className="mt-1">{error}</p>
        </div>
      </div>
    );
  }

  if (!exam || !attempt) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 text-center">
        <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[#48499A] border-t-transparent"></div>
        <p className="mt-3 text-xs text-[#6B7280] font-medium">
          Loading examination environment…
        </p>
      </div>
    );
  }

  const q = exam.questions[current];

  const select = (qid, oid) => {
    setAnswers((prev) => {
      const next = { ...prev, [qid]: oid };
      answersRef.current = next;
      const atId = attemptRef.current?.id;
      if (atId) {
        savePersistedAnswers(atId, next);
      }
      return next;
    });
  };
  const jump = (i) => {
    setCurrent(i);
    setVisited((v) => new Set(v).add(i));
  };
  const toggleMark = () => {
    setMarked((m) => {
      const n = new Set(m);
      if (n.has(q.id)) n.delete(q.id);
      else n.add(q.id);
      return n;
    });
  };

  const isCurrentMarked = marked.has(q.id);

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 space-y-5">
      {/* Active Examination Header */}
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-[#E5E7EB] bg-white p-4.5 shadow-xs">
        <div className="flex items-center gap-3.5 min-w-0">
          <img
            src="/vit-logo.png"
            alt="VIT Logo"
            className="h-9 w-auto object-contain hidden sm:block shrink-0"
          />
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#48499A] bg-[#F0F0FA] px-2 py-0.5 rounded-full border border-[#E2E2F2] shrink-0">
                Official Assessment
              </span>
              <span className="text-xs text-[#6B7280] hidden md:inline truncate">
                Candidate: <strong className="text-[#111827]">{user?.name}</strong>
              </span>
            </div>
            <h1 className="text-base sm:text-lg font-bold text-[#111827] truncate mt-0.5">
              {exam.title}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2.5 sm:gap-3 shrink-0">
          {/* Exit Examination Control */}
          <button
            type="button"
            onClick={() => setShowExitModal(true)}
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#E5E7EB] bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 hover:text-rose-600 hover:border-rose-200 transition-colors cursor-pointer"
            title="Exit examination and return to dashboard"
          >
            <LogOut size={14} className="shrink-0" />
            <span>Exit Examination</span>
          </button>

          {/* Countdown Timer Component */}
          <Timer
            durationMinutes={exam.duration_minutes}
            startTime={attempt.start_time}
            onExpire={() => submitRef.current()}
          />
        </div>
      </div>

      {/* Main Examination Layout: Question + Palette */}
      <div className="grid gap-5 lg:grid-cols-[1fr_310px]">
        {/* Left Column: Active Question Workspace */}
        <div className="vit-card flex flex-col justify-between rounded-2xl border border-[#E5E7EB] bg-white p-6 sm:p-7 shadow-xs">
          <div>
            <Question
              question={q}
              index={current}
              selectedOptionId={answers[q.id] ?? null}
              onSelect={select}
            />
          </div>

          {/* Navigation Action Buttons Bar */}
          <div className="mt-8 border-t border-[#E5E7EB] pt-5">
            <div className="flex flex-wrap items-center gap-2.5">
              {/* Previous Button */}
              <button
                type="button"
                disabled={current === 0}
                onClick={() => jump(current - 1)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 disabled:opacity-40 transition-colors cursor-pointer"
              >
                <ChevronLeft size={16} />
                <span>Previous</span>
              </button>

              {/* Mark for Review Button */}
              <button
                type="button"
                onClick={toggleMark}
                className={`inline-flex items-center gap-1.5 rounded-xl px-4 py-2.5 text-xs font-semibold transition-colors cursor-pointer ${
                  isCurrentMarked
                    ? "bg-amber-500 text-white shadow-2xs hover:bg-amber-600"
                    : "border border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100"
                }`}
              >
                <Bookmark size={15} className={isCurrentMarked ? "fill-current" : ""} />
                <span>{isCurrentMarked ? "Unmark Review" : "Mark for Review"}</span>
              </button>

              {/* Next Button */}
              <button
                type="button"
                disabled={current === exam.questions.length - 1}
                onClick={() => jump(current + 1)}
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-slate-50 disabled:opacity-40 transition-colors cursor-pointer"
              >
                <span>Next</span>
                <ChevronRight size={16} />
              </button>

              {/* Submit Examination Button */}
              <button
                type="button"
                onClick={() => setConfirming(true)}
                className="ml-auto inline-flex items-center gap-2 rounded-xl bg-emerald-600 px-5 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-emerald-700 active:bg-emerald-800 transition-colors cursor-pointer"
              >
                <Send size={15} />
                <span>Submit Exam</span>
              </button>
            </div>

            {error && (
              <div className="mt-4 flex items-center gap-2 text-xs text-rose-600">
                <AlertCircle size={15} />
                <span>{error}</span>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Palette & Session Summary */}
        <div className="space-y-4">
          <QuestionPalette
            questions={exam.questions}
            answers={answers}
            marked={marked}
            visited={visited}
            current={current}
            onJump={jump}
          />

          {/* Quick Metrics Card */}
          <div className="vit-card rounded-2xl border border-[#E5E7EB] bg-white p-4.5 shadow-xs">
            <h4 className="text-xs font-bold uppercase tracking-wider text-[#111827] mb-3">
              Session Overview
            </h4>
            <div className="grid grid-cols-3 gap-2 text-center text-xs">
              <div className="rounded-xl bg-emerald-50 p-2 border border-emerald-100">
                <div className="text-[10px] font-semibold text-emerald-800 uppercase">Answered</div>
                <div className="text-base font-semibold tabular-nums text-emerald-700 mt-0.5">
                  {counts.answered}
                </div>
              </div>
              <div className="rounded-xl bg-amber-50 p-2 border border-amber-100">
                <div className="text-[10px] font-semibold text-amber-800 uppercase">Review</div>
                <div className="text-base font-semibold tabular-nums text-amber-700 mt-0.5">
                  {counts.reviewCount}
                </div>
              </div>
              <div className="rounded-xl bg-slate-50 p-2 border border-slate-200">
                <div className="text-[10px] font-semibold text-slate-600 uppercase">Remaining</div>
                <div className="text-base font-semibold tabular-nums text-slate-800 mt-0.5">
                  {counts.unanswered}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Submission Confirmation Modal */}
      {confirming && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="submit-dialog-title"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150"
          onClick={(e) => {
            if (e.target === e.currentTarget) setConfirming(false);
          }}
        >
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-[#E5E7EB] space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100 shrink-0">
                  <FileCheck2 size={22} />
                </div>
                <div>
                  <h3 id="submit-dialog-title" className="text-base font-bold text-[#111827]">
                    Submit Examination?
                  </h3>
                  <p className="text-xs text-[#6B7280]">
                    Verify your attempt before final grading.
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setConfirming(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
                aria-label="Close dialog"
              >
                <X size={18} />
              </button>
            </div>

            <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5 space-y-2 text-xs">
              <div className="flex justify-between text-slate-700">
                <span>Total Questions:</span>
                <strong className="font-semibold tabular-nums">{exam.questions.length}</strong>
              </div>
              <div className="flex justify-between text-emerald-700 font-semibold">
                <span>Questions Answered:</span>
                <strong className="font-semibold tabular-nums">{counts.answered}</strong>
              </div>
              <div className="flex justify-between text-rose-700">
                <span>Unanswered Questions:</span>
                <strong className="font-semibold tabular-nums">{counts.unanswered}</strong>
              </div>
              {counts.reviewCount > 0 && (
                <div className="flex justify-between text-amber-700">
                  <span>Marked for Review:</span>
                  <strong className="font-semibold tabular-nums">{counts.reviewCount}</strong>
                </div>
              )}
            </div>

            <p className="text-xs text-[#6B7280] leading-relaxed">
              Once submitted, your responses will be evaluated and recorded in the Vidyalankar Institute of Technology examination database. You cannot re-enter this session.
            </p>

            <div className="flex gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setConfirming(false)}
                className="flex-1 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Return to Exam
              </button>
              <button
                type="button"
                onClick={submit}
                disabled={submitting}
                className="flex-1 rounded-xl bg-emerald-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-emerald-700 disabled:opacity-60 transition-colors cursor-pointer"
              >
                {submitting ? "Submitting Responses…" : "Yes, Submit Now"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Exit Examination Confirmation Modal */}
      {showExitModal && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="exit-exam-title"
          aria-describedby="exit-exam-desc"
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-in fade-in duration-150"
          onClick={(e) => {
            if (e.target === e.currentTarget) {
              setShowExitModal(false);
            }
          }}
        >
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl border border-[#E5E7EB] space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2.5">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-amber-50 text-amber-600 border border-amber-100 shrink-0">
                  <AlertCircle size={22} />
                </div>
                <div>
                  <h3 id="exit-exam-title" className="text-base font-bold text-[#111827]">
                    Exit Examination?
                  </h3>
                  <p className="text-xs text-[#6B7280]">
                    Leaving active session without submission
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowExitModal(false)}
                className="text-slate-400 hover:text-slate-600 p-1 rounded-lg"
                aria-label="Close dialog"
              >
                <X size={18} />
              </button>
            </div>

            <p id="exit-exam-desc" className="text-xs text-[#374151] leading-relaxed">
              Are you sure you want to leave this examination? Your current answers may not be submitted, and the examination timer may continue running.
            </p>

            <div className="flex gap-2.5 pt-2">
              <button
                type="button"
                onClick={() => setShowExitModal(false)}
                className="flex-1 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 transition-colors cursor-pointer"
              >
                Continue Examination
              </button>
              <button
                type="button"
                onClick={handleExit}
                className="flex-1 rounded-xl bg-rose-600 px-4 py-2.5 text-xs font-bold text-white shadow-xs hover:bg-rose-700 active:bg-rose-800 transition-colors cursor-pointer"
              >
                Exit Examination
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
