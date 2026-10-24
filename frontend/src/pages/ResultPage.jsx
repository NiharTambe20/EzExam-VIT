import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiGetResult } from "../services/api";
import {
  Award,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Clock,
  ArrowLeft,
  History,
  GraduationCap,
  Percent
} from "lucide-react";

// PRD §16 result page — Official Statement of Marks
export default function ResultPage() {
  const { attemptId } = useParams();
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    apiGetResult(attemptId)
      .then(setResult)
      .catch(() => setError("Official examination record not found for this attempt ID."));
  }, [attemptId]);

  if (error) {
    return (
      <div className="mx-auto max-w-md px-4 py-16 text-center">
        <div className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700 shadow-xs">
          <p className="font-semibold text-rose-800">Result Retrieval Notice</p>
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

  if (!result) {
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center">
        <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[#48499A] border-t-transparent"></div>
        <p className="mt-3 text-xs text-[#6B7280] font-medium">
          Compiling official statement of marks…
        </p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6">
      <div className="vit-card bg-white border border-[#E5E7EB] rounded-2xl shadow-md overflow-hidden">
        {/* Institutional Grade Card Header */}
        <div className="border-b border-[#E5E7EB] bg-gradient-to-r from-[#1F1B47] to-[#282357] p-5 text-white">
          <div className="flex items-center gap-3">
            <img
              src="/vit-logo-sidebar.png"
              alt="VIT Logo"
              className="h-8 w-auto object-contain"
            />
            <div className="border-l border-white/20 pl-3">
              <span className="text-[10px] font-semibold uppercase tracking-wider text-[#B2B3D6]">
                Vidyalankar Institute of Technology, Mumbai
              </span>
              <p className="text-xs font-bold text-white tracking-wide">
                OFFICIAL STATEMENT OF MARKS
              </p>
            </div>
          </div>
        </div>

        {/* Pass/Fail Status Banner */}
        <div
          className={`p-6 text-center text-white ${
            result.passed
              ? "bg-gradient-to-br from-emerald-600 to-teal-700"
              : "bg-gradient-to-br from-rose-600 to-red-700"
          }`}
        >
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-white/20 backdrop-blur-md mb-2">
            {result.passed ? (
              <CheckCircle2 size={26} className="text-white" />
            ) : (
              <XCircle size={26} className="text-white" />
            )}
          </div>
          <span className="text-xs font-bold uppercase tracking-widest text-white/80">
            Assessment Complete
          </span>
          <h1 className="mt-1 text-xl sm:text-2xl font-bold text-white">
            {result.exam_title}
          </h1>
          <div className="mt-2 inline-flex items-center gap-1.5 rounded-full bg-white/20 px-3.5 py-1 text-xs font-bold tracking-wider uppercase text-white backdrop-blur-md">
            {result.passed ? "QUALIFIED / PASSED" : "NOT QUALIFIED / FAILED"}
          </div>
        </div>

        {/* Marks & Breakdown Section */}
        <div className="p-6 sm:p-7 space-y-6">
          {/* Main Score & Percentage Display */}
          <div className="rounded-2xl border border-[#E5E7EB] bg-[#F8F9FE] p-5 text-center">
            <div className="text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
              Total Score Obtained
            </div>
            <div className="mt-1 text-4xl sm:text-5xl font-semibold tabular-nums text-[#111827]">
              {result.score}
              <span className="text-xl font-medium text-slate-400"> / {result.total}</span>
            </div>
            <div className="mt-2 inline-flex items-center gap-1 rounded-full bg-white px-3 py-1 text-xs font-semibold tabular-nums text-[#48499A] border border-[#E2E2F2] shadow-2xs">
              <Percent size={12} />
              <span>Overall Score: {result.percentage}%</span>
            </div>
          </div>

          {/* 3-Pillar Breakdown */}
          <div>
            <h3 className="text-xs font-bold uppercase tracking-wider text-[#111827] mb-3">
              Performance Breakdown
            </h3>
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="rounded-xl border border-emerald-200 bg-emerald-50/70 p-3.5">
                <div className="text-[10.5px] font-semibold uppercase tracking-wide text-emerald-800">
                  Correct
                </div>
                <div className="mt-1 text-2xl font-semibold tabular-nums text-emerald-700">
                  {result.correct}
                </div>
              </div>

              <div className="rounded-xl border border-rose-200 bg-rose-50/70 p-3.5">
                <div className="text-[10.5px] font-semibold uppercase tracking-wide text-rose-800">
                  Wrong
                </div>
                <div className="mt-1 text-2xl font-semibold tabular-nums text-rose-700">
                  {result.wrong}
                </div>
              </div>

              <div className="rounded-xl border border-slate-200 bg-slate-50 p-3.5">
                <div className="text-[10.5px] font-semibold uppercase tracking-wide text-slate-600">
                  Unanswered
                </div>
                <div className="mt-1 text-2xl font-semibold tabular-nums text-slate-700">
                  {result.unanswered}
                </div>
              </div>
            </div>
          </div>

          {/* Action Links */}
          <div className="flex flex-col sm:flex-row gap-3 pt-2">
            <Link
              to="/dashboard"
              className="flex-1 flex items-center justify-center gap-2 rounded-xl bg-[#48499A] px-4 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-xs hover:bg-[#3F408B] active:bg-[#353675] transition-colors"
            >
              <ArrowLeft size={16} />
              <span>Student Dashboard</span>
            </Link>
            <Link
              to="/history"
              className="flex-1 flex items-center justify-center gap-2 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs sm:text-sm font-semibold text-[#111827] shadow-2xs hover:bg-slate-50 transition-colors"
            >
              <History size={16} />
              <span>Full History</span>
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
