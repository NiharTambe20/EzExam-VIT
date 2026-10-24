package com.exam;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.dao.UserDAO;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.model.User;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndAuthorizationTest {

    private final UserDAO userDAO = new UserDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    @Test
    public void testAuthenticationAndRoles() throws SQLException {
        // Admin credentials
        User admin = userDAO.findByEmail("admin@ezexam.com");
        assertNotNull(admin);
        assertEquals("admin", admin.getRole());
        assertTrue(userDAO.authenticate(admin, "admin123"));
        assertFalse(userDAO.authenticate(admin, "wrongpass"));

        // Student credentials
        User student = userDAO.findByEmail("student@ezexam.com");
        assertNotNull(student);
        assertEquals("student", student.getRole());
        assertTrue(userDAO.authenticate(student, "student123"));
        assertFalse(userDAO.authenticate(student, "wrongpass"));
    }

    @Test
    public void testQuestionOptionAnswerLeakageProtection() throws SQLException {
        List<Exam> exams = examDAO.findAll(true);
        assertFalse(exams.isEmpty());
        int examId = exams.get(0).getId();

        // Student perspective (includeAnswers = false)
        List<Question> studentQuestions = questionDAO.findByExamId(examId, false);
        assertFalse(studentQuestions.isEmpty());
        for (Question q : studentQuestions) {
            for (var opt : q.getOptions()) {
                assertNull(opt.getIsCorrect(), "Students MUST NOT receive is_correct values before submission");
            }
        }

        // Admin perspective (includeAnswers = true)
        List<Question> adminQuestions = questionDAO.findByExamId(examId, true);
        assertFalse(adminQuestions.isEmpty());
        for (Question q : adminQuestions) {
            boolean hasCorrect = q.getOptions().stream().anyMatch(o -> Boolean.TRUE.equals(o.getIsCorrect()));
            assertTrue(hasCorrect, "Admin view must see the correct answer flag");
        }
    }

    @Test
    public void testStudentHistoryAndAdminStats() throws SQLException {
        // Student history query
        User student = userDAO.findByEmail("student@ezexam.com");
        assertNotNull(student);
        List<Attempt> history = attemptDAO.findByUserId(student.getId());
        assertNotNull(history);

        // Admin statistics queries
        int studentCount = userDAO.countByRole("student");
        int totalExams = examDAO.countAll();
        int publishedExams = examDAO.countPublished();
        int totalAttempts = attemptDAO.countAll();

        assertTrue(studentCount >= 1);
        assertTrue(totalExams >= 1);
        assertTrue(publishedExams >= 1);
        assertTrue(totalAttempts >= 1);

        List<Map<String, Object>> students = userDAO.getStudentsWithAttemptCounts();
        assertFalse(students.isEmpty());
    }
}
