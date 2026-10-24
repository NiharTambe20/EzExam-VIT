package com.exam;

import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Exam;
import com.exam.model.Option;
import com.exam.model.Question;
import com.exam.util.DBConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuestionDAOTest {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();

    private Exam testExam;
    private Question qWithOptions;
    private Question qWithoutOptions;

    @BeforeEach
    public void setUp() throws SQLException {
        Exam exam = new Exam();
        exam.setTitle("QuestionDAO Test Exam");
        exam.setDescription("Testing single JOIN question/option retrieval");
        exam.setDurationMinutes(20);
        exam.setTotalMarks(10);
        exam.setPassingPercentage(40);
        exam.setStatus("draft");
        testExam = examDAO.create(exam);

        // Q1: Question with 4 options (Option index 2 is correct)
        qWithOptions = questionDAO.create(testExam.getId(), "Sample Question 1", 2, 0.25,
                List.of("Option A", "Option B", "Option C", "Option D"), 2);

        // Q2: Question with NO options (directly insert into questions table without options)
        String sql = "INSERT INTO questions (exam_id, question_text, marks, negative_marks) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, testExam.getId());
            ps.setString(2, "Question Without Options");
            ps.setInt(3, 1);
            ps.setDouble(4, 0.0);
            ps.executeUpdate();
            var rs = ps.getGeneratedKeys();
            if (rs.next()) {
                qWithoutOptions = new Question();
                qWithoutOptions.setId(rs.getInt(1));
            }
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (testExam != null) {
            examDAO.delete(testExam.getId());
        }
    }

    @Test
    public void testFindByExamIdDeduplicationAndOptionOrdering() throws SQLException {
        // Test student view (includeAnswers = false)
        List<Question> questions = questionDAO.findByExamId(testExam.getId(), false);

        assertNotNull(questions, "Result list should not be null");
        assertEquals(2, questions.size(), "Should have exactly 2 distinct questions (no duplicates from JOIN)");

        // Verify Question ordering (q.id ASC)
        assertTrue(questions.get(0).getId() < questions.get(1).getId(), "Questions must be sorted by ID ASC");

        // Verify Q1 has 4 options
        Question q1 = questions.get(0);
        assertEquals(4, q1.getOptions().size(), "Q1 should have exactly 4 child options");

        // Verify option ordering (o.id ASC)
        for (int i = 0; i < q1.getOptions().size() - 1; i++) {
            assertTrue(q1.getOptions().get(i).getId() < q1.getOptions().get(i + 1).getId(),
                    "Options must be sorted in ascending order of option ID");
        }

        // Verify answer visibility: Student MUST NOT see is_correct
        for (Option opt : q1.getOptions()) {
            assertNull(opt.getIsCorrect(), "is_correct must be null when includeAnswers is false");
        }

        // Verify Q2 has 0 options (empty list, not null)
        Question q2 = questions.get(1);
        assertNotNull(q2.getOptions(), "Options list for question without options must not be null");
        assertTrue(q2.getOptions().isEmpty(), "Question without options should have empty options list");
    }

    @Test
    public void testFindByExamIdAdminIncludeAnswers() throws SQLException {
        // Test admin/grading view (includeAnswers = true)
        List<Question> questions = questionDAO.findByExamId(testExam.getId(), true);

        assertEquals(2, questions.size());
        Question q1 = questions.get(0);

        // Verify exactly one option is marked correct
        long correctCount = q1.getOptions().stream()
                .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                .count();
        assertEquals(1, correctCount, "Exactly one option should be marked correct");

        // Verify the 3rd option (index 2) is the correct one ("Option C")
        Option correctOpt = q1.getOptions().get(2);
        assertTrue(Boolean.TRUE.equals(correctOpt.getIsCorrect()), "Index 2 option must be correct");
        assertEquals("Option C", correctOpt.getText());
    }

    @Test
    public void testFindByIdSingleJoin() throws SQLException {
        // Admin view
        Question adminQ = questionDAO.findById(qWithOptions.getId(), true);
        assertNotNull(adminQ);
        assertEquals(4, adminQ.getOptions().size());
        assertTrue(adminQ.getOptions().stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect())));

        // Student view
        Question studentQ = questionDAO.findById(qWithOptions.getId(), false);
        assertNotNull(studentQ);
        assertEquals(4, studentQ.getOptions().size());
        studentQ.getOptions().forEach(opt -> assertNull(opt.getIsCorrect()));

        // Question with no options
        Question emptyQ = questionDAO.findById(qWithoutOptions.getId(), false);
        assertNotNull(emptyQ);
        assertTrue(emptyQ.getOptions().isEmpty());

        // Non-existent question
        Question nonExistent = questionDAO.findById(-999, false);
        assertNull(nonExistent);
    }
}
