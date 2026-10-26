package com.exam;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.service.ExamService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExamServiceWorkflowTest {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final ExamService examService = new ExamService();

    private Exam testExam;
    private Question q1;
    private Question q2;
    private int studentUserId = 2; // Demo student user

    @BeforeEach
    public void setUp() throws SQLException {
        // Create an isolated exam for testing
        Exam exam = new Exam();
        exam.setTitle("ExamService Test Exam");
        exam.setDescription("Testing evaluation and submission workflow");
        exam.setDurationMinutes(15);
        exam.setTotalMarks(4);
        exam.setPassingPercentage(50);
        exam.setNegativeMark(0.5);
        exam.setStatus("published");
        testExam = examDAO.create(exam);

        // Q1: 2 marks, 0.5 negative
        q1 = questionDAO.create(testExam.getId(), "Question 1 Text", 2, 0.5,
                List.of("Q1-A", "Q1-B", "Q1-C", "Q1-D"), 1); // Q1-B is correct

        // Q2: 2 marks, 0.5 negative
        q2 = questionDAO.create(testExam.getId(), "Question 2 Text", 2, 0.5,
                List.of("Q2-A", "Q2-B", "Q2-C", "Q2-D"), 2); // Q2-C is correct
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (testExam != null) {
            // Deleting exam cascades to questions, options, attempts, attempt_answers
            examDAO.delete(testExam.getId());
        }
    }

    @Test
    public void testSuccessfulEvaluationAndSubmission() throws Exception {
        // Step 1: Start attempt
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);
        assertEquals("in-progress", attempt.getStatus());

        // Step 2: Answer Q1 correctly, Q2 wrong
        Integer q1CorrectOptionId = q1.getOptions().get(1).getId();
        Integer q2WrongOptionId = q2.getOptions().get(0).getId(); // Q2-A is wrong

        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q1CorrectOptionId);
        answers.put(String.valueOf(q2.getId()), q2WrongOptionId);

        // Step 3: Evaluate and submit
        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(result);
        assertEquals("submitted", result.getStatus());
        assertEquals(1, result.getCorrect());
        assertEquals(1, result.getWrong());
        assertEquals(0, result.getUnanswered());

        // Score: +2 (for Q1) - 0.5 (for Q2) = 1.5
        assertEquals(1.5, result.getScore(), 0.01);
        assertEquals(4, result.getTotal());
        assertEquals(37.5, result.getPercentage(), 0.01);
        assertFalse(result.getPassed(), "37.5% is below 50% threshold, so should fail");

        // Step 4: Duplicate submission idempotency
        Attempt duplicate = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        assertNotNull(duplicate);
        assertEquals("submitted", duplicate.getStatus());
        assertEquals(1.5, duplicate.getScore(), 0.01);
    }

    @Test
    public void testUnansweredQuestionsHandling() throws Exception {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);
        assertNotNull(attempt);

        // Submit empty answers
        Map<String, Object> answers = new HashMap<>();
        Attempt result = examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);

        assertEquals("submitted", result.getStatus());
        assertEquals(0, result.getCorrect());
        assertEquals(0, result.getWrong());
        assertEquals(2, result.getUnanswered());
        assertEquals(0.0, result.getScore(), 0.01);
        assertFalse(result.getPassed());
    }

    @Test
    public void testRejectInvalidOptionNotBelongingToQuestion() throws SQLException {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        // Try to submit Q2's option for Q1
        Integer q2OptionId = q2.getOptions().get(0).getId();
        Map<String, Object> answers = new HashMap<>();
        answers.put(String.valueOf(q1.getId()), q2OptionId);

        assertThrows(IllegalArgumentException.class, () -> {
            examService.evaluateAndSubmit(attempt.getId(), studentUserId, answers);
        });
    }

    @Test
    public void testRejectSubmissionByAnotherStudent() throws SQLException {
        Attempt attempt = attemptDAO.create(testExam.getId(), studentUserId);

        // Another user (e.g., ID 999) tries to submit student's attempt
        Map<String, Object> answers = new HashMap<>();
        assertThrows(IllegalAccessException.class, () -> {
            examService.evaluateAndSubmit(attempt.getId(), 999, answers);
        });
    }
}
