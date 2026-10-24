import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { LogOut } from "lucide-react";

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // If on split-screen login or register pages, do not render top navbar
  if (location.pathname === "/login" || location.pathname === "/register") {
    return null;
  }

  const isExamHall = location.pathname.includes("/attempt");
  const isDashboard = location.pathname === "/dashboard" || location.pathname === "/admin";

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate("/login");
    }
  };

  const getInitials = (name) => {
    if (!name) return "U";
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  return (
    <header className="sticky top-0 z-40 border-b border-[#E5E7EB] bg-white shadow-xs">
      <div className="mx-auto flex h-[68px] sm:h-[70px] max-w-7xl items-center justify-between px-4 sm:px-6">
        {/* Left Section: Brand Lockup */}
        <Link to="/" className="flex items-center gap-3 group shrink-0">
          <img
            src="/vit-logo.png"
            alt="Vidyalankar Institute of Technology Logo"
            className="h-10 w-auto object-contain shrink-0"
          />
          <div className="flex flex-col justify-center leading-tight">
            <span className="text-base sm:text-lg font-bold tracking-tight text-[#111827]">
              EzExam
            </span>
            <span className="text-[11px] sm:text-xs font-medium text-[#6B7280]">
              VIT Examination Portal
            </span>
          </div>
        </Link>

        {/* Center / Right Section */}
        {isExamHall ? (
          /* Proctored Examination Mode Indicator */
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-rose-50 px-3 py-1 text-xs font-semibold text-rose-700 border border-rose-200">
              <span className="h-2 w-2 rounded-full bg-rose-600 animate-pulse"></span>
              Proctored Examination Mode
            </span>
            {user && (
              <span className="hidden md:inline-flex items-center gap-1.5 text-xs text-slate-600 font-medium">
                Candidate: <strong className="text-slate-900">{user.name}</strong>
              </span>
            )}
          </div>
        ) : (
          /* Standard Institutional Navigation & User Status */
          <div className="flex items-center gap-3 sm:gap-4">
            {user ? (
              <>
                {/* Candidate Identity Lockup */}
                <div className="flex items-center gap-2.5 min-w-0">
                  <div
                    className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#48499A] text-xs font-bold text-white shadow-xs"
                    aria-hidden="true"
                  >
                    {getInitials(user.name)}
                  </div>
                  <div className="hidden sm:flex sm:flex-col leading-tight min-w-0">
                    <span
                      className="text-xs sm:text-sm font-semibold text-[#111827] max-w-[140px] md:max-w-[180px] lg:max-w-[220px] truncate"
                      title={user.name}
                    >
                      {user.name}
                    </span>
                    <span className="text-[10px] text-[#6B7280] capitalize">
                      {user.role}
                    </span>
                  </div>
                  <span className="rounded-full bg-[#F0F0FA] px-2.5 py-0.5 text-[11px] font-semibold text-[#48499A] border border-[#E2E2F2] capitalize shrink-0">
                    {user.role}
                  </span>
                </div>

                {/* Dashboard button only when NOT already on the dashboard */}
                {!isDashboard && (
                  <Link
                    to={user.role === "admin" ? "/admin" : "/dashboard"}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-[#E5E7EB] bg-white px-3 py-1.5 text-xs font-semibold text-[#48499A] shadow-2xs hover:bg-[#F0F0FA] hover:border-[#48499A]/30 transition-colors shrink-0"
                  >
                    <span>Dashboard</span>
                  </Link>
                )}

                {/* Neatly styled Logout button */}
                <button
                  type="button"
                  onClick={handleLogout}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-[#E5E7EB] bg-white px-3 py-1.5 text-xs sm:text-sm font-medium text-slate-700 shadow-2xs hover:bg-rose-50 hover:text-rose-600 hover:border-rose-200 transition-colors cursor-pointer shrink-0"
                  title="Sign out of EzExam"
                  aria-label="Sign out of EzExam"
                >
                  <LogOut size={15} className="shrink-0" />
                  <span className="hidden sm:inline">Logout</span>
                </button>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="rounded-lg px-3.5 py-1.5 text-xs sm:text-sm font-medium text-slate-700 hover:bg-slate-100 transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="rounded-lg bg-[#48499A] px-3.5 py-1.5 text-xs sm:text-sm font-medium text-white shadow-xs hover:bg-[#3F408B] transition-colors"
                >
                  Register
                </Link>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  );
}
