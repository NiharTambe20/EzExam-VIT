import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { apiGetExam, apiStartAttempt } from "../services/api";
import {
  Clock,
  HelpCircle,
  Award,
  Percent,
  AlertTriangle,
  CheckCircle2,
  ShieldCheck,
  ArrowRight,
  ArrowLeft
} from "lucide-react";

export default function ExamInstructions() {
  const { id } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const [exam, setExam] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    apiGetExam(id)
      .then(setExam)
      .catch(() => setError("This examination is no longer available or not published."));
  }, [id]);

  const start = async () => {
    setBusy(true);
    setError("");
    try {
      const attempt = await apiStartAttempt(id, user.id);
      navigate(`/exams/${id}/attempt`, { state: { attemptId: attempt.id } });
    } catch (err) {
      setError(err.message || "Failed to initiate examination attempt.");
    } finally {
      setBusy(false);
    }
  };

  if (error && !exam) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-16 text-center">
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700 shadow-xs">
          <p className="font-semibold text-rose-800">Notice</p>
          <p className="mt-1">{error}</p>
        </div>
        <Link
          to="/dashboard"
          className="mt-5 inline-flex items-center gap-1.5 text-xs font-semibold text-[#48499A] hover:underline"
        >
          <ArrowLeft size={14} />
          <span>Return to Dashboard</span>
        </Link>
      </div>
    );
  }

  if (!exam) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-20 text-center">
        <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[#48499A] border-t-transparent"></div>
        <p className="mt-3 text-xs text-[#6B7280] font-medium">
          Retrieving official examination instructions…
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <div className="vit-card bg-white border border-[#E5E7EB] rounded-2xl shadow-sm overflow-hidden">
        {/* Institutional Header Banner */}
        <div className="border-b border-[#E5E7EB] bg-gradient-to-r from-[#1F1B47] to-[#282357] p-6 text-white">
          <div className="flex items-center gap-3">
            <img
              src="/vit-logo-sidebar.png"
              alt="VIT Logo"
              className="h-8 w-auto object-contain"
            />
            <div className="border-l border-white/20 pl-3">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-[#B2B3D6]">
                Vidyalankar Institute of Technology • Wadala, Mumbai
              </span>
              <p className="text-xs text-white/90 font-medium">
                Autonomous Institute Affiliated to University of Mumbai
              </p>
            </div>
          </div>
        </div>

        <div className="p-6 sm:p-8 space-y-6">
          {/* Examination Title & Subtitle */}
          <div>
            <span className="vit-badge vit-badge-primary mb-2">
              Official Examination Guidelines
            </span>
            <h1 className="text-2xl font-bold text-[#111827] tracking-tight">
              {exam.title}
            </h1>
            <p className="mt-1.5 text-xs sm:text-sm text-[#6B7280] leading-relaxed">
              {exam.description || "Candidates are required to review the rules and timing parameters prior to starting the test."}
            </p>
          </div>

          {/* Parameters Grid */}
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            <div className="rounded-xl border border-[#E5E7EB] bg-[#F8F9FE] p-3 text-center">
              <Clock size={16} className="mx-auto text-[#48499A] mb-1" />
              <div className="text-[10px] font-semibold uppercase tracking-wider text-[#6B7280]">
                Duration
              </div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums text-[#111827]">
                {exam.duration_minutes} Minutes
              </div>
            </div>

            <div className="rounded-xl border border-[#E5E7EB] bg-[#F8F9FE] p-3 text-center">
              <HelpCircle size={16} className="mx-auto text-[#48499A] mb-1" />
              <div className="text-[10px] font-semibold uppercase tracking-wider text-[#6B7280]">
                Questions
              </div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums text-[#111827]">
                {exam.questions.length} MCQs
              </div>
            </div>

            <div className="rounded-xl border border-[#E5E7EB] bg-[#F8F9FE] p-3 text-center">
              <Award size={16} className="mx-auto text-[#48499A] mb-1" />
              <div className="text-[10px] font-semibold uppercase tracking-wider text-[#6B7280]">
                Total Marks
              </div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums text-[#111827]">
                {exam.total_marks} Marks
              </div>
            </div>

            <div className="rounded-xl border border-[#E5E7EB] bg-[#F8F9FE] p-3 text-center">
              <Percent size={16} className="mx-auto text-[#48499A] mb-1" />
              <div className="text-[10px] font-semibold uppercase tracking-wider text-[#6B7280]">
                Passing Score
              </div>
              <div className="mt-0.5 text-sm font-semibold tabular-nums text-[#111827]">
                {exam.passing_percentage}%
              </div>
            </div>
          </div>

          {/* Conduct Regulations */}
          <div className="rounded-xl border border-slate-200 bg-slate-50/70 p-4.5 space-y-3">
            <h3 className="flex items-center gap-2 text-xs font-bold text-[#111827] uppercase tracking-wider">
              <ShieldCheck size={16} className="text-[#48499A]" />
              Rules and Regulations for Online Examination
            </h3>
            <ul className="space-y-2 text-xs text-[#374151] leading-relaxed">
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="text-[#48499A] shrink-0 mt-0.5" />
                <span>
                  The timer begins immediately when you click <strong>Start Examination</strong>. When the countdown reaches <strong>00:00</strong>, your answers are automatically submitted.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="text-[#48499A] shrink-0 mt-0.5" />
                <span>
                  You may freely navigate between questions and update or clear your selected options anytime prior to submission.
                </span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="text-[#48499A] shrink-0 mt-0.5" />
                <span>
                  Use the <strong>Mark for Review</strong> flag to identify questions you want to revisit before final submission.
                </span>
              </li>
              {Number(exam.negative_mark) > 0 && (
                <li className="flex items-start gap-2 text-amber-900 font-medium bg-amber-50 p-2 rounded-lg border border-amber-200">
                  <AlertTriangle size={14} className="text-amber-600 shrink-0 mt-0.5" />
                  <span>
                    Negative marking of <strong>{exam.negative_mark} marks</strong> is deducted for each incorrect answer.
                  </span>
                </li>
              )}
              <li className="flex items-start gap-2">
                <CheckCircle2 size={14} className="text-[#48499A] shrink-0 mt-0.5" />
                <span>
                  Closing the browser tab does not pause the examination timer.
                </span>
              </li>
            </ul>
          </div>

          {/* Error Message */}
          {error && (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
              {error}
            </div>
          )}

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row gap-3 pt-2">
            <Link
              to="/dashboard"
              className="flex-1 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-center text-xs sm:text-sm font-semibold text-[#111827] shadow-2xs hover:bg-slate-50 transition-colors"
            >
              Return to Dashboard
            </Link>
            <button
              onClick={start}
              disabled={busy}
              className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-[#48499A] px-4 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-xs hover:bg-[#3F408B] active:bg-[#353675] disabled:opacity-60 transition-colors"
            >
              <span>{busy ? "Initiating Session…" : "Start Examination"}</span>
              <ArrowRight size={16} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
