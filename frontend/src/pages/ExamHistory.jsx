import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";
import { apiGetResults } from "../services/api";
import {
  History,
  CheckCircle2,
  XCircle,
  ExternalLink,
  GraduationCap
} from "lucide-react";

// PRD §17 examination history — Official Candidate Grade Sheet
export default function ExamHistory() {
  const { user } = useAuth();
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (user) {
      apiGetResults(user.id).then((r) => {
        setResults(r);
        setLoading(false);
      });
    }
  }, [user]);

  return (
    <div className="mx-auto flex max-w-7xl flex-col md:flex-row gap-6 px-4 py-8 sm:px-6">
      <Sidebar role="student" />

      <main className="min-w-0 flex-1 space-y-6">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-[#E5E7EB] pb-5">
          <div>
            <div className="flex items-center gap-2">
              <span className="vit-badge vit-badge-primary">
                Academic Records
              </span>
            </div>
            <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827]">
              Examination History &amp; Results
            </h1>
            <p className="mt-1 text-xs sm:text-sm text-[#6B7280]">
              Official logs of all completed examinations, evaluated scores, and qualification statuses.
            </p>
          </div>
        </div>

        {/* Results Table Container */}
        <div className="vit-table-container">
          {loading ? (
            <div className="p-12 text-center">
              <div className="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[#48499A] border-t-transparent"></div>
              <p className="mt-3 text-xs text-[#6B7280] font-medium">
                Loading examination transcripts…
              </p>
            </div>
          ) : results.length === 0 ? (
            <div className="p-12 text-center text-xs text-[#6B7280]">
              <History size={28} className="mx-auto text-slate-300 mb-2" />
              <p className="font-semibold text-slate-700">No examination records found</p>
              <p className="mt-1">
                You have not completed any examinations yet. Check your dashboard for available tests.
              </p>
              <Link
                to="/dashboard"
                className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-[#48499A] px-3.5 py-1.5 text-xs font-semibold text-white shadow-xs hover:bg-[#3F408B]"
              >
                Go to Available Exams
              </Link>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="vit-table">
                <thead>
                  <tr>
                    <th>Examination Title</th>
                    <th>Score Obtained</th>
                    <th>Percentage</th>
                    <th>Completion Date &amp; Time</th>
                    <th>Result Status</th>
                    <th className="text-right">Official Document</th>
                  </tr>
                </thead>
                <tbody>
                  {results.map((r) => {
                    const pct = r.percentage ?? (r.total > 0 ? Math.round((r.score / r.total) * 100) : 0);

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
                          {new Date(r.submitted_at).toLocaleString("en-IN", {
                            day: "numeric",
                            month: "short",
                            year: "numeric",
                            hour: "2-digit",
                            minute: "2-digit",
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
                            className="inline-flex items-center gap-1 rounded-lg border border-[#E5E7EB] bg-white px-2.5 py-1 text-xs font-semibold text-[#48499A] shadow-2xs hover:bg-[#F0F0FA] hover:border-[#48499A]/30 transition-colors"
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
      </main>
    </div>
  );
}
