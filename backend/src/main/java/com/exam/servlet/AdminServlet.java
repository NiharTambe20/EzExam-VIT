package com.exam.servlet;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.dao.UserDAO;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.util.DateTimeUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AdminServlet", urlPatterns = {"/api/admin/*"})
public class AdminServlet extends BaseServlet {

    private final UserDAO userDAO = new UserDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null) path = "";

        try {
            if (path.matches("^/exams/\\d+$")) {
                int examId = Integer.parseInt(path.replace("/exams/", "").trim());
                Exam exam = examDAO.findById(examId, true, true);
                if (exam == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Exam not found.");
                    return;
                }
                sendJson(resp, HttpServletResponse.SC_OK, exam);
                return;
            }

            switch (path.toLowerCase()) {
                case "/stats":
                    handleStats(resp);
                    break;
                case "/attempts":
                    handleAttempts(resp);
                    break;
                case "/students":
                    handleStudents(resp);
                    break;
                default:
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown admin endpoint: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load admin data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null) path = "";

        try {
            if ("/exams".equalsIgnoreCase(path)) {
                // POST /api/admin/exams
                Exam payload = parseJson(req, Exam.class);
                if (payload == null || payload.getTitle() == null || payload.getTitle().isBlank()) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Exam title is required.");
                    return;
                }

                try {
                    DateTimeUtil.parseTimestamp(payload.getStartTime());
                    DateTimeUtil.parseTimestamp(payload.getEndTime());
                } catch (IllegalArgumentException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                    return;
                }

                Exam created = examDAO.create(payload);
                sendJson(resp, HttpServletResponse.SC_CREATED, created);

            } else if (path.matches("^/exams/\\d+/questions$")) {
                // POST /api/admin/exams/{id}/questions
                String[] parts = path.split("/");
                int examId = Integer.parseInt(parts[2]);

                JsonObject body = parseJson(req, JsonObject.class);
                if (body == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing question body.");
                    return;
                }

                String questionText = body.has("question_text") ? body.get("question_text").getAsString() : "";
                int marks = body.has("marks") ? body.get("marks").getAsInt() : 1;
                double negativeMarks = body.has("negative_marks") ? body.get("negative_marks").getAsDouble() : 0.0;
                int correctIndex = body.has("correctIndex") ? body.get("correctIndex").getAsInt() : 0;

                List<String> options = new ArrayList<>();
                if (body.has("options") && body.get("options").isJsonArray()) {
                    for (JsonElement el : body.getAsJsonArray("options")) {
                        options.add(el.getAsString());
                    }
                }

                Question q = questionDAO.create(examId, questionText, marks, negativeMarks, options, correctIndex);
                sendJson(resp, HttpServletResponse.SC_CREATED, q);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Operation failed: " + e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null) path = "";

        try {
            if (path.matches("^/exams/\\d+$")) {
                // PUT /api/admin/exams/{id}
                int examId = Integer.parseInt(path.replace("/exams/", "").trim());
                Exam payload = parseJson(req, Exam.class);

                try {
                    DateTimeUtil.parseTimestamp(payload.getStartTime());
                    DateTimeUtil.parseTimestamp(payload.getEndTime());
                } catch (IllegalArgumentException e) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                    return;
                }

                Exam updated = examDAO.update(examId, payload);
                sendJson(resp, HttpServletResponse.SC_OK, updated);

            } else if (path.matches("^/questions/\\d+$")) {
                // PUT /api/admin/questions/{id}
                int questionId = Integer.parseInt(path.replace("/questions/", "").trim());
                JsonObject body = parseJson(req, JsonObject.class);

                String questionText = body.has("question_text") ? body.get("question_text").getAsString() : "";
                int marks = body.has("marks") ? body.get("marks").getAsInt() : 1;
                double negativeMarks = body.has("negative_marks") ? body.get("negative_marks").getAsDouble() : 0.0;
                int correctIndex = body.has("correctIndex") ? body.get("correctIndex").getAsInt() : 0;

                List<String> options = new ArrayList<>();
                if (body.has("options") && body.get("options").isJsonArray()) {
                    for (JsonElement el : body.getAsJsonArray("options")) {
                        options.add(el.getAsString());
                    }
                }

                Question updated = questionDAO.update(questionId, questionText, marks, negativeMarks, options, correctIndex);
                sendJson(resp, HttpServletResponse.SC_OK, updated);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Update failed: " + e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAdmin(req, resp)) return;

        String path = req.getPathInfo();
        if (path == null) path = "";

        try {
            if (path.matches("^/exams/\\d+$")) {
                // DELETE /api/admin/exams/{id}
                int examId = Integer.parseInt(path.replace("/exams/", "").trim());
                boolean ok = examDAO.delete(examId);
                JsonObject res = new JsonObject();
                res.addProperty("success", ok);
                sendJson(resp, HttpServletResponse.SC_OK, res);

            } else if (path.matches("^/questions/\\d+$")) {
                // DELETE /api/admin/questions/{id}
                int questionId = Integer.parseInt(path.replace("/questions/", "").trim());
                boolean ok = questionDAO.delete(questionId);
                JsonObject res = new JsonObject();
                res.addProperty("success", ok);
                sendJson(resp, HttpServletResponse.SC_OK, res);

            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Delete failed: " + e.getMessage());
        }
    }

    private void handleStats(HttpServletResponse resp) throws Exception {
        JsonObject stats = new JsonObject();
        stats.addProperty("students", userDAO.countByRole("student"));
        stats.addProperty("exams", examDAO.countAll());
        stats.addProperty("attempts", attemptDAO.countAll());
        stats.addProperty("published", examDAO.countPublished());
        sendJson(resp, HttpServletResponse.SC_OK, stats);
    }

    private void handleAttempts(HttpServletResponse resp) throws Exception {
        sendJson(resp, HttpServletResponse.SC_OK, attemptDAO.getRecentAttempts(20));
    }

    private void handleStudents(HttpServletResponse resp) throws Exception {
        sendJson(resp, HttpServletResponse.SC_OK, userDAO.getStudentsWithAttemptCounts());
    }
}

