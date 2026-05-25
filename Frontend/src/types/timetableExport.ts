/**
 * TypeScript interfaces for Timetable Export feature
 * Matches backend DTOs for type safety
 */

/**
 * Represents a single timetable entry
 */
export interface TimetableEntry {
  id: number;
  courseName: string;
  divisionName: string;
  divisionYear?: number; // Year of the division (1, 2, 3, 4)
  batchName?: string;
  professorName?: string;
  roomName?: string;
  roomNumber?: string;
  dayOfWeek: string;
  timeSlotId: number;
  isLabSession: boolean;
}

/**
 * Represents a time slot with timing information
 */
export interface TimeSlot {
  id: number;
  slotName: string;
  startTime: string; // Format: "HH:mm"
  endTime: string; // Format: "HH:mm"
  isBreak: boolean;
}

/**
 * Main timetable export data structure
 */
export interface TimetableData {
  entityName: string;
  entityIdentifier: string; // employee_id for professor or room_number for room
  academicYearName: string;
  semesterLabel: string;
  timeSlots: TimeSlot[];
  entries: TimetableEntry[];
}

/**
 * Academic year information
 */
export interface AcademicYear {
  id: number;
  yearName: string;
  isCurrent: boolean;
  startDate?: string;
  endDate?: string;
}

/**
 * Professor information
 */
export interface Professor {
  id: number;
  name: string;
  employeeId: string;
  email?: string;
  isActive: boolean;
}

/**
 * Room information
 */
export interface Room {
  id: number;
  name: string;
  roomNumber: string;
  roomType?: string;
  capacity?: number;
  isActive: boolean;
}

/**
 * View type for timetable export
 */
export type ViewType = 'PROFESSOR' | 'ROOM';

/**
 * Semester enum matching backend
 */
export enum Semester {
  SEM_1 = 'SEM_1',
  SEM_2 = 'SEM_2',
  SEM_3 = 'SEM_3',
  SEM_4 = 'SEM_4',
  SEM_5 = 'SEM_5',
  SEM_6 = 'SEM_6',
  SEM_7 = 'SEM_7',
  SEM_8 = 'SEM_8',
}

/**
 * Props for SelectionForm component
 */
export interface SelectionFormProps {
  academicYearId: number | null;
  setAcademicYearId: (id: number | null) => void;
  semester: Semester | null;
  setSemester: (sem: Semester | null) => void;
  viewType: ViewType | null;
  setViewType: (type: ViewType | null) => void;
  entityId: number | null;
  setEntityId: (id: number | null) => void;
}

/**
 * Props for WeeklyGridDisplay component
 */
export interface WeeklyGridDisplayProps {
  data: TimetableData;
  viewType: ViewType;
}

/**
 * Props for ExportButtons component
 */
export interface ExportButtonsProps {
  viewType: ViewType;
  entityId: number;
  academicYearId: number;
  semester: Semester;
}

/**
 * API error response
 */
export interface ApiError {
  message: string;
  status: number;
}
