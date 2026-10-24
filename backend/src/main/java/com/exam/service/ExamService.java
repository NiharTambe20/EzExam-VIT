package com.exam.service;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Attempt;
import com.exam.model.AttemptAnswer;
import com.exam.model.Exam;
import com.exam.model.Option;
import com.exam.model.Question;
import com.exam.util.DBConnection;
import com.exam.util.DateTimeUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExamService {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    /**
     * Evaluates a student's examination submission according to the rules in PRD §15.
     * Prevents double-submission, verifies attempt ownership, ensures atomic transaction safety,
     * and reuses a single database connection to eliminate connection pooling overhead.
     */
    public Attempt evaluateAndSubmit(int attemptId, int userId, Map<String, Object> answersMap) throws SQLException, IllegalAccessException {
        try (Connection con = DBConnection.getConnection()) {
            Attempt attempt = attemptDAO.findById(con, attemptId);
            if (attempt == null) {
                throw new IllegalArgumentException("Examination attempt not found.");
            }

            // Security: verify attempt ownership
            if (!attempt.getUserId().equals(userId)) {
                throw new IllegalAccessException("Unauthorized: Attempt does not belong to the authenticated user.");
            }

            // Idempotency: if already submitted, do not re-calculate
            if ("submitted".equalsIgnoreCase(attempt.getStatus())) {
                return attempt;
            }

            // Reuse exam metrics pre-loaded in attempt query, or fallback to ExamDAO only if missing
            Double examNegativeMark = attempt.getNegativeMark();
            Integer examPassingPercentage = attempt.getPassingPercentage();

            if (examPassingPercentage == null && examNegativeMark == null) {
                Exam exam = examDAO.findById(con, attempt.getExamId(), false, false);
                if (exam == null) {
                    throw new IllegalArgumentException("Examination not found.");
                }
                examNegativeMark = exam.getNegativeMark();
                examPassingPercentage = exam.getPassingPercentage();
            }

            if (examNegativeMark == null) {
                examNegativeMark = 0.0;
            }
            if (examPassingPercentage == null) {
                examPassingPercentage = 40;
            }

            // Fetch questions WITH correct answers for server-side evaluation (single LEFT JOIN query)
            List<Question> questions = questionDAO.findByExamId(con, attempt.getExamId(), true);

            double score = 0.0;
            int total = 0;
            int correct = 0;
            int wrong = 0;
            int unanswered = 0;
            List<AttemptAnswer> answerRows = new ArrayList<>(questions.size());

            for (Question q : questions) {
                int qMarks = q.getMarks() != null ? q.getMarks() : 1;
                total += qMarks;

                // Support both String and Integer keys (e.g. "201" and 201)
                Object selectedVal = null;
                if (answersMap != null) {
                    selectedVal = answersMap.get(String.valueOf(q.getId()));
                    if (selectedVal == null) {
                        selectedVal = answersMap.get(q.getId());
                    }
                }

                Integer selectedOptionId = null;
                if (selectedVal instanceof Number) {
                    selectedOptionId = ((Number) selectedVal).intValue();
                } else if (selectedVal instanceof String && !((String) selectedVal).isBlank()) {
                    try {
                        selectedOptionId = Integer.parseInt((String) selectedVal);
                    } catch (NumberFormatException ignored) {}
                }

                // Non-positive option IDs (e.g. 0 or -1 used by frontends for cleared choice) indicate unanswered
                if (selectedOptionId != null && selectedOptionId <= 0) {
                    selectedOptionId = null;
                }

                // Find the true correct option and verify option membership
                Option correctOpt = null;
                boolean optionBelongsToQuestion = false;
                if (q.getOptions() != null) {
                    for (Option opt : q.getOptions()) {
                        if (selectedOptionId != null && opt.getId().equals(selectedOptionId)) {
                            optionBelongsToQuestion = true;
                        }
                        if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                            correctOpt = opt;
                        }
                    }
                }

                if (selectedOptionId != null && !optionBelongsToQuestion) {
                    throw new IllegalArgumentException("Selected option " + selectedOptionId + " does not belong to question " + q.getId());
                }

                if (selectedOptionId == null) {
                    unanswered++;
                    answerRows.add(new AttemptAnswer(attemptId, q.getId(), null, false, 0.0));
                } else if (correctOpt != null && selectedOptionId.equals(correctOpt.getId())) {
                    correct++;
                    double marks = (double) qMarks;
                    score += marks;
                    answerRows.add(new AttemptAnswer(attemptId, q.getId(), selectedOptionId, true, marks));
                } else {
                    wrong++;
                    double deduction = 0.0;
                    if (q.getNegativeMarks() != null && q.getNegativeMarks() > 0) {
                        deduction = q.getNegativeMarks();
                    } else if (examNegativeMark != null && examNegativeMark > 0) {
                        deduction = examNegativeMark;
                    }
                    score -= deduction;
                    answerRows.add(new AttemptAnswer(attemptId, q.getId(), selectedOptionId, false, -deduction));
                }
            }

            score = Math.round(score * 100.0) / 100.0;
            double percentage = total > 0 ? Math.round(((score / total) * 100.0) * 100.0) / 100.0 : 0.0;
            int passingThreshold = examPassingPercentage != null ? examPassingPercentage : 40;
            boolean passed = percentage >= passingThreshold;

            Timestamp now = new Timestamp((System.currentTimeMillis() / 1000) * 1000);
            boolean success = attemptDAO.submitAttempt(con, attemptId, now, score, total, correct, wrong,
                                                        unanswered, percentage, passed, answerRows);
            if (!success) {
                // Concurrency: another request already marked it as submitted
                return attemptDAO.findById(con, attemptId);
            }

            // Reuse in-memory attempt object to eliminate redundant database SELECT query
            attempt.setStatus("submitted");
            attempt.setSubmittedAt(DateTimeUtil.formatTimestamp(now));
            attempt.setScore(score);
            attempt.setTotal(total);
            attempt.setCorrect(correct);
            attempt.setWrong(wrong);
            attempt.setUnanswered(unanswered);
            attempt.setPercentage(percentage);
            attempt.setPassed(passed);

            return attempt;
        }
    }
}
