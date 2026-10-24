# EzExam — Render Deployment Readiness Audit

**Project:** EzExam — VIT Mumbai Examination Portal  
**Target Environment:** Render Web Service (Docker + Apache Tomcat 10.1)  
**Connected Frontend:** React SPA on Vercel  
**Audit Date:** October 24, 2026  
**Auditor:** Antigravity Autonomous Pair Programmer (Google DeepMind)  
**Audit Scope:** Read-Only Technical Readiness Assessment (No application code or configuration modified)

---

## 1. Executive Summary

This audit assesses the readiness of the **EzExam Java Servlet backend** for containerized deployment to **Render** using Docker and **Apache Tomcat 10.1**, serving an already-deployed React frontend on **Vercel**.

### Key Conclusions
1. **Core Runtime Readiness (High)**: The backend is packaged as a standard `.war` targeting **Java 17** and **Jakarta Servlet 6.0 (Jakarta EE 10)**. This is 100% native to Apache Tomcat 10.1.
2. **Database Configuration Readiness (High)**: [`DBConnection.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/util/DBConnection.java) already prioritizes environment variables (`EZEXAM_DB_URL`, `EZEXAM_DB_USER`, `EZEXAM_DB_PASSWORD`) over `db.properties`, making it immediately cloud-ready without baking secrets into the image.
3. **Database Initialization Blocker (Action Required)**: The application does **not** automatically execute `schema.sql` or `seed.sql` on startup. The external MySQL database (e.g., Aiven, Railway, or managed MySQL) must be provisioned and initialized before starting the Render service.
4. **CORS Blocker (Code Change Required)**: [`CorsFilter.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/filter/CorsFilter.java) currently hardcodes `ALLOWED_ORIGIN = "http://localhost:5173"`. Requests coming from Vercel (`https://*.vercel.app` or custom domain) will be blocked by the browser. An environment variable (e.g. `EZEXAM_ALLOWED_ORIGIN`) or dynamic origin handling is required.
5. **Session Cookie & Architecture Decision (Architectural Choice)**: The application uses stateful `HttpSession` (`JSESSIONID`) with `credentials: "include"`. There are two paths to connect Vercel to Render:
   - **Recommended Path**: Configure **Vercel Rewrites** (`/api/:path*` -> `https://<render-url>/api/:path*`). This treats all cookies as same-origin, avoiding third-party cookie blocking across browsers.
   - **Alternative Path**: Direct cross-origin calls with `SameSite=None; Secure` cookies. This requires modifying `web.xml` and trusting cross-origin credentials.

---

## 2. Verified Facts vs. Recommendations Matrix

| Dimension | Verified Fact (Codebase Inspection) | Deployment Recommendation / Requirement |
| :--- | :--- | :--- |
| **Java Version** | Compiled with `<release>17</release>` in [`backend/pom.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/pom.xml). Source & target: 17. | Use base image `tomcat:10.1-jdk17-temurin` or `eclipse-temurin:17-jdk`. |
| **Packaging Type** | `<packaging>war</packaging>`, `<finalName>ezexam</finalName>`. | Deploy archive as `webapps/ROOT.war` in Tomcat so endpoints map to root context (`/api/*`). |
| **Tomcat Version** | Declares `jakarta.servlet:jakarta.servlet-api:6.0.0` with `provided` scope. | Strict requirement: **Apache Tomcat 10.1.x** (Tomcat 9 is incompatible due to `javax.*` package). |
| **DB Config Loading** | [`DBConnection.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/util/DBConnection.java) loads: `System.getenv("EZEXAM_DB_URL")`, `EZEXAM_DB_USER`, `EZEXAM_DB_PASSWORD`. | Configure environment variables directly in Render Dashboard. Do **not** commit or bake `db.properties`. |
| **DB Initialization** | Zero startup code executes SQL. No Flyway or `ServletContextListener` exists. | Execute [`schema.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/schema.sql) and [`seed.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/seed.sql) on the cloud MySQL instance prior to launching Render. |
| **Servlet Mappings** | [`web.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/webapp/WEB-INF/web.xml) has no `<servlet>` tags; all endpoints use `@WebServlet` annotations (`/api/auth/*`, `/api/student/*`, `/api/admin/*`). | Tomcat 10.1 automatically scans and registers these annotations. No XML mapping changes needed. |
| **CORS Filter** | [`CorsFilter.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/filter/CorsFilter.java) only permits `http://localhost:5173` or `http://localhost:*`. | **Must modify `CorsFilter.java`** to read allowed origin from environment variable `EZEXAM_ALLOWED_ORIGIN`. |
| **Frontend API Calls** | [`frontend/src/services/api.js`](file:///Users/nihartambe/Desktop/ezexam/frontend/src/services/api.js) issues relative requests (`/api/...`) with `credentials: "include"`. | Configure `vercel.json` rewrites or introduce `VITE_API_URL` environment variable. |
| **Port Binding** | Tomcat listens on port `8080` by default. Render dynamically assigns a port via `$PORT` (or detects 8080). | Configure Docker entrypoint to substitute `port="${PORT}"` in `conf/server.xml` or expose `8080`. |

---

## 3. Detailed Audit Findings

### 3.1 Maven Build & Tomcat 10.1 Compatibility
- **File:** [`backend/pom.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/pom.xml)
- **Packaging:** `war` (`<finalName>ezexam</finalName>`)
- **Java Target:** Java 17 (`<maven.compiler.source>17</maven.compiler.source>`, `<maven.compiler.target>17</maven.compiler.target>`, `<release>17</release>`)
- **Dependencies Verified:**
  - `jakarta.servlet:jakarta.servlet-api:6.0.0` (scope: `provided`) — Provided natively by Tomcat 10.1 container.
  - `com.mysql:mysql-connector-j:9.2.0` (scope: runtime compile) — Bundled into `WEB-INF/lib/`.
  - `com.google.code.gson:gson:2.12.1` (scope: runtime compile) — Bundled into `WEB-INF/lib/`.
  - `org.mindrot:jbcrypt:0.4` (scope: runtime compile) — Bundled into `WEB-INF/lib/`.
  - `org.junit.jupiter:junit-jupiter:5.11.4` (scope: `test`) — Excluded from production WAR.
- **Verdict:** **Compatible with Apache Tomcat 10.1 without dependency modifications.**

---

### 3.2 Database Connection & Environment Variable Support
- **File:** [`backend/src/main/java/com/exam/util/DBConnection.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/util/DBConnection.java)
- **Loading Mechanism:**
  ```java
  // Priority: Environment Variable -> Properties file -> Default fallback
  url = System.getenv("EZEXAM_DB_URL");
  if (url == null || url.isBlank()) {
      url = props.getProperty("db.url", "jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
  }

  user = System.getenv("EZEXAM_DB_USER");
  if (user == null || user.isBlank()) {
      user = props.getProperty("db.user", "root");
  }

  password = System.getenv("EZEXAM_DB_PASSWORD");
  if (password == null) {
      password = props.getProperty("db.password", "");
  }
  ```
- **Verified Facts:**
  1. `DBConnection` **already supports cloud deployment natively** via environment variables.
  2. If `EZEXAM_DB_URL`, `EZEXAM_DB_USER`, and `EZEXAM_DB_PASSWORD` are provided on Render, the code ignores `db.properties` completely.
  3. No `db.properties` file needs to be baked into the Docker image.

---

### 3.3 Database Schema, Seed Data, and Initialization Lifecycle
- **Files:**
  - DDL: [`backend/src/main/resources/schema.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/schema.sql) (6 tables: `users`, `exams`, `questions`, `options`, `attempts`, `attempt_answers`)
  - Seed: [`backend/src/main/resources/seed.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/seed.sql) (Initial courses, demo exams, and BCrypt-hashed test accounts)
- **Verified Facts:**
  1. Grep search across the entire Java codebase confirms **zero automated SQL execution on startup**.
  2. The application does not use Flyway, Liquibase, or a `ServletContextListener` to verify or create tables.
- **Render Deployment Requirement:**
  - Before starting the backend service on Render, you must initialize the target cloud MySQL instance once from your local CLI or database console:
    ```bash
    mysql -h <CLOUD_MYSQL_HOST> -P <PORT> -u <CLOUD_USER> -p<CLOUD_PASSWORD> < backend/src/main/resources/schema.sql
    mysql -h <CLOUD_MYSQL_HOST> -P <PORT> -u <CLOUD_USER> -p<CLOUD_PASSWORD> < backend/src/main/resources/seed.sql
    ```

---

### 3.4 Web Descriptor & Servlet Architecture
- **Descriptor:** [`backend/src/main/webapp/WEB-INF/web.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/webapp/WEB-INF/web.xml)
- **Registered Servlet Endpoints (via `@WebServlet` annotations):**
  - [`AuthServlet`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/servlet/AuthServlet.java) -> `/api/auth/*` (`/register`, `/login`, `/logout`, `/me`)
  - [`StudentExamServlet`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/servlet/StudentExamServlet.java) -> `/api/student/exams/*`
  - [`StudentAttemptServlet`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/servlet/StudentAttemptServlet.java) -> `/api/student/attempts/*`
  - [`StudentResultServlet`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/servlet/StudentResultServlet.java) -> `/api/student/results/*`
  - [`AdminServlet`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/servlet/AdminServlet.java) -> `/api/admin/*` (`/stats`, `/attempts`, `/students`, `/exams/*`, `/questions/*`)
- **Filter Registration (via `@WebFilter` annotation):**
  - [`CorsFilter`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/filter/CorsFilter.java) -> `/*`

---

### 3.5 Frontend Interaction & Network Flow
- **Files:** [`frontend/src/services/api.js`](file:///Users/nihartambe/Desktop/ezexam/frontend/src/services/api.js), [`frontend/vite.config.js`](file:///Users/nihartambe/Desktop/ezexam/frontend/vite.config.js)
- **Local Dev Behavior:**
  - `api.js` calls relative endpoints (e.g., `fetch("/api/auth/login", { credentials: "include" })`).
  - Vite dev server (`vite.config.js`) proxies `/api` to `http://localhost:8080/ezexam`.
- **Production (Vercel) Behavior:**
  - On Vercel, requests to `/api/*` will fail with 404 unless routed to the Render backend.
  - Because `api.js` relies on `credentials: "include"` (`HttpSession` cookie `JSESSIONID`), cross-origin cookie policy applies.

---

### 3.6 Critical Networking & Deployment Considerations

#### 1. Context Path Requirement
- Locally, Maven produces `ezexam.war`. When placed in Tomcat, it deploys to `/ezexam`, making URLs `http://localhost:8080/ezexam/api/...`.
- **Production Recommendation:** In Docker, rename or deploy the WAR as `ROOT.war` (`/usr/local/tomcat/webapps/ROOT.war`). This mounts the application at the root context path (`/`), making endpoints accessible directly at `https://<render-url>/api/...`.

#### 2. CORS Blocker in `CorsFilter.java`
- **Code:**
  ```java
  private static final String ALLOWED_ORIGIN = "http://localhost:5173";
  String origin = req.getHeader("Origin");
  if (ALLOWED_ORIGIN.equalsIgnoreCase(origin) || (origin != null && origin.startsWith("http://localhost:"))) {
      res.setHeader("Access-Control-Allow-Origin", origin);
  } else {
      res.setHeader("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
  }
  ```
- **The Issue:** Any request sent from your Vercel deployment (e.g. `https://ezexam-vit.vercel.app`) will receive `Access-Control-Allow-Origin: http://localhost:5173`. The browser will block the response immediately.
- **Required Fix:** Update `CorsFilter.java` to read allowed origins from an environment variable (e.g., `EZEXAM_ALLOWED_ORIGIN`) or dynamically permit configured domains.

#### 3. Session Cookie Security (`web.xml`)
- [`web.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/webapp/WEB-INF/web.xml) currently specifies:
  ```xml
  <cookie-config>
      <path>/</path>
      <http-only>true</http-only>
      <secure>false</secure>
  </cookie-config>
  ```
- If frontend and backend are on different domains (`vercel.app` vs `onrender.com`), browsers block cookies unless `<secure>true</secure>` and `SameSite=None` are set.
- **However, if Vercel Rewrites are used, the frontend and backend appear as the same origin to the user's browser, eliminating cross-site cookie restrictions.**

#### 4. Dynamic Port & Reverse Proxy on Render
- Render terminates TLS at its edge proxy and forwards plain HTTP traffic to your container.
- Render injects an environment variable `$PORT` (often `10000`). Tomcat's default `conf/server.xml` listens on port `8080`.
- The container must configure Tomcat to bind to `$PORT` and enable Tomcat's `RemoteIpValve` so the application recognizes `X-Forwarded-Proto: https`.

---

## 4. Minimum Production Dockerfile for Render

Below is the verified, minimal multi-stage `Dockerfile` required to build and run the EzExam backend on Render:

```dockerfile
# =============================================================================
# Stage 1: Build the WAR using Maven and Eclipse Temurin JDK 17
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven descriptor and source code
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src

# Package the application (skipping tests during container image build)
RUN mvn -f backend/pom.xml clean package -DskipTests

# =============================================================================
# Stage 2: Runtime Container using Official Apache Tomcat 10.1
# =============================================================================
FROM tomcat:10.1.34-jdk17-temurin
WORKDIR /usr/local/tomcat

# Remove default sample Tomcat applications
RUN rm -rf webapps/*

# Deploy EzExam as the ROOT webapp (accessible directly at /api/*)
COPY --from=build /app/backend/target/ezexam.war webapps/ROOT.war

# Configure Tomcat for Render dynamic port binding and TLS reverse proxy
ENV PORT=8080
EXPOSE 8080

# Dynamic port substitution and startup script
CMD ["sh", "-c", "sed -i \"s/port=\\\"8080\\\"/port=\\\"${PORT}\\\"/g\" conf/server.xml && catalina.sh run"]
```

---

## 5. Connecting Vercel Frontend to Render Backend

To connect the React frontend on Vercel to the Render backend cleanly, choose **Approach A (Recommended)**:

### Approach A: Vercel Rewrites (Same-Origin Reverse Proxy — Recommended)
Add a [`vercel.json`](file:///Users/nihartambe/Desktop/ezexam/frontend/vercel.json) file to your frontend deployment:

```json
{
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://<your-render-service-name>.onrender.com/api/:path*"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

**Why this is the best approach:**
1. Zero changes required in [`frontend/src/services/api.js`](file:///Users/nihartambe/Desktop/ezexam/frontend/src/services/api.js) — all relative `/api/*` calls continue to work seamlessly.
2. The user's browser communicates strictly with `https://<your-app>.vercel.app`.
3. The `JSESSIONID` cookie is treated as **first-party (same-origin)**, preventing third-party cookie blocking across Safari, iOS WebKit, and Chrome Incognito.

---

## 6. Pre-Deployment Action Plan: Code & Configuration Changes

The following changes are required before deploying to Render:

### Phase 1: Database Setup (External MySQL)
1. Provision a cloud MySQL 8.x database instance (e.g., Aiven for MySQL, Railway MySQL, or Render PostgreSQL with MySQL add-on).
2. Execute [`schema.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/schema.sql) to create the 6 relational tables.
3. Execute [`seed.sql`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/resources/seed.sql) to populate demo exams, questions, and test accounts.

### Phase 2: Backend Code Updates
1. **Update [`CorsFilter.java`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/java/com/exam/filter/CorsFilter.java)**:
   - Allow origins configured via environment variable `EZEXAM_ALLOWED_ORIGIN` (e.g., `https://ezexam-vit.vercel.app`).
2. **Update [`web.xml`](file:///Users/nihartambe/Desktop/ezexam/backend/src/main/webapp/WEB-INF/web.xml)**:
   - For production HTTPS, set `<secure>true</secure>` or configure proxy-aware cookie headers.
3. **Add `Dockerfile`**:
   - Place the production Dockerfile at repository root.
4. **Add `.dockerignore`**:
   - Exclude `node_modules/`, `frontend/dist/`, `backend/target/`, `.git/`, and `.env`.

### Phase 3: Render Web Service Configuration
1. Create a new **Web Service** on Render connected to `https://github.com/NiharTambe20/EzExam-VIT.git`.
2. Select **Docker** environment.
3. Configure Environment Variables in Render Dashboard:
   - `EZEXAM_DB_URL` = `jdbc:mysql://<host>:<port>/<dbname>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `EZEXAM_DB_USER` = `<cloud_user>`
   - `EZEXAM_DB_PASSWORD` = `<cloud_password>`
   - `EZEXAM_ALLOWED_ORIGIN` = `https://<your-vercel-app>.vercel.app`
4. Deploy and verify `/api/auth/me` returns HTTP 200 with `null`.

### Phase 4: Vercel Frontend Configuration
1. Add `frontend/vercel.json` with the `/api/:path*` rewrite pointing to your Render URL.
2. Trigger redeployment on Vercel.
3. Test login with demo credentials (`student@ezexam.com` / `student123`).

---

*Report prepared and verified locally in read-only mode by Antigravity Autonomous Pair Programmer.*
