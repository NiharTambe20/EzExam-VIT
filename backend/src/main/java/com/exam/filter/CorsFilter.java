package com.exam.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * CORS filter allowing the React frontend (local Vite or deployed Vercel) to communicate
 * with the Jakarta Servlet backend on Tomcat with session cookies / credentials enabled.
 *
 * Configurable via environment variable:
 * - EZEXAM_ALLOWED_ORIGIN: e.g. "https://ezexam-vit.vercel.app" (or comma-separated origins)
 *
 * When EZEXAM_ALLOWED_ORIGIN is not set, defaults to http://localhost:5173 while preserving
 * localhost / 127.0.0.1 origins for local development.
 */
@WebFilter(filterName = "CorsFilter", urlPatterns = {"/*"})
public class CorsFilter implements Filter {

    private static final String DEFAULT_DEV_ORIGIN = "http://localhost:5173";
    private final String configuredAllowedOrigin;
    private final Set<String> allowedOriginsSet;

    public CorsFilter() {
        String envOrigin = System.getenv("EZEXAM_ALLOWED_ORIGIN");
        if (envOrigin != null && !envOrigin.isBlank()) {
            this.configuredAllowedOrigin = envOrigin.trim();
            this.allowedOriginsSet = new HashSet<>();
            for (String origin : this.configuredAllowedOrigin.split(",")) {
                if (!origin.isBlank()) {
                    this.allowedOriginsSet.add(origin.trim().toLowerCase());
                }
            }
        } else {
            this.configuredAllowedOrigin = null;
            this.allowedOriginsSet = null;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");

        if (origin != null && !origin.isBlank()) {
            String originLower = origin.trim().toLowerCase();
            boolean isAllowed = false;

            // 1. Check against configured allowed origin(s) from environment variable
            if (allowedOriginsSet != null && allowedOriginsSet.contains(originLower)) {
                isAllowed = true;
            }
            // 2. Preserve local development support (localhost / 127.0.0.1)
            else if (DEFAULT_DEV_ORIGIN.equalsIgnoreCase(origin)
                    || origin.startsWith("http://localhost:")
                    || origin.startsWith("http://127.0.0.1:")) {
                isAllowed = true;
            }

            if (isAllowed) {
                res.setHeader("Access-Control-Allow-Origin", origin);
            } else if (configuredAllowedOrigin != null && !configuredAllowedOrigin.isBlank()) {
                res.setHeader("Access-Control-Allow-Origin", configuredAllowedOrigin.split(",")[0].trim());
            } else {
                res.setHeader("Access-Control-Allow-Origin", DEFAULT_DEV_ORIGIN);
            }
        } else {
            // Non-CORS or same-origin request
            if (configuredAllowedOrigin != null && !configuredAllowedOrigin.isBlank()) {
                res.setHeader("Access-Control-Allow-Origin", configuredAllowedOrigin.split(",")[0].trim());
            } else {
                res.setHeader("Access-Control-Allow-Origin", DEFAULT_DEV_ORIGIN);
            }
        }

        res.setHeader("Access-Control-Allow-Credentials", "true");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        res.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With");
        res.setHeader("Access-Control-Max-Age", "3600");
        res.setHeader("Vary", "Origin");

        // Short-circuit preflight requests
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            res.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
