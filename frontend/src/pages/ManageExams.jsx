import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import {
  apiCreateExam,
  apiDeleteExam,
  apiGetExams,
  apiUpdateExam,
} from "../services/api";
import {
  BookOpen,
  Plus,
  Edit3,
  Trash2,
  HelpCircle,
  Clock,
  Award,
  CheckCircle2,
  AlertCircle,
  Calendar,
  X
} from "lucide-react";

const EMPTY = {
  title: "",
  description: "",
  duration_minutes: 30,
  total_marks: 20,
  passing_percentage: 40,
  negative_mark: 0,
  status: "draft",
  start_time: "",
  end_time: "",
};

function toInputDateTime(str) {
  if (!str) return "";
  return str.replace(" ", "T").substring(0, 16);
}

// PRD §19 admin exam management — Official Examination Configuration
export default function ManageExams() {
  const [exams, setExams] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = () => apiGetExams().then(setExams);
  useEffect(() => {
    refresh();
  }, []);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.title.trim()) return setError("Examination title is required.");
    const payload = {
      ...form,
      start_time: form.start_time ? form.start_time : null,
      end_time: form.end_time ? form.end_time : null,
    };
    setBusy(true);
    try {
      if (editing) await apiUpdateExam(editing, payload);
      else await apiCreateExam(payload);
      setForm(EMPTY);
      setEditing(null);
      refresh();
    } catch (err) {
      setError(err.message || "Failed to save examination parameters.");
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (exam) => {
    setEditing(exam.id);
    setForm({
      ...exam,
      start_time: toInputDateTime(exam.start_time),
      end_time: toInputDateTime(exam.end_time),
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (id) => {
    if (!window.confirm("Are you sure you want to delete this examination and all associated questions?")) return;
    try {
      await apiDeleteExam(id);
      refresh();
    } catch (err) {
      setError(err.message || "Failed to delete examination.");
    }
  };

  const inputClass =
    "w-full rounded-xl border border-[#E5E7EB] bg-white px-3.5 py-2.5 text-xs sm:text-sm text-[#111827] outline-none transition-all placeholder:text-slate-400 focus:border-[#48499A] focus:ring-3 focus:ring-[#48499A]/15";

  return (
    <div className="mx-auto flex max-w-7xl flex-col md:flex-row gap-6 px-4 py-8 sm:px-6">
      <Sidebar role="admin" />

      <main className="min-w-0 flex-1 space-y-6">
        {/* Page Header */}
        <div className="border-b border-[#E5E7EB] pb-5">
          <div className="flex items-center gap-2">
            <span className="vit-badge vit-badge-primary">
              Examination Configurator
            </span>
          </div>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827]">
            {editing ? "Modify Examination Configuration" : "Configure New Examination"}
          </h1>
          <p className="mt-1 text-xs sm:text-sm text-[#6B7280]">
            Define assessment parameters, scheduling windows, passing criteria, and publication status.
          </p>
        </div>

        {/* Configuration Form Card */}
        <form
          onSubmit={submit}
          className="vit-card rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-xs space-y-5"
        >
          <div className="flex items-center justify-between border-b border-[#E5E7EB] pb-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827] flex items-center gap-2">
              <BookOpen size={16} className="text-[#48499A]" />
              {editing ? `Editing Exam #${editing}` : "Examination Parameters"}
            </h2>
            {editing && (
              <span className="vit-badge vit-badge-warning">
                Editing Mode
              </span>
            )}
          </div>

          {error && (
            <div className="flex items-center gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
              <AlertCircle size={16} className="shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Form Fields Grid */}
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Examination Title *
              </label>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                placeholder="e.g. Data Structures & Algorithms End-Semester Examination"
                className={inputClass}
                required
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Description / Department Guidelines
              </label>
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                placeholder="e.g. Compulsory assessment for third-year Computer Engineering students."
                className={inputClass}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Duration (Minutes) *
              </label>
              <input
                type="number"
                min="1"
                value={form.duration_minutes}
                onChange={(e) =>
                  setForm({ ...form, duration_minutes: Number(e.target.value) })
                }
                className={inputClass}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Total Marks *
              </label>
              <input
                type="number"
                min="1"
                value={form.total_marks}
                onChange={(e) =>
                  setForm({ ...form, total_marks: Number(e.target.value) })
                }
                className={inputClass}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Passing Threshold (%) *
              </label>
              <input
                type="number"
                min="0"
                max="100"
                value={form.passing_percentage}
                onChange={(e) =>
                  setForm({ ...form, passing_percentage: Number(e.target.value) })
                }
                className={inputClass}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Negative Marking per Incorrect Answer
              </label>
              <input
                type="number"
                min="0"
                step="0.25"
                value={form.negative_mark}
                onChange={(e) =>
                  setForm({ ...form, negative_mark: Number(e.target.value) })
                }
                className={inputClass}
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                Publication Status
              </label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
                className={inputClass}
              >
                <option value="draft">Draft (Hidden from Students)</option>
                <option value="published">Published (Available for Taking)</option>
              </select>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                  Valid From (Optional)
                </label>
                <input
                  type="datetime-local"
                  value={form.start_time || ""}
                  onChange={(e) => setForm({ ...form, start_time: e.target.value })}
                  className={inputClass}
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-[#374151] mb-1.5">
                  Valid Until (Optional)
                </label>
                <input
                  type="datetime-local"
                  value={form.end_time || ""}
                  onChange={(e) => setForm({ ...form, end_time: e.target.value })}
                  className={inputClass}
                />
              </div>
            </div>
          </div>

          {/* Form Actions */}
          <div className="flex items-center gap-2.5 pt-2 border-t border-[#E5E7EB]">
            <button
              type="submit"
              disabled={busy}
              className="inline-flex items-center gap-2 rounded-xl bg-[#48499A] px-5 py-2.5 text-xs sm:text-sm font-semibold text-white shadow-xs hover:bg-[#3F408B] active:bg-[#353675] disabled:opacity-60 transition-colors"
            >
              <Plus size={16} />
              <span>{editing ? "Save Changes" : "Create Examination"}</span>
            </button>
            {editing && (
              <button
                type="button"
                onClick={() => {
                  setEditing(null);
                  setForm(EMPTY);
                }}
                className="inline-flex items-center gap-1.5 rounded-xl border border-[#E5E7EB] bg-white px-4 py-2.5 text-xs sm:text-sm font-semibold text-slate-700 hover:bg-slate-50 transition-colors"
              >
                <X size={15} />
                <span>Cancel Editing</span>
              </button>
            )}
          </div>
        </form>

        {/* Existing Examinations Table */}
        <section className="space-y-4 pt-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-[#111827]">
              Configured Examinations ({exams.length})
            </h2>
          </div>

          <div className="vit-table-container">
            <div className="overflow-x-auto">
              <table className="vit-table">
                <thead>
                  <tr>
                    <th>Examination Details</th>
                    <th>Duration</th>
                    <th>Status</th>
                    <th className="text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {exams.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="p-8 text-center text-xs text-[#6B7280]">
                        No examinations created yet. Use the form above to add an exam.
                      </td>
                    </tr>
                  ) : (
                    exams.map((e) => (
                      <tr key={e.id}>
                        <td>
                          <div className="font-semibold text-[#111827]">
                            {e.title}
                          </div>
                          <div className="text-[11px] text-[#6B7280] tabular-nums mt-0.5">
                            {e.question_count ?? 0} Questions • Total {e.total_marks} Marks • Pass: {e.passing_percentage}%
                          </div>
                        </td>
                        <td className="text-xs font-semibold tabular-nums text-slate-700 whitespace-nowrap">
                          {e.duration_minutes} min
                        </td>
                        <td>
                          <span
                            className={`vit-badge ${
                              e.status === "published"
                                ? "vit-badge-success"
                                : "vit-badge-warning"
                            }`}
                          >
                            {e.status === "published" ? (
                              <>
                                <CheckCircle2 size={11} />
                                Published
                              </>
                            ) : (
                              "Draft"
                            )}
                          </span>
                        </td>
                        <td className="text-right whitespace-nowrap">
                          <div className="inline-flex items-center gap-1.5">
                            <Link
                              to={`/admin/exams/${e.id}/questions`}
                              className="inline-flex items-center gap-1 rounded-lg bg-[#48499A] px-2.5 py-1.5 text-xs font-semibold text-white shadow-2xs hover:bg-[#3F408B] transition-colors"
                            >
                              <HelpCircle size={13} />
                              <span>Questions</span>
                            </Link>
                            <button
                              type="button"
                              onClick={() => startEdit(e)}
                              className="inline-flex items-center gap-1 rounded-lg border border-[#E5E7EB] bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-[#F0F0FA] hover:text-[#48499A] transition-colors"
                            >
                              <Edit3 size={13} />
                              <span>Edit</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => remove(e.id)}
                              className="inline-flex items-center gap-1 rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition-colors"
                            >
                              <Trash2 size={13} />
                              <span>Delete</span>
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      </main>
    </div>
  );
}
