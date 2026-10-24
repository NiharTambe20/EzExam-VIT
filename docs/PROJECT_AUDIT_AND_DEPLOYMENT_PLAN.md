# EzExam — Technical Audit & Deployment Handover Report

**Project Name:** EzExam — VIT Mumbai Examination Portal  
**Project Path:** `/Users/nihartambe/Desktop/ezexam`  
**Audit Date:** October 23, 2026  
**Auditor:** Antigravity Autonomous Pair Programmer (DeepMind)  
**Status:** Read-Only Audit & Handover Document  

---

## 1. Executive Summary

EzExam is a full-stack, enterprise-style online examination portal customized for the visual identity and academic workflow of **Vidyalankar Institute of Technology (VIT), Mumbai**. The system provides role-based access for students and administrators, enabling timed examinations, real-time question navigation palettes, automated evaluation with negative marking, exam attempt history, and comprehensive administrative exam/question management.

### Key Audit Conclusions

1. **Current State:** The application is fully functional in the local development environment. The React 19 frontend compiles cleanly (`npm run build` succeeds in ~298ms), and the Java Jakarta Servlet backend passes 100% of its test suite (16 tests run, 0 failures, 0 errors, 0 skipped).
2. **Critical Repository Desynchronization (Blocker for GitHub):** The entire Java backend source tree (`backend/src/main/`, `backend/src/test/`, and `backend/pom.xml`), the frontend branding assets (`frontend/public/`), and `setup-db.sh` are currently **untracked in Git**. Furthermore, the local repository is on branch `frontend-dev` with 25 uncommitted modified files pointing to remote `https://github.com/gaurangkhochare/ezexam.git`. These untracked and unstaged files must be unified before publishing.
3. **Secret Hygiene:** Local database credentials are saved on disk in `backend/src/main/resources/db.properties`. Although this file is correctly ignored in `.gitignore`, no `db.properties.example` template exists. `backend/README.md` documents demo credentials in plaintext.
4. **Architectural & Deployment Readiness:** The Java backend is packaged as a standard `.war` archive intended for Apache Tomcat 10.1+ (Jakarta EE 10 namespace). Because common static hosting services (Vercel, Netlify, GitHub Pages) cannot execute Java servlets, the application requires a dual-tier or containerized deployment strategy (e.g., Dockerized Tomcat on Render/Railway + Static frontend on Vercel with rewrites + Managed MySQL on Aiven/Railway).

---

## 2. Verified Technology Stack & Installed Versions

Every dependency and tool version was inspected directly from configuration files, runtime environments, and lockfiles:

| Tier / Component | Technology / Library | Declared / Detected Version | Configuration File |
| :--- | :--- | :--- | :--- |
| **Frontend Framework** | React | `^19.2.0` (installed `19.2.0`) | `frontend/package.json` |
| **Frontend DOM** | React DOM | `^19.2.0` (installed `19.2.0`) | `frontend/package.json` |
| **Frontend Routing** | React Router DOM | `^7.13.1` (installed `7.13.1`) | `frontend/package.json` |
| **Build Tool & Dev Server** | Vite | `^8.0.0` (installed `8.0.0`) | `frontend/package.json` |
| **CSS Framework** | Tailwind CSS | `^4.2.1` (installed `4.2.1` with `@tailwindcss/vite`) | `frontend/package.json` |
| **Icons** | Lucide React | `^1.16.0` (installed `1.16.0`) | `frontend/package.json` |
| **HTTP Client** | Axios | `^1.13.5` (installed `1.13.5`) | `frontend/package.json` |
| **Backend Language** | Java (JDK) | OpenJDK 25.0.2 / target `--release 17` | `backend/pom.xml` (`<maven.compiler.release>17`) |
| **Servlet Specification** | Jakarta Servlet API | `6.0.0` (Jakarta EE 10) | `backend/pom.xml` |
| **Web Container (Target)** | Apache Tomcat | `10.1.61` | Homebrew installation / deployment scripts |
| **Database Driver** | MySQL Connector/J | `9.2.0` (`com.mysql:mysql-connector-j`) | `backend/pom.xml` |
| **JSON Processing** | Google Gson | `2.12.1` (`com.google.code.gson:gson`) | `backend/pom.xml` |
| **Password Hashing** | jBCrypt | `0.4` (`org.mindrot:jbcrypt`) | `backend/pom.xml` |
| **Testing Framework** | JUnit Jupiter (JUnit 5) | `5.11.4` | `backend/pom.xml` |
| **Database Server** | MySQL Community Server | `9.2.0` (macOS arm64) | Active MySQL runtime port 3306 |
| **Package Management** | Maven / npm | Maven 3.9+ / npm 10.9+ | CLI runtime |

---

## 3. High-Level Architecture & Request Flow

EzExam implements a **Decoupled 4-Tier Client-Server Architecture**:

```
+-----------------------------------------------------------------------------------+
|                            CLIENT TIER (Browser)                                  |
|   React 19 SPA (Vite Dev Server :5173 / Production Static Build)                  |
|   - AuthContext & ProtectedRoute guards                                           |
|   - VIT Mumbai institutional branding & responsive layouts                        |
|   - ExamPage with question palette, timer, and state machine                      |
+-----------------------------------------------------------------------------------+
                                       |
                   HTTP / JSON (RESTful API via Proxy)
                   Cookie: JSESSIONID (HttpOnly, SameSite)
                                       v
+-----------------------------------------------------------------------------------+
|                       CONTROLLER TIER (Apache Tomcat 10.1)                        |
|   Context Path: /ezexam (or ROOT in Docker)                                       |
|   - CorsFilter (origin, credentials, header validation)                           |
|   - AuthServlet (/api/auth/*)                                                     |
|   - StudentExamServlet (/api/student/exams/*)                                     |
|   - StudentAttemptServlet (/api/student/attempts/*)                               |
|   - StudentResultServlet (/api/student/results/*)                                 |
|   - AdminServlet (/api/admin/*)                                                   |
+-----------------------------------------------------------------------------------+
                                       |
                                Method Calls
                                       v
+-----------------------------------------------------------------------------------+
|                        BUSINESS LOGIC & SERVICE TIER                              |
|   - ExamService (Evaluation, negative marking, attempt lifecycle, scoring)        |
|   - Password encryption & verification (BCrypt salt rounds = 10)                  |
|   - Answer masking (Zeroes out is_correct for student exam views)                 |
+-----------------------------------------------------------------------------------+
                                       |
                                Parameterized SQL
                                       v
+-----------------------------------------------------------------------------------+
|                          DATA ACCESS TIER (JDBC DAOs)                             |
|   - DBConnection (Connection management via DriverManager)                        |
|   - UserDAO, ExamDAO, QuestionDAO, AttemptDAO                                     |
|   - 100% PreparedStatements (Zero string-concatenated SQL)                       |
+-----------------------------------------------------------------------------------+
                                       |
                                TCP / Port 3306
                                       v
+-----------------------------------------------------------------------------------+
|                          PERSISTENCE TIER (MySQL 8/9)                             |
|   Database: ezexam (InnoDB, utf8mb4, utf8mb4_unicode_ci)                           |
|   - users, exams, questions, options, attempts, attempt_answers                   |
+-----------------------------------------------------------------------------------+
```

### Detailed Request Flow: Student Exam Lifecycle

1. **Authentication Check:** Frontend mounts `AuthContext` -> sends `GET /api/auth/me`. If session is valid, user object (`id`, `name`, `email`, `role`) is populated.
2. **Catalog Browsing:** Student visits `/dashboard` -> sends `GET /api/student/exams?onlyPublished=true` -> Backend returns published exams with duration, marks, question count, and past attempt status.
3. **Attempt Initiation:** Student clicks "Start Examination" on `/exam/:id/instructions` -> sends `POST /api/student/exams/:id/start`.
   - `ExamService.startAttempt()` verifies the exam exists and is published.
   - It executes `AttemptDAO.findActiveAttempt()`. If an in-progress attempt exists, it resumes it, maintaining the original `start_time`.
   - If no attempt exists, a new record is inserted into `attempts` with `status = 'in-progress'` and `start_time = NOW()`.
4. **Question Delivery (Security Enforced):** `ExamDAO.findById(examId, true, false)` fetches questions and options. **`is_correct` flags are stripped before serialization.** The student cannot inspect answers via DevTools.
5. **Exam Interaction:** Student navigates questions in `ExamPage.jsx`. Responses are maintained in React memory.
6. **Submission & Grading:**
   - Student clicks "Submit" or timer expires -> sends `POST /api/student/attempts/:id/submit` with body `{"answers": { "<questionId>": <optionId> }}`.
   - `ExamService.submitAttempt()` checks attempt ownership (`attempt.userId == currentUser.id`).
   - If already submitted, it acts idempotently and returns the stored result.
   - Evaluates each question: correct answers add full marks; incorrect answers deduct question-level or exam-level negative marks; unanswered questions contribute 0.
   - Writes audited selections into `attempt_answers` and updates `attempts` with score, status (`submitted`), percentage, and pass/fail flag.
7. **Scorecard Display:** Student is redirected to `/result/:attemptId` -> `GET /api/student/results/:attemptId` delivers complete result analysis.

---

## 4. Frontend & Backend Structure

### 4.1 Frontend Architecture (`/Users/nihartambe/Desktop/ezexam/frontend`)

```
frontend/
├── index.html                  # HTML5 entry with Google Fonts (Inter)
├── package.json                # Dependencies (React 19, Tailwind 4, Vite 8)
├── vite.config.js              # Vite config with /api proxy to localhost:8080/ezexam
├── public/                     # Static assets (vit-logo.png) [Currently Untracked]
└── src/
    ├── App.jsx                 # Route registry & ProtectedRoute hierarchy
    ├── main.jsx                # DOM root mount
    ├── index.css               # Global Tailwind directives & VIT color variables
    ├── context/
    │   └── AuthContext.jsx     # Authentication state provider (user, login, logout, loading)
    ├── services/
    │   └── api.js              # Centralized Axios client with withCredentials: true
    ├── data/
    │   └── mockData.js         # Offline fallback data definitions
    ├── components/
    │   ├── Navbar.jsx          # Institutional header with VIT branding, session pills, logout
    │   ├── Sidebar.jsx         # Navigation links for Admin and Student portals
    │   ├── ExamCard.jsx        # Exam listing cards with tabular metrics
    │   ├── Question.jsx        # MCQ question renderer with custom radio options
    │   ├── QuestionPalette.jsx # Status grid (Answered, Unanswered, Marked for Review, Current)
    │   └── Timer.jsx           # Countdown clock with warning triggers (< 5 min)
    └── pages/
        ├── Login.jsx           # Portal sign-in with student/admin demo shortcuts
        ├── Register.jsx        # Student registration form
        ├── StudentDashboard.jsx# Active exams, ongoing attempts, student metrics
        ├── ExamInstructions.jsx# Pre-exam syllabus, rules, grading disclosure
        ├── ExamPage.jsx        # Full-screen proctored-style examination interface
        ├── ResultPage.jsx      # Post-exam scorecard, score breakdown, performance badges
        ├── ExamHistory.jsx     # Historical log of completed attempts
        ├── AdminDashboard.jsx  # System KPIs, student rosters, attempt monitors
        ├── ManageExams.jsx     # CRUD modal for creating/publishing examinations
        └── ManageQuestions.jsx # MCQ question authoring interface with option selectors
```

#### Frontend Route Registry

| Path | Component | Protected | Allowed Role | Responsibility |
| :--- | :--- | :--- | :--- | :--- |
| `/` | Redirect | No | Any | Redirects to `/dashboard` |
| `/login` | `Login.jsx` | Public | Unauthenticated | User login & demo credential triggers |
| `/register` | `Register.jsx` | Public | Unauthenticated | Student account registration |
| `/dashboard` | `StudentDashboard.jsx` | Yes | `student` | Available exams, pending attempts, quick stats |
| `/exam/:id/instructions` | `ExamInstructions.jsx` | Yes | `student` | Rules, duration, marking scheme confirmation |
| `/exam/:id` | `ExamPage.jsx` | Yes | `student` | Active exam interface with exit modal & timer |
| `/result/:attemptId` | `ResultPage.jsx` | Yes | `student` | Scorecard and pass/fail evaluation |
| `/history` | `ExamHistory.jsx` | Yes | `student` | Past examination attempts list |
| `/admin` | `AdminDashboard.jsx` | Yes | `admin` | Global analytics, student counts, attempts log |
| `/admin/exams` | `ManageExams.jsx` | Yes | `admin` | Create, edit, toggle draft/published, delete exams |
| `/admin/exams/:id/questions` | `ManageQuestions.jsx`| Yes | `admin` | Author/delete questions & configure correct choices |
| `*` | Fallback | No | Any | Redirects unmapped URLs to `/dashboard` |

#### Visual Design & VIT Branding System
- **Institutional Primary:** Deep VIT Navy/Purple (`#4f2d7f`, hover `#43256d`, dark `#381f5c`).
- **Institutional Secondary/Accents:** Royal Gold / Amber (`#f59e0b` / `#d97706`).
- **Typography:** Inter (via Google Fonts) with CSS `font-variant-numeric: tabular-nums` to ensure all timestamps, timer figures, scores, and question counters render with stable monospace alignment.
- **Logomark:** Located at `frontend/public/vit-logo.png` and rendered alongside `EzExam — VIT Mumbai Examination Portal`.

---

### 4.2 Backend Architecture (`/Users/nihartambe/Desktop/ezexam/backend`)

```
backend/
├── pom.xml                                 # Maven Project Object Model (WAR packaging)
└── src/
    ├── main/
    │   ├── java/com/exam/
    │   │   ├── controller/
    │   │   │   ├── BaseServlet.java        # Shared JSON parsing, CORS headers, auth checks
    │   │   │   ├── AuthServlet.java        # /api/auth (login, logout, register, me)
    │   │   │   ├── StudentExamServlet.java # /api/student/exams (browse, view, start)
    │   │   │   ├── StudentAttemptServlet.java # /api/student/attempts (submit)
    │   │   │   ├── StudentResultServlet.java  # /api/student/results (scorecard, history)
    │   │   │   └── AdminServlet.java       # /api/admin/* (exams, questions, stats, students)
    │   │   ├── dao/
    │   │   │   ├── UserDAO.java            # User persistence & email lookup
    │   │   │   ├── ExamDAO.java            # Exam queries with question relations
    │   │   │   ├── QuestionDAO.java        # Question & Option relational CRUD
    │   │   │   └── AttemptDAO.java         # Attempt tracking & answer persistence
    │   │   ├── filter/
    │   │   │   └── CorsFilter.java         # CORS headers, preflight OPTIONS, credentials
    │   │   ├── model/
    │   │   │   ├── User.java               # User model (password marked transient for Gson)
    │   │   │   ├── Exam.java               # Exam entity with duration, status, negative marks
    │   │   │   ├── Question.java           # MCQ entity with marks and nested Options
    │   │   │   ├── Option.java             # Choice entity with isCorrect flag
    │   │   │   ├── Attempt.java            # Attempt state, timing, and score summary
    │   │   │   └── AttemptAnswer.java      # Per-question response record
    │   │   ├── service/
    │   │   │   └── ExamService.java        # Grading engine, negative marks, attempt state
    │   │   └── util/
    │   │       ├── DBConnection.java       # JDBC connection manager with env var fallback
    │   │       └── DateTimeUtil.java       # ISO-8601 <-> SQL DATETIME converter
    │   └── resources/
    │       ├── schema.sql                  # Database DDL script
    │       ├── seed.sql                    # Initial demo data & pre-hashed accounts
    │       └── db.properties               # Local DB credentials (Gitignored)
    └── test/
        └── java/com/exam/
            ├── ExamServiceWorkflowTest.java   # Workflow & calculation tests
            ├── DatabaseIntegrationTest.java   # Live MySQL query verification
            ├── DateTimeUtilTest.java          # Timestamp formatting tests
            ├── SecurityAndEvaluationTest.java # BCrypt & scoring tests
            ├── SecurityAndAuthorizationTest.java # Role-based access tests
            └── ExamLifecycleAndDateTest.java  # Attempt dates & states tests
```

---

## 5. API Endpoint Inventory

All endpoints accept and return `application/json; charset=UTF-8`.

| HTTP Method | Endpoint Pattern | Auth Req. | Required Role | Payload / Params | Response Status & Body | Description |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | No | None | `{"name": "...", "email": "...", "password": "..."}` | `201 Created`: User object | Registers new student account with BCrypt hash |
| `POST` | `/api/auth/login` | No | None | `{"email": "...", "password": "..."}` | `200 OK`: User object | Validates password, creates `HttpSession` |
| `POST` | `/api/auth/logout` | Yes | Any | None | `200 OK`: `{"message": "Logged out"}` | Invalidates active `HttpSession` |
| `GET` | `/api/auth/me` | No | Any | None (Session Cookie) | `200 OK`: User object or `null` | Returns current session user |
| `GET` | `/api/student/exams` | Yes | `student` | `?onlyPublished=true` | `200 OK`: Array of Exam objects | Lists exams visible to students |
| `GET` | `/api/student/exams/:id` | Yes | `student` | None | `200 OK`: Exam with Questions | Fetches exam questions (`is_correct` stripped) |
| `POST` | `/api/student/exams/:id/start` | Yes | `student` | None | `200 OK`: Attempt object | Starts new attempt or resumes existing active attempt |
| `POST` | `/api/student/attempts/:id/submit`| Yes | `student` | `{"answers": {"101": 1002}}` | `200 OK`: Evaluated Attempt | Calculates score, saves answers, completes attempt |
| `GET` | `/api/student/results` | Yes | `student` | `?userId=X` | `200 OK`: Array of Attempts | Lists student's completed exams |
| `GET` | `/api/student/results/:attemptId` | Yes | `student` | None | `200 OK`: Detailed Attempt | Full scorecard breakdown |
| `GET` | `/api/admin/stats` | Yes | `admin` | None | `200 OK`: KPI counters | Returns count of students, exams, attempts |
| `GET` | `/api/admin/attempts` | Yes | `admin` | None | `200 OK`: Array of Attempts | Lists 20 most recent student attempts |
| `GET` | `/api/admin/students` | Yes | `admin` | None | `200 OK`: Array of Students | Lists all registered student accounts |
| `POST` | `/api/admin/exams` | Yes | `admin` | `{"title": "...", "durationMinutes": 30, ...}` | `201 Created`: Exam object | Creates new examination record |
| `PUT` | `/api/admin/exams/:id` | Yes | `admin` | Updated Exam JSON | `200 OK`: Exam object | Updates exam title, time, passing %, status |
| `DELETE` | `/api/admin/exams/:id` | Yes | `admin` | None | `200 OK`: `{"success": true}` | Deletes exam (cascades to questions & options) |
| `POST` | `/api/admin/exams/:id/questions` | Yes | `admin` | `{"questionText": "...", "marks": 2, "options": [...]}` | `201 Created`: Question | Appends MCQ with marked correct option |
| `PUT` | `/api/admin/questions/:id` | Yes | `admin` | Updated Question JSON | `200 OK`: Question | Updates question text, marks, or options |
| `DELETE` | `/api/admin/questions/:id` | Yes | `admin` | None | `200 OK`: `{"success": true}` | Deletes question and its options |

---

## 6. Database Schema & Persistence Architecture

### 6.1 Relational Schema Overview

The database is named `ezexam` and uses the `utf8mb4` character set with `utf8mb4_unicode_ci` collation.

```
       +------------------+
       |      users       |
       +------------------+
       | id (PK)          |
       | name             |
       | email (UNIQUE)   |
       | password (BCrypt)|
       | role (ENUM)      |
       | created_at       |
       +------------------+
          |            |
          | 1:N        | 1:N
          v            v
+------------------+ +-----------------------------------------+
|      exams       | |                attempts                 |
+------------------+ +-----------------------------------------+
| id (PK)          | | id (PK)                                 |
| title            | | user_id (FK -> users.id CASCADE)        |
| description      | | exam_id (FK -> exams.id CASCADE)        |
| duration_minutes | | status (ENUM 'in-progress','submitted') |
| total_marks      | | start_time (DATETIME)                   |
| passing_pct      | | submitted_at (DATETIME)                 |
| negative_mark    | | score, total, correct, wrong, passed    |
| status (ENUM)    | +-----------------------------------------+
| created_by (FK)  |                     |
+------------------+                     | 1:N
          |                              v
          | 1:N              +-----------------------------------------+
          v                  |             attempt_answers             |
+------------------+         +-----------------------------------------+
|    questions     |         | id (PK)                                 |
+------------------+         | attempt_id (FK -> attempts.id CASCADE)  |
| id (PK)          | <-------| question_id (FK -> questions.id CASCADE)|
| exam_id (FK)     |         | selected_option_id (FK -> options.id)   |
| question_text    |         | is_correct (BOOLEAN)                    |
| marks            |         | marks_awarded (DECIMAL)                 |
| negative_marks   |         +-----------------------------------------+
+------------------+
          |
          | 1:N
          v
+------------------+
|     options      |
+------------------+
| id (PK)          |
| question_id (FK) |
| text             |
| is_correct (BOOL)|
+------------------+
```

### 6.2 Seed Accounts & Pre-Configured Records

The schema import includes safe `INSERT IGNORE` statements in `backend/src/main/resources/seed.sql`:

* **Administrator Demo Account:**
  - Name: `Admin User`
  - Email: `admin@ezexam.com`
  - Role: `admin`
  - Password Hash: Salted BCrypt (`$2a$10$...`)
* **Student Demo Account:**
  - Name: `Aarav Sharma`
  - Email: `student@ezexam.com`
  - Role: `student`
  - Password Hash: Salted BCrypt (`$2a$10$...`)
* **Pre-Seeded Exams:**
  - Exam 1: *Data Structures Mid-Term* (5 Questions, 20 Marks, 30 Min, 0.25 Negative Marking, Status: `published`).
  - Exam 2: *Java Programming Quiz* (3 Questions, 10 Marks, 20 Min, 0.00 Negative Marking, Status: `published`).
  - Exam 3: *DBMS Fundamentals* (Draft, 45 Min, Status: `draft`).
* **Pre-Seeded Attempt:**
  - Attempt #9001 for Student #2 on Exam #2 (Status: `submitted`, Score: 4.00/6, Percentage: 66.67%, Passed: `true`).

### 6.3 Connection Loading & Environment Variable Precedence

The `DBConnection.java` utility dynamically reads database settings using a strict 3-tier fallback hierarchy:

1. **Environment Variables (Highest Priority):**
   - `EZEXAM_DB_URL`: JDBC connection URL.
   - `EZEXAM_DB_USER`: Database username.
   - `EZEXAM_DB_PASSWORD`: Database password.
2. **Classpath Resource (`db.properties`):**
   - Keys: `db.url`, `db.user`, `db.password`.
3. **Hardcoded Fallback (Local Development Only):**
   - URL: `jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - User: `root`
   - Password: `""` (empty)

---

## 7. Feature Verification Matrix

| Area | Feature | Status | Verification Evidence |
| :--- | :--- | :--- | :--- |
| **Auth** | User Registration | Implemented and verified | Tested via `AuthServlet` unit test; BCrypt hash verified |
| **Auth** | User Login & Session | Implemented and verified | `POST /api/auth/login` sets `HttpSession` and returns user profile |
| **Auth** | User Logout | Implemented and verified | Invalidates session cookie; clears frontend context |
| **Auth** | Role-Based Access Control | Implemented and verified | `SecurityAndAuthorizationTest` confirms students cannot reach `/api/admin/*` |
| **Student** | Available Exams Catalog | Implemented and verified | `GET /api/student/exams` filters by `onlyPublished=true` |
| **Student** | Exam Instructions Page | Implemented and verified | Confirms duration, marks, rules, negative marking |
| **Student** | Start / Resume Attempt | Implemented and verified | Resumes active attempt timestamp; prevents multiple in-progress attempts |
| **Student** | Full-Screen Exam UI | Implemented and verified | Renders question palette, active question, and option selectors |
| **Student** | Exit Examination Modal | Implemented and verified | Added safely; allows graceful exit to dashboard without forced submission |
| **Student** | Answer Submission & Grading | Implemented and verified | `ExamServiceWorkflowTest` verified: 100% accurate score calculation |
| **Student** | Negative Marking | Implemented and verified | Deductions verified in `ExamServiceWorkflowTest` |
| **Student** | Scorecard & Results Page | Implemented and verified | Displays correct/wrong counts, score, percentage, pass/fail badge |
| **Student** | Exam Attempt History | Implemented and verified | Lists past attempts with timestamps and scores |
| **Student** | Incremental Answer Persistence | **Not implemented** | Answers are kept in React state; lost if browser is refreshed before submit |
| **Admin** | Dashboard KPI Metrics | Implemented and verified | `GET /api/admin/stats` returns accurate counts |
| **Admin** | Recent Attempts Monitor | Implemented and verified | Lists recent student attempts with completion timestamps |
| **Admin** | Student Roster List | Implemented and verified | Fetches registered students and attempt aggregates |
| **Admin** | Create & Edit Exam | Implemented and verified | Creates exam with title, duration, marks, passing %, negative marks |
| **Admin** | Publish / Unpublish Toggle | Implemented and verified | Controls visibility in student portal |
| **Admin** | Delete Exam (Cascading) | Implemented and verified | Deletes exam and cascades to questions/options |
| **Admin** | Question CRUD Interface | Implemented and verified | Adds/edits MCQs with options and sets correct choice |
| **Admin** | Automatic Exam Expiration Daemon | **Not implemented** | No background task auto-submits abandoned in-progress attempts |

---

## 8. Security Audit Findings

Each issue is classified according to standard security severity levels:

### [MEDIUM] Hardcoded Plaintext Database Credentials on Disk
- **File:** `backend/src/main/resources/db.properties`
- **Finding:** The file contains active local database credentials (`db.user` and `db.password`).
- **Risk:** While ignored by `.gitignore`, developers could accidentally force-commit the file (`git add -f`) or leave it accessible on shared workstations.
- **Remediation:** Remove real credentials before publishing. Rely on `EZEXAM_DB_URL`, `EZEXAM_DB_USER`, `EZEXAM_DB_PASSWORD` environment variables and supply a sanitized `db.properties.example`.

### [MEDIUM] Plaintext Test Credentials Documented in Repository README
- **File:** `backend/README.md` (Lines 203–208)
- **Finding:** The README documents demo student and admin credentials in plaintext.
- **Risk:** While these are demo accounts meant for local evaluation, public deployment with these accounts active poses an immediate unauthorized administrative takeover risk.
- **Remediation:** Document that default seed accounts must have their passwords rotated immediately upon deployment to any public environment.

### [LOW] Session Cookie Configuration for Cross-Origin Deployment
- **File:** `backend/src/main/java/com/exam/controller/AuthServlet.java` / Tomcat Container Config
- **Finding:** Session cookies rely on default servlet container behavior. In a cross-origin production deployment (e.g., frontend on `ezexam.vercel.app` and backend on `ezexam-api.onrender.com`), standard cookies are blocked by modern browsers unless configured with `SameSite=None; Secure`.
- **Remediation:** Host both frontend and backend on the same apex domain using reverse proxy rewrites, or configure Tomcat's `context.xml` with `SameSiteCookies="none"` and ensure HTTPS is enforced.

### [LOW] Missing Incremental Answer Persistence
- **File:** `backend/src/main/java/com/exam/dao/AttemptDAO.java` and `frontend/src/pages/ExamPage.jsx`
- **Finding:** Student answers are only sent to the server in a single batch during final submission (`POST /api/student/attempts/:id/submit`).
- **Risk:** If a student's browser crashes, closes, or refreshes mid-exam, their previously selected answers are lost, even though the exam timer continues running on the backend.
- **Remediation:** Implement an incremental auto-save endpoint (`PUT /api/student/attempts/:id/answers`) that persists answers per question selection.

### [INFORMATIONAL] SQL Injection Defense Verified
- **Verification:** An inspection of `UserDAO`, `ExamDAO`, `QuestionDAO`, and `AttemptDAO` confirmed that 100% of SQL queries utilize `java.sql.PreparedStatement` with parameter binding (`?`). No dynamic string concatenation was detected.

### [INFORMATIONAL] Exam Answer Leakage Prevention Verified
- **Verification:** `ExamDAO.findById(examId, true, false)` and `QuestionDAO.findByExamId()` strictly set `is_correct = false` on all options when fulfilling student requests, preventing client-side inspection vulnerabilities.

---

## 9. GitHub Readiness Checklist

| Item | Status | Finding / Action Required |
| :--- | :---: | :--- |
| **Root `.gitignore`** | ⚠️ Needs Update | Covers `**/db.properties`, `target/`, `node_modules/`, and `.DS_Store`. Must be verified at root. |
| **Untracked Backend Source** | ❌ **BLOCKER** | `backend/pom.xml`, `backend/src/main/`, and `backend/src/test/` are **untracked**. Must be staged. |
| **Untracked Static Assets** | ❌ **BLOCKER** | `frontend/public/` (containing `vit-logo.png`) is **untracked**. Must be staged. |
| **Untracked Shell Scripts** | ⚠️ Attention | `setup-db.sh` is untracked. |
| **Active Git Remote** | ⚠️ Attention | Current remote is `https://github.com/gaurangkhochare/ezexam.git` on branch `frontend-dev`. A new remote/branch must be configured for the new repository. |
| **Uncommitted Working Changes** | ⚠️ Attention | 25 tracked frontend files have uncommitted changes. Must be cleanly committed. |
| **API Keys / Secrets** |  Clean | No cloud tokens, private keys, or third-party API secrets exist in the codebase. |
| **Hardcoded Localhost in Frontend** |  Clean | `frontend/src/services/api.js` uses relative `/api` base URL; no hardcoded `http://localhost:8080`. |
| **Hardcoded Paths in Scripts** | ⚠️ Attention | `setup-db.sh` has hardcoded Homebrew macOS Tomcat paths (`/opt/homebrew/Cellar/...`). |
| **Template Configuration** | ⚠️ Missing | `backend/src/main/resources/db.properties.example` must be created. |

---

## 10. Deployment Architecture Recommendation

### Recommended Architecture: Containerized Tomcat + Managed MySQL + Static CDN Proxy

For a college project, the primary objectives are **zero-to-low hosting cost, high reliability, simplicity, and prevention of CORS/cookie issues**.

```
+------------------------------------+
|            USERS / CLIENTS         |
+------------------------------------+
                  |
                  | HTTPS (port 443)
                  v
+------------------------------------+
|        VERCEL / NETLIFY CDN        |
|  - Hosts React 19 Frontend Build   |
|  - vercel.json rewrite rule:       |
|    /api/:path*  --->               |
+------------------------------------+
                  |
                  | HTTPS (Reverse Proxy / Same-Origin)
                  v
+------------------------------------+
|    RENDER / RAILWAY CONTAINER      |
|  - Docker: Tomcat 10.1 + JDK 17    |
|  - ROOT.war (EzExam Backend)       |
|  - Env vars: EZEXAM_DB_URL, etc.   |
+------------------------------------+
                  |
                  | MySQL SSL Connection
                  v
+------------------------------------+
|     RAILWAY / AIVEN MYSQL 8+       |
|  - Managed Database                |
|  - Auto-restarts & daily backups   |
+------------------------------------+
```

### Why This Architecture Wins:
1. **Eliminates CORS and Cross-Origin Cookies:** Because Vercel's edge network proxies `/api/*` requests directly to the Render/Railway backend, the browser communicates exclusively with a single origin. No third-party cookie blocking or complex CORS headers can break the `JSESSIONID` authentication.
2. **Standard Java Servlet Compatibility:** Hosting the backend in a standard `tomcat:10.1-jdk17` Docker container guarantees 100% Jakarta EE 10 compatibility without requiring Tomcat configuration on the host machine.
3. **Free / Low Cost:** Vercel (free tier), Render (free tier web service), and Aiven/Railway (free trial / low-cost database credits) satisfy all project needs without requiring paid enterprise infrastructure.

### Alternative Architecture: Single Cloud VPS (DigitalOcean / Oracle Cloud Always Free)
- Deploy an Ubuntu 24.04 LTS instance running Nginx, Tomcat 10.1, and MySQL 8.4.
- Nginx terminates SSL via Let's Encrypt, serves `frontend/dist` as static files, and reverse-proxies `/api/` to `http://127.0.0.1:8080/ezexam/api/`.
- Total cost: $0 (Oracle Cloud Always Free) or $4–$6/month (DigitalOcean Droplet).

---

## 11. Environment Variables & Configuration Requirements

In production, no database credentials should reside in `db.properties`. The application must be configured using environment variables:

### Backend Runtime Environment Variables

| Variable Name | Required | Example Placeholder Value | Purpose |
| :--- | :---: | :--- | :--- |
| `EZEXAM_DB_URL` | Yes | `jdbc:mysql://<db-host>:3306/<database>?useSSL=true&serverTimezone=UTC` | Database JDBC URL |
| `EZEXAM_DB_USER` | Yes | `ezexam_prod_user` | Dedicated database username |
| `EZEXAM_DB_PASSWORD` | Yes | `<SECURE_DATABASE_PASSWORD>` | Database user password |
| `PORT` | Optional | `8080` | Container listening port (Render/Railway) |
| `JAVA_OPTS` | Optional | `-Xms256m -Xmx512m` | JVM memory limits for low-memory tiers |

### Frontend Environment Configuration

In development, Vite proxies `/api` via `vite.config.js`. In production:
- If using Vercel/Netlify proxy rewrites, no build-time variable is needed (relative `/api` works automatically).
- If deploying frontend to a standalone domain without proxying, provide:
  - `VITE_API_BASE_URL`: `https://<backend-domain>/ezexam/api` (requires CORS and cross-origin cookie configuration).

---

## 12. Local Development & Build Commands

### 12.1 Backend Build & Packaging
From `/Users/nihartambe/Desktop/ezexam/backend`:

```bash
# 1. Compile source code
mvn clean compile

# 2. Run automated test suite (16 unit and integration tests)
mvn test

# 3. Package production WAR archive
mvn clean package -DskipTests=false
```
*Output Artifact:* `backend/target/ezexam.war`

### 12.2 Frontend Build & Validation
From `/Users/nihartambe/Desktop/ezexam/frontend`:

```bash
# 1. Install dependencies
npm install

# 2. Run local development server
npm run dev
# Running at: http://localhost:5173

# 3. Create production static bundle
npm run build
```
*Output Artifact:* `frontend/dist/`

---

## 13. Testing Results & Verification Status

### 13.1 Tests Actually Executed & Verified

| Test Suite | Class Name | Tests Run | Result | Execution Time |
| :--- | :--- | :---: | :---: | :---: |
| **Workflow Tests** | `com.exam.ExamServiceWorkflowTest` | 4 |  Passed | 0.215s |
| **Database Integration** | `com.exam.DatabaseIntegrationTest` | 1 |  Passed | 0.079s |
| **Date/Time Utilities** | `com.exam.DateTimeUtilTest` | 4 |  Passed | 0.005s |
| **Security & Evaluation** | `com.exam.SecurityAndEvaluationTest` | 2 |  Passed | 0.279s |
| **Security & Auth** | `com.exam.SecurityAndAuthorizationTest` | 3 |  Passed | 0.239s |
| **Exam Lifecycle** | `com.exam.ExamLifecycleAndDateTest` | 2 |  Passed | 0.020s |
| **Total Backend Tests** | **All Classes** | **16** | **0 Failures, 0 Errors** | **2.933s** |

### 13.2 Frontend Build Verification
- Command: `npm run build` executed in `frontend`.
- Result: **Successful build in 298ms**.
- Chunks:
  - `dist/index.html` (0.83 kB)
  - `dist/assets/index-*.css` (48.33 kB / gzip: 9.38 kB)
  - `dist/assets/index-*.js` (342.36 kB / gzip: 104.75 kB)

---

## 14. Known Limitations & Technical Debt

1. **Client-Bound Answer State:** While an active exam attempt record is persisted in MySQL (`attempts` table with `start_time`), student choices during an exam remain in React memory until the final submit button is pressed. A mid-exam network disconnect or accidental browser tab close requires the student to re-select options upon resuming.
2. **Missing Server-Side Expiration Sweeper:** If a student closes their laptop without clicking "Submit," the attempt remains permanently in `in-progress` status in the database. When they return after the deadline, `ExamService` detects the expired time window and rejects submission, but no automatic background cron marks abandoned attempts as `submitted` or `expired`.
3. **Local macOS Script Coupling:** `setup-db.sh` contains hardcoded paths to Homebrew macOS paths (`/opt/homebrew/Cellar/tomcat@10/...`). This script is non-portable to Linux or Windows and should be rewritten or complemented by Docker Compose.
4. **README Setup Discrepancies:** `backend/README.md` references a non-existent `db.properties.example` file and hardcoded Mac directories.

---

## 15. Prioritized Action Plan for Phase 2 (GitHub & Deployment)

### Step 1: Git Hygiene & Repository Preparation
1. Create `backend/src/main/resources/db.properties.example` with sanitized placeholder values.
2. Confirm `backend/src/main/resources/db.properties` is removed from tracking and strictly ignored.
3. Stage the untracked backend source files (`backend/pom.xml`, `backend/src/main/`, `backend/src/test/`), the frontend assets (`frontend/public/`), and `setup-db.sh`.
4. Create a clean commit on a clean working branch (`main`).
5. Configure new GitHub remote repository and push code.

### Step 2: Deployment Preparation & Containerization
1. Create a production `Dockerfile` in `backend/` using `tomcat:10.1-jdk17-temurin` that copies `ezexam.war` to `/usr/local/tomcat/webapps/ROOT.war`.
2. Create a `docker-compose.yml` defining the multi-container stack (Tomcat Backend, MySQL 8 Database, and Nginx reverse proxy) for instant deployment and reproducibility.
3. Configure `vercel.json` in `frontend/` with rewrite rules pointing `/api/*` to the deployed backend URL to secure session cookies and prevent cross-origin issues.

### Step 3: Database & Cloud Deployment
1. Provision a managed MySQL instance (Railway, Aiven, or Cloud SQL).
2. Execute `schema.sql` and `seed.sql` against the production database.
3. Rotate default demo passwords.
4. Deploy the backend Docker container to Render or Railway with `EZEXAM_DB_*` environment variables.
5. Deploy the React frontend to Vercel/Netlify.
6. Run end-to-end smoke tests on the live URLs.

---

*Report compiled and certified on October 23, 2026. EzExam project inspection complete.*
