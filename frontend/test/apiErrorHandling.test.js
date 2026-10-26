import test from "node:test";
import assert from "node:assert/strict";
import { request } from "../src/services/api.js";

// Save original fetch
const originalFetch = globalThis.fetch;

test("request extracts data.error from standard API error responses", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 400,
    statusText: "",
    text: async () => JSON.stringify({ error: "Invalid email or password." }),
  });

  await assert.rejects(
    async () => request("/api/auth/login"),
    { message: "Invalid email or password." }
  );
});

test("request extracts data.message when error field is not used", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 400,
    statusText: "",
    text: async () => JSON.stringify({ message: "Registration closed." }),
  });

  await assert.rejects(
    async () => request("/api/auth/register"),
    { message: "Registration closed." }
  );
});

test("request maps 504 Gateway Timeout HTML responses to actionable cold start guidance", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 504,
    statusText: "",
    text: async () => "<html><title>504 Gateway Timeout</title><body>Gateway Timeout</body></html>",
  });

  await assert.rejects(
    async () => request("/api/auth/login"),
    (err) => {
      assert.ok(err.message.includes("504 Gateway Timeout"));
      assert.ok(err.message.includes("waking up from standby"));
      return true;
    }
  );
});

test("request maps 502/503 Service Unavailable to friendly service message", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 502,
    statusText: "",
    text: async () => "<html>502 Bad Gateway</html>",
  });

  await assert.rejects(
    async () => request("/api/auth/login"),
    (err) => {
      assert.ok(err.message.includes("502/503"));
      return true;
    }
  );
});

test("request maps 500 error without JSON body to server error message", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 500,
    statusText: "",
    text: async () => "<html>Internal Server Error</html>",
  });

  await assert.rejects(
    async () => request("/api/auth/login"),
    { message: "An internal server error occurred (500). Please try again later." }
  );
});

test("request maps 401 Unauthorized without JSON body to invalid credentials", async () => {
  globalThis.fetch = async () => ({
    ok: false,
    status: 401,
    statusText: "",
    text: async () => "",
  });

  await assert.rejects(
    async () => request("/api/auth/login"),
    { message: "Invalid credentials. Please verify your email and password." }
  );
});

test("request handles network failure gracefully without raw browser errors", async () => {
  globalThis.fetch = async () => {
    throw new TypeError("Failed to fetch");
  };

  await assert.rejects(
    async () => request("/api/auth/login"),
    { message: "Unable to connect to the examination server. Please check your network connection and try again." }
  );

  // Restore fetch
  globalThis.fetch = originalFetch;
});
