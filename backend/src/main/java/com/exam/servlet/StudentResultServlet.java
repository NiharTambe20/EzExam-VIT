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
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required to view examination results.");
            return;
        }

        String path = req.getPathInfo();

        try {
            if (path == null || path.equals("/") || path.isBlank()) {
                // GET /api/student/results or /api/student/results?userId=X
                String userParam = req.getParameter("userId");
                int targetUserId = user.getId();

                if (!"admin".equalsIgnoreCase(user.getRole())) {
                    // Students can only access their own results. Reject conflicting query parameter.
                    if (userParam != null && !userParam.isBlank()) {
                        int requestedId = Integer.parseInt(userParam);
                        if (requestedId != user.getId()) {
                            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied: Cannot view another student's results.");
                            return;
                        }
                    }
                } else {
                    // Admin can query any student's results if requested
                    if (userParam != null && !userParam.isBlank()) {
                        targetUserId = Integer.parseInt(userParam);
                    }
                }

                List<Attempt> list = attemptDAO.findByUserId(targetUserId);
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
                if (!"admin".equalsIgnoreCase(user.getRole()) && !user.getId().equals(attempt.getUserId())) {
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
