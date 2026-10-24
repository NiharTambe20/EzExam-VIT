package com.exam.servlet;

import com.exam.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class BaseServlet extends HttpServlet {

    protected static final Gson GSON = new GsonBuilder().serializeNulls().create();
    public static final String SESSION_USER_KEY = "EZEXAM_AUTH_USER";

    protected void sendJson(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();
        out.print(GSON.toJson(data));
        out.flush();
    }

    protected void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        JsonObject err = new JsonObject();
        err.addProperty("error", message);
        PrintWriter out = resp.getWriter();
        out.print(GSON.toJson(err));
        out.flush();
    }

    protected <T> T parseJson(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return GSON.fromJson(sb.toString(), clazz);
    }

    protected User getAuthenticatedUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            Object obj = session.getAttribute(SESSION_USER_KEY);
            if (obj instanceof User) {
                return (User) obj;
            }
        }
        return null;
    }

    protected boolean requireAuth(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return false;
        }
        return true;
    }

    protected boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getAuthenticatedUser(req);
        if (user == null) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required.");
            return false;
        }
        if (!"admin".equalsIgnoreCase(user.getRole())) {
            sendError(resp, HttpServletResponse.SC_FORBIDDEN, "Access denied: Administrator role required.");
            return false;
        }
        return true;
    }
}
