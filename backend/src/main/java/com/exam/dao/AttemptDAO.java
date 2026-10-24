package com.exam.dao;

import com.exam.model.Attempt;
import com.exam.model.AttemptAnswer;
import com.exam.util.DBConnection;
import com.exam.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttemptDAO {

    public Attempt findActiveAttempt(int examId, int userId) throws SQLException {
        String sql = "SELECT a.*, e.title AS exam_title " +
                     "FROM attempts a " +
                     "JOIN exams e ON a.exam_id = e.id " +
                     "WHERE a.exam_id = ? AND a.user_id = ? AND a.status = 'in-progress'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Attempt findById(int attemptId) throws SQLException {
        String sql = "SELECT a.*, e.title AS exam_title, u.name AS student_name " +
                     "FROM attempts a " +
                     "JOIN exams e ON a.exam_id = e.id " +
                     "JOIN users u ON a.user_id = u.id " +
                     "WHERE a.id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Attempt at = mapRow(rs);
                    at.setStudentName(rs.getString("student_name"));
                    return at;
                }
            }
        }
        return null;
    }

    public Attempt create(int examId, int userId) throws SQLException {
        String sql = "INSERT INTO attempts (user_id, exam_id, status, start_time) VALUES (?, ?, 'in-progress', ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setInt(2, examId);
            ps.setTimestamp(3, now);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getInt(1));
                }
            }
        }
        return null;
    }

    public boolean submitAttempt(int attemptId, double score, int total, int correct,
                                  int wrong, int unanswered, double percentage, boolean passed,
                                  List<AttemptAnswer> answers) throws SQLException {
        String updateAttempt = "UPDATE attempts SET status = 'submitted', submitted_at = ?, score = ?, " +
                               "total = ?, correct = ?, wrong = ?, unanswered = ?, percentage = ?, passed = ? " +
                               "WHERE id = ? AND status = 'in-progress'";
        String insertAns = "INSERT INTO attempt_answers (attempt_id, question_id, selected_option_id, is_correct, marks_awarded) " +
                           "VALUES (?, ?, ?, ?, ?)";
        Timestamp now = new Timestamp(System.currentTimeMillis());

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int updatedRows;
                try (PreparedStatement psAt = con.prepareStatement(updateAttempt)) {
                    psAt.setTimestamp(1, now);
                    psAt.setDouble(2, score);
                    psAt.setInt(3, total);
                    psAt.setInt(4, correct);
                    psAt.setInt(5, wrong);
                    psAt.setInt(6, unanswered);
                    psAt.setDouble(7, percentage);
                    psAt.setBoolean(8, passed);
                    psAt.setInt(9, attemptId);
                    updatedRows = psAt.executeUpdate();
                }

                if (updatedRows == 0) {
                    // Attempt was already submitted or not found; rollback and return false
                    con.rollback();
                    return false;
                }

                if (answers != null && !answers.isEmpty()) {
                    try (PreparedStatement psAns = con.prepareStatement(insertAns)) {
                        for (AttemptAnswer ans : answers) {
                            psAns.setInt(1, attemptId);
                            psAns.setInt(2, ans.getQuestionId());
                            if (ans.getSelectedOptionId() != null) {
                                psAns.setInt(3, ans.getSelectedOptionId());
                            } else {
                                psAns.setNull(3, Types.INTEGER);
                            }
                            psAns.setBoolean(4, Boolean.TRUE.equals(ans.getIsCorrect()));
                            psAns.setDouble(5, ans.getMarksAwarded() != null ? ans.getMarksAwarded() : 0.0);
                            psAns.addBatch();
                        }
                        psAns.executeBatch();
                    }
                }

                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<Attempt> findByUserId(int userId) throws SQLException {
        String sql = "SELECT a.*, e.title AS exam_title " +
                     "FROM attempts a " +
                     "JOIN exams e ON a.exam_id = e.id " +
                     "WHERE a.user_id = ? AND a.status = 'submitted' " +
                     "ORDER BY a.submitted_at DESC";

        List<Attempt> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public List<Attempt> getRecentAttempts(int limit) throws SQLException {
        String sql = "SELECT a.*, e.title AS exam_title, u.name AS student_name " +
                     "FROM attempts a " +
                     "JOIN exams e ON a.exam_id = e.id " +
                     "JOIN users u ON a.user_id = u.id " +
                     "ORDER BY a.start_time DESC " +
                     "LIMIT ?";

        List<Attempt> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Attempt at = mapRow(rs);
                    at.setStudentName(rs.getString("student_name"));
                    list.add(at);
                }
            }
        }
        return list;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM attempts";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Attempt mapRow(ResultSet rs) throws SQLException {
        Attempt a = new Attempt();
        a.setId(rs.getInt("id"));
        a.setUserId(rs.getInt("user_id"));
        a.setExamId(rs.getInt("exam_id"));
        a.setStatus(rs.getString("status"));

        Timestamp startTime = rs.getTimestamp("start_time");
        a.setStartTime(DateTimeUtil.formatTimestamp(startTime));

        Timestamp submittedAt = rs.getTimestamp("submitted_at");
        a.setSubmittedAt(DateTimeUtil.formatTimestamp(submittedAt));

        double score = rs.getDouble("score");
        if (!rs.wasNull()) a.setScore(score);

        int total = rs.getInt("total");
        if (!rs.wasNull()) a.setTotal(total);

        int correct = rs.getInt("correct");
        if (!rs.wasNull()) a.setCorrect(correct);

        int wrong = rs.getInt("wrong");
        if (!rs.wasNull()) a.setWrong(wrong);

        int unanswered = rs.getInt("unanswered");
        if (!rs.wasNull()) a.setUnanswered(unanswered);

        double percentage = rs.getDouble("percentage");
        if (!rs.wasNull()) a.setPercentage(percentage);

        boolean passed = rs.getBoolean("passed");
        if (!rs.wasNull()) a.setPassed(passed);

        try {
            a.setExamTitle(rs.getString("exam_title"));
        } catch (SQLException ignored) {}

        return a;
    }
}

