import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  FiPlus, FiTrash2, FiSend, FiArchive, FiAlertTriangle,
  FiClock, FiBook, FiEdit2, FiRefreshCw, FiDownload, FiMove,
} from 'react-icons/fi';
import toast from 'react-hot-toast';
import {
  DndContext, DragOverlay, useDraggable, useDroppable,
  PointerSensor, useSensor, useSensors,
  type DragStartEvent, type DragEndEvent,
} from '@dnd-kit/core';
import { CSS } from '@dnd-kit/utilities';
import { Card } from '../common/Card';
import { Button } from '../common/Button';
import { Loading } from '../common/Loading';
import { Modal } from '../common/Modal';
import { getErrorMessage } from '../../utils/errorHandler';
import {
  timetableAPI, academicYearAPI, divisionAPI,
  courseAPI, teacherAdminAPI, roomAPI, timeSlotAPI, batchAPI, departmentAPI,
} from '../../services/api';

// ─── Draggable Entry Card ────────────────────────────────────────────
const DraggableEntryCard: React.FC<{
  entry: any;
  onEdit: (entry: any) => void;
  onDelete: (entryId: number) => void;
  onDeleteLabGroup: (groupId: number) => void;
}> = ({ entry, onEdit, onDelete, onDeleteLabGroup }) => {
  const isLab = entry.course?.courseType === 'LAB';
  const hasLabGroup = !!entry.labSessionGroup?.id;

  // Lab group entries are NOT draggable (too complex — span 2 slots + multiple batches)
  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `entry-${entry.id}`,
    data: { entry },
    disabled: hasLabGroup,
  });

  const style = transform ? {
    transform: CSS.Translate.toString(transform),
    opacity: isDragging ? 0.4 : 1,
    zIndex: isDragging ? 50 : 'auto' as any,
  } : { opacity: 1 };

  // Helper function to get professor initials (first letter of each name part)
  const getProfessorInitials = (name: string): string => {
    if (!name) return '';
    return name
      .split(' ')
      .map(part => part.charAt(0).toUpperCase())
      .join('');
  };

  const professorInitials = entry.teacher?.name ? getProfessorInitials(entry.teacher.name) : '-';
  const roomLocation = entry.room?.roomNumber || '-';
  const courseName = entry.course?.name || 'Unknown';
  const batchName = entry.batch?.name || '';

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`group relative p-2 rounded-lg text-xs cursor-pointer transition-all ${
        isLab
          ? 'bg-purple-50 border border-purple-300 hover:bg-purple-100'
          : 'bg-blue-50 border border-blue-300 hover:bg-blue-100'
      } ${!hasLabGroup ? 'touch-none' : ''}`}
      onClick={() => onEdit(entry)}
      {...(hasLabGroup ? {} : { ...attributes, ...listeners })}
    >
      {/* Drag handle indicator for non-lab entries */}
      {!hasLabGroup && (
        <FiMove size={10} className="absolute top-1 left-1 text-gray-300 group-hover:text-gray-500" />
      )}
      
      {/* Display format based on course type */}
      {isLab ? (
        // Lab format: Batch - Course Name - Professor Initials - Room Location
        <p className={`font-semibold truncate text-purple-900`}>
          {batchName} - {courseName} - {professorInitials} - {roomLocation}
        </p>
      ) : (
        // Theory format: Course Name - Professor Initials - Room Location
        <p className={`font-semibold truncate text-blue-900`}>
          {courseName} - {professorInitials} - {roomLocation}
        </p>
      )}
      
      <button
        onClick={(e) => {
          e.stopPropagation();
          if (hasLabGroup) {
            if (window.confirm('Delete entire lab session group (all batches + both periods)?')) {
              onDeleteLabGroup(entry.labSessionGroup.id);
            }
          } else {
            onDelete(entry.id);
          }
        }}
        className="absolute top-1 right-1 hidden group-hover:flex items-center justify-center w-5 h-5 bg-red-500 text-white rounded-full hover:bg-red-600"
        title={hasLabGroup ? "Delete Lab Group" : "Delete"}
      >
        <FiTrash2 size={10} />
      </button>
    </div>
  );
};

// ─── Droppable Cell Wrapper ──────────────────────────────────────────
const DroppableCell: React.FC<{
  day: string;
  slotId: number;
  rowSpan?: number;
  children: React.ReactNode;
}> = ({ day, slotId, rowSpan = 1, children }) => {
  const { isOver, setNodeRef } = useDroppable({
    id: `cell-${day}-${slotId}`,
    data: { day, slotId },
  });

  return (
    <td
      ref={setNodeRef}
      rowSpan={rowSpan}
      className={`border border-gray-300 p-1 transition-all ${rowSpan > 1 ? 'align-top' : ''} ${
        isOver ? 'ring-2 ring-primary-500 bg-primary-50' : ''
      }`}
    >
      {children}
    </td>
  );
};

const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'] as const;
const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday', FRIDAY: 'Friday', SATURDAY: 'Saturday',
};

// ─── Main Page ──────────────────────────────────────────────────────────
export const TimetableManagementPage: React.FC = () => {
  // Selection state
  const [academicYears, setAcademicYears] = useState<any[]>([]);
  const [departments, setDepartments] = useState<any[]>([]);
  const [divisions, setDivisions] = useState<any[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [selectedDepartmentId, setSelectedDepartmentId] = useState<number | null>(null);
  const [selectedYear, setSelectedYear] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<string>('');
  const [selectedDivisionId, setSelectedDivisionId] = useState<number | null>(null);

  // Timetable data
  const [draftEntries, setDraftEntries] = useState<any[]>([]);
  const [timeSlots, setTimeSlots] = useState<any[]>([]);

  // Reference data for the add/edit form
  const [courses, setCourses] = useState<any[]>([]);
  const [teachers, setTeachers] = useState<any[]>([]);
  const [rooms, setRooms] = useState<any[]>([]);
  const [batches, setBatches] = useState<any[]>([]);

  // UI state
  const [isLoading, setIsLoading] = useState(true);
  const [isActionLoading, setIsActionLoading] = useState(false);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingEntry, setEditingEntry] = useState<any>(null);
  const [conflicts, setConflicts] = useState<string[]>([]);

  // Analytics and Dashboards state (Phase 2)
  const [analytics, setAnalytics] = useState<any>({
    teacherWorkloads: {},
    roomBookingCounts: {},
    slotDensity: {},
  });
  const [activeAnalyticsTab, setActiveAnalyticsTab] = useState<'unscheduled' | 'heatmap' | 'slotOccupancy'>('unscheduled');
  const [isAnalyticsOpen, setIsAnalyticsOpen] = useState(true);
  const [selectedAnalyticsSlot, setSelectedAnalyticsSlot] = useState<{ day: string; slotId: number } | null>(null);

  const [slotFreeRooms, setSlotFreeRooms] = useState<any[]>([]);
  const [slotFreeTeachers, setSlotFreeTeachers] = useState<any[]>([]);
  const [slotFreeBatches, setSlotFreeBatches] = useState<any[]>([]);
  const [isLoadingSlotDetails, setIsLoadingSlotDetails] = useState(false);

  // Filter time slots by the selected division's slot type (TYPE_1, TYPE_2, etc.)
  // This prevents showing duplicate slots when multiple types exist in the database.
  const filteredTimeSlots = useMemo(() => {
    const selectedDiv = divisions.find((d: any) => d.id === selectedDivisionId);
    const slotType = selectedDiv?.timeSlotType || 'TYPE_1';
    return timeSlots.filter((s: any) => !s.type || s.type === slotType);
  }, [timeSlots, divisions, selectedDivisionId]);

  // Get available branches (departments) for selected academic year
  const availableBranches = useMemo(() => {
    if (!selectedYearId) return [];
    // Show only departments that have divisions in the selected academic year
    const departmentIds = new Set(divisions.map((d: any) => d.department?.id).filter(Boolean));
    return departments
      .filter((dept: any) => departmentIds.has(dept.id))
      .sort((a, b) => a.name.localeCompare(b.name));
  }, [departments, divisions, selectedYearId]);

  // Get available years - always show 1-4
  const availableYears = useMemo(() => {
    return [1, 2, 3, 4];
  }, []);

  // Check if divisions exist for selected year
  const divisionsExistForYear = useMemo(() => {
    if (!selectedYearId || !selectedDepartmentId || !selectedYear) return true;
    return divisions.some((d: any) => 
      d.department?.id === selectedDepartmentId && d.year === selectedYear
    );
  }, [divisions, selectedYearId, selectedDepartmentId, selectedYear]);

  // Get available semesters for selected year - always show both semesters
  const availableSemesters = useMemo(() => {
    if (!selectedYear) return [];
    // Year 1 = Sem 1, 2; Year 2 = Sem 3, 4; Year 3 = Sem 5, 6; Year 4 = Sem 7, 8
    const semStart = (selectedYear - 1) * 2 + 1;
    return [
      { value: `SEM_${semStart}`, label: `Semester ${semStart}` },
      { value: `SEM_${semStart + 1}`, label: `Semester ${semStart + 1}` },
    ];
  }, [selectedYear]);

  // Get filtered divisions based on all selections
  const filteredDivisions = useMemo(() => {
    let filtered = divisions;
    if (selectedDepartmentId) {
      filtered = filtered.filter((d: any) => d.department?.id === selectedDepartmentId);
    }
    if (selectedYear) {
      filtered = filtered.filter((d: any) => d.year === selectedYear);
    }
    return filtered.sort((a, b) => a.name.localeCompare(b.name));
  }, [divisions, selectedDepartmentId, selectedYear]);

  // Phase 2: Compute courses for the selected division and semester
  const divisionCourses = useMemo(() => {
    const selectedDiv = divisions.find((d: any) => d.id === selectedDivisionId);
    if (!selectedDiv) return [];
    return courses.filter((c: any) =>
      (!selectedDiv.department || (c.department && c.department.id === selectedDiv.department.id)) &&
      (c.year === selectedDiv.year) &&
      (!selectedSemester || c.semester === selectedSemester)
    );
  }, [courses, divisions, selectedDivisionId, selectedSemester]);

  // Phase 2: Calculate scheduled vs unscheduled allocations for division courses
  const courseAllocationStats = useMemo(() => {
    return divisionCourses.map((c: any) => {
      const isLab = c.courseType === 'LAB';
      const scheduledCount = draftEntries.filter((e: any) => e.course?.id === c.id).length;
      
      let progress = 0;
      let targetText = '';
      let scheduledText = '';
      
      if (isLab) {
        // Lab course: check how many unique batches are allocated
        const allocatedBatches = new Set(
          draftEntries
            .filter((e: any) => e.course?.id === c.id && e.batch)
            .map((e: any) => e.batch.id)
        ).size;
        progress = batches.length > 0 ? (allocatedBatches / batches.length) * 100 : 100;
        targetText = `${batches.length} Batches`;
        scheduledText = `${allocatedBatches} Batches`;
      } else {
        // Theory course: count hours scheduled
        progress = c.hoursPerWeek > 0 ? (scheduledCount / c.hoursPerWeek) * 100 : 100;
        targetText = `${c.hoursPerWeek} Hours`;
        scheduledText = `${scheduledCount} Hours`;
      }
      
      return {
        id: c.id,
        name: c.name,
        code: c.code,
        isLab,
        progress: Math.min(progress, 100),
        scheduledText,
        targetText,
        isComplete: progress >= 100,
      };
    });
  }, [divisionCourses, draftEntries, batches]);

  // Pre-filled day/slot when clicking a cell
  const [prefillDay, setPrefillDay] = useState<string | null>(null);
  const [prefillSlotId, setPrefillSlotId] = useState<number | null>(null);

  // ─── Initial load ───────────────────────────────────────────────────
  useEffect(() => {
    const loadInitial = async () => {
      try {
        const [yearsRes, slotsRes, coursesRes, teachersRes, roomsRes, deptsRes] = await Promise.all([
          academicYearAPI.getAll(),
          timeSlotAPI.getAll(),
          courseAPI.getAll(),
          teacherAdminAPI.getAll(),
          roomAPI.getAll(),
          departmentAPI.getAll(),
        ]);

        const years = Array.isArray(yearsRes.data) ? yearsRes.data : [];
        setAcademicYears(years);

        // Keep ALL slots (including breaks) — breaks render as grey rows in the grid
        const slots = (Array.isArray(slotsRes.data) ? slotsRes.data : [])
          .sort((a: any, b: any) => a.startTime.localeCompare(b.startTime));
        setTimeSlots(slots);

        setCourses(Array.isArray(coursesRes.data) ? coursesRes.data : []);
        setTeachers(Array.isArray(teachersRes.data) ? teachersRes.data : []);
        setRooms(Array.isArray(roomsRes.data) ? roomsRes.data : []);
        setDepartments(Array.isArray(deptsRes.data) ? deptsRes.data : []);

        // Auto-select current year
        const current = years.find((y: any) => y.isCurrent);
        if (current) {
          setSelectedYearId(current.id);
        }
      } catch (err) {
        toast.error('Failed to load initial data');
        if (import.meta.env.DEV) console.error(err);
      } finally {
        setIsLoading(false);
      }
    };
    loadInitial();
  }, []);

  // Load divisions when year, department, or year changes
  useEffect(() => {
    if (!selectedYearId) {
      setDivisions([]);
      setSelectedDepartmentId(null);
      setSelectedYear(null);
      setSelectedSemester('');
      setSelectedDivisionId(null);
      return;
    }
    const loadDivisions = async () => {
      try {
        const res = await divisionAPI.getByAcademicYear(selectedYearId);
        const divs = Array.isArray(res.data) ? res.data : [];
        setDivisions(divs);
        // Reset downstream selections
        setSelectedDepartmentId(null);
        setSelectedYear(null);
        setSelectedSemester('');
        setSelectedDivisionId(null);
        setDraftEntries([]);
      } catch {
        setDivisions([]);
      }
    };
    loadDivisions();
  }, [selectedYearId]);

  // Reset downstream selections when department changes
  useEffect(() => {
    if (selectedDepartmentId !== null) {
      setSelectedYear(null);
      setSelectedDivisionId(null);
      setSelectedSemester('');
      setDraftEntries([]);
    }
  }, [selectedDepartmentId]);

  // Reset downstream selections when year changes
  useEffect(() => {
    if (selectedYear !== null) {
      setSelectedDivisionId(null);
      setSelectedSemester('');
      setDraftEntries([]);
    }
  }, [selectedYear]);

  // Reset downstream selections when division changes
  useEffect(() => {
    if (selectedDivisionId !== null) {
      setSelectedSemester('');
      setDraftEntries([]);
    }
  }, [selectedDivisionId]);

  // Load batches when division changes
  useEffect(() => {
    if (!selectedDivisionId) {
      setBatches([]);
      return;
    }
    const loadBatches = async () => {
      try {
        const res = await batchAPI.getByDivision(selectedDivisionId);
        setBatches(Array.isArray(res.data) ? res.data : []);
      } catch {
        setBatches([]);
      }
    };
    loadBatches();
  }, [selectedDivisionId]);

  // ─── Fetch draft entries ────────────────────────────────────────────
  const fetchDraft = useCallback(async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    try {
      setIsActionLoading(true);
      // Use getEditable instead of getDraft to show both DRAFT and PUBLISHED entries
      const res = await timetableAPI.getEditable(selectedDivisionId, selectedYearId, selectedSemester);
      setDraftEntries(Array.isArray(res.data) ? res.data : []);
    } catch {
      setDraftEntries([]);
    } finally {
      setIsActionLoading(false);
    }
  }, [selectedDivisionId, selectedYearId, selectedSemester]);

  useEffect(() => {
    fetchDraft();
  }, [fetchDraft]);

  // Fetch Analytics DTO (Phase 2)
  const fetchAnalytics = useCallback(async () => {
    if (!selectedYearId) return;
    try {
      const res = await timetableAPI.getAnalytics(selectedYearId);
      setAnalytics(res.data || { teacherWorkloads: {}, roomBookingCounts: {}, slotDensity: {} });
    } catch (err) {
      console.error('Failed to load analytics:', err);
    }
  }, [selectedYearId]);

  useEffect(() => {
    fetchAnalytics();
  }, [selectedYearId, draftEntries, fetchAnalytics]);

  // Load live slot occupancy details when slot selection changes (Phase 2)
  useEffect(() => {
    if (!selectedAnalyticsSlot || !selectedYearId || !selectedSemester) return;
    
    const loadSlotDetails = async () => {
      try {
        setIsLoadingSlotDetails(true);
        const [roomsRes, teachersRes, batchesRes] = await Promise.all([
          timetableAPI.getAvailableRooms(
            selectedAnalyticsSlot.day,
            selectedAnalyticsSlot.slotId,
            selectedYearId,
            selectedSemester,
            selectedDivisionId || undefined
          ),
          timetableAPI.getAvailableTeachers(
            selectedAnalyticsSlot.day,
            selectedAnalyticsSlot.slotId,
            selectedYearId,
            selectedSemester
          ),
          selectedDivisionId ? timetableAPI.getAvailableBatches(
            selectedDivisionId,
            selectedAnalyticsSlot.day,
            selectedAnalyticsSlot.slotId,
            selectedYearId,
            selectedSemester
          ) : Promise.resolve({ data: [] }),
        ]);
        setSlotFreeRooms(Array.isArray(roomsRes.data) ? roomsRes.data : []);
        setSlotFreeTeachers(Array.isArray(teachersRes.data) ? teachersRes.data : []);
        setSlotFreeBatches(Array.isArray(batchesRes.data) ? batchesRes.data : []);
      } catch (err) {
        console.error('Failed to load slot details:', err);
      } finally {
        setIsLoadingSlotDetails(false);
      }
    };

    loadSlotDetails();
  }, [selectedAnalyticsSlot, selectedYearId, selectedSemester, selectedDivisionId]);

  // ─── Grid lookup ────────────────────────────────────────────────────
  const getEntriesForCell = useCallback((day: string, slotId: number) =>
    draftEntries.filter((e: any) =>
      e.dayOfWeek === day &&
      e.timeSlot?.id === slotId &&
      (!selectedSemester || e.semester === selectedSemester || !e.semester)
    ), [draftEntries, selectedSemester]);

  const formatTime = (t: string) => (t ? t.substring(0, 5) : '');

  // ─── Cell Merging Helper ────────────────────────────────────────────
  // Check if a lab entry should be merged with the next slot
  const shouldMergeLabEntry = useCallback((entry: any, day: string, slotIdx: number): boolean => {
    if (!entry.course || entry.course.courseType !== 'LAB') return false;
    if (slotIdx + 1 >= filteredTimeSlots.length) return false;

    const nextSlot = filteredTimeSlots[slotIdx + 1];
    if (nextSlot.isBreak) return false;

    const nextEntries = getEntriesForCell(day, nextSlot.id);
    
    // Find matching entry in next slot
    // Match by: same course, same teacher, same room, same day
    // If labSessionGroup exists, also match by that
    const nextEntry = nextEntries.find((e: any) => {
      if (!e.course || e.course.courseType !== 'LAB') return false;
      if (e.course.id !== entry.course.id) return false;
      if (e.teacher?.id !== entry.teacher?.id) return false;
      if (e.room?.id !== entry.room?.id) return false;
      if (e.dayOfWeek !== entry.dayOfWeek) return false;
      
      // If both have labSessionGroup, they must match
      if (entry.labSessionGroup && e.labSessionGroup) {
        return entry.labSessionGroup.id === e.labSessionGroup.id;
      }
      
      // If both have batch, they must match
      if (entry.batch && e.batch) {
        return entry.batch.id === e.batch.id;
      }
      
      // Otherwise, match by course/teacher/room is enough
      return true;
    });

    return !!nextEntry;
  }, [filteredTimeSlots, getEntriesForCell]);

  // Track which cells should be skipped (because they're part of a merged cell)
  const mergedCells = useMemo(() => {
    const merged = new Set<string>();
    filteredTimeSlots.forEach((slot: any, slotIdx: number) => {
      if (slot.isBreak) return;
      DAYS.forEach((day) => {
        const entries = getEntriesForCell(day, slot.id);
        entries.forEach((entry: any) => {
          if (shouldMergeLabEntry(entry, day, slotIdx)) {
            // Mark the next slot as merged
            const nextSlot = filteredTimeSlots[slotIdx + 1];
            merged.add(`${day}-${nextSlot.id}`);
          }
        });
      });
    });
    return merged;
  }, [filteredTimeSlots, getEntriesForCell, shouldMergeLabEntry]);

  // ─── Add / Edit entry ──────────────────────────────────────────────
  const openAddModal = (day?: string, slotId?: number) => {
    setEditingEntry(null);
    setConflicts([]);
    setPrefillDay(day || null);
    setPrefillSlotId(slotId || null);
    setShowAddModal(true);
  };

  const openEditModal = (entry: any) => {
    setEditingEntry(entry);
    setConflicts([]);
    setPrefillDay(null);
    setPrefillSlotId(null);
    setShowAddModal(true);
  };

  const handleDeleteEntry = async (entryId: number) => {
    if (!window.confirm('Delete this timetable entry?')) return;
    try {
      await timetableAPI.deleteEntry(entryId);
      toast.success('Entry deleted');
      fetchDraft();
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    }
  };

  const handleDeleteLabGroup = async (groupId: number) => {
    try {
      const res = await timetableAPI.deleteLabGroup(groupId);
      toast.success(`Lab group deleted (${(res.data as any).entriesDeleted} entries removed)`);
      fetchDraft();
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    }
  };

  // ─── Drag-and-Drop ──────────────────────────────────────────────
  const [activeDragEntry, setActiveDragEntry] = useState<any>(null);
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 8 } }) // 8px drag threshold to avoid accidental drags
  );

  const handleDragStart = (event: DragStartEvent) => {
    setActiveDragEntry(event.active.data.current?.entry || null);
  };

  const handleDragEnd = async (event: DragEndEvent) => {
    setActiveDragEntry(null);
    const { active, over } = event;
    if (!over || !active.data.current?.entry) return;

    const entry = active.data.current.entry;
    const targetData = over.data.current as { day: string; slotId: number } | undefined;
    if (!targetData) return;

    const { day: newDay, slotId: newSlotId } = targetData;

    // Same cell — do nothing
    if (entry.dayOfWeek === newDay && entry.timeSlot?.id === newSlotId) return;

    // Check if target cell already has an entry (swap scenario)
    const targetEntries = getEntriesForCell(newDay, newSlotId);
    const targetEntry = targetEntries.find((e: any) => !e.labSessionGroup); // Non-lab entry to swap with

    if (targetEntry) {
      // Swap flow
      if (!window.confirm(`Swap "${entry.course?.name}" with "${targetEntry.course?.name}"?`)) return;

      try {
        setIsActionLoading(true);
        // Move entry A to target
        await timetableAPI.updateEntry(entry.id, {
          ...buildPayloadFromEntry(entry),
          dayOfWeek: newDay,
          timeSlotId: newSlotId,
        });
        // Move entry B to source
        await timetableAPI.updateEntry(targetEntry.id, {
          ...buildPayloadFromEntry(targetEntry),
          dayOfWeek: entry.dayOfWeek,
          timeSlotId: entry.timeSlot.id,
        });
        toast.success('Entries swapped!');
        fetchDraft();
      } catch (err: any) {
        toast.error(err.response?.data?.conflicts?.[0] || getErrorMessage(err));
        fetchDraft(); // Refresh to revert visual state
      } finally {
        setIsActionLoading(false);
      }
    } else {
      // Simple move
      try {
        setIsActionLoading(true);
        await timetableAPI.updateEntry(entry.id, {
          ...buildPayloadFromEntry(entry),
          dayOfWeek: newDay,
          timeSlotId: newSlotId,
        });
        toast.success('Entry moved!');
        fetchDraft();
      } catch (err: any) {
        const conflicts = err.response?.data?.conflicts;
        if (conflicts?.length) {
          toast.error(conflicts[0]);
        } else {
          toast.error(getErrorMessage(err));
        }
      } finally {
        setIsActionLoading(false);
      }
    }
  };

  // Build an update payload from an existing entry (preserving all fields)
  const buildPayloadFromEntry = (entry: any) => ({
    divisionId: selectedDivisionId,
    academicYearId: selectedYearId,
    courseId: entry.course?.id,
    teacherId: entry.teacher?.id,
    roomId: entry.room?.id,
    timeSlotId: entry.timeSlot?.id,
    dayOfWeek: entry.dayOfWeek,
    semester: entry.semester || undefined,
    notes: entry.notes || undefined,
    batchId: entry.batch?.id || undefined,
    labSessionGroupId: entry.labSessionGroup?.id || undefined,
  });

  // ─── Export helpers ──────────────────────────────────────────────
  const triggerDownload = (data: any, filename: string) => {
    const blob = new Blob([data]);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportPDF = async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    try {
      const res = await timetableAPI.exportDivisionPDF(selectedDivisionId, selectedYearId, selectedSemester);
      triggerDownload(res.data, `timetable_division_${selectedDivisionId}_${selectedSemester}.pdf`);
      toast.success('PDF downloaded!');
    } catch { toast.error('Failed to export PDF'); }
  };

  const handleExportExcel = async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    try {
      const res = await timetableAPI.exportDivisionExcel(selectedDivisionId, selectedYearId, selectedSemester);
      triggerDownload(res.data, `timetable_division_${selectedDivisionId}_${selectedSemester}.xlsx`);
      toast.success('Excel downloaded!');
    } catch { toast.error('Failed to export Excel'); }
  };

  // ─── Validation + Publish ────────────────────────────────────────
  const [showValidation, setShowValidation] = useState(false);
  const [validationResult, setValidationResult] = useState<any>(null);

  const handlePublish = async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    // First run validation
    try {
      setIsActionLoading(true);
      const valRes = await timetableAPI.validate(selectedDivisionId, selectedYearId);
      setValidationResult(valRes.data);
      setShowValidation(true);
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    } finally {
      setIsActionLoading(false);
    }
  };

  const confirmPublish = async (force: boolean = false) => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.publish(selectedDivisionId, selectedYearId, selectedSemester, force);
      if (res.status === 200) {
        toast.success(res.data.message || 'Timetable published!');
        setShowValidation(false);
        setValidationResult(null);
        fetchDraft();
      }
    } catch (err: any) {
      // If publish returns 409 with errors, show them
      if (err.response?.status === 409) {
        setValidationResult(err.response.data);
      } else {
        toast.error(getErrorMessage(err));
      }
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleArchive = async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    if (!window.confirm(`Archive the currently published timetable for ${selectedSemester}?`)) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.archive(selectedDivisionId, selectedYearId, selectedSemester);
      toast.success(res.data.message || 'Timetable archived');
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleClearDraft = async () => {
    if (!selectedDivisionId || !selectedYearId || !selectedSemester) return;
    if (!window.confirm(`Clear ALL draft entries for ${selectedSemester}? This cannot be undone.`)) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.clearDraft(selectedDivisionId, selectedYearId, selectedSemester);
      toast.success(res.data.message || 'Draft cleared');
      fetchDraft();
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    } finally {
      setIsActionLoading(false);
    }
  };

  // ─── Render ─────────────────────────────────────────────────────────
  if (isLoading) return <Loading />;

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-1">Timetable Builder</h1>
          <p className="text-gray-600">Create and manage division timetables</p>
        </div>
        {selectedDivisionId && selectedYearId && selectedSemester && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={fetchDraft} isLoading={isActionLoading}>
              <FiRefreshCw className="mr-1" /> Refresh
            </Button>
            <Button variant="outline" onClick={handleClearDraft} className="text-red-600 border-red-300 hover:bg-red-50">
              <FiTrash2 className="mr-1" /> Clear Draft
            </Button>
            <Button variant="outline" onClick={handleArchive}>
              <FiArchive className="mr-1" /> Archive Published
            </Button>
            <Button variant="outline" onClick={handleExportPDF}>
              <FiDownload className="mr-1" /> PDF
            </Button>
            <Button variant="outline" onClick={handleExportExcel}>
              <FiDownload className="mr-1" /> Excel
            </Button>
            <Button variant="primary" onClick={handlePublish} isLoading={isActionLoading}>
              <FiSend className="mr-1" /> Publish
            </Button>
          </div>
        )}
      </div>

      {/* Selectors */}
      <Card className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          {/* 1. Academic Year */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Academic Year <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedYearId || ''}
              onChange={(e) => setSelectedYearId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select Academic Year</option>
              {academicYears.map((y: any) => (
                <option key={y.id} value={y.id}>
                  {y.yearName || `Academic Year ${y.id}`} {y.isCurrent ? '(Current)' : ''}
                </option>
              ))}
            </select>
          </div>

          {/* 2. Branch (Department) */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Branch <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedDepartmentId || ''}
              onChange={(e) => setSelectedDepartmentId(e.target.value ? Number(e.target.value) : null)}
              disabled={!selectedYearId || availableBranches.length === 0}
            >
              <option value="">Select Branch</option>
              {availableBranches.map((dept: any) => (
                <option key={dept.id} value={dept.id}>
                  {dept.name}
                </option>
              ))}
            </select>
            {selectedYearId && availableBranches.length === 0 && (
              <p className="text-xs text-orange-600 mt-1">No branches configured</p>
            )}
          </div>

          {/* 3. Year */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Year <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedYear || ''}
              onChange={(e) => setSelectedYear(e.target.value ? Number(e.target.value) : null)}
              disabled={!selectedDepartmentId}
            >
              <option value="">Select Year</option>
              {availableYears.map((year: number) => (
                <option key={year} value={year}>
                  {year === 1 ? 'First Year (FY)' :
                   year === 2 ? 'Second Year (SY)' :
                   year === 3 ? 'Third Year (TY)' :
                   'Fourth Year (B.Tech)'}
                </option>
              ))}
            </select>
          </div>

          {/* 4. Division */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Division <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedDivisionId || ''}
              onChange={(e) => setSelectedDivisionId(e.target.value ? Number(e.target.value) : null)}
              disabled={!selectedYear || filteredDivisions.length === 0}
            >
              <option value="">Select Division</option>
              {filteredDivisions.map((d: any) => (
                <option key={d.id} value={d.id}>
                  {d.name}
                </option>
              ))}
            </select>
            {selectedYear && !divisionsExistForYear && (
              <p className="text-xs text-orange-600 mt-1">⚠️ No divisions configured for this year</p>
            )}
          </div>

          {/* 5. Semester */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Semester <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedSemester}
              onChange={(e) => setSelectedSemester(e.target.value)}
              disabled={!selectedDivisionId || availableSemesters.length === 0}
            >
              <option value="">Select Semester</option>
              {availableSemesters.map((sem) => (
                <option key={sem.value} value={sem.value}>
                  {sem.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </Card>

      {/* Grid or placeholder */}
      {!selectedDivisionId || !selectedYearId || !selectedSemester ? (
        <Card>
          <div className="text-center py-16">
            <FiBook className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 text-lg mb-2">Select all filters to start building the timetable</p>
            <p className="text-gray-400 text-sm">
              Choose: Academic Year → Branch → Year → Division → Semester
            </p>
          </div>
        </Card>
      ) : (
        <>
          {/* Collapsible Analytics Dashboard (Phase 2) */}
          <Card className="mb-6 overflow-hidden border border-gray-200 shadow-sm">
            <div
              className="flex items-center justify-between px-4 py-3 bg-gray-50 border-b border-gray-100 cursor-pointer hover:bg-gray-100 transition-colors"
              onClick={() => setIsAnalyticsOpen(!isAnalyticsOpen)}
            >
              <div className="flex items-center gap-2">
                <FiRefreshCw className={`text-primary-800 ${isActionLoading ? 'animate-spin' : ''}`} />
                <span className="font-semibold text-gray-800 text-sm">Timetable Analytics & Dashboard</span>
              </div>
              <span className="text-xs text-primary-700 font-medium hover:underline">
                {isAnalyticsOpen ? 'Collapse ▴' : 'Expand ▾'}
              </span>
            </div>

            {isAnalyticsOpen && (
              <div className="p-4 bg-white space-y-4">
                {/* Tabs Selector */}
                <div className="flex border-b border-gray-200 text-xs">
                  <button
                    onClick={() => setActiveAnalyticsTab('unscheduled')}
                    className={`pb-2 px-4 font-semibold transition-all ${
                      activeAnalyticsTab === 'unscheduled'
                        ? 'border-b-2 border-primary-600 text-primary-900'
                        : 'text-gray-500 hover:text-gray-800'
                    }`}
                  >
                    Unscheduled Subjects Tracker
                  </button>
                  <button
                    onClick={() => setActiveAnalyticsTab('heatmap')}
                    className={`pb-2 px-4 font-semibold transition-all ${
                      activeAnalyticsTab === 'heatmap'
                        ? 'border-b-2 border-primary-600 text-primary-900'
                        : 'text-gray-500 hover:text-gray-800'
                    }`}
                  >
                    Timetable Density Heat Map
                  </button>
                  <button
                    onClick={() => setActiveAnalyticsTab('slotOccupancy')}
                    className={`pb-2 px-4 font-semibold transition-all ${
                      activeAnalyticsTab === 'slotOccupancy'
                        ? 'border-b-2 border-primary-600 text-primary-900'
                        : 'text-gray-500 hover:text-gray-800'
                    }`}
                  >
                    Live Slot Occupancy Drawer
                  </button>
                </div>

                {/* Tab content */}
                {activeAnalyticsTab === 'unscheduled' && (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                    {courseAllocationStats.length === 0 ? (
                      <p className="col-span-full text-xs text-gray-500 italic text-center py-4">No courses configured for this branch/year.</p>
                    ) : (
                      courseAllocationStats.map((stat: any) => (
                        <div key={stat.id} className="border border-gray-100 rounded-lg p-3 bg-gray-50 shadow-sm flex flex-col justify-between">
                          <div>
                            <div className="flex items-center justify-between mb-1.5">
                              <span className="font-semibold text-gray-800 text-xs truncate max-w-[180px]" title={stat.name}>{stat.name}</span>
                              <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full ${stat.isLab ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'}`}>
                                {stat.isLab ? 'LAB' : 'THEORY'}
                              </span>
                            </div>
                            <div className="flex items-center justify-between text-[10px] text-gray-500 mb-1">
                              <span>Scheduled: {stat.scheduledText}</span>
                              <span>Target: {stat.targetText}</span>
                            </div>
                          </div>
                          <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                            <div
                              className={`h-1.5 rounded-full transition-all duration-300 ${stat.isComplete ? 'bg-green-500' : 'bg-amber-400 animate-pulse'}`}
                              style={{ width: `${stat.progress}%` }}
                            />
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}

                {activeAnalyticsTab === 'heatmap' && (
                  <div>
                    <p className="text-[10px] text-gray-500 mb-3">Global slot utilization density (busy counts across all divisions):</p>
                    <div className="overflow-x-auto">
                      <table className="w-full border-collapse text-left text-xs min-w-[700px]">
                        <thead>
                          <tr className="bg-gray-100 text-gray-700 font-semibold border-b border-gray-200">
                            <th className="p-2 border border-gray-200 w-24">Slot</th>
                            {DAYS.map(day => <th key={day} className="p-2 border border-gray-200 text-center">{DAY_LABELS[day]}</th>)}
                          </tr>
                        </thead>
                        <tbody>
                          {filteredTimeSlots.filter(s => !s.isBreak).map(slot => (
                            <tr key={slot.id} className="hover:bg-gray-50">
                              <td className="p-2 border border-gray-200 bg-gray-50 font-medium">
                                {slot.slotName} ({formatTime(slot.startTime)})
                              </td>
                              {DAYS.map(day => {
                                const densityCount = analytics.slotDensity[`${day}_${slot.id}`] || 0;
                                let colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-100'; // high availability
                                if (densityCount >= 1 && densityCount <= 2) {
                                  colorClass = 'bg-sky-50 text-sky-700 border-sky-100';
                                } else if (densityCount >= 3 && densityCount <= 5) {
                                  colorClass = 'bg-amber-50 text-amber-700 border-amber-100';
                                } else if (densityCount >= 6) {
                                  colorClass = 'bg-rose-50 text-rose-700 font-bold border-rose-100'; // congested
                                }

                                return (
                                  <td
                                    key={day}
                                    onClick={() => {
                                      setSelectedAnalyticsSlot({ day, slotId: slot.id });
                                      setActiveAnalyticsTab('slotOccupancy');
                                    }}
                                    className={`p-2 border text-center cursor-pointer transition-colors hover:opacity-80 ${colorClass}`}
                                    title="Click to view live occupancy details"
                                  >
                                    {densityCount} Busy
                                  </td>
                                );
                              })}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

                {activeAnalyticsTab === 'slotOccupancy' && (
                  <div className="space-y-3">
                    {selectedAnalyticsSlot ? (
                      <div>
                        <div className="flex items-center justify-between bg-primary-50 text-primary-900 px-3 py-2 rounded-lg mb-3">
                          <span className="font-semibold text-xs">
                            Live Occupancy for {DAY_LABELS[selectedAnalyticsSlot.day]} - {
                              filteredTimeSlots.find(s => s.id === selectedAnalyticsSlot.slotId)?.slotName || `Period ${selectedAnalyticsSlot.slotId}`
                            }
                          </span>
                          <button
                            onClick={() => setSelectedAnalyticsSlot(null)}
                            className="text-[10px] hover:underline"
                          >
                            Reset Slot selection
                          </button>
                        </div>
                        
                        {isLoadingSlotDetails ? (
                          <div className="text-center py-6 text-xs text-gray-500 animate-pulse">Querying live slot details...</div>
                        ) : (
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                            {/* Free Rooms */}
                            <div className="border border-gray-100 rounded-lg p-3 bg-gray-50">
                              <h4 className="font-semibold text-gray-800 text-xs mb-2 border-b pb-1">Available Rooms ({slotFreeRooms.length})</h4>
                              <div className="max-h-40 overflow-y-auto space-y-1 text-[10px]">
                                {slotFreeRooms.length === 0 ? (
                                  <p className="text-gray-500 italic">No rooms free at this time.</p>
                                ) : (
                                  slotFreeRooms.map(r => (
                                    <div key={r.id} className="flex justify-between bg-white border border-gray-200 rounded px-2 py-1">
                                      <span className="font-medium text-gray-700">{r.roomNumber} ({r.roomType || 'classroom'})</span>
                                      <span className="text-gray-400">cap: {r.capacity}</span>
                                    </div>
                                  ))
                                )}
                              </div>
                            </div>

                            {/* Free Teachers */}
                            <div className="border border-gray-100 rounded-lg p-3 bg-gray-50">
                              <h4 className="font-semibold text-gray-800 text-xs mb-2 border-b pb-1">Available Teachers ({slotFreeTeachers.length})</h4>
                              <div className="max-h-40 overflow-y-auto space-y-1 text-[10px]">
                                {slotFreeTeachers.length === 0 ? (
                                  <p className="text-gray-500 italic">No professors free at this time.</p>
                                ) : (
                                  slotFreeTeachers.map(t => {
                                    const teacherHours = analytics.teacherWorkloads[t.id] || 0;
                                    const teacherMax = t.maxWeeklyHours || 30;
                                    return (
                                      <div key={t.id} className="flex justify-between bg-white border border-gray-200 rounded px-2 py-1">
                                        <span className="font-medium text-gray-700 truncate max-w-[120px]">{t.name}</span>
                                        <span className={teacherHours >= teacherMax * 0.9 ? 'text-amber-600 font-medium' : 'text-gray-400'}>
                                          {teacherHours.toFixed(0)}/{teacherMax} hrs
                                        </span>
                                      </div>
                                    );
                                  })
                                )}
                              </div>
                            </div>

                            {/* Free Batches */}
                            <div className="border border-gray-100 rounded-lg p-3 bg-gray-50">
                              <h4 className="font-semibold text-gray-800 text-xs mb-2 border-b pb-1">Available Batches ({slotFreeBatches.length})</h4>
                              <div className="max-h-40 overflow-y-auto space-y-1 text-[10px]">
                                {slotFreeBatches.length === 0 ? (
                                  <p className="text-gray-500 italic">No batches free at this time.</p>
                                ) : (
                                  slotFreeBatches.map(b => (
                                    <div key={b.id} className="bg-white border border-gray-200 rounded px-2 py-1 text-center">
                                      <span className="font-medium text-gray-700">{b.name}</span>
                                    </div>
                                  ))
                                )}
                              </div>
                            </div>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="text-center py-6 text-xs text-gray-400 italic">
                        Click on a slot in the Timetable Density Heat Map tab to view live resource lists for that cell!
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </Card>

          {/* Entry count */}
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm text-gray-600">
              {draftEntries.length} {draftEntries.length === 1 ? 'entry' : 'entries'}
            </p>
          </div>

          {/* Timetable Grid */}
          <Card>
            {filteredTimeSlots.length === 0 ? (
              <div className="text-center py-12">
                <p className="text-gray-500">No time slots configured for this division's schedule type. Add time slots first.</p>
              </div>
            ) : (
              <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
              <div className="overflow-x-auto">
                <table className="w-full border-collapse min-w-[900px]">
                  <thead>
                    <tr className="bg-primary-800 text-white">
                      <th className="border border-gray-300 p-3 text-left font-semibold w-28">Time</th>
                      {DAYS.map((day) => (
                        <th key={day} className="border border-gray-300 p-3 text-center font-semibold">
                          {DAY_LABELS[day]}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {filteredTimeSlots.map((slot: any, slotIdx: number) => {
                      // ── Break row: grey banner ──
                      if (slot.isBreak) {
                        return (
                          <tr key={slot.id} className="bg-gray-100">
                            <td className="border border-gray-200 p-2">
                              <div className="flex items-center gap-1 text-gray-400">
                                <FiClock size={14} />
                                <div>
                                  <div className="text-sm">{formatTime(slot.startTime)} - {formatTime(slot.endTime)}</div>
                                  <div className="text-xs">{slot.slotName}</div>
                                </div>
                              </div>
                            </td>
                            <td colSpan={6} className="border border-gray-200 p-2 text-center text-gray-400 italic text-sm">
                              {slot.slotName}
                            </td>
                          </tr>
                        );
                      }

                      // ── Regular slot row ──
                      return (
                      <tr key={slot.id} className="hover:bg-gray-50">
                        <td className="border border-gray-300 p-2 bg-gray-50">
                          <div className="flex items-center gap-1">
                            <FiClock className="text-primary-800" size={14} />
                            <div>
                              <div className="text-sm font-medium">{formatTime(slot.startTime)} - {formatTime(slot.endTime)}</div>
                              <div className="text-xs text-gray-500">{slot.slotName}</div>
                            </div>
                          </div>
                        </td>
{DAYS.map((day) => {
                          const cellKey = `${day}-${slot.id}`;
                          
                          // Skip this cell if it's part of a merged cell from previous row
                          if (mergedCells.has(cellKey)) {
                            return null; // Cell is handled by rowSpan from previous row
                          }

                          const cellEntries = getEntriesForCell(day, slot.id);
                          
                          // Check if any entry in this cell should merge with next slot
                          const mergeEntry = cellEntries.find((e: any) => shouldMergeLabEntry(e, day, slotIdx));
                          const shouldSpan = !!mergeEntry;

                          // Check if there's a theory lecture in this slot
                          const hasTheoryLecture = cellEntries.some((e: any) => e.course?.courseType === 'THEORY');
                          
                          // Check if there are lab entries and get allocated batches
                          const labEntries = cellEntries.filter((e: any) => e.course?.courseType === 'LAB');
                          const allocatedBatchIds = labEntries.map((e: any) => e.batch?.id).filter(Boolean);
                          const allBatchesAllocated = batches.length > 0 && allocatedBatchIds.length >= batches.length;
                          
                          // Determine if add button should be shown
                          const showAddButton = !hasTheoryLecture && !allBatchesAllocated;

                          return (
                            <DroppableCell key={cellKey} day={day} slotId={slot.id} rowSpan={shouldSpan ? 2 : 1}>
                              {cellEntries.length > 0 ? (
                                <div className="space-y-1">
                                  {cellEntries.map((entry: any) => (
                                    <DraggableEntryCard
                                      key={entry.id}
                                      entry={entry}
                                      onEdit={openEditModal}
                                      onDelete={handleDeleteEntry}
                                      onDeleteLabGroup={handleDeleteLabGroup}
                                    />
                                  ))}
                                  {showAddButton && (
                                    <button
                                      onClick={() => openAddModal(day, slot.id)}
                                      className="w-full p-1 text-xs text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded transition-colors"
                                    >
                                      + Add
                                    </button>
                                  )}
                                </div>
                              ) : (
                                showAddButton && (
                                  <button
                                    onClick={() => openAddModal(day, slot.id)}
                                    className="w-full h-16 flex items-center justify-center text-gray-300 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all"
                                    title="Add entry"
                                  >
                                    <FiPlus size={20} />
                                  </button>
                                )
                              )}
                            </DroppableCell>
                          );
                        }).filter(Boolean)}
                      </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {/* Drag overlay — shows floating card while dragging */}
              <DragOverlay>
                {activeDragEntry ? (
                  <div className="p-2 rounded-lg text-xs bg-white border-2 border-primary-500 shadow-lg opacity-90 w-32">
                    <p className="font-semibold text-primary-900 truncate">{activeDragEntry.course?.name}</p>
                    <p className="text-gray-600 truncate">{activeDragEntry.teacher?.name}</p>
                    <p className="text-gray-500 truncate">{activeDragEntry.room?.roomNumber}</p>
                  </div>
                ) : null}
              </DragOverlay>
              </DndContext>
            )}
          </Card>
        </>
      )}

      {/* Add / Edit Modal */}
      {showAddModal && selectedDivisionId && selectedYearId && (
        <EntryFormModal
          isOpen={showAddModal}
          onClose={() => { setShowAddModal(false); setEditingEntry(null); setConflicts([]); }}
          editingEntry={editingEntry}
          divisionId={selectedDivisionId}
          academicYearId={selectedYearId}
          courses={courses}
          teachers={teachers}
          rooms={rooms}
          timeSlots={filteredTimeSlots}
          batches={batches}
          prefillDay={prefillDay}
          prefillSlotId={prefillSlotId}
          defaultSemester={selectedSemester}
          conflicts={conflicts}
          setConflicts={setConflicts}
          onSaved={fetchDraft}
          formatTime={formatTime}
          analytics={analytics}
          draftEntries={draftEntries}
        />
      )}

      {/* Pre-Publish Validation Modal */}
      {showValidation && validationResult && (
        <Modal isOpen={showValidation} onClose={() => { setShowValidation(false); setValidationResult(null); }} title="Pre-Publish Validation">
          <div className="space-y-4">
            {/* Summary */}
            <div className="grid grid-cols-3 gap-3 text-center">
              <div className="bg-blue-50 rounded-lg p-3">
                <p className="text-2xl font-bold text-blue-700">{validationResult.totalEntries || 0}</p>
                <p className="text-xs text-blue-600">Entries</p>
              </div>
              <div className="bg-green-50 rounded-lg p-3">
                <p className="text-2xl font-bold text-green-700">{validationResult.totalTeachers || 0}</p>
                <p className="text-xs text-green-600">Teachers</p>
              </div>
              <div className="bg-purple-50 rounded-lg p-3">
                <p className="text-2xl font-bold text-purple-700">{validationResult.daysScheduled || 0}/6</p>
                <p className="text-xs text-purple-600">Days</p>
              </div>
            </div>

            {/* Errors (blocking) */}
            {validationResult.errors?.length > 0 && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-3">
                <p className="font-semibold text-red-800 mb-2"><FiAlertTriangle className="inline mr-1" /> Errors — Must fix before publishing</p>
                {validationResult.errors.map((err: any, i: number) => (
                  <p key={i} className="text-sm text-red-700 mb-1">- {err.message}</p>
                ))}
              </div>
            )}

            {/* Warnings (informational) */}
            {validationResult.warnings?.length > 0 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3">
                <p className="font-semibold text-yellow-800 mb-2">Warnings — Review before publishing</p>
                {validationResult.warnings.map((warn: any, i: number) => (
                  <p key={i} className="text-sm text-yellow-700 mb-1">- {warn.message}</p>
                ))}
              </div>
            )}

            {/* No issues */}
            {(!validationResult.errors || validationResult.errors.length === 0) &&
             (!validationResult.warnings || validationResult.warnings.length === 0) && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-center">
                <p className="text-green-800 font-semibold">All checks passed! Timetable is ready to publish.</p>
              </div>
            )}

            {/* Actions */}
            <div className="flex gap-3 pt-2">
              <Button variant="outline" onClick={() => { setShowValidation(false); setValidationResult(null); }} className="flex-1">
                Go Back & Fix
              </Button>
              <Button
                variant="primary"
                onClick={() => confirmPublish(false)}
                isLoading={isActionLoading}
                disabled={validationResult.errors?.length > 0}
                className="flex-1"
              >
                <FiSend className="mr-1" /> Publish Now
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};

// ─── Custom Searchable Select Component ───────────────────────────────
interface SearchableSelectProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string; subLabel?: string; badge?: string }[];
  placeholder: string;
  required?: boolean;
  disabled?: boolean;
  loading?: boolean;
  emptyMessage?: string;
}

const SearchableSelect: React.FC<SearchableSelectProps> = ({
  label, value, onChange, options, placeholder, required = false, disabled = false, loading = false, emptyMessage = 'No options found'
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [search, setSearch] = useState('');
  const containerRef = React.useRef<HTMLDivElement>(null);

  // Close dropdown on click outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const filteredOptions = useMemo(() => {
    if (!search) return options;
    const s = search.toLowerCase();
    return options.filter(opt =>
      opt.label.toLowerCase().includes(s) ||
      (opt.subLabel && opt.subLabel.toLowerCase().includes(s))
    );
  }, [search, options]);

  const selectedOption = useMemo(() => {
    return options.find(opt => opt.value === value);
  }, [value, options]);

  // Reset search when opening/closing
  useEffect(() => {
    if (isOpen) setSearch('');
  }, [isOpen]);

  return (
    <div className="relative" ref={containerRef}>
      <label className="block text-sm font-medium text-gray-700 mb-1">
        {label} {required && <span className="text-red-500">*</span>}
        {loading && <span className="text-xs text-gray-400 ml-2 animate-pulse">(Loading...)</span>}
      </label>
      
      <div
        onClick={() => !disabled && !loading && setIsOpen(!isOpen)}
        className={`flex items-center justify-between w-full border rounded-lg px-3 py-2 text-sm bg-white shadow-sm cursor-pointer transition-all ${
          disabled || loading
            ? 'bg-gray-50 border-gray-200 text-gray-400 cursor-not-allowed'
            : 'border-gray-300 hover:border-primary-500 focus-within:ring-2 focus-within:ring-primary-500'
        }`}
      >
        <span className={selectedOption ? 'text-gray-900 font-medium' : 'text-gray-400'}>
          {selectedOption ? selectedOption.label : placeholder}
        </span>
        <svg
          className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'transform rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {isOpen && (
        <div className="absolute z-[100] mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-xl overflow-hidden animate-in fade-in slide-in-from-top-1 duration-100">
          <div className="p-2 border-b border-gray-100 bg-gray-50">
            <input
              type="text"
              autoFocus
              className="w-full border border-gray-300 rounded-md px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder="Search..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onClick={(e) => e.stopPropagation()}
            />
          </div>
          <ul className="max-h-60 overflow-y-auto py-1">
            {filteredOptions.length === 0 ? (
              <li className="px-4 py-3 text-xs text-gray-500 text-center italic">{emptyMessage}</li>
            ) : (
              filteredOptions.map((opt) => {
                const isSelected = opt.value === value;
                return (
                  <li
                    key={opt.value}
                    onClick={() => {
                      onChange(opt.value);
                      setIsOpen(false);
                    }}
                    className={`flex flex-col px-4 py-2 text-sm cursor-pointer transition-colors ${
                      isSelected
                        ? 'bg-primary-50 text-primary-900 font-semibold'
                        : 'hover:bg-gray-50 text-gray-700'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span>{opt.label}</span>
                      {opt.badge && (
                        <span className="text-[10px] font-semibold bg-primary-100 text-primary-800 px-1.5 py-0.5 rounded-full">
                          {opt.badge}
                        </span>
                      )}
                    </div>
                    {opt.subLabel && (
                      <span className={`text-[10px] mt-0.5 ${isSelected ? 'text-primary-700' : 'text-gray-400'}`}>
                        {opt.subLabel}
                      </span>
                    )}
                  </li>
                );
              })
            )}
          </ul>
        </div>
      )}
    </div>
  );
};

// ─── Entry Form Modal ─────────────────────────────────────────────────
interface EntryFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  editingEntry: any;
  divisionId: number;
  academicYearId: number;
  courses: any[];
  teachers: any[];
  rooms: any[];
  timeSlots: any[];
  batches: any[];
  prefillDay: string | null;
  prefillSlotId: number | null;
  defaultSemester?: string;
  conflicts: string[];
  setConflicts: (c: string[]) => void;
  onSaved: () => void;
  formatTime: (t: string) => string;
  analytics: any;
  draftEntries: any[];
}

const EntryFormModal: React.FC<EntryFormModalProps> = ({
  isOpen, onClose, editingEntry, divisionId, academicYearId,
  courses, teachers, rooms, timeSlots, batches,
  prefillDay, prefillSlotId, defaultSemester, conflicts, setConflicts, onSaved,
  formatTime, analytics, draftEntries,
}) => {
  const [isSaving, setIsSaving] = useState(false);
  const [form, setForm] = useState({
    courseId: '',
    teacherId: '',
    roomId: '',
    timeSlotId: '',
    dayOfWeek: '',
    batchId: '',
    semester: '',
    notes: '',
  });

  // Phase 3: Availability filtering state
  const [availableRooms, setAvailableRooms] = useState<any[]>([]);
  const [availableTeachers, setAvailableTeachers] = useState<any[]>([]);
  const [isLoadingAvailability, setIsLoadingAvailability] = useState(false);

  // Phase 4: Credit-based course selection state
  const [availableCourses, setAvailableCourses] = useState<any[]>([]);
  const [isLoadingCourses, setIsLoadingCourses] = useState(false);

  // Batch filtering for lab courses
  const [availableBatches, setAvailableBatches] = useState<any[]>([]);
  const [isLoadingBatches, setIsLoadingBatches] = useState(false);

  useEffect(() => {
    if (editingEntry) {
      setForm({
        courseId: editingEntry.course?.id?.toString() || '',
        teacherId: editingEntry.teacher?.id?.toString() || '',
        roomId: editingEntry.room?.id?.toString() || '',
        timeSlotId: editingEntry.timeSlot?.id?.toString() || '',
        dayOfWeek: editingEntry.dayOfWeek || '',
        batchId: editingEntry.batch?.id?.toString() || '',
        semester: editingEntry.semester || defaultSemester || '',
        notes: editingEntry.notes || '',
      });
    } else {
      setForm({
        courseId: '',
        teacherId: '',
        roomId: '',
        timeSlotId: prefillSlotId?.toString() || '',
        dayOfWeek: prefillDay || '',
        batchId: '',
        semester: defaultSemester || '',
        notes: '',
      });
    }
  }, [editingEntry, prefillDay, prefillSlotId, defaultSemester]);

  // Phase 4: Load available courses with credit instances when semester changes
  useEffect(() => {
    if (!isOpen || editingEntry) return; // Only for new entries
    
    const loadAvailableCourses = async () => {
      try {
        setIsLoadingCourses(true);
        const res = await courseAPI.getAvailableWithCredits(
          divisionId,
          academicYearId,
          form.semester || undefined
        );
        setAvailableCourses(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error('Failed to load available courses:', err);
        setAvailableCourses(courses); // Fallback to all courses
      } finally {
        setIsLoadingCourses(false);
      }
    };

    loadAvailableCourses();
  }, [isOpen, divisionId, academicYearId, form.semester, editingEntry, courses]);

  // Unified reactive real-time availability refresh
  useEffect(() => {
    if (!form.dayOfWeek || !form.timeSlotId || !form.semester) {
      setAvailableRooms(rooms);
      setAvailableTeachers(teachers);
      setAvailableBatches(batches);
      return;
    }

    const loadAvailability = async () => {
      try {
        setIsLoadingAvailability(true);
        
        // Fetch busy/available resources from backend
        const [roomsRes, teachersRes, batchesRes] = await Promise.all([
          timetableAPI.getAvailableRooms(
            form.dayOfWeek,
            Number(form.timeSlotId),
            academicYearId,
            form.semester,
            divisionId,
            form.courseId ? Number(form.courseId) : undefined,
            form.batchId ? Number(form.batchId) : undefined
          ),
          timetableAPI.getAvailableTeachers(
            form.dayOfWeek,
            Number(form.timeSlotId),
            academicYearId,
            form.semester,
            form.courseId ? Number(form.courseId) : undefined
          ),
          timetableAPI.getAvailableBatches(
            divisionId,
            form.dayOfWeek,
            Number(form.timeSlotId),
            academicYearId,
            form.semester
          ),
        ]);

        const freeRooms = Array.isArray(roomsRes.data) ? roomsRes.data : [];
        const freeTeachers = Array.isArray(teachersRes.data) ? teachersRes.data : [];
        const freeBatches = Array.isArray(batchesRes.data) ? batchesRes.data : [];

        // If editing, make sure current selections are included in the available options
        if (editingEntry) {
          if (editingEntry.room?.id && !freeRooms.find((r: any) => r.id === editingEntry.room.id)) {
            const currentRoom = rooms.find((r: any) => r.id === editingEntry.room.id);
            if (currentRoom) freeRooms.unshift(currentRoom);
          }
          if (editingEntry.teacher?.id && !freeTeachers.find((t: any) => t.id === editingEntry.teacher.id)) {
            const currentTeacher = teachers.find((t: any) => t.id === editingEntry.teacher.id);
            if (currentTeacher) freeTeachers.unshift(currentTeacher);
          }
        }

        setAvailableRooms(freeRooms);
        setAvailableTeachers(freeTeachers);

        // Handle batch selection intersection for LAB courses
        if (form.courseId) {
          const selectedCourse = editingEntry
            ? courses.find((c: any) => c.id === Number(form.courseId))
            : availableCourses.find((c: any) => c.id === Number(form.courseId));

          if (selectedCourse?.courseType === 'LAB') {
            setIsLoadingBatches(true);
            try {
              // 1. Get batches that still need this lab course allocated
              const courseBatchesRes = await courseAPI.getAvailableBatchesForCourse(
                Number(form.courseId),
                divisionId,
                academicYearId
              );
              const courseBatches = Array.isArray(courseBatchesRes.data) ? courseBatchesRes.data : [];

              // 2. Intersect: batch must be free in the slot AND need the course
              let intersected = courseBatches.filter((cb: any) =>
                freeBatches.some((fb: any) => fb.id === cb.id)
              );

              // 3. If editing, preserve the current batch choice
              if (editingEntry && editingEntry.batch?.id) {
                const currentBatch = batches.find((b: any) => b.id === editingEntry.batch.id);
                if (currentBatch && !intersected.find((b: any) => b.id === currentBatch.id)) {
                  intersected.unshift(currentBatch);
                }
              }

              setAvailableBatches(intersected);
            } catch (err) {
              console.error('Failed to load course-specific batches:', err);
              setAvailableBatches(freeBatches); // fallback to free batches
            } finally {
              setIsLoadingBatches(false);
            }
          } else {
            setAvailableBatches(freeBatches);
          }
        } else {
          setAvailableBatches(freeBatches);
        }

      } catch (err) {
        console.error('Failed to load availability:', err);
        setAvailableRooms(rooms);
        setAvailableTeachers(teachers);
        setAvailableBatches(batches);
      } finally {
        setIsLoadingAvailability(false);
      }
    };

    loadAvailability();
  }, [
    form.dayOfWeek,
    form.timeSlotId,
    form.semester,
    form.courseId,
    form.batchId,
    academicYearId,
    divisionId,
    rooms,
    teachers,
    batches,
    editingEntry,
    courses,
    availableCourses
  ]);

  const handleChange = (field: string, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setConflicts([]);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!form.courseId || !form.teacherId || !form.roomId || !form.timeSlotId || !form.dayOfWeek) {
      toast.error('Please fill in all required fields');
      return;
    }

    // Validate batch is selected for lab courses
    if (isLabCourse && !form.batchId) {
      toast.error('Please select a batch for lab courses');
      return;
    }

    const payload: any = {
      divisionId,
      academicYearId,
      courseId: Number(form.courseId),
      teacherId: Number(form.teacherId),
      roomId: Number(form.roomId),
      timeSlotId: Number(form.timeSlotId),
      dayOfWeek: form.dayOfWeek,
    };
    
    // Only add optional fields if they have valid values
    if (form.batchId && String(form.batchId).trim() !== '') {
      payload.batchId = Number(form.batchId);
    }
    if (form.semester && String(form.semester).trim() !== '') {
      payload.semester = form.semester;
    }
    if (form.notes && String(form.notes).trim() !== '') {
      payload.notes = form.notes;
    }
    
    console.log('DEBUG Frontend: Submitting payload:', payload);

    try {
      setIsSaving(true);
      setConflicts([]);

      if (editingEntry) {
        await timetableAPI.updateEntry(editingEntry.id, payload);
        toast.success('Entry updated');
      } else {
        await timetableAPI.addEntry(payload);
        toast.success('Entry added');
      }
      onSaved();
      onClose();
    } catch (err: any) {
      if (err.response?.status === 409) {
        // Conflict detection response
        const data = err.response.data;
        const conflictList: string[] = data?.conflicts || [];
        if (conflictList.length > 0) {
          setConflicts(conflictList);
        } else {
          toast.error(data?.message || 'Scheduling conflict detected');
        }
      } else {
        toast.error(getErrorMessage(err));
      }
    } finally {
      setIsSaving(false);
    }
  };

  const selectedCourse = editingEntry 
    ? courses.find((c: any) => c.id === Number(form.courseId))
    : availableCourses.find((c: any) => c.id === Number(form.courseId));
  const isLabCourse = selectedCourse?.courseType === 'LAB';

  // Display courses list - use credit-based for new entries, all courses for editing
  const displayCourses = editingEntry ? courses : availableCourses;

  // Options mapping for SearchableSelects
  const courseOptions = useMemo(() => {
    return displayCourses.map((c: any) => ({
      value: c.id.toString(),
      label: `${c.displayName || c.name} ${c.courseType === 'LAB' ? '(Lab)' : ''}`,
      subLabel: c.code ? `Code: ${c.code}` : undefined,
      badge: c.courseType === 'LAB' ? 'LAB' : 'THEORY',
    }));
  }, [displayCourses]);

  const teacherOptions = useMemo(() => {
    return availableTeachers.map((t: any) => {
      const workloadHours = analytics?.teacherWorkloads?.[t.id] || 0;
      const maxHours = t.maxWeeklyHours || 30;
      const isNearLimit = workloadHours >= maxHours * 0.9;
      return {
        value: t.id.toString(),
        label: t.name,
        subLabel: `Dept: ${t.department?.name || 'N/A'} | Workload: ${workloadHours.toFixed(1)} / ${maxHours} hrs`,
        badge: isNearLimit ? 'NEAR LIMIT' : undefined,
      };
    });
  }, [availableTeachers, analytics]);

  const roomOptions = useMemo(() => {
    return availableRooms.map((r: any) => {
      const bookingCount = analytics?.roomBookingCounts?.[r.id] || 0;
      return {
        value: r.id.toString(),
        label: `${r.roomNumber} ${r.name ? `- ${r.name}` : ''}`,
        subLabel: `Type: ${r.roomType || 'classroom'} | Bookings: ${bookingCount} slots`,
        badge: r.roomType,
      };
    });
  }, [availableRooms, analytics]);

  const batchOptions = useMemo(() => {
    return availableBatches.map((b: any) => ({
      value: b.id.toString(),
      label: b.name,
      subLabel: b.strength ? `Strength: ${b.strength}` : undefined,
    }));
  }, [availableBatches]);

  const conflictSuggestions = useMemo(() => {
    if (conflicts.length === 0 || !form.teacherId || !form.roomId || !form.courseId) return [];
    const tId = Number(form.teacherId);
    const rId = Number(form.roomId);
    const suggestions: any[] = [];
    
    timeSlots.forEach((slot: any) => {
      if (slot.isBreak) return;
      DAYS.forEach((day) => {
        if (day === form.dayOfWeek && slot.id.toString() === form.timeSlotId) return;
        const isTBusy = draftEntries.some(e => e.teacher?.id === tId && e.dayOfWeek === day && e.timeSlot?.id === slot.id && (!editingEntry || e.id !== editingEntry.id));
        const isRBusy = draftEntries.some(e => e.room?.id === rId && e.dayOfWeek === day && e.timeSlot?.id === slot.id && (!editingEntry || e.id !== editingEntry.id));
        const isDBusy = draftEntries.some(e => e.division?.id === divisionId && e.dayOfWeek === day && e.timeSlot?.id === slot.id && (!editingEntry || e.id !== editingEntry.id));
        if (!isTBusy && !isRBusy && !isDBusy) {
          suggestions.push({
            day,
            slotId: slot.id,
            label: `${DAY_LABELS[day]} - ${slot.slotName} (${formatTime(slot.startTime)} - ${formatTime(slot.endTime)})`
          });
        }
      });
    });
    return suggestions.slice(0, 3);
  }, [conflicts, form.teacherId, form.roomId, form.courseId, form.dayOfWeek, form.timeSlotId, timeSlots, draftEntries, editingEntry, divisionId, formatTime]);

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={editingEntry ? 'Edit Timetable Entry' : 'Add Timetable Entry'}
      size="lg"
    >
      <form onSubmit={handleSubmit} className="space-y-4">
        {/* Conflict warnings */}
        {conflicts.length > 0 && (
          <div className="bg-red-50 border border-red-300 rounded-lg p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <FiAlertTriangle className="text-red-600" />
              <span className="font-semibold text-red-800">Scheduling Conflicts Detected</span>
            </div>
            <ul className="text-sm text-red-700 space-y-1 mb-3">
              {conflicts.map((c, i) => (
                <li key={i}>- {c}</li>
              ))}
            </ul>
            
            {/* Suggested alternative free slots */}
            {conflictSuggestions.length > 0 && (
              <div className="text-xs bg-white border border-red-250 rounded-lg p-3 text-gray-900 shadow-sm mt-3">
                <span className="font-semibold flex items-center gap-1.5 text-primary-800 mb-2">
                  <span className="inline-block w-2.5 h-2.5 rounded-full bg-primary-500 animate-pulse"></span>
                  Recommended Free Slots:
                </span>
                <div className="space-y-2">
                  {conflictSuggestions.map((s, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => {
                        handleChange('dayOfWeek', s.day);
                        handleChange('timeSlotId', s.slotId.toString());
                        setConflicts([]);
                      }}
                      className="w-full flex items-center justify-between text-left text-xs bg-primary-50 hover:bg-primary-100 border border-primary-200 hover:border-primary-300 rounded-md px-3 py-2 text-primary-700 transition-all font-semibold"
                    >
                      <span>{s.label}</span>
                      <span className="text-[10px] bg-primary-600 text-white px-2 py-0.5 rounded font-bold uppercase tracking-wider">Apply</span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Day */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Day <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              value={form.dayOfWeek}
              onChange={(e) => handleChange('dayOfWeek', e.target.value)}
              disabled={true}
              required
            >
              <option value="">Select Day</option>
              {DAYS.map((d) => (
                <option key={d} value={d}>{DAY_LABELS[d]}</option>
              ))}
            </select>
          </div>

          {/* Time Slot */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Time Slot <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
              value={form.timeSlotId}
              onChange={(e) => handleChange('timeSlotId', e.target.value)}
              disabled={true}
              required
            >
              <option value="">Select Time Slot</option>
              {timeSlots.filter((s: any) => !s.isBreak).map((s: any) => (
                <option key={s.id} value={s.id}>
                  {s.slotName} ({formatTime(s.startTime)} - {formatTime(s.endTime)})
                </option>
              ))}
            </select>
          </div>

          {/* Course - Searchable Selection */}
          <SearchableSelect
            label="Course"
            value={form.courseId}
            onChange={(val) => handleChange('courseId', val)}
            options={courseOptions}
            placeholder="Select Course"
            required
            loading={isLoadingCourses}
            emptyMessage="No available courses found with pending credits."
          />

          {/* Teacher - Searchable Selection */}
          <SearchableSelect
            label="Teacher"
            value={form.teacherId}
            onChange={(val) => handleChange('teacherId', val)}
            options={teacherOptions}
            placeholder="Select Teacher"
            required
            loading={isLoadingAvailability}
            emptyMessage="No teachers available. All are occupied or over limit."
          />

          {/* Room - Searchable Selection */}
          <SearchableSelect
            label="Room"
            value={form.roomId}
            onChange={(val) => handleChange('roomId', val)}
            options={roomOptions}
            placeholder="Select Room"
            required
            loading={isLoadingAvailability}
            emptyMessage="No rooms available. All are occupied or wrong type."
          />

          {/* Batch - Hidden for theory, searchable for lab */}
          {isLabCourse ? (
            <SearchableSelect
              label="Batch"
              value={form.batchId}
              onChange={(val) => handleChange('batchId', val)}
              options={batchOptions}
              placeholder="Select Batch"
              required
              loading={isLoadingBatches}
              emptyMessage="No batches available for this lab slot."
            />
          ) : (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Batch
              </label>
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 bg-gray-100 cursor-not-allowed text-gray-500 font-medium"
                value=""
                disabled
              >
                <option value="">Full Division</option>
              </select>
              <p className="text-xs text-gray-500 mt-1">
                Theory courses are scheduled for the full division
              </p>
            </div>
          )}
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Notes (optional)</label>
          <textarea
            className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            rows={2}
            value={form.notes}
            onChange={(e) => handleChange('notes', e.target.value)}
            placeholder="Any additional notes..."
          />
        </div>

        {/* Actions */}
        <div className="flex justify-end gap-3 pt-2">
          <Button variant="outline" type="button" onClick={onClose}>Cancel</Button>
          <Button variant="primary" type="submit" isLoading={isSaving}>
            {editingEntry ? (
              <><FiEdit2 className="mr-1" /> Update Entry</>
            ) : (
              <><FiPlus className="mr-1" /> Add Entry</>
            )}
          </Button>
        </div>
      </form>
    </Modal>
  );
};
