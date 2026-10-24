import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Eye, EyeOff, ArrowRight, ShieldCheck, AlertCircle } from "lucide-react";

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError("");
    if (!form.email || !form.password) return setError("Please enter both institutional email and password.");
    setBusy(true);
    try {
      const user = await login(form);
      const dest = location.state?.from ?? (user.role === "admin" ? "/admin" : "/dashboard");
      navigate(dest, { replace: true });
    } catch (err) {
      setError(err.message || "Invalid credentials. Please verify your email and password.");
    } finally {
      setBusy(false);
    }
  };

  const fillDemo = (role) => {
    if (role === "student") {
      setForm({ email: "student@ezexam.com", password: "student123" });
    } else {
      setForm({ email: "admin@ezexam.com", password: "admin123" });
    }
    setError("");
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
              Official Examination &amp; Assessment Portal
            </h2>
            <p className="text-xs text-white/75 leading-relaxed">
              Autonomous Institute Affiliated to University of Mumbai • Accredited &apos;A+&apos; by NAAC
            </p>
          </div>
        </div>
      </div>

      {/* RIGHT SIDE: Centered Institutional Login Form (50% desktop) */}
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

            <h1 className="login-title">Welcome Back.</h1>
            <p className="login-subtitle">
              Sign in with your institutional credentials to access your examinations and academic records.
            </p>
          </div>

          {/* Error Alert */}
          {error && (
            <div className="mb-4 flex items-start gap-2.5 rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700 animate-in fade-in duration-200">
              <AlertCircle size={16} className="shrink-0 text-rose-600 mt-0.5" />
              <div className="flex-1 font-medium">{error}</div>
            </div>
          )}

          {/* Login Form */}
          <form onSubmit={submit} className="login-form" noValidate>
            {/* Email Field */}
            <div className="login-field">
              <label htmlFor="login-email" className="login-label">
                Institutional Email
              </label>
              <div className="login-input-group">
                <input
                  id="login-email"
                  type="email"
                  name="email"
                  autoComplete="email"
                  className="login-input"
                  placeholder="e.g. student@ezexam.com"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  required
                />
              </div>
            </div>

            {/* Password Field with Eye Toggle */}
            <div className="login-field">
              <label htmlFor="login-password" className="login-label">
                Account Password
              </label>
              <div className="login-input-group">
                <input
                  id="login-password"
                  type={showPassword ? "text" : "password"}
                  name="password"
                  autoComplete="current-password"
                  className="login-input has-icon"
                  placeholder="Enter your password"
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
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="login-submit-btn shadow-sm"
              disabled={busy}
            >
              <span>{busy ? "Authenticating…" : "Sign In to EzExam"}</span>
              <ArrowRight size={16} />
            </button>
          </form>

          {/* Quick Demo Access Bar */}
          <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50/80 p-3.5 text-xs text-slate-600">
            <div className="flex items-center justify-between font-semibold text-slate-800 mb-2">
              <span className="flex items-center gap-1.5 text-[11.5px]">
                <ShieldCheck size={14} className="text-[#48499A]" />
                Demo Credentials
              </span>
              <span className="text-[10.5px] text-slate-400 font-normal">Click to auto-fill</span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => fillDemo("student")}
                className="flex flex-col text-left rounded-lg bg-white p-2 border border-slate-200 hover:border-[#48499A] hover:bg-[#F0F0FA]/50 transition-colors cursor-pointer"
              >
                <span className="font-semibold text-[#48499A] text-[11px]">Student Account</span>
                <span className="text-[10px] text-slate-500 font-medium">student@ezexam.com</span>
              </button>
              <button
                type="button"
                onClick={() => fillDemo("admin")}
                className="flex flex-col text-left rounded-lg bg-white p-2 border border-slate-200 hover:border-[#48499A] hover:bg-[#F0F0FA]/50 transition-colors cursor-pointer"
              >
                <span className="font-semibold text-slate-800 text-[11px]">Admin Account</span>
                <span className="text-[10px] text-slate-500 font-medium">admin@ezexam.com</span>
              </button>
            </div>
          </div>

          {/* Registration Link */}
          <div className="mt-5 text-center text-xs text-slate-500">
            Don&apos;t have an examination account?{" "}
            <Link to="/register" className="font-semibold text-[#48499A] hover:underline">
              Register as Student
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
