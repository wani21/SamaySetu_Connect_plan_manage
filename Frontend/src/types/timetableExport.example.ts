/**
 * Example usage of timetableExport types
 * This file demonstrates how to use the TypeScript interfaces
 * and validates they match the backend DTOs
 */

import type {
  TimetableEntry,
  TimeSlot,
  TimetableData,
  AcademicYear,
  Professor,
  Room,
  ViewType,
} from './timetableExport';
import { Semester } from './timetableExport';

// Example TimeSlot
export const exampleTimeSlot: TimeSlot = {
  id: 1,
  slotName: 'Slot 1',
  startTime: '09:00',
  endTime: '09:55',
  isBreak: false,
};

// Example break TimeSlot
export const exampleBreakSlot: TimeSlot = {
  id: 4,
  slotName: 'Short Break',
  startTime: '10:45',
  endTime: '11:00',
  isBreak: true,
};

// Example TimetableEntry for theory class
export const exampleTheoryEntry: TimetableEntry = {
  id: 1,
  courseName: 'Data Structures and Algorithms',
  divisionName: 'CS 3 A',
  roomName: 'Lecture Hall 1',
  roomNumber: 'H301',
  dayOfWeek: 'MONDAY',
  timeSlotId: 1,
  isLabSession: false,
};

// Example TimetableEntry for lab session
export const exampleLabEntry: TimetableEntry = {
  id: 2,
  courseName: 'Database Management Lab',
  divisionName: 'CS 3 B',
  batchName: 'B1',
  roomName: 'Computer Lab 2',
  roomNumber: 'H204B',
  dayOfWeek: 'TUESDAY',
  timeSlotId: 5,
  isLabSession: true,
};

// Example TimetableEntry for room view (includes professor name)
export const exampleRoomViewEntry: TimetableEntry = {
  id: 3,
  courseName: 'Operating Systems',
  divisionName: 'CS 3 A',
  professorName: 'Dr. John Smith',
  roomNumber: 'H301',
  dayOfWeek: 'WEDNESDAY',
  timeSlotId: 2,
  isLabSession: false,
};

// Example TimetableData for professor view
export const exampleProfessorTimetable: TimetableData = {
  entityName: 'Dr. Jane Doe',
  entityIdentifier: 'EMP001',
  academicYearName: '2023-2024',
  semesterLabel: 'Semester 1',
  timeSlots: [
    exampleTimeSlot,
    {
      id: 2,
      slotName: 'Slot 2',
      startTime: '09:55',
      endTime: '10:45',
      isBreak: false,
    },
    exampleBreakSlot,
  ],
  entries: [
    exampleTheoryEntry,
    exampleLabEntry,
  ],
};

// Example TimetableData for room view
export const exampleRoomTimetable: TimetableData = {
  entityName: 'Computer Lab 1',
  entityIdentifier: 'H204A',
  academicYearName: '2023-2024',
  semesterLabel: 'Semester 2',
  timeSlots: [
    exampleTimeSlot,
    {
      id: 2,
      slotName: 'Slot 2',
      startTime: '09:55',
      endTime: '10:45',
      isBreak: false,
    },
  ],
  entries: [
    exampleRoomViewEntry,
    {
      id: 4,
      courseName: 'Computer Networks Lab',
      divisionName: 'CS 3 C',
      batchName: 'B2',
      professorName: 'Dr. Alice Brown',
      roomNumber: 'H204A',
      dayOfWeek: 'THURSDAY',
      timeSlotId: 5,
      isLabSession: true,
    },
  ],
};

// Example AcademicYear
export const exampleAcademicYear: AcademicYear = {
  id: 1,
  yearName: '2023-2024',
  isCurrent: true,
  startDate: '2023-08-01',
  endDate: '2024-07-31',
};

// Example Professor
export const exampleProfessor: Professor = {
  id: 1,
  name: 'Dr. Jane Doe',
  employeeId: 'EMP001',
  email: 'jane.doe@example.com',
  isActive: true,
};

// Example Room
export const exampleRoom: Room = {
  id: 1,
  name: 'Computer Lab 1',
  roomNumber: 'H204A',
  roomType: 'LAB',
  capacity: 60,
  isActive: true,
};

// Example ViewType usage
export const professorViewType: ViewType = 'PROFESSOR';
export const roomViewType: ViewType = 'ROOM';

// Example Semester usage
export const semester1: Semester = Semester.SEM_1;
export const semester2: Semester = Semester.SEM_2;

/**
 * Helper function to validate TimetableData structure
 * This demonstrates type safety in action
 */
export function validateTimetableData(data: TimetableData): boolean {
  return (
    typeof data.entityName === 'string' &&
    typeof data.entityIdentifier === 'string' &&
    typeof data.academicYearName === 'string' &&
    typeof data.semesterLabel === 'string' &&
    Array.isArray(data.timeSlots) &&
    Array.isArray(data.entries)
  );
}

/**
 * Helper function to filter entries by day
 * This demonstrates how to work with the types
 */
export function getEntriesByDay(
  data: TimetableData,
  dayOfWeek: string
): TimetableEntry[] {
  return data.entries.filter((entry) => entry.dayOfWeek === dayOfWeek);
}

/**
 * Helper function to check if a time slot is a break
 */
export function isBreakSlot(slot: TimeSlot): boolean {
  return slot.isBreak;
}

/**
 * Helper function to format professor display name
 */
export function formatProfessorName(professor: Professor): string {
  return `${professor.name} (${professor.employeeId})`;
}

/**
 * Helper function to format room display name
 */
export function formatRoomName(room: Room): string {
  return `${room.name} (${room.roomNumber})`;
}
