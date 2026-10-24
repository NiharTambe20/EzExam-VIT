import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import { apiAdminAttempts, apiAdminStats, apiGetExams } from "../services/api";
import {
  Users,
  BookOpen,
  FileCheck2,
  CheckCircle2,
  Plus,
  ArrowRight,
  ShieldAlert,
  Clock,
  ExternalLink
} from "lucide-react";

// PRD §18 admin dashboard overview — Institutional Assessment Administration
export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [recent, setRecent] = useState([]);
  const [exams, setExams] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    (async () => {
      try {
        const [s, a, e] = await Promise.all([
          apiAdminStats(),
          apiAdminAttempts(),
          apiGetExams(),
        ]);
        setStats(s);
        setRecent(a.slice(0, 8));
        setExams(e.slice(0, 5));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const statConfig = stats
    ? [
        {
          label: "Enrolled Candidates",
          value: stats.students,
          icon: Users,
          bg: "bg-[#F0F0FA]",
          color: "text-[#48499A]",
        },
        {
          label: "Total Examinations",
          value: stats.exams,
          icon: BookOpen,
          bg: "bg-blue-50",
          color: "text-blue-700",
        },
        {
          label: "Evaluated Attempts",
          value: stats.attempts,
          icon: FileCheck2,
          bg: "bg-purple-50",
          color: "text-purple-700",
        },
        {
          label: "Active Published",
          value: stats.published,
          icon: CheckCircle2,
          bg: "bg-emerald-50",
          color: "text-emerald-700",
        },
      ]
    : [];

  return (
    <div className="mx-auto flex max-w-7xl flex-col md:flex-row gap-6 px-4 py-8 sm:px-6">
      <Sidebar role="admin" />

      <main className="min-w-0 flex-1 space-y-6">
        {/* Top Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border-b border-[#E5E7EB] pb-5">
          <div>
            <div className="flex items-center gap-2">
              <span className="vit-badge vit-badge-primary">
                IT Administration
              </span>
            </div>
            <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827]">
              Examination Controller Overview
            </h1>
            <p className="mt-1 text-xs sm:text-sm text-[#6B7280]">
              Institutional metrics, candidate enrollment stats, and recent assessment submissions.
            </p>
          </div>

          <Link
            to="/admin/exams"
            className="inline-flex items-center gap-2 rounded-xl bg-[#48499A] px-4 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-xs hover:bg-[#3F408B] active:bg-[#353675] transition-colors self-start sm:self-auto"
          >
            <Plus size={16} />
            <span>Create Examination</span>
          </Link>
        </div>

        {/* 4 Metric Cards */}
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          {statConfig.map((item) => {
            const Icon = item.icon;
            return (
              <div
                key={item.label}
                className="vit-card vit-card-hover rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-xs transition-all"
              >
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-semibold uppercase tracking-wider text-[#6B7280]">
                    {item.label}
                  </span>
                  <div className={`flex h-8 w-8 items-center justify-center rounded-lg ${item.bg} ${item.color}`}>
                    <Icon size={18} />
                  </div>
                </div>
                <div className="mt-3 text-3xl font-semibold tabular-nums text-[#111827]">
                  {loading ? "—" : item.value}
                </div>
              </div>
            );
          })}
        </div>

        {/* Data Grid: Recent Exams & Recent Attempts */}
        <div className="grid gap-6 lg:grid-cols-2">
          {/* Recent Exams Section */}
          <section className="vit-card rounded-2xl border border-[#E5E7EB] bg-white overflow-hidden shadow-xs">
            <div className="flex items-center justify-between border-b border-[#E5E7EB] bg-[#F8F9FE] px-5 py-3.5">
              <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827] flex items-center gap-2">
                <BookOpen size={16} className="text-[#48499A]" />
                Recent Examinations
              </h2>
              <Link
                to="/admin/exams"
                className="text-xs font-semibold text-[#48499A] hover:underline"
              >
                Manage All
              </Link>
            </div>

            <div className="divide-y divide-[#E5E7EB]/60">
              {exams.length === 0 ? (
                <p className="p-5 text-xs text-[#6B7280]">No examinations created yet.</p>
              ) : (
                exams.map((e) => (
                  <Link
                    key={e.id}
                    to={`/admin/exams/${e.id}/questions`}
                    className="flex items-center justify-between px-5 py-3.5 text-xs hover:bg-[#F8F9FE] transition-colors group"
                  >
                    <div>
                      <span className="font-semibold text-[#111827] group-hover:text-[#48499A]">
                        {e.title}
                      </span>
                      <p className="text-[11px] text-[#6B7280] tabular-nums mt-0.5">
                        {e.question_count ?? 0} questions • {e.duration_minutes} min • {e.total_marks} marks
                      </p>
                    </div>
                    <span
                      className={`vit-badge ${
                        e.status === "published"
                          ? "vit-badge-success"
                          : "vit-badge-warning"
                      }`}
                    >
                      {e.status}
                    </span>
                  </Link>
                ))
              )}
            </div>
          </section>

          {/* Recent Candidate Attempts */}
          <section className="vit-card rounded-2xl border border-[#E5E7EB] bg-white overflow-hidden shadow-xs">
            <div className="flex items-center justify-between border-b border-[#E5E7EB] bg-[#F8F9FE] px-5 py-3.5">
              <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827] flex items-center gap-2">
                <FileCheck2 size={16} className="text-[#48499A]" />
                Recent Candidate Submissions
              </h2>
              <span className="text-xs text-[#6B7280] font-medium tabular-nums">
                {recent.length} recent
              </span>
            </div>

            <div className="divide-y divide-[#E5E7EB]/60">
              {recent.length === 0 ? (
                <p className="p-5 text-xs text-[#6B7280]">No attempts recorded yet.</p>
              ) : (
                recent.map((a) => (
                  <div
                    key={a.id}
                    className="flex items-center justify-between px-5 py-3 text-xs"
                  >
                    <div className="min-w-0 flex-1">
                      <p className="font-semibold text-[#111827] truncate">
                        {a.student_name}
                      </p>
                      <p className="text-[11px] text-[#6B7280] truncate">
                        {a.exam_title}
                      </p>
                    </div>

                    <div className="text-right ml-3 shrink-0">
                      {a.status === "submitted" ? (
                        <div className="flex items-center gap-1.5">
                          <span className="font-semibold tabular-nums text-slate-800">
                            {a.score} / {a.total}
                          </span>
                          <span
                            className={`vit-badge ${
                              a.passed ? "vit-badge-success" : "vit-badge-danger"
                            }`}
                          >
                            {a.passed ? "Pass" : "Fail"}
                          </span>
                        </div>
                      ) : (
                        <span className="vit-badge vit-badge-warning">
                          In Progress
                        </span>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </main>
    </div>
  );
}
