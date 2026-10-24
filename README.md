# EzExam — VIT Mumbai Examination Portal

An enterprise-grade online examination management portal tailored for the academic workflow and institutional identity of **Vidyalankar Institute of Technology (VIT), Mumbai**. EzExam delivers a secure, timed exam environment with anti-cheating controls, interactive question palettes, automated evaluation with negative marking, and comprehensive administrator oversight.

---

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Repository Structure](#repository-structure)
- [Database Initialization](#database-initialization)
- [Database Configuration](#database-configuration)
- [Backend Setup & Execution](#backend-setup--execution)
- [Frontend Setup & Execution](#frontend-setup--execution)
- [Demo Credentials](#demo-credentials)
- [API Architecture](#api-architecture)
- [Current Deployment Status](#current-deployment-status)
- [License](#license)

---

## Features

### Student Examination Experience
- **Institutional Authentication**: Secure login and self-registration for students with BCrypt password hashing.
- **Available Exam Catalog**: View published exams with course details, total questions, duration, and passing criteria.
- **Exam Instructions & Acknowledgement**: Pre-exam briefing screen enforcing institutional testing rules.
- **Timed Exam Runner**: Real-time persistent countdown timer with auto-submission when time expires.
- **Interactive Question Palette**: Visual question status indicators (Answered, Flagged for Review, Visited, Unvisited).
- **Security & Integrity**: Correct answers are strictly excluded from student API payloads to prevent client-side inspection.
- **Automated Score Evaluation**: Instant calculation of correct/incorrect answers, negative marking deduction, percentage, and pass/fail status.
- **Exam History & Scorecards**: Student dashboard displaying historical attempts and detailed question-by-question breakdown.

### Administrator Console
- **Analytics Dashboard**: Real-time counters for registered students, total exams, published exams, and completed attempts.
- **Recent Attempts Feed**: Audit trail of the latest examination attempts across the institute.
- **Exam Lifecycle Management**: Create, edit, publish/unpublish, and delete exams with configurable duration, passing percentage, and negative marking factors.
- **MCQ Question Authoring**: Add, edit, and delete multiple-choice questions with 4 options and answer designation.
- **Student Performance Directory**: View student roster with individual attempt statistics.

---

## Technology Stack

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend Framework** | React | 19.2.0 | User interface & component architecture |
| **Build Tooling** | Vite | 8.x | Lightning-fast HMR and production bundling |
| **Styling** | Tailwind CSS / Vanilla CSS | 4.x | VIT Mumbai identity (`#48499A`), glassmorphism, responsive design |
| **Icons** | Lucide React | 1.16.x | UI iconography |
| **Routing** | React Router DOM | 7.x | Client-side routing with role-based `ProtectedRoute` guards |
| **HTTP Client** | Axios & Fetch API | 1.13.x | API communication with session cookie support (`credentials: 'include'`) |
| **Backend Language** | Java (JDK) | 17+ (compiled `--release 17`) | Robust object-oriented server logic |
| **Servlet Specification** | Jakarta Servlet API | 6.0.0 (Jakarta EE 10) | HTTP request handling, session management, CORS filtering |
| **Web Container** | Apache Tomcat | 10.1.x | Enterprise servlet container running `ezexam.war` |
| **Database Driver** | MySQL Connector/J | 9.2.0 | High-performance JDBC connectivity |
| **JSON Serialization** | Google Gson | 2.12.1 | Fast JSON serialization with transient field exclusion |
| **Password Hashing** | jBCrypt | 0.4 | Salted BCrypt password hashing (10 rounds) |
| **Testing** | JUnit Jupiter (JUnit 5) | 5.11.4 | Automated unit and integration testing |
| **Relational Database** | MySQL Server | 8.0+ / 9.x | ACID-compliant relational data store on port 3306 |

---

## Prerequisites

Before setting up EzExam locally, ensure the following software is installed:

- **Java Development Kit (JDK)**: JDK 17 or higher (tested with OpenJDK 17, 21, and 25/27).
  ```bash
  java -version
  ```
- **Apache Maven**: Version 3.9 or higher.
  ```bash
  mvn -version
  ```
- **MySQL Server**: Version 8.0, 8.4, 9.0, or higher, running locally on port `3306`.
  ```bash
  mysql --version
  ```
- **Apache Tomcat**: Version 10.1.x (Jakarta EE 10 namespace required; Tomcat 9 is incompatible due to `javax.*` vs `jakarta.*` packages).
- **Node.js & npm**: Node.js v18 or higher with npm v9+.
  ```bash
  node -v
  npm -v
  ```

---

## Repository Structure

```text
ezexam/
├── .gitignore                          # Configured security & build exclusions
├── .vscode/                            # Shared workspace editor settings
├── LICENSE                             # Project license
├── README.md                           # Main project documentation (this file)
├── setup-db.sh                         # Interactive database initialization script
├── backend/
│   ├── pom.xml                         # Maven build configuration (Java 17 / WAR)
│   ├── README.md                       # Detailed backend architecture documentation
│   └── src/
│       ├── main/
│       │   ├── java/com/exam/
│       │   │   ├── dao/                # Data Access Objects (UserDAO, ExamDAO, etc.)
│       │   │   ├── filter/             # CorsFilter (cross-origin request management)
│       │   │   ├── model/              # Domain models (User, Exam, Question, Attempt)
│       │   │   ├── service/            # Business logic (AuthService, ExamService)
│       │   │   ├── servlet/            # Jakarta Servlets (Auth, Student, Admin)
│       │   │   └── util/               # DBConnection, DateTimeUtil
│       │   ├── resources/
│       │   │   ├── db.properties.example # Safe credential template with placeholders
│       │   │   ├── schema.sql          # Relational DDL (6 normalized tables)
│       │   │   └── seed.sql            # Seed dataset (demo exams, questions, accounts)
│       │   └── webapp/WEB-INF/web.xml  # Deployment descriptor & servlet mapping
│       └── test/java/com/exam/         # JUnit 5 test suite (16 tests)
├── frontend/
│   ├── index.html                      # Single page application entry point
│   ├── package.json                    # Frontend dependencies and npm scripts
│   ├── vite.config.js                  # Vite configuration & /api reverse proxy
│   ├── public/                         # Static institutional assets (logos, campus images)
│   └── src/
│       ├── App.jsx                     # Route definitions & navigation shell
│       ├── index.css                   # VIT institutional design system tokens & utilities
│       ├── components/                 # Reusable UI components (Timer, Palette, Cards)
│       ├── context/                    # AuthContext (state, role, session tracking)
│       ├── pages/                      # Application views (Student, Admin, Exams)
│       └── services/                   # API client layer (api.js)
└── docs/
    ├── GITHUB_PREPARATION_REPORT.md   # Local preparation and security audit report
    └── PROJECT_AUDIT_AND_DEPLOYMENT_PLAN.md # Technical audit & deployment specifications
```

---

## Database Initialization

EzExam requires a MySQL database named `ezexam`. You can initialize the database using either the automated setup script or manual SQL commands.

### Option A: Automated Setup (Recommended)

Run the included setup script from the project root:

```bash
chmod +x setup-db.sh
./setup-db.sh
```

The script interactively prompts for your local MySQL administrative credentials (hidden input), creates the `ezexam` database, applies the schema and seed data, creates an application user, and configures `db.properties`.

### Option B: Manual Initialization

1. Log into your MySQL server:
   ```bash
   mysql -u root -p
   ```
2. Import the schema to create the required 6 tables:
   ```bash
   mysql -u root -p < backend/src/main/resources/schema.sql
   ```
3. Import the seed data to create initial exams, questions, and demo accounts:
   ```bash
   mysql -u root -p < backend/src/main/resources/seed.sql
   ```

---

## Database Configuration

The backend reads database credentials with the following priority:
1. **Environment Variables**: `EZEXAM_DB_URL`, `EZEXAM_DB_USER`, `EZEXAM_DB_PASSWORD`
2. **Properties File**: `backend/src/main/resources/db.properties`
3. **Localhost Fallback**: Standard default values

### Using `db.properties` (Recommended for Local Dev)

Copy the safe example template:

```bash
cp backend/src/main/resources/db.properties.example backend/src/main/resources/db.properties
```

Edit `backend/src/main/resources/db.properties` with your local credentials:

```properties
db.url=jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=your_mysql_username
db.password=your_mysql_password
```

> **Security Note:** `db.properties` is strictly excluded in `.gitignore` and must never be committed to source control.

### Using Environment Variables (Alternative)

```bash
export EZEXAM_DB_URL="jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export EZEXAM_DB_USER="your_mysql_username"
export EZEXAM_DB_PASSWORD="your_mysql_password"
```

---

## Backend Setup & Execution

### 1. Run Backend Automated Tests

Verify that your local Java environment, Maven dependencies, and business logic compile and pass:

```bash
cd backend
mvn test
```

All 16 tests in `backend/src/test/` should pass with 0 failures and 0 errors:
- `ExamServiceWorkflowTest` (Lifecycle, scoring, negative marking, attempt states)
- `DatabaseIntegrationTest` (Live connection and DAO query verification)
- `DateTimeUtilTest` (ISO-8601 parsing and date formatting)
- `SecurityAndEvaluationTest` (BCrypt password verification and evaluation logic)
- `SecurityAndAuthorizationTest` (Session security and authorization rules)
- `ExamLifecycleAndDateTest` (Exam start/end windows and date boundary validation)

### 2. Package the WAR File

```bash
mvn clean package
```

The compiled archive is generated at:
```text
backend/target/ezexam.war
```

### 3. Deploy to Apache Tomcat 10.1

1. Copy the packaged WAR file to your Tomcat `webapps/` directory:
   ```bash
   # Homebrew macOS example:
   cp backend/target/ezexam.war /opt/homebrew/Cellar/tomcat@10/10.1.61/libexec/webapps/

   # Standard Linux/macOS path:
   cp backend/target/ezexam.war /usr/local/tomcat/webapps/
   ```

2. Start the Tomcat server:
   ```bash
   # Homebrew example:
   /opt/homebrew/opt/tomcat@10/bin/catalina start

   # Standard Tomcat script:
   $CATALINA_HOME/bin/startup.sh
   ```

3. Verify the deployment:
   ```bash
   curl -i http://localhost:8080/ezexam/api/auth/me
   ```

---

## Frontend Setup & Execution

### 1. Install Node Dependencies

From the project root:

```bash
cd frontend
npm install
```

### 2. Start the Development Server

```bash
npm run dev
```

The frontend will be available at:
```text
http://localhost:5173
```

> **API Proxying:** In development mode, Vite automatically proxies all requests from `http://localhost:5173/api/*` to the Tomcat backend at `http://localhost:8080/ezexam/api/*`. Ensure Tomcat is running on port 8080.

### 3. Production Frontend Build

To validate or build the static production distribution:

```bash
npm run build
```

The optimized static assets will be output to `frontend/dist/`.

---

## Demo Credentials

The seed database (`backend/src/main/resources/seed.sql`) pre-populates the following accounts for evaluation:

| Role | Email Address | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Student** | `student@ezexam.com` | `student123` | Taking examinations, reviewing history, viewing scorecards |
| **Administrator** | `admin@ezexam.com` | `admin123` | Managing exams, adding MCQs, inspecting student performance |

> **Note:** These credentials use salted BCrypt hashes generated at 10 rounds. In a production environment, seed credentials should be removed or changed immediately.

---

## API Architecture

The backend exposes a REST-like API under `/api/*`:

- **Authentication (`/api/auth/*`)**:
  - `POST /api/auth/register` — Student account registration
  - `POST /api/auth/login` — Authentication and `HttpSession` establishment
  - `POST /api/auth/logout` — Session termination
  - `GET  /api/auth/me` — Current authenticated user profile
- **Student Operations (`/api/student/*`)**:
  - `GET  /api/student/exams` — List published exams
  - `GET  /api/student/exams/{id}` — Fetch exam details & MCQs (answers redacted)
  - `POST /api/student/exams/{id}/start` — Initiate or resume timed attempt
  - `POST /api/student/attempts/{id}/submit` — Submit answers for automated scoring
  - `GET  /api/student/results` — Student attempt history
  - `GET  /api/student/results/{id}` — Detailed exam scorecard
- **Administrator Console (`/api/admin/*`)**:
  - `GET    /api/admin/stats` — High-level metric counters
  - `GET    /api/admin/attempts` — Recent student submission log
  - `GET    /api/admin/students` — Student directory with attempt metrics
  - `POST   /api/admin/exams` — Create new examination
  - `PUT    /api/admin/exams/{id}` — Update exam parameters
  - `DELETE /api/admin/exams/{id}` — Delete exam and cascaded data
  - `POST   /api/admin/exams/{id}/questions` — Author new MCQ with 4 options
  - `PUT    /api/admin/questions/{id}` — Update question text, options, or marks
  - `DELETE /api/admin/questions/{id}` — Remove question

---

## Current Deployment Status

> **Important Deployment Notice:**
> **Deployment is not yet completed.**
>
> - The application has been fully developed, integrated, and validated in the **local development environment** (macOS, Apache Tomcat 10.1, MySQL 8+, React 19/Vite).
> - Backend tests have been executed locally with 100% pass rates (16/16 tests passing).
> - Frontend production build succeeds cleanly without errors.
> - **Cloud deployment** (such as containerized deployment on Render/Railway/AWS or managed MySQL database provisioning) has **not** yet been performed and is planned for a subsequent release.

---

## License

This project is licensed under the MIT License — see the [LICENSE](file:///Users/nihartambe/Desktop/ezexam/LICENSE) file for details.

