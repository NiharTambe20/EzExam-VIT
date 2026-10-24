package com.exam.servlet;

import com.exam.model.Attempt;
import com.exam.model.User;
import com.exam.service.ExamService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "StudentAttemptServlet", urlPatterns = {"/api/student/attempts/*"})
public class StudentAttemptServlet extends BaseServlet {

    private final ExamService examService = new ExamService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "";

        // POST /api/student/attempts/{id}/submit
        if (path.endsWith("/submit")) {
            String idStr = path.replace("/submit", "").replace("/", "").trim();
            try {
                int attemptId = Integer.parseInt(idStr);

                User user = getAuthenticatedUser(req);
                Integer userId = user != null ? user.getId() : null;

                JsonObject body = parseJson(req, JsonObject.class);
                if (body == null) {
                    body = new JsonObject();
                }

                // If no session user, check if body carries userId (for testing/flexibility)
                if (userId == null && body.has("userId")) {
                    userId = body.get("userId").getAsInt();
                }

                if (userId == null) {
                    sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required to submit an examination.");
                    return;
                }

                // Extract answersMap: can be { "101": 1002 }, { "answers": { "101": 1002 } }, or { "answers": "{\"101\": 1002}" }
                Map<String, Object> answersMap = new HashMap<>();
                JsonObject answersObj = null;
                if (body.has("answers")) {
                    JsonElement answersElem = body.get("answers");
                    if (answersElem.isJsonObject()) {
                        answersObj = answersElem.getAsJsonObject();
                    } else if (answersElem.isJsonPrimitive() && answersElem.getAsJsonPrimitive().isString()) {
                        try {
                            JsonElement parsed = com.google.gson.JsonParser.parseString(answersElem.getAsString());
                            if (parsed.isJsonObject()) {
                                answersObj = parsed.getAsJsonObject();
                            }
                        } catch (Exception ignored) {}
                    }
                }
                if (answersObj == null) {
                    answersObj = body;
                }

                for (Map.Entry<String, JsonElement> entry : answersObj.entrySet()) {
                    if (!"userId".equalsIgnoreCase(entry.getKey())) {
                        if (entry.getValue() == null || entry.getValue().isJsonNull()) {
                            answersMap.put(entry.getKey(), null);
                        } else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                            answersMap.put(entry.getKey(), entry.getValue().getAsInt());
                        } else if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                            try {
                                answersMap.put(entry.getKey(), Integer.parseInt(entry.getValue().getAsString()));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                Attempt evaluated = examService.evaluateAndSubmit(attemptId, userId != null ? userId : 0, answersMap);
                sendJson(resp, HttpServletResponse.SC_OK, evaluated);

            } catch (NumberFormatException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid attempt ID.");
            } catch (IllegalArgumentException e) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (IllegalAccessException e) {
                sendError(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to submit examination attempt.");
            }
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
        }
    }
}
