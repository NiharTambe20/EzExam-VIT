import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import ExamCard from "../components/ExamCard";
import { useAuth } from "../context/AuthContext";
import { apiGetExams, apiGetResults } from "../services/api";
import {
  BookOpen,
  History,
  GraduationCap,
  Calendar,
  AlertCircle,
  ExternalLink,
  CheckCircle2,
  XCircle,
  ChevronRight
} from "lucide-react";

export default function StudentDashboard() {
  const { user } = useAuth();
  const [exams, setExams] = useState([]);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    (async () => {
      try {
        const [e, r] = await Promise.all([
          apiGetExams({ onlyPublished: true }),
          user ? apiGetResults(user.id) : Promise.resolve([]),
        ]);
        setExams(e);
        setResults(r.slice(0, 5));
      } catch (err) {
        setError("Unable to connect to the server. Please try again.");
      } finally {
        setLoading(false);
      }
    })();
  }, [user]);

  const firstName = user?.name ? user.name.split(" ")[0] : "Student";

  return (
    <div className="mx-auto flex max-w-7xl flex-col md:flex-row gap-6 px-4 py-8 sm:px-6">
      <Sidebar role="student" />

      <main className="min-w-0 flex-1 space-y-6">
        {/* Welcome Banner — VIT Purple Gradient */}
        <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-[#48499A] via-[#3F408B] to-[#35306B] p-6 sm:p-8 text-white shadow-md">
          <div className="relative z-10 max-w-2xl space-y-2">
            <div className="inline-flex items-center gap-2 rounded-full bg-white/15 px-3 py-1 text-xs font-semibold backdrop-blur-md">
              <GraduationCap size={14} />
              <span>Vidyalankar Institute of Technology, Mumbai</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">
              Welcome back, {firstName}!
            </h1>
            <p className="text-xs sm:text-sm text-white/85 leading-relaxed">
              Access your scheduled examinations, review official guidelines, and track your statement of marks.
            </p>
          </div>

          {/* Decorative Background Flame/Shield Accent */}
          <div className="absolute right-0 top-0 -mt-6 -mr-6 h-48 w-48 rounded-full bg-white/5 blur-2xl pointer-events-none" />
        </div>

        {/* Server Error Alert */}
        {error && (
          <div className="flex items-center gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-xs sm:text-sm text-rose-700">
            <AlertCircle size={18} className="shrink-0 text-rose-600" />
            <div className="flex-1 font-medium">{error}</div>
          </div>
        )}

        {/* Section 1: Available Examinations */}
        <section className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#F0F0FA] text-[#48499A]">
                <BookOpen size={18} />
              </div>
              <div>
                <h2 className="text-lg font-bold text-[#111827]">
                  Available Examinations
                </h2>
                <p className="text-xs text-[#6B7280]">
                  Published assessments currently open for candidates
                </p>
              </div>
            </div>
            <span className="vit-badge vit-badge-primary tabular-nums font-semibold">
              {exams.length} Available
            </span>
          </div>

          {loading ? (
            <div className="rounded-2xl border border-[#E5E7EB] bg-white p-12 text-center">
              <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[#48499A] border-t-transparent"></div>
              <p className="mt-3 text-xs text-[#6B7280] font-medium">Loading available examinations…</p>
            </div>
          ) : exams.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-[#E5E7EB] bg-white p-10 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#F0F0FA] text-[#48499A]">
                <BookOpen size={20} />
              </div>
              <h3 className="mt-3 text-sm font-semibold text-[#111827]">No active examinations</h3>
              <p className="mt-1 text-xs text-[#6B7280]">
                There are no examinations scheduled at this time. Check back later or contact your department.
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {exams.map((exam) => (
                <ExamCard key={exam.id} exam={exam} />
              ))}
            </div>
          )}
        </section>

        {/* Section 2: Previous Examination Results */}
        <section className="space-y-4 pt-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-[#F0F0FA] text-[#48499A]">
                <History size={18} />
              </div>
              <div>
                <h2 className="text-lg font-bold text-[#111827]">
                  Recent Examination Results
                </h2>
                <p className="text-xs text-[#6B7280]">
                  Your latest graded submissions and performance history
                </p>
              </div>
            </div>
            <Link
              to="/history"
              className="inline-flex items-center gap-1 text-xs font-semibold text-[#48499A] hover:underline"
            >
              <span>View full history</span>
              <ChevronRight size={14} />
            </Link>
          </div>

          <div className="vit-table-container">
            {results.length === 0 ? (
              <div className="p-8 text-center text-xs text-[#6B7280]">
                <History size={24} className="mx-auto text-slate-300 mb-2" />
                No attempts recorded yet. Completed assessments will appear here.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="vit-table">
                  <thead>
                    <tr>
                      <th>Examination Title</th>
                      <th>Score</th>
                      <th>Percentage</th>
                      <th>Submission Date</th>
                      <th>Result Status</th>
                      <th className="text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((r) => {
                      const pct = r.total > 0 ? Math.round((r.score / r.total) * 100) : 0;
                      return (
                        <tr key={r.id}>
                          <td className="font-semibold text-[#111827]">
                            {r.exam_title}
                          </td>
                          <td className="text-xs font-semibold tabular-nums text-slate-800">
                            {r.score} <span className="text-slate-400 font-normal">/ {r.total}</span>
                          </td>
                          <td className="text-xs font-medium tabular-nums text-slate-600">
                            {pct}%
                          </td>
                          <td className="text-xs text-slate-500 whitespace-nowrap">
                            {new Date(r.submitted_at).toLocaleDateString("en-IN", {
                              day: "numeric",
                              month: "short",
                              year: "numeric"
                            })}
                          </td>
                          <td>
                            <span
                              className={`vit-badge ${
                                r.passed ? "vit-badge-success" : "vit-badge-danger"
                              }`}
                            >
                              {r.passed ? (
                                <>
                                  <CheckCircle2 size={11} />
                                  Passed
                                </>
                              ) : (
                                <>
                                  <XCircle size={11} />
                                  Failed
                                </>
                              )}
                            </span>
                          </td>
                          <td className="text-right">
                            <Link
                              to={`/results/${r.id}`}
                              className="inline-flex items-center gap-1 text-xs font-semibold text-[#48499A] hover:underline"
                            >
                              <span>Marks Card</span>
                              <ExternalLink size={12} />
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
