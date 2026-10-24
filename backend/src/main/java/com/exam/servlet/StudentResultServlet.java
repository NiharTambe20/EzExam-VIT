package com.exam.servlet;

import com.exam.dao.AttemptDAO;
import com.exam.model.Attempt;
import com.exam.model.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "StudentResultServlet", urlPatterns = {"/api/student/results/*"})
public class StudentResultServlet extends BaseServlet {

    private final AttemptDAO attemptDAO = new AttemptDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/") || path.isBlank()) {
                // GET /api/student/results?userId=2
                User user = getAuthenticatedUser(req);
                Integer userId = user != null ? user.getId() : null;

                String userParam = req.getParameter("userId");
                if (userParam != null && !userParam.isBlank()) {
                    userId = Integer.parseInt(userParam);
                }

                if (userId == null) {
                    sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "User ID is required to fetch results.");
                    return;
                }

                List<Attempt> list = attemptDAO.findByUserId(userId);
                sendJson(resp, HttpServletResponse.SC_OK, list);
            } else {
                // GET /api/student/results/{id}
                String idStr = path.replace("/", "").trim();
                int attemptId = Integer.parseInt(idStr);

                Attempt attempt = attemptDAO.findById(attemptId);
                if (attempt == null) {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Result not found.");
                    return;
                }

                // Security: ensure students can only view their own results
                User user = getAuthenticatedUser(req);
                if (user != null && !"admin".equalsIgnoreCase(user.getRole()) && !user.getId().equals(attempt.getUserId())) {
                    sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied: Cannot view another student's result.");
                    return;
                }

                sendJson(resp, HttpServletResponse.SC_OK, attempt);
            }
        } catch (NumberFormatException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid ID parameter.");
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load examination results.");
        }
    }
}
