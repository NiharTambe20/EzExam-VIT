// Real HTTP REST client connecting to the Java Servlet backend via Vite proxy (/api -> Tomcat /ezexam/api)
// Replaces previous browser localStorage mock per PRD §23 contract.

export async function request(endpoint, options = {}) {
  let res;
  try {
    res = await fetch(endpoint, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
      credentials: "include", // Sends session cookies (JSESSIONID) for stateful auth
    });
  } catch {
    throw new Error(
      "Unable to connect to the examination server. Please check your network connection and try again."
    );
  }

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
    let errorMsg = null;
    if (data && typeof data === "object") {
      errorMsg = data.error || data.message;
    } else if (typeof data === "string" && data.trim() && !data.trim().startsWith("<")) {
      errorMsg = data.trim();
    }

    if (!errorMsg) {
      if (res.status === 504) {
        errorMsg = "Server took too long to respond (504 Gateway Timeout). If the backend is waking up from standby, please wait a few seconds and try again.";
      } else if (res.status === 502 || res.status === 503) {
        errorMsg = "Backend examination service is temporarily unavailable (502/503). Please try again shortly.";
      } else if (res.status === 500) {
        errorMsg = "An internal server error occurred (500). Please try again later.";
      } else if (res.status === 401) {
        errorMsg = "Invalid credentials. Please verify your email and password.";
      } else if (res.status === 403) {
        errorMsg = "Access denied: Unauthorized operation (403).";
      } else if (res.status === 404) {
        errorMsg = "Requested endpoint was not found (404).";
      } else {
        errorMsg = res.statusText || `Request failed with status ${res.status}`;
      }
    }

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
