package com.exam.service;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Attempt;
import com.exam.model.AttemptAnswer;
import com.exam.model.Exam;
import com.exam.model.Option;
import com.exam.model.Question;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExamService {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    /**
     * Evaluates a student's examination submission according to the rules in PRD §15.
     * Prevents double-submission and verifies that the attempt belongs to the user.
     */
    public Attempt evaluateAndSubmit(int attemptId, int userId, Map<String, Object> answersMap) throws SQLException, IllegalAccessException {
        Attempt attempt = attemptDAO.findById(attemptId);
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

        Exam exam = examDAO.findById(attempt.getExamId(), false, false);
        if (exam == null) {
            throw new IllegalArgumentException("Examination not found.");
        }

        // Fetch questions WITH correct answers for server-side evaluation
        List<Question> questions = questionDAO.findByExamId(exam.getId(), true);

        double score = 0.0;
        int correct = 0;
        int wrong = 0;
        int unanswered = 0;
        List<AttemptAnswer> answerRows = new ArrayList<>();

        for (Question q : questions) {
            Object selectedVal = answersMap != null ? answersMap.get(String.valueOf(q.getId())) : null;
            Integer selectedOptionId = null;
            if (selectedVal instanceof Number) {
                selectedOptionId = ((Number) selectedVal).intValue();
            } else if (selectedVal instanceof String && !((String) selectedVal).isBlank()) {
                try {
                    selectedOptionId = Integer.parseInt((String) selectedVal);
                } catch (NumberFormatException ignored) {}
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
                double marks = q.getMarks() != null ? q.getMarks() : 1.0;
                score += marks;
                answerRows.add(new AttemptAnswer(attemptId, q.getId(), selectedOptionId, true, marks));
            } else {
                wrong++;
                double deduction = 0.0;
                if (q.getNegativeMarks() != null && q.getNegativeMarks() > 0) {
                    deduction = q.getNegativeMarks();
                } else if (exam.getNegativeMark() != null && exam.getNegativeMark() > 0) {
                    deduction = exam.getNegativeMark();
                }
                score -= deduction;
                answerRows.add(new AttemptAnswer(attemptId, q.getId(), selectedOptionId, false, -deduction));
            }
        }

        score = Math.round(score * 100.0) / 100.0;
        int total = questions.stream().mapToInt(q -> q.getMarks() != null ? q.getMarks() : 1).sum();
        double percentage = total > 0 ? Math.round(((score / total) * 100.0) * 100.0) / 100.0 : 0.0;
        int passingThreshold = exam.getPassingPercentage() != null ? exam.getPassingPercentage() : 40;
        boolean passed = percentage >= passingThreshold;

        boolean success = attemptDAO.submitAttempt(attemptId, score, total, correct, wrong,
                                                    unanswered, percentage, passed, answerRows);
        if (!success) {
            // Concurrency: another request already marked it as submitted
            return attemptDAO.findById(attemptId);
        }

        return attemptDAO.findById(attemptId);
    }
}
