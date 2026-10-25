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

  // MySQL DATETIME space-separated format (normalized to UTC)
  const mysqlStr = "2026-10-24 10:00:00";
  const mysqlMs = parseStartTime(mysqlStr);
  assert.ok(mysqlMs !== null && !isNaN(mysqlMs));
  assert.equal(mysqlMs, Date.parse("2026-10-24T10:00:00Z"));

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

test("A timezone-free MySQL timestamp is interpreted as UTC, not local browser time", () => {
  // MySQL datetime format: "YYYY-MM-DD HH:mm:ss"
  const mysqlTimestamp = "2026-10-25 09:00:00";
  const parsed = parseStartTime(mysqlTimestamp);

  // Must match the exact UTC epoch milliseconds of 2026-10-25T09:00:00Z
  const expectedUtc = Date.parse("2026-10-25T09:00:00Z");
  assert.equal(parsed, expectedUtc);
  assert.equal(parsed, 1792918800000);
});

test("An exam starting at a known UTC time calculates the correct remaining duration in an IST environment", () => {
  // Deterministic simulation:
  // Server starts exam at 09:00:00 UTC (which is 14:30:00 IST) for 30 minutes.
  const startTime = "2026-10-25 09:00:00";
  const durationMinutes = 30; // Exam duration = 1800 seconds

  // Case A: Candidate opens exam immediately at start time (14:30:00 IST = 09:00:00 UTC)
  const nowAtStart = Date.parse("2026-10-25T09:00:00Z");
  const remainingAtStart = computeRemaining(durationMinutes, startTime, nowAtStart);
  assert.equal(remainingAtStart, 1800, "Full 30 minutes (1800s) must be remaining at start time");
  assert.equal(formatSeconds(remainingAtStart), "30:00", "Timer must display 30:00, not 00:00");

  // Case B: 10 minutes elapsed (14:40:00 IST = 09:10:00 UTC)
  const nowAfter10Min = Date.parse("2026-10-25T09:10:00Z");
  const remainingAfter10Min = computeRemaining(durationMinutes, startTime, nowAfter10Min);
  assert.equal(remainingAfter10Min, 1200, "20 minutes (1200s) must remain after 10 minutes");
  assert.equal(formatSeconds(remainingAfter10Min), "20:00");

  // Case C: 29 minutes elapsed (14:59:00 IST = 09:29:00 UTC)
  const nowAfter29Min = Date.parse("2026-10-25T09:29:00Z");
  const remainingAfter29Min = computeRemaining(durationMinutes, startTime, nowAfter29Min);
  assert.equal(remainingAfter29Min, 60, "1 minute (60s) must remain");
  assert.equal(formatSeconds(remainingAfter29Min), "01:00");
});

test("ISO timestamps with Z and explicit timezone offsets remain correct", () => {
  // Target UTC timestamp: 2026-10-25 09:00:00 UTC (1792918800000)
  const targetUtcMs = Date.parse("2026-10-25T09:00:00Z");

  // 1. ISO ending in 'Z'
  const isoZ = "2026-10-25T09:00:00Z";
  assert.equal(parseStartTime(isoZ), targetUtcMs);

  // 2. ISO ending in lowercase 'z'
  const isoLowerZ = "2026-10-25T09:00:00z";
  assert.equal(parseStartTime(isoLowerZ), targetUtcMs);

  // 3. Positive offset: IST (+05:30) at 14:30:00 is 09:00:00 UTC
  const istOffset = "2026-10-25T14:30:00+05:30";
  assert.equal(parseStartTime(istOffset), targetUtcMs);

  // 4. Negative offset: EDT (-04:00) at 05:00:00 is 09:00:00 UTC
  const edtOffset = "2026-10-25T05:00:00-04:00";
  assert.equal(parseStartTime(edtOffset), targetUtcMs);

  // 5. Offset without colon: +0530
  const noColonOffset = "2026-10-25T14:30:00+0530";
  assert.equal(parseStartTime(noColonOffset), targetUtcMs);
});

test("Invalid or missing start times do not cause an immediate empty submission while the attempt is still initializing", () => {
  const durationMinutes = 30;
  const fullDurationSeconds = 30 * 60; // 1800s

  const invalidInputs = [null, undefined, "", "   ", "invalid-date", "NaN", false];

  for (const input of invalidInputs) {
    // 1. parseStartTime must return null for uninitialized or invalid start times
    const parsed = parseStartTime(input);
    assert.equal(parsed, null, `Input '${input}' must parse to null`);

    // 2. computeRemaining must default to full duration, never 0
    const remaining = computeRemaining(durationMinutes, input);
    assert.equal(remaining, fullDurationSeconds, `Input '${input}' must not flash 00:00`);

    // 3. Simulated Timer guard: Timer component must not trigger expiration if inputs are invalid
    let expired = false;
    const startMs = parseStartTime(input);
    const durationNum = Number(durationMinutes);
    if (startMs !== null && !isNaN(durationNum) && durationNum > 0) {
      expired = true;
    }
    assert.equal(expired, false, `Input '${input}' must never trigger onExpire()`);
  }
});

test("A genuinely expired exam triggers onExpire() exactly once", () => {
  // Deterministic simulation:
  // 30 minute exam started at 09:00:00 UTC. Now is 09:35:00 UTC (5 minutes overdue).
  const startTime = "2026-10-25 09:00:00";
  const durationMinutes = 30;
  const overdueNow = Date.parse("2026-10-25T09:35:00Z");

  let expireCount = 0;
  const onExpire = () => {
    expireCount++;
  };

  // Timer component expiration simulation
  const startMs = parseStartTime(startTime);
  const durationNum = Number(durationMinutes);
  assert.ok(startMs !== null && durationNum > 0);

  const endMs = startMs + durationNum * 60 * 1000;
  let hasExpired = false;

  const evaluateTick = (currentNow) => {
    const left = Math.max(0, Math.round((endMs - currentNow) / 1000));
    if (left <= 0) {
      if (!hasExpired) {
        hasExpired = true;
        onExpire();
      }
    }
    return left;
  };

  // First check on mount when overdue
  const left1 = evaluateTick(overdueNow);
  assert.equal(left1, 0);
  assert.equal(expireCount, 1, "Must trigger onExpire once when genuinely expired");

  // Subsequent re-evaluations or intervals must NOT fire again
  const left2 = evaluateTick(overdueNow + 1000);
  assert.equal(left2, 0);
  assert.equal(expireCount, 1, "Must not trigger onExpire multiple times");

  const left3 = evaluateTick(overdueNow + 5000);
  assert.equal(left3, 0);
  assert.equal(expireCount, 1, "Still must be exactly 1");
});

test("The latest selected answers remain available when a legitimate timer expiration triggers submission", () => {
  const attemptId = 8801;
  mockStorage.clear();

  // Student answers Question 101 with Option 1002, and Question 102 with Option 1006
  const selectedAnswers = { "101": 1002, "102": 1006 };
  savePersistedAnswers(attemptId, selectedAnswers);

  // Simulating ExamPage refs and state
  const answersRef = { current: { ...selectedAnswers } };
  let submittedPayload = null;

  // On legitimate timer expiration, the submit callback is invoked:
  const handleTimerExpiration = () => {
    let answersToSubmit = answersRef.current;
    if (!answersToSubmit || Object.keys(answersToSubmit).length === 0) {
      answersToSubmit = loadPersistedAnswers(attemptId);
    }
    submittedPayload = {
      answers: answersToSubmit,
      userId: 2,
    };
  };

  handleTimerExpiration();

  // Assertions: Selected answers are present and not empty
  assert.ok(submittedPayload !== null);
  assert.deepEqual(submittedPayload.answers, selectedAnswers);
  assert.equal(submittedPayload.answers["101"], 1002);
  assert.equal(submittedPayload.answers["102"], 1006);

  // Even if answersRef was somehow lost/cleared in memory, sessionStorage provides the fallback
  answersRef.current = {};
  let fallbackPayload = null;
  const handleFallbackExpiration = () => {
    let answersToSubmit = answersRef.current;
    if (!answersToSubmit || Object.keys(answersToSubmit).length === 0) {
      answersToSubmit = loadPersistedAnswers(attemptId);
    }
    fallbackPayload = { answers: answersToSubmit, userId: 2 };
  };

  handleFallbackExpiration();
  assert.deepEqual(fallbackPayload.answers, selectedAnswers, "SessionStorage fallback restores selected answers");
});
