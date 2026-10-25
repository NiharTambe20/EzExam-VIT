package com.exam.servlet;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.User;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "StudentExamServlet", urlPatterns = {"/api/student/exams/*"})
public class StudentExamServlet extends BaseServlet {

    private final ExamDAO examDAO = new ExamDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!requireAuth(req, resp)) return;

        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/") || path.isBlank()) {
                // GET /api/student/exams
                String onlyPubParam = req.getParameter("onlyPublished");
                boolean onlyPublished = "true".equalsIgnoreCase(onlyPubParam) || onlyPubParam == null;
                List<Exam> exams = examDAO.findAll(onlyPublished);
                sendJson(resp, HttpServletResponse.SC_OK, exams);
            } else {
                // GET /api/student/exams/{id}
                String idStr = path.replace("/", "").trim();
                int examId = Integer.parseInt(idStr);
                Exam exam = examDAO.findById(examId, true, false); // false = do not leak correct answers!
                if (exam == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Examination not found.");
                    return;
                }
                sendJson(resp, HttpServletResponse.SC_OK, exam);
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid examination ID.");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load examination.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "";

        // POST /api/student/exams/{id}/start
        if (path.endsWith("/start")) {
            String idStr = path.replace("/start", "").replace("/", "").trim();
            try {
                int examId = Integer.parseInt(idStr);

                // Authenticate user strictly from session
                User user = getAuthenticatedUser(req);
                if (user == null) {
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "User authentication required to start an exam.");
                    return;
                }

                int userId = user.getId();

                // Reject conflicting userId if supplied in request body
                JsonObject body = parseJson(req, JsonObject.class);
                if (body != null && body.has("userId")) {
                    try {
                        int suppliedUserId = body.get("userId").getAsInt();
                        if (suppliedUserId != userId) {
                            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Cannot start an exam on behalf of another user.");
                            return;
                        }
                    } catch (Exception e) {
                        sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid userId format in request.");
                        return;
                    }
                }

                Exam exam = examDAO.findById(examId, false, false);
                if (exam == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Examination not found.");
                    return;
                }
                if (!"published".equalsIgnoreCase(exam.getStatus())) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "This examination is no longer available.");
                    return;
                }

                // Check for existing in-progress attempt to resume
                Attempt active = attemptDAO.findActiveAttempt(examId, userId);
                if (active != null) {
                    sendJson(resp, HttpServletResponse.SC_OK, active);
                    return;
                }

                // Create new attempt
                Attempt created = attemptDAO.create(examId, userId);
                sendJson(resp, HttpServletResponse.SC_CREATED, created);

            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid examination ID.");
            } catch (Exception e) {
                e.printStackTrace();
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to start examination attempt.");
            }
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
        }
    }
}
