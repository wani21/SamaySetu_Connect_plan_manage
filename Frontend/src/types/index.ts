// User & Auth types
export interface User {
  email: string;
  role: 'ADMIN' | 'TEACHER' | 'HOD' | 'TIMETABLE_COORDINATOR';
  token: string;
  name?: string;
  firstLogin?: boolean;
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
  totalTeachers: number;
  totalCourses: number;
  totalDivisions: number;
  totalRooms: number;
  totalDepartments: number;
  totalBatches: number;
  totalTimeSlots: number;
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
