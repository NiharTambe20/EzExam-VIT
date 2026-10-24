import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Eye, EyeOff, UserPlus, AlertCircle } from "lucide-react";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "", confirm: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.name.trim()) return setError("Name cannot be empty.");
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return setError("Email must have a valid format.");
    if (form.password.length < 6) return setError("Password must be at least 6 characters.");
    if (form.password !== form.confirm) return setError("Password and confirmation must match.");
    setBusy(true);
    try {
      await register({ name: form.name.trim(), email: form.email.trim(), password: form.password });
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(err.message || "Failed to create examination account.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      {/* LEFT SIDE: VIT Campus Building Visual (50% desktop, full viewport height) */}
      <div className="login-image-panel">
        <img
          src="/vit-campus.jpg"
          alt="Vidyalankar Institute of Technology, Mumbai Campus"
          className="login-campus-image"
          loading="eager"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-[#1F1B47]/80 via-transparent to-black/20 pointer-events-none flex flex-col justify-end p-10 text-white">
          <div className="max-w-md space-y-1.5 backdrop-blur-xs bg-black/20 p-5 rounded-2xl border border-white/10">
            <span className="text-xs font-semibold tracking-wider uppercase text-white/80">
              Vidyalankar Institute of Technology, Mumbai
            </span>
            <h2 className="text-2xl font-bold leading-tight">
              Candidate Registration
            </h2>
            <p className="text-xs text-white/75 leading-relaxed">
              Create your official student examination account to register and attempt scheduled semester and unit assessments.
            </p>
          </div>
        </div>
      </div>

      {/* RIGHT SIDE: Centered Institutional Registration Form (50% desktop) */}
      <div className="login-form-panel">
        <div className="login-form-wrapper">
          {/* Official VIT Logo Header */}
          <div className="login-header-group">
            <div className="login-logo-lockup">
              <img
                src="/vit-logo.png"
                alt="Vidyalankar Institute of Technology Logo"
                className="login-logo-img"
              />
              <div className="login-brand-info">
                <span className="login-brand-name">EzExam Portal</span>
                <span className="login-brand-institute">
                  Vidyalankar Institute of Technology
                  <br />
                  Wadala, Mumbai
                </span>
              </div>
            </div>

            <h1 className="login-title">Create Account.</h1>
            <p className="login-subtitle">
              Register as an authorized candidate to access the online examination system.
            </p>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-4 flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 animate-in fade-in duration-200">
              <AlertCircle size={16} className="shrink-0 text-rose-600 mt-0.5" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          {/* Registration Form */}
          <form onSubmit={submit} className="login-form" noValidate>
            {/* Full Name */}
            <div className="login-field">
              <label htmlFor="reg-name" className="login-label">
                Full Name
              </label>
              <div className="login-input-group">
                <input
                  id="reg-name"
                  type="text"
                  autoComplete="name"
                  className="login-input"
                  placeholder="e.g. Aditi Sharma"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                />
              </div>
            </div>

            {/* Institutional Email */}
            <div className="login-field">
              <label htmlFor="reg-email" className="login-label">
                Institutional Email
              </label>
              <div className="login-input-group">
                <input
                  id="reg-email"
                  type="email"
                  autoComplete="email"
                  className="login-input"
                  placeholder="e.g. aditi.sharma@vit.edu.in"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  required
                />
              </div>
            </div>

            {/* Password & Confirm Grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="login-field">
                <label htmlFor="reg-password" className="login-label">
                  Password
                </label>
                <div className="login-input-group">
                  <input
                    id="reg-password"
                    type={showPassword ? "text" : "password"}
                    autoComplete="new-password"
                    className="login-input has-icon"
                    placeholder="Min. 6 chars"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    required
                  />
                  <button
                    type="button"
                    className="login-password-toggle"
                    onClick={() => setShowPassword((prev) => !prev)}
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>

              <div className="login-field">
                <label htmlFor="reg-confirm" className="login-label">
                  Confirm Password
                </label>
                <div className="login-input-group">
                  <input
                    id="reg-confirm"
                    type={showConfirm ? "text" : "password"}
                    autoComplete="new-password"
                    className="login-input has-icon"
                    placeholder="Repeat password"
                    value={form.confirm}
                    onChange={(e) => setForm({ ...form, confirm: e.target.value })}
                    required
                  />
                  <button
                    type="button"
                    className="login-password-toggle"
                    onClick={() => setShowConfirm((prev) => !prev)}
                    aria-label={showConfirm ? "Hide confirm password" : "Show confirm password"}
                    tabIndex={-1}
                  >
                    {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="login-submit-btn shadow-sm mt-2"
              disabled={busy}
            >
              <UserPlus size={16} />
              <span>{busy ? "Registering Candidate…" : "Complete Registration"}</span>
            </button>
          </form>

          {/* Login Link */}
          <div className="mt-5 text-center text-xs text-slate-500">
            Already have an examination account?{" "}
            <Link to="/login" className="font-semibold text-[#48499A] hover:underline">
              Sign in here
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
