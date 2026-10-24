package com.exam.dao;

import com.exam.model.Exam;
import com.exam.util.DBConnection;
import com.exam.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {

    private final QuestionDAO questionDAO = new QuestionDAO();

    public List<Exam> findAll(boolean onlyPublished) throws SQLException {
        String sql = "SELECT e.id, e.title, e.description, e.duration_minutes, e.total_marks, " +
                     "e.passing_percentage, e.negative_mark, e.status, e.start_time, e.end_time, e.created_by, " +
                     "COUNT(q.id) AS question_count " +
                     "FROM exams e " +
                     "LEFT JOIN questions q ON e.id = q.exam_id " +
                     (onlyPublished ? "WHERE e.status = 'published' " : "") +
                     "GROUP BY e.id, e.title, e.description, e.duration_minutes, e.total_marks, " +
                     "e.passing_percentage, e.negative_mark, e.status, e.start_time, e.end_time, e.created_by " +
                     "ORDER BY e.id DESC";

        List<Exam> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Exam exam = mapRow(rs);
                exam.setQuestionCount(rs.getInt("question_count"));
                list.add(exam);
            }
        }
        return list;
    }

    public Exam findById(int id, boolean includeQuestions, boolean includeAnswers) throws SQLException {
        String sql = "SELECT e.id, e.title, e.description, e.duration_minutes, e.total_marks, " +
                     "e.passing_percentage, e.negative_mark, e.status, e.start_time, e.end_time, e.created_by, " +
                     "COUNT(q.id) AS question_count " +
                     "FROM exams e " +
                     "LEFT JOIN questions q ON e.id = q.exam_id " +
                     "WHERE e.id = ? " +
                     "GROUP BY e.id, e.title, e.description, e.duration_minutes, e.total_marks, " +
                     "e.passing_percentage, e.negative_mark, e.status, e.start_time, e.end_time, e.created_by";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exam exam = mapRow(rs);
                    exam.setQuestionCount(rs.getInt("question_count"));
                    if (includeQuestions) {
                        exam.setQuestions(questionDAO.findByExamId(id, includeAnswers));
                    }
                    return exam;
                }
            }
        }
        return null;
    }

    public Exam create(Exam exam) throws SQLException {
        String sql = "INSERT INTO exams (title, description, duration_minutes, total_marks, " +
                     "passing_percentage, negative_mark, status, start_time, end_time, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, exam.getTitle());
            ps.setString(2, exam.getDescription());
            ps.setInt(3, exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 30);
            ps.setInt(4, exam.getTotalMarks() != null ? exam.getTotalMarks() : 20);
            ps.setInt(5, exam.getPassingPercentage() != null ? exam.getPassingPercentage() : 40);
            ps.setDouble(6, exam.getNegativeMark() != null ? exam.getNegativeMark() : 0.0);
            ps.setString(7, exam.getStatus() != null ? exam.getStatus() : "draft");

            Timestamp startTs = DateTimeUtil.parseTimestamp(exam.getStartTime());
            if (startTs != null) {
                ps.setTimestamp(8, startTs);
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            Timestamp endTs = DateTimeUtil.parseTimestamp(exam.getEndTime());
            if (endTs != null) {
                ps.setTimestamp(9, endTs);
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }

            if (exam.getCreatedBy() != null) {
                ps.setInt(10, exam.getCreatedBy());
            } else {
                ps.setNull(10, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return findById(rs.getInt(1), false, false);
                }
            }
        }
        return null;
    }

    public Exam update(int id, Exam exam) throws SQLException {
        String sql = "UPDATE exams SET title = ?, description = ?, duration_minutes = ?, total_marks = ?, " +
                     "passing_percentage = ?, negative_mark = ?, status = ?, start_time = ?, end_time = ? " +
                     "WHERE id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, exam.getTitle());
            ps.setString(2, exam.getDescription());
            ps.setInt(3, exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 30);
            ps.setInt(4, exam.getTotalMarks() != null ? exam.getTotalMarks() : 20);
            ps.setInt(5, exam.getPassingPercentage() != null ? exam.getPassingPercentage() : 40);
            ps.setDouble(6, exam.getNegativeMark() != null ? exam.getNegativeMark() : 0.0);
            ps.setString(7, exam.getStatus() != null ? exam.getStatus() : "draft");

            Timestamp startTs = DateTimeUtil.parseTimestamp(exam.getStartTime());
            if (startTs != null) {
                ps.setTimestamp(8, startTs);
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            Timestamp endTs = DateTimeUtil.parseTimestamp(exam.getEndTime());
            if (endTs != null) {
                ps.setTimestamp(9, endTs);
            } else {
                ps.setNull(9, Types.TIMESTAMP);
            }

            ps.setInt(10, id);
            ps.executeUpdate();
            return findById(id, false, false);
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM exams";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countPublished() throws SQLException {
        String sql = "SELECT COUNT(*) FROM exams WHERE status = 'published'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Exam mapRow(ResultSet rs) throws SQLException {
        Exam e = new Exam();
        e.setId(rs.getInt("id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setDurationMinutes(rs.getInt("duration_minutes"));
        e.setTotalMarks(rs.getInt("total_marks"));
        e.setPassingPercentage(rs.getInt("passing_percentage"));
        e.setNegativeMark(rs.getDouble("negative_mark"));
        e.setStatus(rs.getString("status"));

        Timestamp startTs = rs.getTimestamp("start_time");
        e.setStartTime(DateTimeUtil.formatTimestamp(startTs));

        Timestamp endTs = rs.getTimestamp("end_time");
        e.setEndTime(DateTimeUtil.formatTimestamp(endTs));

        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) {
            e.setCreatedBy(createdBy);
        }
        return e;
    }
}

