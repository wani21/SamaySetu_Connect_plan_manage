import axios from 'axios';

// Use environment variable for API URL, fallback to localhost for development
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8083';

// Create axios instance
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Read JWT token from whichever storage the authStore used.
// authStore uses localStorage when rememberMe=true, sessionStorage when false.
// This function must check BOTH — otherwise tokens are silently missing.
function getToken(): string | null {
  return localStorage.getItem('jwt_token') || sessionStorage.getItem('jwt_token');
}

// Guard against multiple parallel 401s each triggering a redirect.
// Without this, 8 simultaneous requests that all 401 would each call
// window.location.href = '/login', causing a rapid reload loop.
let isRedirectingToLogin = false;

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    // Don't add Authorization header for auth endpoints
    if (config.url && config.url.startsWith('/auth')) {
      return config;
    }

    const token = getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Centralized logout + redirect (runs at most once per redirect cycle)
function forceLogout() {
  if (isRedirectingToLogin) return;
  isRedirectingToLogin = true;

  localStorage.removeItem('jwt_token');
  localStorage.removeItem('auth-storage');
  sessionStorage.removeItem('jwt_token');
  sessionStorage.removeItem('auth-storage');
  window.location.href = '/login';
}

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 401 Unauthorized - token expired or invalid
      forceLogout();
    } else if (error.response?.status === 403) {
      // 403 Forbidden — only force logout if it's a structured auth error from SecurityConfig.
      // SecurityConfig returns: {"status":403,"error":"AccessDenied"}
      // Validation errors return different shapes and should NOT trigger logout.
      const errorCode = error.response?.data?.error;
      if (errorCode === 'AccessDenied' || errorCode === 'AuthRequired') {
        forceLogout();
      }
    }
    return Promise.reject(error);
  }
);

// Auth API
// TODO: Import proper types from types/index.ts for better TypeScript support
export const authAPI = {
  register: (registerData: { email: string; password: string; name?: string }) => api.post('/auth/register', registerData),
  login: (credentials: { email: string; password: string }) => api.post('/auth/login', credentials),
  verifyEmail: (token: string) => api.get(`/auth/verify-email?token=${token}`),
  forgotPassword: (email: string) => api.post('/auth/forgot-password', { email }),
  resetPassword: (resetData: { token: string; newPassword: string }) => api.post('/auth/reset-password', resetData),
  changeFirstPassword: (changePasswordData: { email: string; newPassword: string }) => api.post('/auth/change-first-password', changePasswordData),
  generateHash: (password: string) => api.post('/auth/', { password }),
};

// Teacher API
// TODO: Import proper types from types/index.ts for better TypeScript support
export const teacherAPI = {
  create: (teacherData: any) => api.post('/api/teachers', teacherData),
  getProfile: () => api.get('/api/teachers/profile'),
  updateProfile: (profileData: any) => api.put('/api/teachers/profile', profileData),
  getAll: () => api.get('/api/teachers'),
  getById: (id: number) => api.get(`/api/teachers/${id}`),
};

// Department API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const departmentAPI = {
  getAll: () => api.get('/admin/api/departments'),
  getById: (id: number) => api.get(`/admin/api/departments/${id}`),
  getByAcademicYear: (academicYearId: number) => api.get(`/admin/api/departments/academic-year/${academicYearId}`),
  create: (departmentData: any) => api.post('/admin/api/departments', departmentData),
  update: (id: number, departmentData: any) => api.put(`/admin/api/departments/${id}`, departmentData),
  delete: (id: number) => api.delete(`/admin/api/departments/${id}`),
  copyToAcademicYear: (sourceAcademicYearId: number, targetAcademicYearId: number, departmentIds: number[]) =>
    api.post('/admin/api/departments/copy', { sourceAcademicYearId, targetAcademicYearId, departmentIds }),
};

// Teacher API (Admin) - uses same endpoints as regular teacher API
// TODO: Import proper types from types/index.ts for better TypeScript support
export const teacherAdminAPI = {
  getAll: () => api.get('/api/teachers'),
  getById: (id: number) => api.get(`/api/teachers/${id}`),
  create: (teacherData: any) => api.post('/api/teachers', teacherData),
  update: (id: number, teacherData: any) => api.put(`/api/teachers/${id}`, teacherData),
  delete: (id: number) => api.delete(`/api/teachers/${id}`),
  getPendingApprovals: () => api.get('/api/teachers/pending-approvals'),
  approve: (id: number) => api.post(`/api/teachers/${id}/approve`),
  reject: (id: number, rejectionReason?: string) => api.post(`/api/teachers/${id}/reject`, rejectionReason),
};

// Course API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const courseAPI = {
  getAll: () => api.get('/admin/api/courses'),
  getById: (id: number) => api.get(`/admin/api/courses/${id}`),
  create: (courseData: any) => api.post('/admin/api/courses', courseData),
  update: (id: number, courseData: any) => api.put(`/admin/api/courses/${id}`, courseData),
  delete: (id: number) => api.delete(`/admin/api/courses/${id}`),
};

// Room API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const roomAPI = {
  getAll: () => api.get('/admin/api/rooms'),
  getById: (id: number) => api.get(`/admin/api/rooms/${id}`),
  create: (roomData: any) => api.post('/admin/api/rooms', roomData),
  update: (id: number, roomData: any) => api.put(`/admin/api/rooms/${id}`, roomData),
  delete: (id: number) => api.delete(`/admin/api/rooms/${id}`),
};

// Academic Year API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const academicYearAPI = {
  getAll: () => api.get('/admin/api/academic-years'),
  getById: (id: number) => api.get(`/admin/api/academic-years/${id}`),
  create: (academicYearData: any) => api.post('/admin/api/academic-years', academicYearData),
  update: (id: number, academicYearData: any) => api.put(`/admin/api/academic-years/${id}`, academicYearData),
  delete: (id: number) => api.delete(`/admin/api/academic-years/${id}`),
  getCurrent: () => api.get('/admin/api/academic-years/current'),
};

// Division API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const divisionAPI = {
  getAll: () => api.get('/admin/api/divisions'),
  getById: (id: number) => api.get(`/admin/api/divisions/${id}`),
  create: (divisionData: any) => api.post('/admin/api/divisions', divisionData),
  update: (id: number, divisionData: any) => api.put(`/admin/api/divisions/${id}`, divisionData),
  delete: (id: number) => api.delete(`/admin/api/divisions/${id}`),
  getByAcademicYear: (yearId: number) => api.get(`/admin/api/divisions/academic-year/${yearId}`),
};

// Time Slot API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const timeSlotAPI = {
  getAll: () => api.get('/admin/api/time-slots'),
  getById: (id: number) => api.get(`/admin/api/time-slots/${id}`),
  create: (timeSlotData: any) => api.post('/admin/api/time-slots', timeSlotData),
  update: (id: number, timeSlotData: any) => api.put(`/admin/api/time-slots/${id}`, timeSlotData),
  delete: (id: number) => api.delete(`/admin/api/time-slots/${id}`),
  getActive: () => api.get('/admin/api/time-slots/active'),
  getByType: (type: string) => api.get(`/admin/api/time-slots/type/${type}`),
};

// Batch API (Admin)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const batchAPI = {
  getAll: () => api.get('/admin/api/batches'),
  getById: (id: number) => api.get(`/admin/api/batches/${id}`),
  create: (batchData: any) => api.post('/admin/api/batches', batchData),
  update: (id: number, batchData: any) => api.put(`/admin/api/batches/${id}`, batchData),
  delete: (id: number) => api.delete(`/admin/api/batches/${id}`),
  getByDivision: (divisionId: number) => api.get(`/admin/api/batches/division/${divisionId}`),
};

// Time Slot API (Public - for teachers)
export const timeSlotPublicAPI = {
  getAll: () => api.get('/api/time-slots'),
};

// Academic Year API (Public - for teachers)
// Read-only access to academic years — needed by teacher dashboard, timetable, availability pages
export const academicYearPublicAPI = {
  getAll: () => api.get('/api/academic-years'),
  getCurrent: () => api.get('/api/academic-years/current'),
};

// Timetable API (for viewing published timetables)
export const timetableAPI = {
  getByDivision: (divisionId: number, academicYearId: number) =>
    api.get(`/api/timetable/division/${divisionId}?academicYearId=${academicYearId}`),
  getByTeacher: (teacherId: number, academicYearId: number) =>
    api.get(`/api/timetable/teacher/${teacherId}?academicYearId=${academicYearId}`),
  getDraft: (divisionId: number, academicYearId: number) =>
    api.get(`/api/timetable/draft?divisionId=${divisionId}&academicYearId=${academicYearId}`),
  // Admin write operations
  addEntry: (entryData: any) => api.post('/api/timetable/entries', entryData),
  updateEntry: (id: number, entryData: any) => api.put(`/api/timetable/entries/${id}`, entryData),
  deleteEntry: (id: number) => api.delete(`/api/timetable/entries/${id}`),
  validate: (divisionId: number, academicYearId: number) =>
    api.get(`/api/timetable/validate?divisionId=${divisionId}&academicYearId=${academicYearId}`),
  publish: (divisionId: number, academicYearId: number, force: boolean = false) =>
    api.post(`/api/timetable/publish?divisionId=${divisionId}&academicYearId=${academicYearId}&force=${force}`),
  archive: (divisionId: number, academicYearId: number) =>
    api.post(`/api/timetable/archive?divisionId=${divisionId}&academicYearId=${academicYearId}`),
  clearDraft: (divisionId: number, academicYearId: number) =>
    api.delete(`/api/timetable/draft?divisionId=${divisionId}&academicYearId=${academicYearId}`),
  createLabGroup: (labGroupData: any) => api.post('/api/timetable/lab-groups', labGroupData),
  // Export
  exportDivisionPDF: (divisionId: number, academicYearId: number) =>
    api.get(`/api/timetable/export/division/${divisionId}/pdf?academicYearId=${academicYearId}`, { responseType: 'blob' }),
  exportDivisionExcel: (divisionId: number, academicYearId: number) =>
    api.get(`/api/timetable/export/division/${divisionId}/excel?academicYearId=${academicYearId}`, { responseType: 'blob' }),
  exportTeacherPDF: (teacherId: number, academicYearId: number) =>
    api.get(`/api/timetable/export/teacher/${teacherId}/pdf?academicYearId=${academicYearId}`, { responseType: 'blob' }),
  exportTeacherExcel: (teacherId: number, academicYearId: number) =>
    api.get(`/api/timetable/export/teacher/${teacherId}/excel?academicYearId=${academicYearId}`, { responseType: 'blob' }),
  createLabSession: (labData: any) => api.post('/api/timetable/lab-session', labData),
  deleteLabGroup: (groupId: number) => api.delete(`/api/timetable/lab-groups/${groupId}`),
  copyFromDivision: (sourceDivisionId: number, targetDivisionId: number, academicYearId: number) =>
    api.post(`/api/timetable/copy?sourceDivisionId=${sourceDivisionId}&targetDivisionId=${targetDivisionId}&academicYearId=${academicYearId}`),
};

// Staff API (for restricted profile updates)
// TODO: Import proper types from types/index.ts for better TypeScript support
export const staffAPI = {
  getProfile: () => api.get('/api/staff/profile'),
  updateProfile: (profileData: any) => api.put('/api/staff/profile', profileData),
  changePassword: (passwordData: any) => api.post('/api/staff/change-password', passwordData),
  getAvailability: () => api.get('/api/staff/availability'),
  saveAvailability: (entries: any[]) => api.put('/api/staff/availability', entries),
};

// Admin API
// TODO: Import proper types from types/index.ts for better TypeScript support
export const adminAPI = {
  uploadStaff: (staffFile: File) => {
    const formData = new FormData();
    formData.append('file', staffFile);
    return api.post('/admin/upload-staff', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  downloadStaffTemplate: () => api.get('/admin/download-staff-template', {
    responseType: 'blob', // Important for file downloads
  }),
  createStaffManually: (staffData: any) => api.post('/admin/create-staff', staffData),
  updateStaff: (id: number, staffData: any) => api.put(`/admin/update-staff/${id}`, staffData),
  uploadCourses: (courseFile: File, departmentId: number, year: number) => {
    const formData = new FormData();
    formData.append('file', courseFile);
    formData.append('departmentId', departmentId.toString());
    formData.append('year', year.toString());
    return api.post('/admin/upload-courses', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },
  downloadCoursesTemplate: () => api.get('/admin/download-courses-template', {
    responseType: 'blob',
  }),
};

export default api;
