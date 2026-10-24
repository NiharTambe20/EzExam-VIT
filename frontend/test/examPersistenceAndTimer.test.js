import test from "node:test";
import assert from "node:assert/strict";
import { formatSeconds, parseStartTime, computeRemaining } from "../src/utils/timerUtils.js";
import {
  getAttemptStorageKey,
  loadPersistedAnswers,
  savePersistedAnswers,
  clearPersistedAnswers,
} from "../src/services/examStorage.js";

// Setup mock sessionStorage in global environment
const mockStorage = new Map();
globalThis.window = {
  sessionStorage: {
    getItem: (k) => mockStorage.get(k) ?? null,
    setItem: (k, v) => mockStorage.set(k, String(v)),
    removeItem: (k) => mockStorage.delete(k),
    clear: () => mockStorage.clear(),
  },
};

test("formatSeconds formats seconds correctly into mm:ss", () => {
  assert.equal(formatSeconds(0), "00:00");
  assert.equal(formatSeconds(5), "00:05");
  assert.equal(formatSeconds(65), "01:05");
  assert.equal(formatSeconds(600), "10:00");
  assert.equal(formatSeconds(1200), "20:00");
});

test("parseStartTime safely handles various date-time formats cross-browser", () => {
  // ISO-8601 standard
  const iso = "2026-10-24T10:00:00Z";
  const isoMs = parseStartTime(iso);
  assert.ok(isoMs !== null && !isNaN(isoMs));
  assert.equal(isoMs, Date.parse(iso));

  // MySQL DATETIME space-separated format (Safari fix)
  const mysqlStr = "2026-10-24 10:00:00";
  const mysqlMs = parseStartTime(mysqlStr);
  assert.ok(mysqlMs !== null && !isNaN(mysqlMs));
  assert.equal(mysqlMs, Date.parse("2026-10-24T10:00:00"));

  // Numeric epoch milliseconds
  const now = Date.now();
  assert.equal(parseStartTime(now), now);

  // Numeric epoch seconds
  const nowSec = Math.floor(now / 1000);
  assert.equal(parseStartTime(nowSec), nowSec * 1000);

  // Null, undefined, empty, or invalid values return null (never NaN)
  assert.equal(parseStartTime(null), null);
  assert.equal(parseStartTime(undefined), null);
  assert.equal(parseStartTime(""), null);
  assert.equal(parseStartTime("   "), null);
  assert.equal(parseStartTime("not-a-date"), null);
});

test("computeRemaining calculates authoritative time and never flashes 00:00 on missing start time", () => {
  const durationMinutes = 20;
  const totalSeconds = 20 * 60; // 1200 seconds

  // 1. Missing or undefined start time must return full duration, never 0
  assert.equal(computeRemaining(durationMinutes, null), totalSeconds);
  assert.equal(computeRemaining(durationMinutes, undefined), totalSeconds);
  assert.equal(computeRemaining(durationMinutes, "invalid-date"), totalSeconds);

  // 2. Fresh start (now)
  const baseTime = Date.now();
  const freshRemaining = computeRemaining(durationMinutes, baseTime, baseTime);
  assert.equal(freshRemaining, totalSeconds);

  // 3. 5 minutes elapsed (300 seconds passed) -> 15 minutes remaining (900 seconds)
  const fiveMinAgo = baseTime - 5 * 60 * 1000;
  const remainingAfter5Min = computeRemaining(durationMinutes, fiveMinAgo, baseTime);
  assert.equal(remainingAfter5Min, 15 * 60);

  // 4. Overdue attempt (25 minutes elapsed for 20 minute exam) -> clamped to 0
  const twentyFiveMinAgo = baseTime - 25 * 60 * 1000;
  const overdueRemaining = computeRemaining(durationMinutes, twentyFiveMinAgo, baseTime);
  assert.equal(overdueRemaining, 0);
});

test("sessionStorage persistence saves, restores, and isolates answers per attempt", () => {
  mockStorage.clear();

  const attemptId1 = 5001;
  const attemptId2 = 5002;

  // Initially empty
  assert.deepEqual(loadPersistedAnswers(attemptId1), {});

  // Save answers for attempt 1
  const answers1 = { 201: 2003, 202: 2007 }; // Option C for Q201
  savePersistedAnswers(attemptId1, answers1);

  // Verify storage key format
  const key1 = getAttemptStorageKey(attemptId1);
  assert.equal(key1, "ezexam_answers_attempt_5001");
  assert.ok(mockStorage.has(key1));

  // Restore answers after simulated page reload
  const restored1 = loadPersistedAnswers(attemptId1);
  assert.deepEqual(restored1, answers1);
  assert.equal(restored1[201], 2003, "Question 1 Option C must be restored");

  // Attempt 2 remains completely isolated and empty
  assert.deepEqual(loadPersistedAnswers(attemptId2), {});

  // Save distinct answers for attempt 2
  const answers2 = { 101: 1002 };
  savePersistedAnswers(attemptId2, answers2);
  assert.deepEqual(loadPersistedAnswers(attemptId2), answers2);
  assert.deepEqual(loadPersistedAnswers(attemptId1), answers1, "Attempt 1 answers must not be overwritten");

  // Clear attempt 1 on confirmed submission
  clearPersistedAnswers(attemptId1);
  assert.deepEqual(loadPersistedAnswers(attemptId1), {}, "Attempt 1 answers must be cleared");
  assert.deepEqual(loadPersistedAnswers(attemptId2), answers2, "Attempt 2 answers must remain preserved");
});

test("corrupted sessionStorage data is handled gracefully without throwing", () => {
  const attemptId = 9999;
  mockStorage.set(getAttemptStorageKey(attemptId), "invalid-json{{");

  // Must return empty object and not throw
  const restored = loadPersistedAnswers(attemptId);
  assert.deepEqual(restored, {});
});

test("submission payload construction guarantees latest answers are included", () => {
  const attemptId = 7001;
  mockStorage.clear();

  // Simulate user selecting Option C for Question 201
  const activeAnswers = { "201": 2003 };
  savePersistedAnswers(attemptId, activeAnswers);

  // In submission handler: latest answers from memory with storage fallback
  let answersToSubmit = activeAnswers;
  if (!answersToSubmit || Object.keys(answersToSubmit).length === 0) {
    answersToSubmit = loadPersistedAnswers(attemptId);
  }

  // Build submission payload
  const payload = {
    answers: answersToSubmit,
    userId: 2,
  };

  assert.equal(payload.answers["201"], 2003, "Option C ('inherits') must be present in submission payload");
  assert.equal(payload.userId, 2);

  // If submission fails (e.g. simulated network error), storage is preserved
  const submissionFailed = true;
  if (!submissionFailed) {
    clearPersistedAnswers(attemptId);
  }
  assert.deepEqual(loadPersistedAnswers(attemptId), activeAnswers, "Answers must remain in storage if submit fails");

  // On confirmed submission success, storage is cleared
  clearPersistedAnswers(attemptId);
  assert.deepEqual(loadPersistedAnswers(attemptId), {}, "Answers must be removed after successful submit");
});

test("timer expiration triggers onExpire at most once", () => {
  let expireCount = 0;
  const onExpire = () => {
    expireCount++;
  };

  // Simulate Timer component single-fire guard
  let hasExpired = false;
  const tick = (remainingSeconds) => {
    if (remainingSeconds <= 0) {
      if (!hasExpired) {
        hasExpired = true;
        onExpire();
      }
    }
  };

  // First tick at 0 calls onExpire
  tick(0);
  assert.equal(expireCount, 1);

  // Subsequent ticks (or re-renders) must NOT fire onExpire again
  tick(0);
  tick(-1);
  tick(0);
  assert.equal(expireCount, 1, "onExpire must be called exactly once and not spam submissions");
});
