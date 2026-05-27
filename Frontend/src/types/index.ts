// User & Auth types
export interface User {
  email: string;
  role: 'ADMIN' | 'SUPER_ADMIN' | 'DEPARTMENT_ADMIN' | 'HOD' | 'TIMETABLE_COORDINATOR' | 'TEACHER';
  token: string;
  name?: string;
  firstLogin?: boolean;
  departmentId?: number; // Include department ID if present
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  phone?: string;
  department?: string;
}

// Academic types
export interface AcademicYear {
  id: number;
  name: string;
  startDate?: string;
  endDate?: string;
  isActive?: boolean;
}

export interface Department {
  id: number;
  name: string;
  academicYearId?: number;
  hodName?: string;
}

export interface Course {
  id: number;
  name: string;
  code?: string;
  credits?: number;
  semester?: number;
  departmentId?: number;
  type?: string;
}

export interface Division {
  id: number;
  name: string;
  departmentId?: number;
  classTeacherId?: number;
  strength?: number;
  year?: number;
  branch?: string;
  totalStudents?: number;
  department?: Department;
  academicYear?: AcademicYear;
}

export interface Batch {
  id: number;
  name: string;
  divisionId?: number;
}

export interface Room {
  id: number;
  name: string;
  building?: string;
  wing?: string;
  capacity?: number;
  type?: string;
}

export interface TimeSlot {
  id: number;
  startTime: string;
  endTime: string;
  type?: string;
  scheduleType?: number;
}

export interface Teacher {
  id: number;
  name: string;
  email: string;
  phone?: string;
  department?: string;
  designation?: string;
  approvalStatus?: string;
  isApproved?: boolean;
  minWeeklyHours?: number;
  maxWeeklyHours?: number;
}

export interface TimetableEntry {
  id: number;
  dayOfWeek: string;
  timeSlotId: number;
  courseId: number;
  teacherId: number;
  roomId: number;
  divisionId: number;
  batchId?: number;
  academicYearId: number;
  type?: string;
}

// API Response types
export interface ApiResponse<T> {
  data: T;
  message?: string;
  status: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  page: number;
  size: number;
}

// Dashboard types
export interface DashboardStats {
  teachersCount: number;
  coursesCount: number;
  divisionsCount: number;
  roomsCount: number;
  overallUtilization: number;
  recentActivities: RecentActivity[];
}

export interface RecentActivity {
  id: number;
  divisionName: string;
  courseName: string;
  courseCode: string;
  teacherName: string;
  roomName: string;
  dayOfWeek: string;
  slotName: string;
  status: string;
  updatedAt: string;
}

export interface StaffMember {
  id: number;
  name: string;
  email: string;
  phone?: string;
  department?: string;
  designation?: string;
  approvalStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  isApproved: boolean;
  firstLogin?: boolean;
}

// Timetable Export types
export * from './timetableExport';
