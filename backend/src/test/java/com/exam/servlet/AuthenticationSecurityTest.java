package com.exam.servlet;

import com.exam.dao.AttemptDAO;
import com.exam.dao.ExamDAO;
import com.exam.dao.QuestionDAO;
import com.exam.dao.UserDAO;
import com.exam.model.Attempt;
import com.exam.model.Exam;
import com.exam.model.Question;
import com.exam.model.User;
import com.exam.util.DBConnection;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationSecurityTest {

    private final UserDAO userDAO = new UserDAO();
    private final ExamDAO examDAO = new ExamDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();

    private final StudentExamServlet studentExamServlet = new StudentExamServlet();
    private final StudentAttemptServlet studentAttemptServlet = new StudentAttemptServlet();
    private final StudentResultServlet studentResultServlet = new StudentResultServlet();
    private final AdminServlet adminServlet = new AdminServlet();

    private static final Gson GSON = new Gson();

    private Exam testExam;
    private Question q1;
    private User studentA;
    private User studentB;
    private User adminUser;

    @BeforeEach
    public void setUp() throws SQLException {
        // Seed or retrieve test users
        adminUser = userDAO.findByEmail("admin@ezexam.com");
        assertNotNull(adminUser, "Admin user must exist in database");

        studentA = userDAO.findByEmail("student@ezexam.com");
        assertNotNull(studentA, "Student A (demo student) must exist in database");

        // Create an isolated Student B
        String studentBEmail = "student_b_sec_" + System.currentTimeMillis() + "@ezexam.com";
        studentB = userDAO.create("Student B", studentBEmail, "student123", "student");
        assertNotNull(studentB, "Student B must be created successfully");

        // Create an isolated test exam
        Exam exam = new Exam();
        exam.setTitle("Auth Security Test Exam");
        exam.setDescription("Testing authentication enforcement and IDOR prevention");
        exam.setDurationMinutes(30);
        exam.setTotalMarks(5);
        exam.setPassingPercentage(50);
        exam.setNegativeMark(0.0);
        exam.setStatus("published");
        testExam = examDAO.create(exam);

        q1 = questionDAO.create(testExam.getId(), "Sample Security Question?", 5, 0.0,
                List.of("Option A", "Option B", "Option C", "Option D"), 1); // Option B is correct
    }

    @AfterEach
    public void tearDown() throws SQLException {
        if (testExam != null) {
            examDAO.delete(testExam.getId());
        }
        if (studentB != null && studentB.getId() != null) {
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
                ps.setInt(1, studentB.getId());
                ps.executeUpdate();
            }
        }
    }

    // ==========================================
    // CASE 1: Unauthenticated exam-start requests are rejected (401)
    // ==========================================
    @Test
    public void testUnauthenticatedExamStartRejected() throws Exception {
        MockRequest req = MockRequest.unauthenticated("/" + testExam.getId() + "/start", "POST", null, null);
        MockResponse resp = new MockResponse();

        studentExamServlet.doPost(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
        JsonObject body = GSON.fromJson(resp.getBody(), JsonObject.class);
        assertNotNull(body);
        assertTrue(body.has("error"), "Expected error message in response");
    }

    // ==========================================
    // CASE 2: Unauthenticated answer-submission requests are rejected (401)
    // ==========================================
    @Test
    public void testUnauthenticatedAnswerSubmissionRejected() throws Exception {
        // Create an attempt for student A directly in DB
        Attempt attempt = attemptDAO.create(testExam.getId(), studentA.getId());
        assertNotNull(attempt);

        JsonObject submission = new JsonObject();
        JsonObject answers = new JsonObject();
        answers.addProperty(String.valueOf(q1.getId()), q1.getOptions().get(1).getId());
        submission.add("answers", answers);

        // Unauthenticated request (no session)
        MockRequest req = MockRequest.unauthenticated("/" + attempt.getId() + "/submit", "POST", null, submission.toString());
        MockResponse resp = new MockResponse();

        studentAttemptServlet.doPost(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus());
        JsonObject errBody = GSON.fromJson(resp.getBody(), JsonObject.class);
        assertNotNull(errBody);
        assertTrue(errBody.has("error"));
    }

    // ==========================================
    // CASE 3: Unauthenticated result requests are rejected (401)
    // ==========================================
    @Test
    public void testUnauthenticatedResultRequestsRejected() throws Exception {
        // 3A: List results unauthenticated
        MockRequest reqList = MockRequest.unauthenticated("", "GET", null, null);
        MockResponse respList = new MockResponse();
        studentResultServlet.doGet(reqList.asHttpServletRequest(), respList.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, respList.getStatus());

        // 3B: Specific attempt result unauthenticated
        Attempt attempt = attemptDAO.create(testExam.getId(), studentA.getId());
        MockRequest reqSingle = MockRequest.unauthenticated("/" + attempt.getId(), "GET", null, null);
        MockResponse respSingle = new MockResponse();
        studentResultServlet.doGet(reqSingle.asHttpServletRequest(), respSingle.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, respSingle.getStatus());

        // 3C: Exam details unauthenticated
        MockRequest reqExam = MockRequest.unauthenticated("/" + testExam.getId(), "GET", null, null);
        MockResponse respExam = new MockResponse();
        studentExamServlet.doGet(reqExam.asHttpServletRequest(), respExam.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, respExam.getStatus());
    }

    // ==========================================
    // CASE 4: Supplying another user's userId cannot override the authenticated session (403)
    // ==========================================
    @Test
    public void testSupplyingAnotherUserIdCannotOverrideSession() throws Exception {
        // 4A: In exam start: Student A passes Student B's userId in body
        JsonObject startBody = new JsonObject();
        startBody.addProperty("userId", studentB.getId());
        MockRequest reqStart = MockRequest.authenticated("/" + testExam.getId() + "/start", "POST", studentA, null, startBody.toString());
        MockResponse respStart = new MockResponse();
        studentExamServlet.doPost(reqStart.asHttpServletRequest(), respStart.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respStart.getStatus(),
                "Conflicting userId in start attempt body must be rejected with 403 Forbidden");

        // 4B: In submit attempt: Student A passes Student B's userId in body
        Attempt attemptA = attemptDAO.create(testExam.getId(), studentA.getId());
        JsonObject submitBody = new JsonObject();
        submitBody.addProperty("userId", studentB.getId());
        JsonObject answers = new JsonObject();
        submitBody.add("answers", answers);

        MockRequest reqSubmit = MockRequest.authenticated("/" + attemptA.getId() + "/submit", "POST", studentA, null, submitBody.toString());
        MockResponse respSubmit = new MockResponse();
        studentAttemptServlet.doPost(reqSubmit.asHttpServletRequest(), respSubmit.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respSubmit.getStatus(),
                "Conflicting userId in submit attempt body must be rejected with 403 Forbidden");

        // 4C: In results list: Student A passes Student B's userId in query param
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("userId", String.valueOf(studentB.getId()));
        MockRequest reqResults = MockRequest.authenticated("", "GET", studentA, queryParams, null);
        MockResponse respResults = new MockResponse();
        studentResultServlet.doGet(reqResults.asHttpServletRequest(), respResults.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respResults.getStatus(),
                "Student requesting another user's results via ?userId= must be rejected with 403 Forbidden");
    }

    // ==========================================
    // CASE 5: Student A cannot access Student B's attempt (403)
    // ==========================================
    @Test
    public void testStudentACannotAccessStudentBAttempt() throws Exception {
        // Create an attempt belonging to Student B
        Attempt attemptB = attemptDAO.create(testExam.getId(), studentB.getId());
        assertNotNull(attemptB);

        // Student A requests Student B's attempt
        MockRequest req = MockRequest.authenticated("/" + attemptB.getId(), "GET", studentA, null, null);
        MockResponse resp = new MockResponse();
        studentResultServlet.doGet(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resp.getStatus(),
                "Student A must not be allowed to access Student B's attempt");
    }

    // ==========================================
    // CASE 6: Student A cannot submit answers to Student B's attempt (403)
    // ==========================================
    @Test
    public void testStudentACannotSubmitAnswersToStudentBAttempt() throws Exception {
        // Create an in-progress attempt belonging to Student B
        Attempt attemptB = attemptDAO.create(testExam.getId(), studentB.getId());
        assertNotNull(attemptB);

        JsonObject body = new JsonObject();
        JsonObject answers = new JsonObject();
        answers.addProperty(String.valueOf(q1.getId()), q1.getOptions().get(1).getId());
        body.add("answers", answers);

        // Student A tries to submit Student B's attempt
        MockRequest req = MockRequest.authenticated("/" + attemptB.getId() + "/submit", "POST", studentA, null, body.toString());
        MockResponse resp = new MockResponse();
        studentAttemptServlet.doPost(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resp.getStatus(),
                "Student A must not be allowed to submit answers for Student B's attempt");

        // Verify Student B's attempt is still in-progress in the database
        Attempt current = attemptDAO.findById(attemptB.getId());
        assertEquals("in-progress", current.getStatus());
    }

    // ==========================================
    // CASE 7: Student A cannot retrieve Student B's results (403)
    // ==========================================
    @Test
    public void testStudentACannotRetrieveStudentBResults() throws Exception {
        // Submit an attempt for Student B
        Attempt attemptB = attemptDAO.create(testExam.getId(), studentB.getId());
        attemptDAO.submitAttempt(attemptB.getId(), 5.0, 5, 1, 0, 0, 100.0, true, List.of());

        // Student A requests Student B's history
        Map<String, String> params = new HashMap<>();
        params.put("userId", String.valueOf(studentB.getId()));
        MockRequest req = MockRequest.authenticated("", "GET", studentA, params, null);
        MockResponse resp = new MockResponse();
        studentResultServlet.doGet(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, resp.getStatus(),
                "Student A must be forbidden from querying Student B's results list");

        // Student A queries without userId parameter -> receives only Student A's results
        MockRequest reqOwn = MockRequest.authenticated("", "GET", studentA, null, null);
        MockResponse respOwn = new MockResponse();
        studentResultServlet.doGet(reqOwn.asHttpServletRequest(), respOwn.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_OK, respOwn.getStatus());
        JsonArray arr = GSON.fromJson(respOwn.getBody(), JsonArray.class);
        assertNotNull(arr);
        for (int i = 0; i < arr.size(); i++) {
            JsonObject item = arr.get(i).getAsJsonObject();
            assertEquals(studentA.getId().intValue(), item.get("user_id").getAsInt(),
                    "Results must only contain attempts belonging to the authenticated student");
        }
    }

    // ==========================================
    // CASE 8: An authenticated student can still start and submit their own exam
    // ==========================================
    @Test
    public void testAuthenticatedStudentCanStartAndSubmitOwnExam() throws Exception {
        // Step 1: Start attempt
        MockRequest reqStart = MockRequest.authenticated("/" + testExam.getId() + "/start", "POST", studentA, null, null);
        MockResponse respStart = new MockResponse();
        studentExamServlet.doPost(reqStart.asHttpServletRequest(), respStart.asHttpServletResponse());

        assertTrue(respStart.getStatus() == HttpServletResponse.SC_CREATED || respStart.getStatus() == HttpServletResponse.SC_OK);
        JsonObject createdAttempt = GSON.fromJson(respStart.getBody(), JsonObject.class);
        assertNotNull(createdAttempt);
        int attemptId = createdAttempt.get("id").getAsInt();
        assertEquals(studentA.getId().intValue(), createdAttempt.get("user_id").getAsInt());
        assertEquals("in-progress", createdAttempt.get("status").getAsString());

        // Step 2: Submit attempt with correct answer to Q1
        JsonObject submitBody = new JsonObject();
        JsonObject answers = new JsonObject();
        answers.addProperty(String.valueOf(q1.getId()), q1.getOptions().get(1).getId());
        submitBody.add("answers", answers);

        MockRequest reqSubmit = MockRequest.authenticated("/" + attemptId + "/submit", "POST", studentA, null, submitBody.toString());
        MockResponse respSubmit = new MockResponse();
        studentAttemptServlet.doPost(reqSubmit.asHttpServletRequest(), respSubmit.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_OK, respSubmit.getStatus());
        JsonObject submittedAttempt = GSON.fromJson(respSubmit.getBody(), JsonObject.class);
        assertEquals("submitted", submittedAttempt.get("status").getAsString());
        assertEquals(5.0, submittedAttempt.get("score").getAsDouble(), 0.01);
        assertTrue(submittedAttempt.get("passed").getAsBoolean());

        // Step 3: View own result
        MockRequest reqResult = MockRequest.authenticated("/" + attemptId, "GET", studentA, null, null);
        MockResponse respResult = new MockResponse();
        studentResultServlet.doGet(reqResult.asHttpServletRequest(), respResult.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_OK, respResult.getStatus());
        JsonObject resultObj = GSON.fromJson(respResult.getBody(), JsonObject.class);
        assertEquals(attemptId, resultObj.get("id").getAsInt());
    }

    // ==========================================
    // CASE 9: Legitimate administrator operations continue to work
    // ==========================================
    @Test
    public void testAdministratorOperationsContinueToWork() throws Exception {
        // Create and submit an attempt for Student B
        Attempt attemptB = attemptDAO.create(testExam.getId(), studentB.getId());
        attemptDAO.submitAttempt(attemptB.getId(), 5.0, 5, 1, 0, 0, 100.0, true, List.of());

        // 9A: Admin accesses admin statistics
        MockRequest reqStats = MockRequest.authenticated("/stats", "GET", adminUser, null, null);
        MockResponse respStats = new MockResponse();
        adminServlet.doGet(reqStats.asHttpServletRequest(), respStats.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_OK, respStats.getStatus());
        JsonObject stats = GSON.fromJson(respStats.getBody(), JsonObject.class);
        assertTrue(stats.has("students"));
        assertTrue(stats.has("exams"));

        // 9B: Admin queries Student B's results via /api/student/results?userId=X
        Map<String, String> params = new HashMap<>();
        params.put("userId", String.valueOf(studentB.getId()));
        MockRequest reqStudentBResults = MockRequest.authenticated("", "GET", adminUser, params, null);
        MockResponse respStudentBResults = new MockResponse();
        studentResultServlet.doGet(reqStudentBResults.asHttpServletRequest(), respStudentBResults.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_OK, respStudentBResults.getStatus(),
                "Admin should be able to view any student's results");

        // 9C: Admin inspects Student B's single attempt result
        MockRequest reqSingle = MockRequest.authenticated("/" + attemptB.getId(), "GET", adminUser, null, null);
        MockResponse respSingle = new MockResponse();
        studentResultServlet.doGet(reqSingle.asHttpServletRequest(), respSingle.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_OK, respSingle.getStatus(),
                "Admin should be able to view any attempt's result");

        // 9D: Non-admin trying to access admin stats is rejected with 403 Forbidden
        MockRequest reqForbidden = MockRequest.authenticated("/stats", "GET", studentA, null, null);
        MockResponse respForbidden = new MockResponse();
        adminServlet.doGet(reqForbidden.asHttpServletRequest(), respForbidden.asHttpServletResponse());
        assertEquals(HttpServletResponse.SC_FORBIDDEN, respForbidden.getStatus(),
                "Student accessing admin endpoint must be rejected with 403");
    }

    // ==========================================
    // CASE 10: Existing authorization and session-expiration behavior remains correct
    // ==========================================
    @Test
    public void testAuthorizationAndSessionExpirationBehavior() throws Exception {
        // Create an active session and then invalidate it
        MockSessionHolder sessionHolder = new MockSessionHolder();
        sessionHolder.attributes.put(BaseServlet.SESSION_USER_KEY, studentA);
        sessionHolder.invalidate(); // Session expires

        MockRequest req = MockRequest.withSession("/" + testExam.getId() + "/start", "POST", sessionHolder, null, null);
        MockResponse resp = new MockResponse();
        studentExamServlet.doPost(req.asHttpServletRequest(), resp.asHttpServletResponse());

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, resp.getStatus(),
                "Expired or invalidated session must result in 401 Unauthorized");
    }

    // ==========================================
    // Lightweight Test Mocks (zero third-party dependencies)
    // ==========================================

    private static class MockSessionHolder {
        boolean valid = true;
        final Map<String, Object> attributes = new HashMap<>();

        void invalidate() {
            valid = false;
            attributes.clear();
        }

        HttpSession asHttpSession() {
            return (HttpSession) Proxy.newProxyInstance(
                    HttpSession.class.getClassLoader(),
                    new Class<?>[]{HttpSession.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getAttribute".equals(name)) {
                            if (!valid) return null;
                            return attributes.get(args[0]);
                        }
                        if ("setAttribute".equals(name)) {
                            if (!valid) throw new IllegalStateException("Session invalidated");
                            attributes.put((String) args[0], args[1]);
                            return null;
                        }
                        if ("removeAttribute".equals(name)) {
                            attributes.remove(args[0]);
                            return null;
                        }
                        if ("invalidate".equals(name)) {
                            invalidate();
                            return null;
                        }
                        if ("toString".equals(name)) return "MockHttpSession";
                        if ("hashCode".equals(name)) return 1;
                        if ("equals".equals(name)) return proxy == args[0];
                        return null;
                    }
            );
        }
    }

    private static class MockRequest {
        private final String pathInfo;
        private final String method;
        private final MockSessionHolder sessionHolder;
        private final Map<String, String> params;
        private final String body;

        private MockRequest(String pathInfo, String method, MockSessionHolder sessionHolder, Map<String, String> params, String body) {
            this.pathInfo = pathInfo;
            this.method = method;
            this.params = params != null ? params : new HashMap<>();
            this.body = body != null ? body : "";
            this.sessionHolder = sessionHolder;
        }

        static MockRequest unauthenticated(String pathInfo, String method, Map<String, String> params, String body) {
            return new MockRequest(pathInfo, method, null, params, body);
        }

        static MockRequest authenticated(String pathInfo, String method, User sessionUser, Map<String, String> params, String body) {
            MockSessionHolder holder = null;
            if (sessionUser != null) {
                holder = new MockSessionHolder();
                holder.attributes.put(BaseServlet.SESSION_USER_KEY, sessionUser);
            }
            return new MockRequest(pathInfo, method, holder, params, body);
        }

        static MockRequest withSession(String pathInfo, String method, MockSessionHolder sessionHolder, Map<String, String> params, String body) {
            return new MockRequest(pathInfo, method, sessionHolder, params, body);
        }

        HttpServletRequest asHttpServletRequest() {
            return (HttpServletRequest) Proxy.newProxyInstance(
                    HttpServletRequest.class.getClassLoader(),
                    new Class<?>[]{HttpServletRequest.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("getPathInfo".equals(name)) return pathInfo;
                        if ("getParameter".equals(name)) return params.get(args[0]);
                        if ("getSession".equals(name)) {
                            boolean create = (args != null && args.length > 0) ? (Boolean) args[0] : true;
                            if (sessionHolder != null) return sessionHolder.asHttpSession();
                            return null;
                        }
                        if ("getReader".equals(name)) return new BufferedReader(new StringReader(body));
                        if ("getMethod".equals(name)) return method;
                        if ("getContentType".equals(name)) return "application/json";
                        if ("getCharacterEncoding".equals(name)) return "UTF-8";
                        if ("toString".equals(name)) return "MockHttpServletRequest";
                        if ("hashCode".equals(name)) return 1;
                        if ("equals".equals(name)) return proxy == args[0];
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(boolean.class)) return false;
                        if (returnType.equals(int.class)) return 0;
                        if (returnType.equals(long.class)) return 0L;
                        return null;
                    }
            );
        }
    }

    private static class MockResponse {
        private int status = HttpServletResponse.SC_OK;
        private final StringWriter bodyWriter = new StringWriter();
        private final PrintWriter printWriter = new PrintWriter(bodyWriter);

        HttpServletResponse asHttpServletResponse() {
            return (HttpServletResponse) Proxy.newProxyInstance(
                    HttpServletResponse.class.getClassLoader(),
                    new Class<?>[]{HttpServletResponse.class},
                    (proxy, method, args) -> {
                        String name = method.getName();
                        if ("setStatus".equals(name)) {
                            status = (Integer) args[0];
                            return null;
                        }
                        if ("getStatus".equals(name)) return status;
                        if ("getWriter".equals(name)) return printWriter;
                        if ("sendError".equals(name)) {
                            status = (Integer) args[0];
                            if (args.length > 1 && args[1] != null) {
                                printWriter.print(args[1]);
                            }
                            return null;
                        }
                        if ("setContentType".equals(name) || "setCharacterEncoding".equals(name)) {
                            return null;
                        }
                        if ("toString".equals(name)) return "MockHttpServletResponse";
                        if ("hashCode".equals(name)) return 1;
                        if ("equals".equals(name)) return proxy == args[0];
                        Class<?> returnType = method.getReturnType();
                        if (returnType.equals(boolean.class)) return false;
                        if (returnType.equals(int.class)) return 0;
                        return null;
                    }
            );
        }

        int getStatus() {
            return status;
        }

        String getBody() {
            printWriter.flush();
            return bodyWriter.toString();
        }
    }
}
