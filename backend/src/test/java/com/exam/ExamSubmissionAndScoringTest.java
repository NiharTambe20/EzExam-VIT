package com.exam;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Attempt;
import com.exam.model.AttemptAnswer;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.service.ExamService;
import com.exam.util.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExamSubmissionAndScoringTest {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final ExamService examService = new ExamService();

    private Exam testExam;
    private Question q1;
    private Question q2;
    private Question q3;
    private final int studentUserId = 2; // Demo student user

    @BeforeEach
    public void setUp() throws SQLException {
        // Create an isolated exam for testing evaluation, negative marking, and transactions
        Exam exam = new Exam();
        exam.setTitle("Submission & Scoring Test Exam");
        exam.setDescription("Testing evaluation, persistence, idempotency, and rollback");
        exam.setDurationMinutes(20);
        exam.setTotalMarks(6);
        exam.setPassingPercentage(50);
        exam.setNegativeMark(0.5);
        exam.setStatus("published");
        testExam = examDAO.create(exam);

        // Q1: 2 marks, negative 0.5 (Option index 1 is correct)
        q1 = questionDAO.create(testExam.getId(), "Q1: Java inheritance keyword?", 2, 0.5,
                List.of("implements", "extends", "inherits", "super"), 1); // "extends" is correct

        // Q2: 2 marks, negative 0.5 (Option index 2 is correct)
        q2 = questionDAO.create(testExam.getId(), "Q2: Non-duplicate collection?", 2, 0.5,
                List.of("ArrayList", "LinkedList", "HashSet", "Vector"), 2); // "HashSet" is correct

        // Q3: 2 marks, negative 0.5 (Option index 1 is correct)
        q3 = questionDAO.create(testExam.getId(), "Q3: Default int value?", 2, 0.5,
                List.of("null", "0", "undefined", "1"), 1); // "0" is correct
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (testExam != null) {
            examDAO.delete(testExam.getId());
        }
    }

    @Test
    public void testAllCorrectSubmissionAndPersistence() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);
        assertEquals("in-progress", attempt.getStatus());

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1.getOptions().get(1).getId()); // "extends"
        answers.put(String.valueOf(q2.getId()), q2.getOptions().get(2).getId()); // "HashSet"
        answers.put(String.valueOf(q3.getId()), q3.getOptions().get(1).getId()); // "0"

        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertEquals(3, result.getCorrect());
        assertEquals(0, result.getWrong());
        assertEquals(0, result.getUnanswered());
        assertEquals(6.0, result.getScore(), 0.01);
        assertEquals(6, result.getTotal());
        assertEquals(100.0, result.getPercentage(), 0.01);
        assertTrue(result.getPassed());

        // Verify persistence in attempt_answers table
        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size());
        for (AttemptAnswerRecord row : rows) {
            assertTrue(row.isCorrect);
            assertEquals(2.0, row.marksAwarded, 0.01);
            assertNotNull(row.selectedOptionId);
        }
    }

    @Test
    public void testMixedSubmissionCorrectWrongUnanswered() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1.getOptions().get(1).getId()); // Q1 correct (+2.0)
        answers.put(String.valueOf(q2.getId()), q2.getOptions().get(0).getId()); // Q2 wrong (-0.5)
        // Q3 left unanswered (0.0)

        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertEquals(1, result.getCorrect());
        assertEquals(1, result.getWrong());
        assertEquals(1, result.getUnanswered());
        assertEquals(1.5, result.getScore(), 0.01); // 2.0 - 0.5 = 1.5
        assertEquals(6, result.getTotal());
        assertEquals(25.0, result.getPercentage(), 0.01);
        assertFalse(result.getPassed(), "25% is below 50% passing threshold");

        // Verify rows in attempt_answers
        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size());

        AttemptAnswerRecord r1 = findRowForQuestion(rows, q1.getId());
        assertNotNull(r1);
        assertTrue(r1.isCorrect);
        assertEquals(2.0, r1.marksAwarded, 0.01);
        assertEquals(q1.getOptions().get(1).getId(), r1.selectedOptionId);

        AttemptAnswerRecord r2 = findRowForQuestion(rows, q2.getId());
        assertNotNull(r2);
        assertFalse(r2.isCorrect);
        assertEquals(-0.5, r2.marksAwarded, 0.01);
        assertEquals(q2.getOptions().get(0).getId(), r2.selectedOptionId);

        AttemptAnswerRecord r3 = findRowForQuestion(rows, q3.getId());
        assertNotNull(r3);
        assertFalse(r3.isCorrect);
        assertEquals(0.0, r3.marksAwarded, 0.01);
        assertNull(r3.selectedOptionId, "Unanswered question must store NULL selected_option_id");
    }

    @Test
    public void testAllUnansweredSubmissionReplicatesScoringDiscrepancy() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);

        // Submit empty answers map (reproducing what server received when scoring discrepancy occurred)
        Map<String, Object> emptyAnswers = new HashMap<>();
        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, emptyAnswers);

        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertEquals(0, result.getCorrect());
        assertEquals(0, result.getWrong());
        assertEquals(3, result.getUnanswered());
        assertEquals(0.0, result.getScore(), 0.01);
        assertEquals(6, result.getTotal());
        assertEquals(0.0, result.getPercentage(), 0.01);
        assertFalse(result.getPassed());

        // Verify all 3 rows persisted with null selected_option_id
        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size());
        for (AttemptAnswerRecord row : rows) {
            assertNull(row.selectedOptionId);
            assertFalse(row.isCorrect);
            assertEquals(0.0, row.marksAwarded, 0.01);
        }
    }

    @Test
    public void testQuestion1OptionCSelectedProvesDiscrepancyDiagnosis() throws Exception {
        // If the submitted payload had actually contained Option C ("inherits"),
        // wrong would be 1 and unanswered would be 2, NOT 3 unanswered!
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        Integer q1OptionCId = q1.getOptions().get(2).getId(); // Option C: "inherits" (wrong)
        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1OptionCId);

        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertEquals(0, result.getCorrect());
        assertEquals(1, result.getWrong(), "Option C ('inherits') is wrong, so wrong MUST be 1");
        assertEquals(2, result.getUnanswered(), "Only remaining 2 questions are unanswered");
        assertEquals(-0.5, result.getScore(), 0.01);
    }

    @Test
    public void testDuplicateSubmissionIdempotency() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1.getOptions().get(1).getId());

        // First submission
        Attempt first = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertEquals("submitted", first.getStatus());
        double firstScore = first.getScore();
        String firstSubmittedAt = first.getSubmittedAt();

        // Second submission attempt with altered payload
        Map<String, Object> alteredAnswers = new HashMap<>();
        alteredAnswers.put(String.valueOf(q1.getId()), q1.getOptions().get(0).getId());
        alteredAnswers.put(String.valueOf(q2.getId()), q2.getOptions().get(2).getId());

        Attempt second = examService.evaluateAndSubmit(attempt.getId(), studentUserId, alteredAnswers);
        assertEquals("submitted", second.getStatus());
        assertEquals(firstScore, second.getScore(), 0.01, "Duplicate submission must return original score");
        assertEquals(firstSubmittedAt, second.getSubmittedAt(), "Submission timestamp must not change");

        // Verify attempt_answers table was not duplicated
        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size(), "Duplicate submission must not add duplicate answer rows");
    }

    @Test
    public void testAuthorizationRejection() throws SQLException {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        int unauthorizedUserId = 999;
        Map<String, Object> answers = new HashMap<>();

        assertThrows(IllegalAccessException.class, () -> {
            examService.evaluateAndSubmit(attempt.getId(), unauthorizedUserId, answers);
        });

        // Verify attempt is still in-progress
        Attempt fresh = attemptDAO.findById(attempt.getId());
        assertEquals("in-progress", fresh.getStatus());
    }

    @Test
    public void testInvalidOptionRejectionDoesNotModifyAttempt() throws SQLException {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        // Put an invalid option ID that does not belong to Q1
        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), 999999);

        assertThrows(IllegalArgumentException.class, () -> {
            examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        });

        Attempt fresh = attemptDAO.findById(attempt.getId());
        assertEquals("in-progress", fresh.getStatus(), "Failed validation must leave attempt in-progress");

        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(0, rows.size(), "No answer records should be saved on validation failure");
    }

    @Test
    public void testTransactionRollbackOnBatchInsertFailure() throws SQLException {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertEquals("in-progress", attempt.getStatus());

        // Prepare answer rows where one row causes a foreign key constraint failure
        // (question_id 999999 does not exist in questions table)
        List<AttemptAnswer> invalidAnswers = new ArrayList<>();
        invalidAnswers.add(new AttemptAnswer(attempt.getId(), q1.getId(), q1.getOptions().get(0).getId(), false, 0.0));
        invalidAnswers.add(new AttemptAnswer(attempt.getId(), 999999, null, false, 0.0)); // FK violation!

        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection con = DBConnection.getConnection()) {
            assertThrows(SQLException.class, () -> {
                attemptDAO.submitAttempt(con, attempt.getId(), now, 2.0, 6, 1, 0, 2, 33.33, false, invalidAnswers);
            });
        }

        // Verify transaction was rolled back: attempt must still be in-progress and no partial answers saved
        Attempt refreshed = attemptDAO.findById(attempt.getId());
        assertEquals("in-progress", refreshed.getStatus(), "Attempt status must remain in-progress after rollback");
        assertNull(refreshed.getSubmittedAt(), "submitted_at must remain null after rollback");

        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(0, rows.size(), "No answer rows should remain committed after rollback");
    }

    @Test
    public void testAllWrongSubmissionAndNegativeMarksPersistence() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1.getOptions().get(0).getId()); // "implements" (wrong, -0.5)
        answers.put(String.valueOf(q2.getId()), q2.getOptions().get(0).getId()); // "ArrayList" (wrong, -0.5)
        answers.put(String.valueOf(q3.getId()), q3.getOptions().get(0).getId()); // "null" (wrong, -0.5)

        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertEquals(0, result.getCorrect());
        assertEquals(3, result.getWrong());
        assertEquals(0, result.getUnanswered());
        assertEquals(-1.5, result.getScore(), 0.01);
        assertEquals(6, result.getTotal());
        assertEquals(-25.0, result.getPercentage(), 0.01);
        assertFalse(result.getPassed());

        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size());
        for (AttemptAnswerRecord r : rows) {
            assertNotNull(r.selectedOptionId);
            assertFalse(r.isCorrect);
            assertEquals(-0.5, r.marksAwarded, 0.01);
        }
    }

    @Test
    public void testClearedOptionTreatedAsUnanswered() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1.getOptions().get(1).getId()); // Q1 correct (+2.0)
        answers.put(String.valueOf(q2.getId()), 0);  // Cleared choice option 0
        answers.put(String.valueOf(q3.getId()), -1); // Cleared choice option -1

        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(result);
        assertEquals(1, result.getCorrect());
        assertEquals(0, result.getWrong());
        assertEquals(2, result.getUnanswered());
        assertEquals(2.0, result.getScore(), 0.01);

        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size());
        AttemptAnswerRecord r2 = findRowForQuestion(rows, q2.getId());
        assertNotNull(r2);
        assertNull(r2.selectedOptionId, "Option ID 0 must be persisted as null");
        assertEquals(0.0, r2.marksAwarded, 0.01);

        AttemptAnswerRecord r3 = findRowForQuestion(rows, q3.getId());
        assertNotNull(r3);
        assertNull(r3.selectedOptionId, "Option ID -1 must be persisted as null");
        assertEquals(0.0, r3.marksAwarded, 0.01);
    }

    @Test
    public void testConcurrentDuplicateSubmissionThreadSafety() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        Map<String, Object> answersA = new HashMap<>();
        answersA.put(String.valueOf(q1.getId()), q1.getOptions().get(1).getId());

        Map<String, Object> answersB = new HashMap<>();
        answersB.put(String.valueOf(q1.getId()), q1.getOptions().get(0).getId());

        // Concurrently invoke evaluateAndSubmit
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Future<Attempt> f1 = executor.submit(() -> examService.evaluateAndSubmit(attempt.getId(), studentUserId, answersA));
        java.util.concurrent.Future<Attempt> f2 = executor.submit(() -> examService.evaluateAndSubmit(attempt.getId(), studentUserId, answersB));

        Attempt r1 = f1.get();
        Attempt r2 = f2.get();
        executor.shutdown();

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals("submitted", r1.getStatus());
        assertEquals("submitted", r2.getStatus());

        // Verify that attempt_answers has exactly 3 rows and not duplicated (no phantom/double batch writes)
        List<AttemptAnswerRecord> rows = fetchAttemptAnswers(attempt.getId());
        assertEquals(3, rows.size(), "Concurrent submission must not duplicate rows in attempt_answers");
    }

    // Helper classes and methods for verifying database state
    private static class AttemptAnswerRecord {
        int questionId;
        Integer selectedOptionId;
        boolean isCorrect;
        double marksAwarded;
    }

    private List<AttemptAnswerRecord> fetchAttemptAnswers(int attemptId) throws SQLException {
        String sql = "SELECT question_id, selected_option_id, is_correct, marks_awarded FROM attempt_answers WHERE attempt_id = ? ORDER BY question_id ASC";
        List<AttemptAnswerRecord> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AttemptAnswerRecord r = new AttemptAnswerRecord();
                    r.questionId = rs.getInt("question_id");
                    int optId = rs.getInt("selected_option_id");
                    r.selectedOptionId = rs.wasNull() ? null : optId;
                    r.isCorrect = rs.getBoolean("is_correct");
                    r.marksAwarded = rs.getDouble("marks_awarded");
                    list.add(r);
                }
            }
        }
        return list;
    }

    private AttemptAnswerRecord findRowForQuestion(List<AttemptAnswerRecord> rows, int questionId) {
        for (AttemptAnswerRecord r : rows) {
            if (r.questionId == questionId) return r;
        }
        return null;
    }
}
