import { Routes, Route, Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { TeacherDashboard } from './pages/TeacherDashboard';
import { AdminDashboard } from './pages/AdminDashboard';
import { VerifyEmailPage } from './pages/VerifyEmailPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { ChangeFirstPasswordPage } from './pages/ChangeFirstPasswordPage';
import { ProtectedRoute, getHomePath } from './components/auth/ProtectedRoute';
import { useAuthStore } from './store/authStore';
import ErrorBoundary from './components/common/ErrorBoundary';

// Roles that use admin-style dashboard layout
const ADMIN_LAYOUT_ROLES = ['ADMIN', 'HOD', 'TIMETABLE_COORDINATOR'];

function App() {
  const { isAuthenticated, user, _hasHydrated } = useAuthStore();
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    if (_hasHydrated) {
      setIsReady(true);
    }
  }, [_hasHydrated]);

  if (!isReady) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <ErrorBoundary>
      <Routes>
        {/* Public Routes */}
        <Route
          path="/login"
          element={isAuthenticated ? <Navigate to={getHomePath(user?.role)} /> : <LoginPage />}
        />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/verify-email" element={<VerifyEmailPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route path="/change-first-password" element={<ChangeFirstPasswordPage />} />

        {/* Teacher Routes (TEACHER role only) */}
        <Route
          path="/dashboard/*"
          element={
            <ProtectedRoute allowedRoles={['TEACHER']}>
              <TeacherDashboard />
            </ProtectedRoute>
          }
        />

        {/* Admin-Layout Routes (ADMIN, HOD, TIMETABLE_COORDINATOR) */}
        <Route
          path="/admin/*"
          element={
            <ProtectedRoute allowedRoles={ADMIN_LAYOUT_ROLES}>
              <AdminDashboard />
            </ProtectedRoute>
          }
        />

        {/* Default Route */}
        <Route path="/" element={<Navigate to={isAuthenticated ? getHomePath(user?.role) : "/login"} />} />
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </ErrorBoundary>
  );
}

export default App;
