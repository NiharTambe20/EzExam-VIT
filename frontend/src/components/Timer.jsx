import { useEffect, useState } from "react";
import { Clock, AlertTriangle } from "lucide-react";

export function formatSeconds(total) {
  const m = Math.floor(Math.max(0, total) / 60);
  const s = Math.max(0, total) % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

// PRD §12 — countdown display; auto-submit handled by parent via onExpire.
export default function Timer({ durationMinutes, startTime, onExpire }) {
  const [remaining, setRemaining] = useState(durationMinutes * 60);

  useEffect(() => {
    const end = new Date(startTime).getTime() + durationMinutes * 60 * 1000;
    const tick = () => {
      const left = Math.max(0, Math.round((end - Date.now()) / 1000));
      setRemaining(left);
      if (left <= 0) onExpire?.();
    };
    tick();
    const id = setInterval(tick, 1000);
    return () => clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [durationMinutes, startTime]);

  const danger = remaining < 300;

  return (
    <div
      role="timer"
      aria-live="polite"
      className={`flex items-center gap-2.5 rounded-xl px-4 py-2 text-sm font-semibold transition-all duration-200 ${
        danger
          ? "border-2 border-rose-400 bg-rose-50 text-rose-700 animate-pulse shadow-xs"
          : "border border-[#282357] bg-[#1F1B47] text-white shadow-sm"
      }`}
    >
      {danger ? (
        <AlertTriangle size={17} className="text-rose-600 shrink-0" />
      ) : (
        <Clock size={17} className="text-[#8B8CD9] shrink-0" />
      )}
      <div className="flex flex-col text-right leading-none">
        <span className={`text-[10px] font-medium uppercase tracking-wider ${danger ? "text-rose-600" : "text-[#B2B3D6]"}`}>
          {danger ? "Expiring Soon" : "Time Left"}
        </span>
        <span className="text-base font-semibold tabular-nums tracking-tight">
          {formatSeconds(remaining)}
        </span>
      </div>
    </div>
  );
}
