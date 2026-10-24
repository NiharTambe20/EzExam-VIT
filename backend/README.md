# EzExam Backend — Advanced Java (Jakarta Servlets + JDBC + MySQL)

An online examination portal built with traditional **Advanced Java** architecture, adhering to the Jakarta EE 10 specification, and integrated with a modern React + Vite frontend.

---

## 1. Architecture Overview

EzExam follows the standard enterprise **4-Tier MVC / DAO** architecture:

```text
[React 19 Frontend (Vite :5173)]
              │
         HTTP / JSON (REST API via proxy)
              │
              ▼
    [CorsFilter & Jakarta Servlets (:8080)]  <-- Controller Tier
              │
              ▼
         [Service Layer]                     <-- Business Logic Tier (Evaluation, Scoring)
              │
              ▼
            [DAO Layer]                      <-- Data Access Tier (Parameterized SQL)
              │
              ▼
      [JDBC (DBConnection)]                  <-- Persistence Tier
              │
              ▼
      [MySQL Database Server]                <-- Relational Storage
```

1. **Presentation Tier**: React SPA running on Vite (port 5173). It interacts with `/api/*` endpoints.
2. **Controller Tier**: Jakarta Servlets (`HttpServlet`) handling `doGet`, `doPost`, `doPut`, `doDelete`, and managing user sessions (`HttpSession`).
3. **Business Logic Tier**: Encapsulates scoring rules, negative marking calculations, idempotency checks (preventing double submissions), and credential validation.
4. **Data Access Tier**: Data Access Objects (`UserDAO`, `ExamDAO`, `QuestionDAO`, `AttemptDAO`) executing parameterized queries via `PreparedStatement` to eliminate SQL injection.
5. **Database Tier**: Relational schema in MySQL with foreign key relationships, cascade rules, and indexes.

---

## 2. Technology Stack

* **Language**: Java 17+ (Compiled with `--release 17` for maximum LTS compatibility)
* **Web & Servlet Engine**: Apache Tomcat 10.1 (Jakarta EE 10 / Jakarta Servlet 6.0)
* **Build System**: Apache Maven 3.9+
* **Database Driver**: MySQL Connector/J 9.2.0 (`com.mysql:mysql-connector-j`)
* **JSON Processing**: Google Gson 2.12.1 (`com.google.code.gson:gson`)
* **Password Security**: jBCrypt 0.4 (`org.mindrot:jbcrypt`) with 10 salt rounds
* **Testing**: JUnit Jupiter 5.11.4

---

## 3. Prerequisites

* **Java Development Kit**: JDK 17, 21, or 25 installed.
* **Apache Maven**: `mvn -version`
* **MySQL Server**: MySQL 8.0, 8.4, 9.0, or 26.x running on port 3306.
* **Apache Tomcat 10.1**: For deploying `ezexam.war`.
* **Node.js**: v18+ and npm for the frontend.

---

## 4. Environment Configuration

Database credentials are read with the following precedence:
1. Environment variables (`EZEXAM_DB_URL`, `EZEXAM_DB_USER`, `EZEXAM_DB_PASSWORD`)
2. `src/main/resources/db.properties` (gitignored to protect secrets)
3. Default localhost fallback

### Setting up `db.properties`
Copy `backend/src/main/resources/db.properties.example` to `backend/src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=YOUR_MYSQL_PASSWORD
```

Alternatively, export environment variables:
```bash
export EZEXAM_DB_URL="jdbc:mysql://localhost:3306/ezexam?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
export EZEXAM_DB_USER="root"
export EZEXAM_DB_PASSWORD="your_password"
```

---

## 5. Database Creation & Schema Import

Run the following command in terminal to create the database and tables:

```bash
mysql -u root -p < backend/src/main/resources/schema.sql
```

This creates 6 relational tables:
1. `users`: Stores accounts with unique email, BCrypt password hash, and role (`admin` or `student`).
2. `exams`: Exam configurations with time limits, passing percentage, negative marking, and draft/published status.
3. `questions`: MCQs tied to an exam with marks and question-level negative mark override.
4. `options`: 4 choices per question with `is_correct` boolean indicator.
5. `attempts`: Tracks student attempts (`in-progress` vs `submitted`), scores, percentages, and pass/fail state.
6. `attempt_answers`: Detailed audit trail of student choices and marks awarded per question.

---

## 6. Demo Data Initialization

To populate initial exams, sample MCQs, and pre-hashed demo accounts:

```bash
mysql -u root -p < backend/src/main/resources/seed.sql
```

---

## 7. Maven Build Instructions

From the `backend` directory:

```bash
cd backend

# Compile Java source files and verify dependencies
mvn clean compile

# Run unit tests (verifies BCrypt hashing and score calculation)
mvn test

# Package the Web Application Archive (WAR)
mvn clean package
```

The resulting deployment archive is located at:
`backend/target/ezexam.war`

---

## 8. Tomcat Deployment & Startup

### Option A: Via Homebrew Tomcat 10
1. Copy the compiled WAR into Tomcat's `webapps` directory:
   ```bash
   cp backend/target/ezexam.war /opt/homebrew/Cellar/tomcat@10/10.1.61/libexec/webapps/
   ```
2. Start Tomcat:
   ```bash
   /opt/homebrew/opt/tomcat@10/bin/catalina start
   ```
3. Verify deployment:
   ```bash
   curl -i http://localhost:8080/ezexam/api/auth/me
   ```

### Option B: Stopping Tomcat
```bash
/opt/homebrew/opt/tomcat@10/bin/catalina stop
```

---

## 9. Frontend Startup Instructions

1. From the project root:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
2. Open your browser at `http://localhost:5173`.
3. Vite proxies all requests from `http://localhost:5173/api/*` directly to `http://localhost:8080/ezexam/api/*`.

---

## 10. API Endpoint Documentation

### Authentication (`/api/auth/*`)
* `POST /api/auth/register`: Create student account (`{ name, email, password }`).
* `POST /api/auth/login`: Authenticate and set `HttpSession` (`{ email, password }`).
* `POST /api/auth/logout`: Invalidate session.
* `GET  /api/auth/me`: Get active user profile or `null`.

### Student Exam Portal (`/api/student/*`)
* `GET  /api/student/exams?onlyPublished=true`: Browse available exams with question counts.
* `GET  /api/student/exams/{id}`: Fetch exam and questions (**`is_correct` is strictly omitted** to prevent client cheating).
* `POST /api/student/exams/{id}/start`: Start a new timed attempt or resume an active one.
* `POST /api/student/attempts/{id}/submit`: Submit answers map (`{ [questionId]: optionId }`). Evaluates score, wrong/correct counts, applies negative marks, and saves outcome.
* `GET  /api/student/results?userId={id}`: View completed examination history.
* `GET  /api/student/results/{id}`: View detailed post-exam scorecard.

### Administrator Console (`/api/admin/*`)
* `GET    /api/admin/stats`: Get system counters (students, exams, attempts, published).
* `GET    /api/admin/attempts`: List 20 most recent student attempts.
* `GET    /api/admin/students`: List students with attempt counts.
* `POST   /api/admin/exams`: Create new examination.
* `PUT    /api/admin/exams/{id}`: Edit exam parameters and status.
* `DELETE /api/admin/exams/{id}`: Delete exam and cascade remove associated questions.
* `POST   /api/admin/exams/{id}/questions`: Add 4-option MCQ with answer indicator.
* `PUT    /api/admin/questions/{id}`: Update MCQ text, marks, or options.
* `DELETE /api/admin/questions/{id}`: Delete MCQ.

---

## 11. Test Credentials

* **Student Account**:
  * Email: `student@ezexam.com`
  * Password: `student123`
* **Administrator Account**:
  * Email: `admin@ezexam.com`
  * Password: `admin123`

---

## 12. Common Errors and Solutions

| Symptom | Root Cause | Solution |
| :--- | :--- | :--- |
| `CommunicationsLinkException` | MySQL Server is not running or credentials invalid. | Verify MySQL service (`brew services start mysql` or `/usr/local/mysql/support-files/mysql.server start`) and check `db.properties`. |
| `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | MySQL Connector/J driver missing from classpath. | Run `mvn clean package` to ensure the driver is bundled into `WEB-INF/lib/` inside the WAR. |
| `HTTP 401 Unauthorized` | Attempting to access protected endpoints without an active session. | Log in first via `/api/auth/login`. |
| `CORS Error in Browser` | Origin header mismatch between React (:5173) and Tomcat (:8080). | Handled by `CorsFilter.java` and Vite proxy in `vite.config.js`. |
| `HTTP 403 Forbidden` on Submission | Student attempting to submit another student's attempt. | Server ownership validation prevents unauthorized attempt submission. |

---

## 13. Security Features Implemented

1. **Password Hashing**: Passwords stored exclusively as BCrypt salted hashes (`$2a$10$...`). Plaintext passwords are never saved.
2. **Information Leakage Prevention**:
   - `User.java` marks `password` as `transient` so Gson never serializes passwords in responses.
   - `QuestionDAO.findByExamId` zeroes out `is_correct` when serving student requests.
3. **SQL Injection Prevention**: 100% of SQL statements in `UserDAO`, `ExamDAO`, `QuestionDAO`, and `AttemptDAO` use `PreparedStatement` with parameterized placeholders (`?`).
4. **Idempotent Submissions**: Once an attempt is marked `'submitted'`, repeated calls return the existing score rather than re-evaluating.
5. **Session Cookies**: Handled via `HttpSession` with `HttpOnly` cookie protection.
