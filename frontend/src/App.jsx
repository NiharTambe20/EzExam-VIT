import { BrowserRouter, Navigate, Route, Routes, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar";
import ProtectedRoute from "./components/ProtectedRoute";
import { AuthProvider, useAuth } from "./context/AuthContext";
import AdminDashboard from "./pages/AdminDashboard";
import ExamHistory from "./pages/ExamHistory";
import ExamInstructions from "./pages/ExamInstructions";
import ExamPage from "./pages/ExamPage";
import Login from "./pages/Login";
import ManageExams from "./pages/ManageExams";
import ManageQuestions from "./pages/ManageQuestions";
import Register from "./pages/Register";
import ResultPage from "./pages/ResultPage";
import StudentDashboard from "./pages/StudentDashboard";

function HomeRedirect() {
  const { user, loading } = useAuth();
  if (loading) return null;
  if (!user) return <Navigate to="/login" replace />;
  return <Navigate to={user.role === "admin" ? "/admin" : "/dashboard"} replace />;
}

function AppFooter() {
  const location = useLocation();
  if (
    location.pathname === "/login" ||
    location.pathname === "/register" ||
    location.pathname.includes("/attempt")
  ) {
    return null;
  }

  return (
    <footer className="border-t border-[#E5E7EB] bg-white py-6 text-center text-xs text-[#6B7280] mt-auto">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 space-y-1">
        <p className="font-semibold text-slate-800">
          EzExam • Vidyalankar Institute of Technology, Wadala, Mumbai
        </p>
        <p className="text-[11px] text-slate-500">
          Autonomous Institute Affiliated to University of Mumbai • Accredited &apos;A+&apos; by NAAC
        </p>
      </div>
    </footer>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen bg-[#F8F9FE] text-slate-900 antialiased flex flex-col justify-between">
          <Navbar />
          <div className="flex-1">
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              <Route path="/dashboard" element={<ProtectedRoute roles={["student"]}><StudentDashboard /></ProtectedRoute>} />
              <Route path="/history" element={<ProtectedRoute roles={["student"]}><ExamHistory /></ProtectedRoute>} />
              <Route path="/exams/:id" element={<ProtectedRoute roles={["student"]}><ExamInstructions /></ProtectedRoute>} />
              <Route path="/exams/:id/attempt" element={<ProtectedRoute roles={["student"]}><ExamPage /></ProtectedRoute>} />
              <Route path="/results/:attemptId" element={<ProtectedRoute roles={["student"]}><ResultPage /></ProtectedRoute>} />

              <Route path="/admin" element={<ProtectedRoute roles={["admin"]}><AdminDashboard /></ProtectedRoute>} />
              <Route path="/admin/exams" element={<ProtectedRoute roles={["admin"]}><ManageExams /></ProtectedRoute>} />
              <Route path="/admin/exams/:id/questions" element={<ProtectedRoute roles={["admin"]}><ManageQuestions /></ProtectedRoute>} />

              <Route path="/" element={<HomeRedirect />} />
              <Route path="*" element={
                <div className="mx-auto max-w-md px-4 py-20 text-center">
                  <h1 className="text-4xl font-extrabold text-[#48499A]">404</h1>
                  <p className="mt-2 text-sm text-slate-500">Page not found.</p>
                </div>
              } />
            </Routes>
          </div>
          <AppFooter />
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
}
