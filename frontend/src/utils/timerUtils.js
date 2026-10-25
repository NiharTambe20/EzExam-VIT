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
 * Normalizes timezone-free timestamps from the backend (such as MySQL DATETIME
 * "YYYY-MM-DD HH:mm:ss" or ISO "YYYY-MM-DDTHH:mm:ss") to UTC ISO-8601 ("...Z")
 * before parsing, ensuring proper countdown calculation across all client timezones.
 */
export function parseStartTime(startTime) {
  if (!startTime && startTime !== 0) return null;
  if (typeof startTime === "number") {
    return startTime < 1e11 ? Math.round(startTime * 1000) : Math.round(startTime);
  }
  if (typeof startTime === "string") {
    const trimmed = startTime.trim();
    if (!trimmed) return null;

    // Numeric timestamp encoded as string (epoch seconds or milliseconds)
    if (/^\d+(\.\d+)?$/.test(trimmed)) {
      const num = Number(trimmed);
      if (!isNaN(num)) {
        return num < 1e11 ? Math.round(num * 1000) : Math.round(num);
      }
    }

    // Safari / cross-browser fix: normalize space between date and time to 'T'
    let iso = trimmed.includes(" ") ? trimmed.replace(" ", "T") : trimmed;

    // Check if a timezone offset or Z indicator is already present:
    // - Ends in 'Z' or 'z'
    // - Or ends with timezone offset: +HH:mm, -HH:mm, +HHmm, -HHmm, +HH, -HH
    const hasTz = /(?:Z|[+-]\d{2}(?::?\d{2})?)$/i.test(iso);
    if (!hasTz) {
      // Backend timestamps without timezone information represent UTC wall-clock time
      iso += "Z";
    }

    const direct = Date.parse(iso);
    if (!isNaN(direct)) {
      return direct;
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
