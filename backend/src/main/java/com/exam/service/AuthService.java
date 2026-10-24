package com.exam.service;

import com.exam.dao.UserDAO;
import com.exam.model.User;

import java.sql.SQLException;
import java.util.regex.Pattern;

public class AuthService {

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private final UserDAO userDAO = new UserDAO();

    public User register(String name, String email, String password) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty.");
        }
        if (email == null || !EMAIL_REGEX.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Email must have a valid format.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        email = email.trim().toLowerCase();
        User existing = userDAO.findByEmail(email);
        if (existing != null) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        return userDAO.create(name.trim(), email, password, "student");
    }

    public User login(String email, String password) throws SQLException {
        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        email = email.trim().toLowerCase();
        User user = userDAO.findByEmail(email);
        if (user == null || !userDAO.authenticate(user, password)) {
            throw new SecurityException("Invalid email or password.");
        }

        return user;
    }
}
