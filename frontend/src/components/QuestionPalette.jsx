import { Bookmark } from "lucide-react";

// PRD §13 — five visual states for the question palette.
export default function QuestionPalette({
  questions,
  answers,
  marked,
  visited,
  current,
  onJump,
}) {
  const stateOf = (q, i) => {
    const answered = answers[q.id] != null;
    const isMarked = marked.has(q.id);
    const isVisited = visited.has(q.id) || i === current;
    if (answered && isMarked) return "answered-review";
    if (answered) return "answered";
    if (isMarked) return "review";
    if (isVisited) return "unanswered";
    return "unvisited";
  };

  const styles = {
    unvisited: "bg-white text-slate-600 border-[#E5E7EB] hover:bg-slate-50",
    answered: "bg-emerald-600 text-white border-emerald-600 hover:bg-emerald-700 shadow-2xs",
    unanswered: "bg-rose-50 text-rose-700 border-rose-300 hover:bg-rose-100",
    review: "bg-amber-500 text-white border-amber-500 hover:bg-amber-600 shadow-2xs",
    "answered-review": "bg-emerald-600 text-white border-amber-400 ring-2 ring-amber-400 shadow-2xs",
  };

  return (
    <div className="vit-card rounded-2xl border border-[#E5E7EB] bg-white p-5 shadow-xs">
      <div className="flex items-center justify-between border-b border-[#E5E7EB] pb-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-[#111827]">
          Question Palette
        </h3>
        <span className="text-[11px] font-semibold tabular-nums text-[#48499A] bg-[#F0F0FA] px-2 py-0.5 rounded-full border border-[#E2E2F2]">
          {questions.length} Total
        </span>
      </div>

      {/* Grid of Question Numbers */}
      <div className="mt-4 grid grid-cols-5 gap-2">
        {questions.map((q, i) => {
          const state = stateOf(q, i);
          const isCurrent = i === current;

          return (
            <button
              key={q.id}
              onClick={() => onJump(i)}
              title={`Question ${i + 1}: ${state}`}
              className={`relative flex h-9.5 items-center justify-center rounded-xl border text-xs font-semibold tabular-nums transition-all cursor-pointer ${
                styles[state]
              } ${
                isCurrent
                  ? "ring-3 ring-[#48499A] ring-offset-2 scale-105 z-10"
                  : ""
              }`}
            >
              {i + 1}
              {marked.has(q.id) && (
                <span className="absolute -top-1 -right-1 flex h-3 w-3 items-center justify-center rounded-full bg-amber-400 text-slate-900 shadow-xs">
                  <Bookmark size={8} className="fill-current" />
                </span>
              )}
            </button>
          );
        })}
      </div>

      {/* Status Legend */}
      <div className="mt-5 border-t border-[#E5E7EB] pt-4 space-y-2 text-[11px] text-[#6B7280]">
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-md bg-emerald-600 shrink-0 shadow-2xs" />
          <span>Answered</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-md border border-rose-300 bg-rose-50 shrink-0" />
          <span>Unanswered (visited)</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-md bg-amber-500 shrink-0 shadow-2xs" />
          <span>Marked for Review</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 rounded-md border border-[#E5E7EB] bg-white shrink-0" />
          <span>Unvisited</span>
        </div>
      </div>
    </div>
  );
}
