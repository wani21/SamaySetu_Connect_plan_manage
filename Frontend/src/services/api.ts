import axios from 'axios';

const API_BASE_URL = 'http://localhost:8083';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only logout on authentication/authorization errors, not validation errors
    if (error.response?.status === 401) {
      // 401 Unauthorized - token expired or invalid
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('auth-storage');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      // 403 Forbidden - check if it's actually an auth issue or validation error
      const errorMessage = error.response?.data?.message || error.response?.data || '';
      const isAuthError = typeof errorMessage === 'string' && 
        (errorMessage.toLowerCase().includes('token') || 
         errorMessage.toLowerCase().includes('unauthorized') ||
         errorMessage.toLowerCase().includes('forbidden'));
      
      if (isAuthError) {
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('auth-storage');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
export const authAPI = {
  register: (data: any) => api.post('/auth/register', data),
  login: (data: any) => api.post('/auth/login', data),
  verifyEmail: (token: string) => api.get(`/auth/verify-email?token=${token}`),
  forgotPassword: (email: string) => api.post('/auth/forgot-password', { email }),
  resetPassword: (data: any) => api.post('/auth/reset-password', data),
  generateHash: (password: string) => api.post('/auth/', { password }),
};

// Teacher API
export const teacherAPI = {
  create: (data: any) => api.post('/api/teachers', data),
  getProfile: () => api.get('/api/teachers/profile'),
  updateProfile: (data: any) => api.put('/api/teachers/profile', data),
  getAll: () => api.get('/api/teachers'),
  getById: (id: number) => api.get(`/api/teachers/${id}`),
};

// Department API (Admin)
export const departmentAPI = {
  getAll: () => api.get('/admin/api/departments'),
  getById: (id: number) => api.get(`/admin/api/departments/${id}`),
  create: (data: any) => api.post('/admin/api/departments', data),
  update: (id: number, data: any) => api.put(`/admin/api/departments/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/departments/${id}`),
};

// Teacher API (Admin) - uses same endpoints as regular teacher API
export const teacherAdminAPI = {
  getAll: () => api.get('/api/teachers'),
  getById: (id: number) => api.get(`/api/teachers/${id}`),
  create: (data: any) => api.post('/api/teachers', data),
  update: (id: number, data: any) => api.put(`/api/teachers/${id}`, data),
  delete: (id: number) => api.delete(`/api/teachers/${id}`),
  getPendingApprovals: () => api.get('/api/teachers/pending-approvals'),
  approve: (id: number) => api.post(`/api/teachers/${id}/approve`),
  reject: (id: number, reason?: string) => api.post(`/api/teachers/${id}/reject`, reason),
};

// Course API (Admin)
export const courseAPI = {
  getAll: () => api.get('/admin/api/courses'),
  getById: (id: number) => api.get(`/admin/api/courses/${id}`),
  create: (data: any) => api.post('/admin/api/courses', data),
  update: (id: number, data: any) => api.put(`/admin/api/courses/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/courses/${id}`),
};

// Room API (Admin)
export const roomAPI = {
  getAll: () => api.get('/admin/api/rooms'),
  getById: (id: number) => api.get(`/admin/api/rooms/${id}`),
  create: (data: any) => api.post('/admin/api/rooms', data),
  update: (id: number, data: any) => api.put(`/admin/api/rooms/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/rooms/${id}`),
};

// Academic Year API (Admin)
export const academicYearAPI = {
  getAll: () => api.get('/admin/api/academic-years'),
  getById: (id: number) => api.get(`/admin/api/academic-years/${id}`),
  create: (data: any) => api.post('/admin/api/academic-years', data),
  update: (id: number, data: any) => api.put(`/admin/api/academic-years/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/academic-years/${id}`),
  getCurrent: () => api.get('/admin/api/academic-years/current'),
};

// Division API (Admin)
export const divisionAPI = {
  getAll: () => api.get('/admin/api/divisions'),
  getById: (id: number) => api.get(`/admin/api/divisions/${id}`),
  create: (data: any) => api.post('/admin/api/divisions', data),
  update: (id: number, data: any) => api.put(`/admin/api/divisions/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/divisions/${id}`),
  getByAcademicYear: (yearId: number) => api.get(`/admin/api/divisions/academic-year/${yearId}`),
};

// Time Slot API (Admin)
export const timeSlotAPI = {
  getAll: () => api.get('/admin/api/time-slots'),
  getById: (id: number) => api.get(`/admin/api/time-slots/${id}`),
  create: (data: any) => api.post('/admin/api/time-slots', data),
  update: (id: number, data: any) => api.put(`/admin/api/time-slots/${id}`, data),
  delete: (id: number) => api.delete(`/admin/api/time-slots/${id}`),
  getActive: () => api.get('/admin/api/time-slots/active'),
};

// Time Slot API (Public - for teachers)
export const timeSlotPublicAPI = {
  getAll: () => api.get('/api/time-slots'),
};

export default api;
