import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: string[];
}

// Roles that get the admin-style dashboard layout
const ADMIN_ROLES = ['ADMIN', 'HOD', 'TIMETABLE_COORDINATOR'];

// Helper function to validate JWT format (basic check)
const isValidJWT = (token: string | undefined): boolean => {
  if (!token) return false;
  const parts = token.split('.');
  return parts.length === 3 && parts.every(part => part.length > 0);
};

// Get the correct home path for a given role
export const getHomePath = (role: string | undefined): string => {
  if (!role) return '/login';
  if (ADMIN_ROLES.includes(role)) return '/admin/dashboard';
  return '/dashboard';
};

export const isAdminRole = (role: string | undefined): boolean => {
  return !!role && ADMIN_ROLES.includes(role);
};

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  allowedRoles
}) => {
  const { isAuthenticated, user } = useAuthStore();

  // Check if user is authenticated and has a valid token
  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  // Validate token format
  if (!isValidJWT(user.token)) {
    return <Navigate to="/login" replace />;
  }

  // Check role access
  if (allowedRoles && !allowedRoles.includes(user.role)) {
    return <Navigate to={getHomePath(user.role)} replace />;
  }

  return <>{children}</>;
};
