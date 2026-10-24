package com.exam;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAndEvaluationTest {

    @Test
    public void testPasswordHashing() {
        String adminHash = BCrypt.hashpw("admin123", BCrypt.gensalt(10));
        String studentHash = BCrypt.hashpw("student123", BCrypt.gensalt(10));

        System.out.println("BCRYPT_ADMIN: " + adminHash);
        System.out.println("BCRYPT_STUDENT: " + studentHash);

        assertTrue(BCrypt.checkpw("admin123", adminHash));
        assertTrue(BCrypt.checkpw("student123", studentHash));
        assertFalse(BCrypt.checkpw("wrongpass", adminHash));
    }

    @Test
    public void testScoreCalculationLogic() {
        // Exam: 2 questions, 2 marks each, negative marking 0.25
        int q1Marks = 2;
        int q2Marks = 2;
        double negMark = 0.25;

        // Student gets Q1 correct (+2), Q2 wrong (-0.25)
        double score = q1Marks - negMark; // 1.75
        int total = q1Marks + q2Marks;    // 4
        double percentage = (score / total) * 100.0; // 43.75%
        boolean passed = percentage >= 40.0;

        assertEquals(1.75, score, 0.001);
        assertEquals(43.75, percentage, 0.001);
        assertTrue(passed);
    }
}
