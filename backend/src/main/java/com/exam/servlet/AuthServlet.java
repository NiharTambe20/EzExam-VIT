package com.exam.servlet;

import com.exam.model.User;
import com.exam.service.AuthService;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet(name = "AuthServlet", urlPatterns = {"/api/auth/*"})
public class AuthServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "";

        if ("/me".equalsIgnoreCase(path) || "".equals(path)) {
            User user = getAuthenticatedUser(req);
            if (user != null) {
                sendJson(resp, HttpServletResponse.SC_OK, user);
            } else {
                sendJson(resp, HttpServletResponse.SC_OK, null);
            }
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) path = "";

        try {
            switch (path.toLowerCase()) {
                case "/register":
                    handleRegister(req, resp);
                    break;
                case "/login":
                    handleLogin(req, resp);
                    break;
                case "/logout":
                    handleLogout(req, resp);
                    break;
                default:
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Unknown endpoint: " + path);
            }
        } catch (IllegalArgumentException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (SecurityException e) {
            sendError(resp, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred.");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = parseJson(req, JsonObject.class);
        if (body == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing request body.");
            return;
        }

        String name = body.has("name") ? body.get("name").getAsString() : null;
        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;

        User user = authService.register(name, email, password);

        // Bind user to session upon successful registration
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user);

        sendJson(resp, HttpServletResponse.SC_CREATED, user);
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonObject body = parseJson(req, JsonObject.class);
        if (body == null) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing credentials.");
            return;
        }

        String email = body.has("email") ? body.get("email").getAsString() : null;
        String password = body.has("password") ? body.get("password").getAsString() : null;

        User user = authService.login(email, password);

        // Create or reuse session
        HttpSession session = req.getSession(true);
        session.setAttribute(SESSION_USER_KEY, user);

        sendJson(resp, HttpServletResponse.SC_OK, user);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        JsonObject res = new JsonObject();
        res.addProperty("message", "Logged out successfully.");
        sendJson(resp, HttpServletResponse.SC_OK, res);
    }
}
