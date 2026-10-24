import { Link } from "react-router-dom";
import { Clock, HelpCircle, Award, ArrowRight, CheckCircle2 } from "lucide-react";

export default function ExamCard({ exam }) {
  return (
    <div className="vit-card vit-card-hover flex flex-col p-5 bg-white border border-[#E5E7EB] rounded-2xl shadow-xs transition-all duration-200 hover:-translate-y-0.5">
      {/* Title & Status */}
      <div className="flex items-start justify-between gap-3">
        <h3 className="text-base font-bold text-[#111827] line-clamp-1 group-hover:text-[#48499A]">
          {exam.title}
        </h3>
        <span
          className={`shrink-0 vit-badge ${
            exam.status === "published"
              ? "vit-badge-success"
              : "vit-badge-warning"
          }`}
        >
          {exam.status === "published" ? (
            <>
              <CheckCircle2 size={11} />
              Published
            </>
          ) : (
            exam.status
          )}
        </span>
      </div>

      {/* Description */}
      <p className="mt-2 line-clamp-2 text-xs text-[#6B7280] leading-relaxed min-h-[34px]">
        {exam.description || "Official assessment for enrolled students."}
      </p>

      {/* Metadata Metrics */}
      <dl className="mt-4 grid grid-cols-3 gap-2 text-center">
        <div className="rounded-xl bg-[#F8F9FE] p-2.5 border border-[#E5E7EB]/60">
          <dt className="text-[10px] uppercase font-semibold tracking-wider text-[#6B7280] flex items-center justify-center gap-1">
            <Clock size={11} className="text-[#48499A]" />
            Time
          </dt>
          <dd className="mt-0.5 text-xs font-semibold tabular-nums text-[#111827]">
            {exam.duration_minutes} min
          </dd>
        </div>

        <div className="rounded-xl bg-[#F8F9FE] p-2.5 border border-[#E5E7EB]/60">
          <dt className="text-[10px] uppercase font-semibold tracking-wider text-[#6B7280] flex items-center justify-center gap-1">
            <HelpCircle size={11} className="text-[#48499A]" />
            Questions
          </dt>
          <dd className="mt-0.5 text-xs font-semibold tabular-nums text-[#111827]">
            {exam.question_count ?? "—"}
          </dd>
        </div>

        <div className="rounded-xl bg-[#F8F9FE] p-2.5 border border-[#E5E7EB]/60">
          <dt className="text-[10px] uppercase font-semibold tracking-wider text-[#6B7280] flex items-center justify-center gap-1">
            <Award size={11} className="text-[#48499A]" />
            Marks
          </dt>
          <dd className="mt-0.5 text-xs font-semibold tabular-nums text-[#111827]">
            {exam.total_marks}
          </dd>
        </div>
      </dl>

      {/* Start Button */}
      <Link
        to={`/exams/${exam.id}`}
        className="mt-5 flex items-center justify-center gap-2 rounded-xl bg-[#48499A] px-4 py-2.5 text-xs font-semibold text-white shadow-xs transition-colors hover:bg-[#3F408B] active:bg-[#353675]"
      >
        <span>Start Examination</span>
        <ArrowRight size={14} />
      </Link>
    </div>
  );
}
