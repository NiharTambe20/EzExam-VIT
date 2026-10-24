/**
 * Exam Answer Storage Utility
 * Manages sessionStorage persistence for active examination attempts.
 * Ensures in-progress answers survive browser refresh, navigation, or temporary drops,
 * and are cleaned up only after confirmed successful submission or when no longer resumable.
 */

export const getAttemptStorageKey = (attemptId) => `ezexam_answers_attempt_${attemptId}`;

/**
 * Loads persisted answers for a given attempt ID from sessionStorage.
 * Returns an empty object if no answers exist, or on parsing/storage errors.
 */
export function loadPersistedAnswers(attemptId) {
  if (!attemptId) return {};
  try {
    if (typeof window === "undefined" || !window.sessionStorage) return {};
    const raw = window.sessionStorage.getItem(getAttemptStorageKey(attemptId));
    if (raw) {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
        return parsed;
      }
    }
  } catch (e) {
    console.warn("Failed to load persisted answers from sessionStorage", e);
  }
  return {};
}

/**
 * Saves in-progress answers for a given attempt ID to sessionStorage.
 */
export function savePersistedAnswers(attemptId, answers) {
  if (!attemptId || !answers) return;
  try {
    if (typeof window === "undefined" || !window.sessionStorage) return;
    window.sessionStorage.setItem(getAttemptStorageKey(attemptId), JSON.stringify(answers));
  } catch (e) {
    console.warn("Failed to save answers to sessionStorage", e);
  }
}

/**
 * Clears persisted answers for a given attempt ID from sessionStorage.
 * Called only after confirmed successful submission or when attempt is no longer resumable.
 */
export function clearPersistedAnswers(attemptId) {
  if (!attemptId) return;
  try {
    if (typeof window === "undefined" || !window.sessionStorage) return;
    window.sessionStorage.removeItem(getAttemptStorageKey(attemptId));
  } catch (e) {
    console.warn("Failed to clear persisted answers from sessionStorage", e);
  }
}
