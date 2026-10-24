package com.exam;

import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.model.Exam;
import com.exam.model.Question;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExamLifecycleAndDateTest {

    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();

    @Test
    public void testExamCreationWithOmittedOptionalDates() throws SQLException {
        Exam exam = new Exam();
        exam.setTitle("Automated Test Exam - Blank Dates");
        exam.setDescription("Testing handling of empty start_time and end_time");
        exam.setDurationMinutes(25);
        exam.setTotalMarks(10);
        exam.setPassingPercentage(50);
        exam.setNegativeMark(0.25);
        exam.setStatus("draft");
        exam.setStartTime(""); // Empty string reproducing Bug #1
        exam.setEndTime(null);

        Exam created = examDAO.create(exam);
        assertNotNull(created, "Exam should be created without SQL data truncation error");
        assertNotNull(created.getId());
        assertNull(created.getStartTime(), "Start time should be SQL NULL and Java null");
        assertNull(created.getEndTime(), "End time should be SQL NULL and Java null");

        // Test editing exam with empty dates
        created.setTitle("Automated Test Exam - Updated");
        created.setStartTime("");
        created.setEndTime("");
        Exam updated = examDAO.update(created.getId(), created);
        assertNotNull(updated);
        assertEquals("Automated Test Exam - Updated", updated.getTitle());
        assertNull(updated.getStartTime());
        assertNull(updated.getEndTime());

        // Test question creation for this exam
        List<String> options = List.of("Option A", "Option B", "Option C", "Option D");
        Question q = questionDAO.create(created.getId(), "What is 2 + 2?", 2, 0.5, options, 1);
        assertNotNull(q);
        assertEquals(2, q.getMarks());
        assertEquals(0.5, q.getNegativeMarks());
        assertEquals(4, q.getOptions().size());
        assertTrue(q.getOptions().get(1).getIsCorrect());
        assertFalse(q.getOptions().get(0).getIsCorrect());

        // Cleanup test exam
        boolean deleted = examDAO.delete(created.getId());
        assertTrue(deleted, "Test exam should be deleted cleanly");
        assertNull(examDAO.findById(created.getId(), false, false));
    }

    @Test
    public void testExamCreationWithValidDates() throws SQLException {
        Exam exam = new Exam();
        exam.setTitle("Automated Test Exam - Valid Dates");
        exam.setDescription("Testing valid ISO dates");
        exam.setDurationMinutes(45);
        exam.setTotalMarks(20);
        exam.setPassingPercentage(40);
        exam.setNegativeMark(0.0);
        exam.setStatus("draft");
        exam.setStartTime("2026-11-01T10:00:00");
        exam.setEndTime("2026-11-30T18:00:00");

        Exam created = examDAO.create(exam);
        assertNotNull(created);
        assertNotNull(created.getStartTime());
        assertNotNull(created.getEndTime());
        assertTrue(created.getStartTime().startsWith("2026-11-01 10:00:00"));
        assertTrue(created.getEndTime().startsWith("2026-11-30 18:00:00"));

        // Cleanup
        examDAO.delete(created.getId());
    }
}
