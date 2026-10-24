// Real HTTP REST client connecting to the Java Servlet backend via Vite proxy (/api -> Tomcat /ezexam/api)
// Replaces previous browser localStorage mock per PRD §23 contract.

async function request(endpoint, options = {}) {
  const res = await fetch(endpoint, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    credentials: "include", // Sends session cookies (JSESSIONID) for stateful auth
  });

  const text = await res.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const errorMsg = (data && data.error) ? data.error : (res.statusText || "Request failed");
    throw new Error(errorMsg);
  }

  return data;
}

// ---------- Auth: /api/auth/* ----------
export async function apiRegister({ name, email, password }) {
  return request("/api/auth/register", {
    method: "POST",
    body: JSON.stringify({ name, email, password }),
  });
}

export async function apiLogin({ email, password }) {
  return request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export async function apiLogout() {
  return request("/api/auth/logout", {
    method: "POST",
  });
}

export async function apiCurrentUser() {
  try {
    return await request("/api/auth/me");
  } catch {
    return null;
  }
}

// ---------- Student: /api/student/* ----------
export async function apiGetExams({ onlyPublished = false } = {}) {
  return request(`/api/student/exams?onlyPublished=${onlyPublished}`);
}

export async function apiGetExam(id, { includeAnswers = false } = {}) {
  const url = includeAnswers ? `/api/admin/exams/${id}` : `/api/student/exams/${id}`;
  return request(url);
}

export async function apiStartAttempt(examId, userId) {
  return request(`/api/student/exams/${examId}/start`, {
    method: "POST",
    body: JSON.stringify({ userId }),
  });
}

export async function apiSubmitAttempt(attemptId, answersMap, userId) {
  return request(`/api/student/attempts/${attemptId}/submit`, {
    method: "POST",
    body: JSON.stringify({ answers: answersMap, userId }),
  });
}

export async function apiGetResults(userId) {
  return request(`/api/student/results?userId=${userId}`);
}

export async function apiGetResult(attemptId) {
  return request(`/api/student/results/${attemptId}`);
}

// ---------- Admin: /api/admin/* ----------
export async function apiAdminStats() {
  return request("/api/admin/stats");
}

export async function apiAdminAttempts() {
  return request("/api/admin/attempts");
}

export async function apiAdminStudents() {
  return request("/api/admin/students");
}

export async function apiCreateExam(payload) {
  return request("/api/admin/exams", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function apiUpdateExam(id, payload) {
  return request(`/api/admin/exams/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function apiDeleteExam(id) {
  return request(`/api/admin/exams/${id}`, {
    method: "DELETE",
  });
}

export async function apiCreateQuestion(examId, payload) {
  return request(`/api/admin/exams/${examId}/questions`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function apiUpdateQuestion(id, payload) {
  return request(`/api/admin/questions/${id}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function apiDeleteQuestion(id) {
  return request(`/api/admin/questions/${id}`, {
    method: "DELETE",
  });
}
