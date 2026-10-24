import { NavLink, useNavigate } from "react-router-dom";
import {
  LayoutDashboard,
  History,
  BookOpen,
  LogOut,
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function Sidebar({ role }) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate("/login");
    }
  };

  const studentLinks = [
    { to: "/dashboard", label: "Available Exams", icon: LayoutDashboard },
    { to: "/history", label: "Exam History & Results", icon: History },
  ];

  const adminLinks = [
    { to: "/admin", label: "Portal Overview", icon: LayoutDashboard },
    { to: "/admin/exams", label: "Manage Examinations", icon: BookOpen },
  ];

  const links = role === "admin" ? adminLinks : studentLinks;

  return (
    <aside className="w-full md:w-[260px] shrink-0" aria-label="Portal Navigation">
      <div className="sticky top-[86px] flex flex-col overflow-hidden rounded-2xl bg-[#1F1B47] text-[#B2B3D6] shadow-xl border border-[#282357]">
        {/* Institutional Sidebar Header */}
        <div className="border-b border-[#282357] p-5">
          <div className="flex items-center gap-3 min-w-0">
            <img
              src="/vit-logo-sidebar.png"
              alt="Vidyalankar Institute of Technology"
              className="h-9 w-auto object-contain shrink-0"
            />
            <div className="min-w-0 flex-1">
              <h2 className="text-base font-bold tracking-tight text-white leading-tight truncate">
                EzExam
              </h2>
              <p className="text-xs font-medium text-[#B2B3D6] truncate leading-tight mt-0.5">
                Examination Portal
              </p>
            </div>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 space-y-1.5 p-3.5">
          <div className="px-3 py-1.5 text-[10.5px] font-semibold uppercase tracking-wider text-[#8A8CB5]">
            Navigation
          </div>
          {links.map((link) => {
            const Icon = link.icon;
            return (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.to === "/dashboard" || link.to === "/admin"}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium transition-all duration-150 ${
                    isActive
                      ? "text-white bg-gradient-to-r from-[rgba(72,73,154,0.6)] to-[rgba(53,48,107,0.8)] border-l-[3px] border-[#8B8CD9] shadow-inner font-semibold"
                      : "text-[#B2B3D6] hover:bg-white/[0.07] hover:text-white hover:translate-x-0.5"
                  }`
                }
              >
                <Icon size={18} className="shrink-0" />
                <span className="truncate">{link.label}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Institutional Footer & Logout */}
        <div className="border-t border-[#282357] p-3.5 mt-auto">
          <button
            type="button"
            onClick={handleLogout}
            className="flex w-full items-center gap-3 rounded-xl px-3.5 py-2.5 text-sm font-medium text-[#B2B3D6] transition-colors hover:bg-rose-500/15 hover:text-rose-300 cursor-pointer"
            title="Sign out of EzExam"
          >
            <LogOut size={17} className="shrink-0" />
            <span>Sign Out</span>
          </button>
        </div>
      </div>
    </aside>
  );
}
