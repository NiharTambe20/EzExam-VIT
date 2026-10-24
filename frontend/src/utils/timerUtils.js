/**
 * Timer utility functions for formatting and authoritative duration calculation.
 */

export function formatSeconds(total) {
  const m = Math.floor(Math.max(0, total) / 60);
  const s = Math.max(0, total) % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}

/**
 * Safely parses various date formats into epoch milliseconds.
 * Normalizes space-separated MySQL DATETIME values ("YYYY-MM-DD HH:mm:ss")
 * to ISO-8601 ("YYYY-MM-DDTHH:mm:ss") so Safari and strict JS engines do not fail with NaN.
 */
export function parseStartTime(startTime) {
  if (!startTime && startTime !== 0) return null;
  if (typeof startTime === "number") {
    return startTime < 1e11 ? startTime * 1000 : startTime;
  }
  if (typeof startTime === "string") {
    const trimmed = startTime.trim();
    if (!trimmed) return null;

    // Safari fix: replace space between date and time with 'T'
    const isoLike = trimmed.includes(" ") ? trimmed.replace(" ", "T") : trimmed;
    const direct = Date.parse(isoLike);
    if (!isNaN(direct)) {
      return direct;
    }

    // Fallback: try appending Z if timezone was omitted and direct parse failed
    const withZ = Date.parse(isoLike + "Z");
    if (!isNaN(withZ)) {
      return withZ;
    }
  }

  const d = new Date(startTime);
  const time = d.getTime();
  return isNaN(time) ? null : time;
}

/**
 * Calculates remaining seconds from authoritative start time and duration.
 * If startTime is not yet available, returns full duration to avoid flashing 00:00.
 */
export function computeRemaining(durationMinutes, startTime, nowMs = Date.now()) {
  const totalSeconds = (Number(durationMinutes) || 0) * 60;
  if (!startTime && startTime !== 0) {
    return totalSeconds;
  }

  const startMs = parseStartTime(startTime);
  if (startMs === null) {
    return totalSeconds;
  }

  const endMs = startMs + totalSeconds * 1000;
  const diffSeconds = Math.round((endMs - nowMs) / 1000);
  return Math.max(0, diffSeconds);
}
