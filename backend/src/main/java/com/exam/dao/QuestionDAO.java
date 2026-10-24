package com.exam.dao;

import com.exam.model.Option;
import com.exam.model.Question;
import com.exam.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    public List<Question> findByExamId(int examId, boolean includeAnswers) throws SQLException {
        String qSql = "SELECT id, exam_id, question_text, marks, negative_marks FROM questions WHERE exam_id = ? ORDER BY id ASC";
        String optSql = "SELECT id, question_id, text, is_correct FROM options WHERE question_id = ? ORDER BY id ASC";

        List<Question> questions = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement psQ = con.prepareStatement(qSql)) {
            psQ.setInt(1, examId);
            try (ResultSet rsQ = psQ.executeQuery()) {
                while (rsQ.next()) {
                    Question q = new Question();
                    int qId = rsQ.getInt("id");
                    q.setId(qId);
                    q.setExamId(rsQ.getInt("exam_id"));
                    q.setQuestionText(rsQ.getString("question_text"));
                    q.setMarks(rsQ.getInt("marks"));
                    q.setNegativeMarks(rsQ.getDouble("negative_marks"));

                    // Fetch child options
                    try (PreparedStatement psOpt = con.prepareStatement(optSql)) {
                        psOpt.setInt(1, qId);
                        try (ResultSet rsOpt = psOpt.executeQuery()) {
                            List<Option> options = new ArrayList<>();
                            while (rsOpt.next()) {
                                Option opt = new Option();
                                opt.setId(rsOpt.getInt("id"));
                                opt.setQuestionId(qId);
                                opt.setText(rsOpt.getString("text"));
                                if (includeAnswers) {
                                    opt.setIsCorrect(rsOpt.getBoolean("is_correct"));
                                } else {
                                    opt.setIsCorrect(null); // Never leak is_correct to students!
                                }
                                options.add(opt);
                            }
                            q.setOptions(options);
                        }
                    }
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    public Question findById(int id, boolean includeAnswers) throws SQLException {
        String qSql = "SELECT id, exam_id, question_text, marks, negative_marks FROM questions WHERE id = ?";
        String optSql = "SELECT id, question_id, text, is_correct FROM options WHERE question_id = ? ORDER BY id ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement psQ = con.prepareStatement(qSql)) {
            psQ.setInt(1, id);
            try (ResultSet rsQ = psQ.executeQuery()) {
                if (rsQ.next()) {
                    Question q = new Question();
                    q.setId(rsQ.getInt("id"));
                    q.setExamId(rsQ.getInt("exam_id"));
                    q.setQuestionText(rsQ.getString("question_text"));
                    q.setMarks(rsQ.getInt("marks"));
                    q.setNegativeMarks(rsQ.getDouble("negative_marks"));

                    try (PreparedStatement psOpt = con.prepareStatement(optSql)) {
                        psOpt.setInt(1, id);
                        try (ResultSet rsOpt = psOpt.executeQuery()) {
                            List<Option> options = new ArrayList<>();
                            while (rsOpt.next()) {
                                Option opt = new Option();
                                opt.setId(rsOpt.getInt("id"));
                                opt.setQuestionId(id);
                                opt.setText(rsOpt.getString("text"));
                                if (includeAnswers) {
                                    opt.setIsCorrect(rsOpt.getBoolean("is_correct"));
                                } else {
                                    opt.setIsCorrect(null);
                                }
                                options.add(opt);
                            }
                            q.setOptions(options);
                        }
                    }
                    return q;
                }
            }
        }
        return null;
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
