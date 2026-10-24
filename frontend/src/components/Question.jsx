export default function Question({ question, index, selectedOptionId, onSelect }) {
  const letters = ["A", "B", "C", "D", "E", "F"];

  return (
    <div className="space-y-5">
      {/* Question Header & Weight */}
      <div className="flex items-center justify-between border-b border-[#E5E7EB] pb-3">
        <div className="flex items-center gap-2">
          <span className="rounded-lg bg-[#48499A] px-2.5 py-1 text-xs font-semibold tabular-nums text-white uppercase tracking-wider">
            Q{index + 1}
          </span>
          <span className="text-xs font-semibold text-[#6B7280]">
            Multiple Choice Question
          </span>
        </div>
        <span className="vit-badge vit-badge-primary font-semibold tabular-nums">
          +{question.marks} {question.marks === 1 ? "Mark" : "Marks"}
        </span>
      </div>

      {/* Question Text */}
      <h2 className="text-lg sm:text-xl font-bold text-[#111827] leading-relaxed">
        {question.question_text}
      </h2>

      {/* Options Group */}
      <div
        className="space-y-3 pt-2"
        role="radiogroup"
        aria-label={`Question ${index + 1}`}
      >
        {question.options.map((opt, optIdx) => {
          const active = selectedOptionId != null && String(selectedOptionId) === String(opt.id);
          const letter = letters[optIdx] ?? String(optIdx + 1);

          return (
            <button
              key={opt.id}
              type="button"
              role="radio"
              aria-checked={active}
              onClick={() => onSelect(question.id, opt.id)}
              className={`group flex w-full items-center gap-3.5 rounded-xl border-2 p-3.5 text-left text-sm font-medium transition-all duration-150 cursor-pointer ${
                active
                  ? "border-[#48499A] bg-[#F0F0FA] text-[#111827] shadow-xs"
                  : "border-[#E5E7EB] bg-white text-slate-800 hover:border-[#48499A]/40 hover:bg-[#F8F9FE]"
              }`}
            >
              {/* Option Letter Pill */}
              <span
                className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs transition-colors ${
                  active
                    ? "bg-[#48499A] text-white font-bold shadow-xs"
                    : "bg-slate-100 text-slate-600 font-semibold group-hover:bg-[#F0F0FA] group-hover:text-[#48499A]"
                }`}
              >
                {letter}
              </span>

              {/* Option Text */}
              <span className="flex-1 text-sm leading-snug">{opt.text}</span>

              {/* Radio Indicator */}
              <span
                className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border-2 transition-colors ${
                  active ? "border-[#48499A]" : "border-slate-300 group-hover:border-slate-400"
                }`}
              >
                {active && <span className="h-2.5 w-2.5 rounded-full bg-[#48499A]" />}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
