import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import {
  apiCreateQuestion,
  apiDeleteQuestion,
  apiGetExam,
  apiUpdateQuestion,
} from "../services/api";
import {
  ArrowLeft,
  HelpCircle,
  Plus,
  Edit3,
  Trash2,
  CheckCircle2,
  AlertCircle,
  Award,
  MinusCircle,
  X
} from "lucide-react";

const EMPTY = {
  question_text: "",
  marks: 1,
  negative_marks: 0,
  options: ["", "", "", ""],
  correctIndex: 1,
};

// PRD §20 admin question management (single-correct MCQ)
export default function ManageQuestions() {
  const { id } = useParams();
  const [exam, setExam] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [editing, setEditing] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const refresh = () =>
    apiGetExam(id, { includeAnswers: true }).then(setExam);

  useEffect(() => {
    refresh();
  }, [id]);

  const setOpt = (i, val) =>
    setForm((f) => ({
      ...f,
      options: f.options.map((o, j) => (j === i ? val : o)),
    }));

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.question_text.trim()) return setError("Question text is required.");
    if (form.options.some((o) => !o.trim()))
      return setError("All four MCQ options are required.");

    setBusy(true);
    try {
      if (editing) await apiUpdateQuestion(editing, form);
      else await apiCreateQuestion(id, form);
      setForm(EMPTY);
      setEditing(null);
      refresh();
    } catch (err) {
      setError(err.message || "Failed to save question.");
    } finally {
      setBusy(false);
    }
  };

  const startEdit = (q) => {
    setEditing(q.id);
    setForm({
      question_text: q.question_text,
      marks: q.marks,
      negative_marks: q.negative_marks,
      options: q.options.map((o) => o.text),
      correctIndex: Math.max(
        0,
        q.options.findIndex((o) => o.is_correct)
      ),
    });
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const remove = async (qid) => {
    if (!window.confirm("Are you sure you want to delete this question?")) return;
    try {
      await apiDeleteQuestion(qid);
      refresh();
    } catch (err) {
      setError(err.message || "Failed to delete question.");
    }
  };

  const inputClass =
    "w-full rounded-xl border border-[#E5E7EB] bg-white px-3.5 py-2.5 text-xs sm:text-sm text-[#111827] outline-none transition-all placeholder:text-slate-400 focus:border-[#48499A] focus:ring-3 focus:ring-[#48499A]/15";

  const letters = ["A", "B", "C", "D"];

  return (
    <div className="mx-auto flex max-w-7xl flex-col md:flex-row gap-6 px-4 py-8 sm:px-6">
      <Sidebar role="admin" />

      <main className="min-w-0 flex-1 space-y-6">
        {/* Navigation Breadcrumb & Header */}
        <div className="border-b border-[#E5E7EB] pb-5">
          <Link
            to="/admin/exams"
            className="inline-flex items-center gap-1.5 text-xs font-semibold text-[#48499A] hover:underline mb-2"
          >
            <ArrowLeft size={14} />
            <span>Back to Examinations</span>
          </Link>
          <div className="flex items-center gap-2">
            <span className="vit-badge vit-badge-primary">
              Question Bank Builder
            </span>
          </div>
          <h1 className="mt-1 text-2xl font-bold tracking-tight text-[#111827]">
            {exam ? exam.title : "Question Management"}
          </h1>
          <p className="mt-1 text-xs sm:text-sm text-[#6B7280]">
            Add and manage multiple-choice questions with answer keys, weights, and negative marking.
          </p>
        </div>

        {/* Question Form Builder Card */}
        <form
          onSubmit={submit}
          className="vit-card rounded-2xl border border-[#E5E7EB] bg-white p-6 shadow-xs space-y-5"
        >
          <div className="flex items-center justify-between border-b border-[#E5E7EB] pb-3">
            <h2 className="text-xs font-bold uppercase tracking-wider text-[#111827] flex items-center gap-2">
              <HelpCircle size={16} className="text-[#48499A]" />
              {editing ? `Editing Question #${editing}` : "Add Question"}
            </h2>
            {editing && (
              <span className="vit-badge vit-badge-warning">Editing Mode</span>
            )}
          </div>

          {error && (
            <div className="flex items-center gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">
              <AlertCircle size={16} className="shrink-0 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {/* Question Text */}
          <div>
            <label className="block text-xs font-semibold text-[#374151] mb-1.5">
              Question Statement *
            </label>
            <textarea
              value={form.question_text}
              onChange={(e) =>
                setForm({ ...form, question_text: e.target.value })
              }
              placeholder="e.g. Which of the following data structures operates on a Last-In-First-Out (LIFO) basis?"
              rows={3}
              className={`${inputClass} leading-relaxed`}
              required
            />
          </div>

          {/* 4 Options Grid */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-semibold text-[#374151]">
                Answer Options &amp; Correct Key Selection *
              </label>
              <span className="text-[11px] text-[#6B7280]">
                Select the radio button next to the correct answer
              </span>
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              {form.options.map((opt, i) => {
                const isCorrect = Number(form.correctIndex) === i;

                return (
                  <div
                    key={i}
                    className={`flex items-center gap-3 rounded-xl border p-3 transition-colors ${
                      isCorrect
                        ? "border-emerald-500 bg-emerald-50/70"
                        : "border-[#E5E7EB] bg-white hover:border-slate-300"
                    }`}
                  >
                    <input
                      type="radio"
                      name="correct"
                      checked={isCorrect}
                      onChange={() => setForm({ ...form, correctIndex: i })}
                      title="Select as correct answer"
                      className="h-4 w-4 text-emerald-600 cursor-pointer accent-emerald-600"
                    />
                    <span
                      className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-xs font-bold ${
                        isCorrect
                          ? "bg-emerald-600 text-white"
                          : "bg-slate-100 text-slate-700"
                      }`}
                    >
                      {letters[i]}
                    </span>
                    <input
                      value={opt}
                      onChange={(e) => setOpt(i, e.target.value)}
                      placeholder={`Option ${letters[i]} description`}
                      className="w-full bg-transparent text-xs sm:text-sm outline-none text-[#111827] placeholder:text-slate-400"
                      required
                    />
                    {isCorrect && (
                      <span className="shrink-0 text-[10px] font-bold uppercase tracking-wider text-emerald-700">
                        Key
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Marks and Negative Marks Grid */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5 flex items-center gap-1.5">
                <Award size={14} className="text-[#48499A]" />
                Marks Awarded *
              </label>
              <input
                type="number"
                min="1"
                value={form.marks}
                onChange={(e) =>
                  setForm({ ...form, marks: Number(e.target.value) })
                }
                className={inputClass}
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#374151] mb-1.5 flex items-center gap-1.5">
                <MinusCircle size={14} className="text-rose-600" />
                Negative Marks (Deduction)
              </label>
              <input
                type="number"
                min="0"
                step="0.25"
                value={form.negative_marks}
                onChange={(e) =>
                  setForm({ ...form, negative_marks: Number(e.target.value) })
                }
                className={inputClass}
              />
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
              <span>{editing ? "Save Question Changes" : "Add Question"}</span>
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

        {/* Configured Questions List */}
        <section className="space-y-4 pt-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-[#111827]">
              Examination Question Bank (<span className="tabular-nums font-semibold">{exam?.questions.length ?? 0}</span>)
            </h2>
          </div>

          <div className="space-y-3.5">
            {exam?.questions.map((q, i) => (
              <div
                key={q.id}
                className="vit-card rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-xs transition-all hover:border-[#D1D5DB]"
              >
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-3 flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="rounded-md bg-[#48499A] px-2 py-0.5 text-xs font-semibold tabular-nums text-white">
                        Q{i + 1}
                      </span>
                      <span className="vit-badge vit-badge-primary text-[11px] font-semibold tabular-nums">
                        +{q.marks} Marks
                      </span>
                      {Number(q.negative_marks) > 0 && (
                        <span className="vit-badge vit-badge-danger text-[11px] font-semibold tabular-nums">
                          −{q.negative_marks} Neg
                        </span>
                      )}
                    </div>

                    <h3 className="text-sm font-bold text-[#111827] leading-relaxed">
                      {q.question_text}
                    </h3>

                    {/* Options Preview */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                      {q.options.map((o, oi) => (
                        <div
                          key={o.id}
                          className={`flex items-center gap-2.5 rounded-lg px-3 py-2 text-xs ${
                            o.is_correct
                              ? "border border-emerald-300 bg-emerald-50 text-emerald-900 font-semibold"
                              : "border border-[#E5E7EB] bg-slate-50/60 text-slate-700"
                          }`}
                        >
                          <span
                            className={`flex h-5 w-5 shrink-0 items-center justify-center rounded text-[10px] font-bold ${
                              o.is_correct
                                ? "bg-emerald-600 text-white"
                                : "bg-slate-200 text-slate-700"
                            }`}
                          >
                            {letters[oi]}
                          </span>
                          <span className="truncate flex-1">{o.text}</span>
                          {o.is_correct && (
                            <CheckCircle2
                              size={14}
                              className="text-emerald-600 shrink-0"
                            />
                          )}
                        </div>
                      ))}
                    </div>
                  </div>

                  {/* Edit / Delete Buttons */}
                  <div className="flex shrink-0 items-center gap-1.5 self-start">
                    <button
                      type="button"
                      onClick={() => startEdit(q)}
                      className="inline-flex items-center gap-1 rounded-lg border border-[#E5E7EB] bg-white px-2.5 py-1.5 text-xs font-semibold text-slate-700 shadow-2xs hover:bg-[#F0F0FA] hover:text-[#48499A] transition-colors"
                      title="Edit question parameters"
                    >
                      <Edit3 size={13} />
                      <span className="hidden sm:inline">Edit</span>
                    </button>
                    <button
                      type="button"
                      onClick={() => remove(q.id)}
                      className="inline-flex items-center gap-1 rounded-lg border border-rose-200 bg-rose-50 px-2.5 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-100 transition-colors"
                      title="Delete question from examination"
                    >
                      <Trash2 size={13} />
                      <span className="hidden sm:inline">Delete</span>
                    </button>
                  </div>
                </div>
              </div>
            ))}

            {exam && exam.questions.length === 0 && (
              <div className="rounded-2xl border border-dashed border-[#E5E7EB] bg-white p-10 text-center text-xs text-[#6B7280]">
                <HelpCircle size={28} className="mx-auto text-slate-300 mb-2" />
                <p className="font-semibold text-slate-700">No questions configured yet</p>
                <p className="mt-1">
                  Use the Question Bank Builder above to add multiple-choice questions to this examination.
                </p>
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
