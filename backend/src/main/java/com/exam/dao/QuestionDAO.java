package com.exam.dao;

import com.exam.model.Option;
import com.exam.model.Question;
import com.exam.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionDAO {

    public List<Question> findByExamId(int examId, boolean includeAnswers) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            return findByExamId(con, examId, includeAnswers);
        }
    }

    public List<Question> findByExamId(Connection con, int examId, boolean includeAnswers) throws SQLException {
        String sql = "SELECT q.id AS q_id, q.exam_id AS q_exam_id, q.question_text AS q_question_text, " +
                     "q.marks AS q_marks, q.negative_marks AS q_negative_marks, " +
                     "o.id AS o_id, o.question_id AS o_question_id, o.text AS o_text, o.is_correct AS o_is_correct " +
                     "FROM questions q " +
                     "LEFT JOIN options o ON q.id = o.question_id " +
                     "WHERE q.exam_id = ? " +
                     "ORDER BY q.id ASC, o.id ASC";

        Map<Integer, Question> questionMap = new LinkedHashMap<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qId = rs.getInt("q_id");
                    Question q = questionMap.get(qId);
                    if (q == null) {
                        q = new Question();
                        q.setId(qId);
                        q.setExamId(rs.getInt("q_exam_id"));
                        q.setQuestionText(rs.getString("q_question_text"));
                        q.setMarks(rs.getInt("q_marks"));
                        q.setNegativeMarks(rs.getDouble("q_negative_marks"));
                        questionMap.put(qId, q);
                    }

                    int optId = rs.getInt("o_id");
                    if (!rs.wasNull()) {
                        Option opt = new Option();
                        opt.setId(optId);
                        opt.setQuestionId(qId);
                        opt.setText(rs.getString("o_text"));
                        if (includeAnswers) {
                            opt.setIsCorrect(rs.getBoolean("o_is_correct"));
                        } else {
                            opt.setIsCorrect(null); // Never leak is_correct to students!
                        }
                        q.getOptions().add(opt);
                    }
                }
            }
        }
        return new ArrayList<>(questionMap.values());
    }

    public Question findById(int id, boolean includeAnswers) throws SQLException {
        String sql = "SELECT q.id AS q_id, q.exam_id AS q_exam_id, q.question_text AS q_question_text, " +
                     "q.marks AS q_marks, q.negative_marks AS q_negative_marks, " +
                     "o.id AS o_id, o.question_id AS o_question_id, o.text AS o_text, o.is_correct AS o_is_correct " +
                     "FROM questions q " +
                     "LEFT JOIN options o ON q.id = o.question_id " +
                     "WHERE q.id = ? " +
                     "ORDER BY o.id ASC";

        Question q = null;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (q == null) {
                        q = new Question();
                        q.setId(rs.getInt("q_id"));
                        q.setExamId(rs.getInt("q_exam_id"));
                        q.setQuestionText(rs.getString("q_question_text"));
                        q.setMarks(rs.getInt("q_marks"));
                        q.setNegativeMarks(rs.getDouble("q_negative_marks"));
                    }

                    int optId = rs.getInt("o_id");
                    if (!rs.wasNull()) {
                        Option opt = new Option();
                        opt.setId(optId);
                        opt.setQuestionId(q.getId());
                        opt.setText(rs.getString("o_text"));
                        if (includeAnswers) {
                            opt.setIsCorrect(rs.getBoolean("o_is_correct"));
                        } else {
                            opt.setIsCorrect(null); // Never leak is_correct to students!
                        }
                        q.getOptions().add(opt);
                    }
                }
            }
        }
        return q;
    }

    public Question create(int examId, String questionText, int marks, double negativeMarks,
                           List<String> optionTexts, int correctIndex) throws SQLException {
        String insertQ = "INSERT INTO questions (exam_id, question_text, marks, negative_marks) VALUES (?, ?, ?, ?)";
        String insertOpt = "INSERT INTO options (question_id, text, is_correct) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int questionId;
                try (PreparedStatement psQ = con.prepareStatement(insertQ, Statement.RETURN_GENERATED_KEYS)) {
                    psQ.setInt(1, examId);
                    psQ.setString(2, questionText);
                    psQ.setInt(3, marks);
                    psQ.setDouble(4, negativeMarks);
                    psQ.executeUpdate();
                    try (ResultSet rs = psQ.getGeneratedKeys()) {
                        if (rs.next()) {
                            questionId = rs.getInt(1);
                        } else {
                            throw new SQLException("Failed to obtain generated question ID.");
                        }
                    }
                }

                if (optionTexts != null) {
                    try (PreparedStatement psOpt = con.prepareStatement(insertOpt)) {
                        for (int i = 0; i < optionTexts.size(); i++) {
                            psOpt.setInt(1, questionId);
                            psOpt.setString(2, optionTexts.get(i));
                            psOpt.setBoolean(3, i == correctIndex);
                            psOpt.addBatch();
                        }
                        psOpt.executeBatch();
                    }
                }

                con.commit();
                return findById(questionId, true);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public Question update(int questionId, String questionText, int marks, double negativeMarks,
                           List<String> optionTexts, int correctIndex) throws SQLException {
        String updateQ = "UPDATE questions SET question_text = ?, marks = ?, negative_marks = ? WHERE id = ?";
        String deleteOpts = "DELETE FROM options WHERE question_id = ?";
        String insertOpt = "INSERT INTO options (question_id, text, is_correct) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement psQ = con.prepareStatement(updateQ)) {
                    psQ.setString(1, questionText);
                    psQ.setInt(2, marks);
                    psQ.setDouble(3, negativeMarks);
                    psQ.setInt(4, questionId);
                    psQ.executeUpdate();
                }

                if (optionTexts != null && !optionTexts.isEmpty()) {
                    try (PreparedStatement psDel = con.prepareStatement(deleteOpts)) {
                        psDel.setInt(1, questionId);
                        psDel.executeUpdate();
                    }

                    try (PreparedStatement psOpt = con.prepareStatement(insertOpt)) {
                        for (int i = 0; i < optionTexts.size(); i++) {
                            psOpt.setInt(1, questionId);
                            psOpt.setString(2, optionTexts.get(i));
                            psOpt.setBoolean(3, i == correctIndex);
                            psOpt.addBatch();
                        }
                        psOpt.executeBatch();
                    }
                }

                con.commit();
                return findById(questionId, true);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public boolean delete(int questionId) throws SQLException {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            return ps.executeUpdate() > 0;
        }
    }
}
