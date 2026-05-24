import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
  FiPlus, FiTrash2, FiSend, FiArchive, FiAlertTriangle,
  FiClock, FiBook, FiMapPin, FiEdit2, FiRefreshCw, FiDownload, FiMove,
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
  courseAPI, teacherAdminAPI, roomAPI, timeSlotAPI, batchAPI,
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
      <p className={`font-semibold truncate ${isLab ? 'text-purple-900' : 'text-blue-900'}`}>
        {entry.course?.name || 'Unknown'}
      </p>
      <div className="flex items-center gap-1 text-gray-600 mt-0.5">
        <FiMapPin size={10} />
        <span className="truncate">{entry.room?.roomNumber || '-'}</span>
      </div>
      <div className="flex items-center gap-1 text-gray-600">
        <span className="truncate">{entry.teacher?.name || '-'}</span>
      </div>
      {entry.batch && (
        <span className="inline-block mt-0.5 px-1.5 py-0.5 bg-purple-200 text-purple-800 rounded text-[10px]">
          {entry.batch?.name || 'Batch'}
        </span>
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
  const [divisions, setDivisions] = useState<any[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
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

  // Semester filter
  const [selectedSemester, setSelectedSemester] = useState<string>('');

  // Filter time slots by the selected division's slot type (TYPE_1, TYPE_2, etc.)
  // This prevents showing duplicate slots when multiple types exist in the database.
  const filteredTimeSlots = useMemo(() => {
    const selectedDiv = divisions.find((d: any) => d.id === selectedDivisionId);
    const slotType = selectedDiv?.timeSlotType || 'TYPE_1';
    return timeSlots.filter((s: any) => !s.type || s.type === slotType);
  }, [timeSlots, divisions, selectedDivisionId]);

  // Lab session wizard
  const [showLabWizard, setShowLabWizard] = useState(false);

  // Pre-filled day/slot when clicking a cell
  const [prefillDay, setPrefillDay] = useState<string | null>(null);
  const [prefillSlotId, setPrefillSlotId] = useState<number | null>(null);

  // ─── Initial load ───────────────────────────────────────────────────
  useEffect(() => {
    const loadInitial = async () => {
      try {
        const [yearsRes, slotsRes, coursesRes, teachersRes, roomsRes] = await Promise.all([
          academicYearAPI.getAll(),
          timeSlotAPI.getAll(),
          courseAPI.getAll(),
          teacherAdminAPI.getAll(),
          roomAPI.getAll(),
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

  // Load divisions when year changes
  useEffect(() => {
    if (!selectedYearId) {
      setDivisions([]);
      setSelectedDivisionId(null);
      return;
    }
    const loadDivisions = async () => {
      try {
        const res = await divisionAPI.getByAcademicYear(selectedYearId);
        const divs = Array.isArray(res.data) ? res.data : [];
        setDivisions(divs);
        setSelectedDivisionId(null);
        setDraftEntries([]);
      } catch {
        setDivisions([]);
      }
    };
    loadDivisions();
  }, [selectedYearId]);

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
    if (!selectedDivisionId || !selectedYearId) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.getDraft(selectedDivisionId, selectedYearId);
      setDraftEntries(Array.isArray(res.data) ? res.data : []);
    } catch {
      setDraftEntries([]);
    } finally {
      setIsActionLoading(false);
    }
  }, [selectedDivisionId, selectedYearId]);

  useEffect(() => {
    fetchDraft();
  }, [fetchDraft]);

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

  // ─── Copy from another division ──────────────────────────────────
  const [showCopyModal, setShowCopyModal] = useState(false);
  const [copySourceId, setCopySourceId] = useState<string>('');

  const handleCopyFromDivision = async () => {
    if (!copySourceId || !selectedDivisionId || !selectedYearId) return;
    if (!window.confirm('Copy all draft entries from the selected division? Existing entries in this division will NOT be removed.')) return;

    try {
      setIsActionLoading(true);
      const res = await timetableAPI.copyFromDivision(Number(copySourceId), selectedDivisionId, selectedYearId);
      toast.success(`Copied ${(res.data as any).entriesCopied} entries successfully`);
      setShowCopyModal(false);
      setCopySourceId('');
      fetchDraft();
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    } finally {
      setIsActionLoading(false);
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
    if (!selectedDivisionId || !selectedYearId) return;
    try {
      const res = await timetableAPI.exportDivisionPDF(selectedDivisionId, selectedYearId);
      triggerDownload(res.data, `timetable_division_${selectedDivisionId}.pdf`);
      toast.success('PDF downloaded!');
    } catch { toast.error('Failed to export PDF'); }
  };

  const handleExportExcel = async () => {
    if (!selectedDivisionId || !selectedYearId) return;
    try {
      const res = await timetableAPI.exportDivisionExcel(selectedDivisionId, selectedYearId);
      triggerDownload(res.data, `timetable_division_${selectedDivisionId}.xlsx`);
      toast.success('Excel downloaded!');
    } catch { toast.error('Failed to export Excel'); }
  };

  // ─── Validation + Publish ────────────────────────────────────────
  const [showValidation, setShowValidation] = useState(false);
  const [validationResult, setValidationResult] = useState<any>(null);

  const handlePublish = async () => {
    if (!selectedDivisionId || !selectedYearId) return;
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
    if (!selectedDivisionId || !selectedYearId) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.publish(selectedDivisionId, selectedYearId, force);
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
    if (!selectedDivisionId || !selectedYearId) return;
    if (!window.confirm('Archive the currently published timetable for this division?')) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.archive(selectedDivisionId, selectedYearId);
      toast.success(res.data.message || 'Timetable archived');
    } catch (err: any) {
      toast.error(getErrorMessage(err));
    } finally {
      setIsActionLoading(false);
    }
  };

  const handleClearDraft = async () => {
    if (!selectedDivisionId || !selectedYearId) return;
    if (!window.confirm('Clear ALL draft entries for this division? This cannot be undone.')) return;
    try {
      setIsActionLoading(true);
      const res = await timetableAPI.clearDraft(selectedDivisionId, selectedYearId);
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
        {selectedDivisionId && selectedYearId && (
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={fetchDraft} isLoading={isActionLoading}>
              <FiRefreshCw className="mr-1" /> Refresh
            </Button>
            <Button variant="outline" onClick={() => setShowCopyModal(true)}>
              <FiBook className="mr-1" /> Copy From Division
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
            <Button variant="primary" onClick={() => setShowLabWizard(true)}>
              <FiPlus className="mr-1" /> Lab Session
            </Button>
            <Button variant="primary" onClick={handlePublish} isLoading={isActionLoading}>
              <FiSend className="mr-1" /> Publish
            </Button>
          </div>
        )}
      </div>

      {/* Selectors */}
      <Card className="mb-6">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Academic Year</label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedYearId || ''}
              onChange={(e) => setSelectedYearId(e.target.value ? Number(e.target.value) : null)}
            >
              <option value="">Select Academic Year</option>
              {academicYears.map((y: any) => (
                <option key={y.id} value={y.id}>
                  {y.name || y.yearRange} {y.isCurrent ? '(Current)' : ''}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Division</label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedDivisionId || ''}
              onChange={(e) => setSelectedDivisionId(e.target.value ? Number(e.target.value) : null)}
              disabled={!selectedYearId}
            >
              <option value="">Select Division</option>
              {divisions.map((d: any) => (
                <option key={d.id} value={d.id}>
                  {d.name} — Year {d.year} {d.department?.name ? `(${d.department.name})` : ''}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Semester</label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={selectedSemester}
              onChange={(e) => setSelectedSemester(e.target.value)}
              disabled={!selectedDivisionId}
            >
              <option value="">All Semesters</option>
              <option value="SEM_1">Semester 1</option>
              <option value="SEM_2">Semester 2</option>
              <option value="SEM_3">Semester 3</option>
              <option value="SEM_4">Semester 4</option>
              <option value="SEM_5">Semester 5</option>
              <option value="SEM_6">Semester 6</option>
              <option value="SEM_7">Semester 7</option>
              <option value="SEM_8">Semester 8</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Grid or placeholder */}
      {!selectedDivisionId || !selectedYearId ? (
        <Card>
          <div className="text-center py-16">
            <FiBook className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p className="text-gray-500 text-lg">Select an academic year and division to start building the timetable</p>
          </div>
        </Card>
      ) : (
        <>
          {/* Entry count */}
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm text-gray-600">
              {draftEntries.length} draft {draftEntries.length === 1 ? 'entry' : 'entries'}
            </p>
            <Button variant="primary" onClick={() => openAddModal()}>
              <FiPlus className="mr-1" /> Add Entry
            </Button>
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
                                  <button
                                    onClick={() => openAddModal(day, slot.id)}
                                    className="w-full p-1 text-xs text-gray-400 hover:text-primary-600 hover:bg-gray-100 rounded transition-colors"
                                  >
                                    + Add
                                  </button>
                                </div>
                              ) : (
                                <button
                                  onClick={() => openAddModal(day, slot.id)}
                                  className="w-full h-16 flex items-center justify-center text-gray-300 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all"
                                  title="Add entry"
                                >
                                  <FiPlus size={20} />
                                </button>
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
          timeSlots={timeSlots}
          batches={batches}
          prefillDay={prefillDay}
          prefillSlotId={prefillSlotId}
          defaultSemester={selectedSemester}
          conflicts={conflicts}
          setConflicts={setConflicts}
          onSaved={fetchDraft}
        />
      )}

      {/* Copy From Division Modal */}
      {showCopyModal && (
        <Modal isOpen={showCopyModal} onClose={() => { setShowCopyModal(false); setCopySourceId(''); }} title="Copy Timetable From Another Division">
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Copy all draft entries (excluding lab sessions) from a source division into the currently selected division.
            </p>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Source Division</label>
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2"
                value={copySourceId}
                onChange={(e) => setCopySourceId(e.target.value)}
              >
                <option value="">Select source division...</option>
                {divisions
                  .filter((d: any) => d.id !== selectedDivisionId)
                  .map((d: any) => (
                    <option key={d.id} value={d.id}>
                      {d.name} — Year {d.year} {d.department?.name ? `(${d.department.name})` : ''}
                    </option>
                  ))}
              </select>
            </div>
            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-3 text-sm text-yellow-800">
              <strong>Note:</strong> Lab session entries are NOT copied. You will need to set up lab sessions separately for this division.
            </div>
            <div className="flex gap-3 pt-2">
              <Button variant="outline" onClick={() => { setShowCopyModal(false); setCopySourceId(''); }} className="flex-1">
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleCopyFromDivision}
                isLoading={isActionLoading}
                disabled={!copySourceId}
                className="flex-1"
              >
                Copy Entries
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Lab Session Wizard Modal */}
      {showLabWizard && selectedDivisionId && selectedYearId && (
        <LabSessionWizard
          isOpen={showLabWizard}
          onClose={() => setShowLabWizard(false)}
          divisionId={selectedDivisionId}
          academicYearId={selectedYearId}
          courses={courses.filter((c: any) => c.courseType === 'LAB')}
          teachers={teachers}
          rooms={rooms}
          timeSlots={timeSlots}
          batches={batches}
          defaultSemester={selectedSemester}
          onSaved={fetchDraft}
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

// ─── Lab Session Wizard ──────────────────────────────────────────────

interface LabWizardProps {
  isOpen: boolean;
  onClose: () => void;
  divisionId: number;
  academicYearId: number;
  courses: any[];
  teachers: any[];
  rooms: any[];
  timeSlots: any[];
  batches: any[];
  defaultSemester?: string;
  onSaved: () => void;
}

const LabSessionWizard: React.FC<LabWizardProps> = ({
  isOpen, onClose, divisionId, academicYearId,
  courses, teachers, rooms, timeSlots, batches,
  defaultSemester, onSaved,
}) => {
  const [isSaving, setIsSaving] = useState(false);
  const [conflicts, setConflicts] = useState<string[]>([]);

  const [courseId, setCourseId] = useState('');
  const [dayOfWeek, setDayOfWeek] = useState('');
  const [timeSlotId, setTimeSlotId] = useState('');
  const [semester, setSemester] = useState(defaultSemester || '');
  const [batchId, setBatchId] = useState('');
  const [teacherId, setTeacherId] = useState('');
  const [roomId, setRoomId] = useState('');

  // Availability filtering state
  const [availableRooms, setAvailableRooms] = useState<any[]>([]);
  const [availableTeachers, setAvailableTeachers] = useState<any[]>([]);
  const [isLoadingAvailability, setIsLoadingAvailability] = useState(false);

  // Batch filtering for selected lab course
  const [availableBatches, setAvailableBatches] = useState<any[]>([]);
  const [isLoadingBatches, setIsLoadingBatches] = useState(false);

  // Available lab courses (with batch-level tracking)
  const [availableCourses, setAvailableCourses] = useState<any[]>([]);
  const [isLoadingCourses, setIsLoadingCourses] = useState(false);

  // Load available lab courses when modal opens
  useEffect(() => {
    if (!isOpen) return;
    
    const loadAvailableCourses = async () => {
      try {
        setIsLoadingCourses(true);
        const res = await courseAPI.getAvailableWithCredits(
          divisionId,
          academicYearId,
          semester || undefined
        );
        const labCourses = (Array.isArray(res.data) ? res.data : []).filter((c: any) => c.courseType === 'LAB');
        setAvailableCourses(labCourses);
      } catch (err) {
        console.error('Failed to load available courses:', err);
        setAvailableCourses(courses);
      } finally {
        setIsLoadingCourses(false);
      }
    };

    loadAvailableCourses();
  }, [isOpen, divisionId, academicYearId, semester, courses]);

  // Load available batches when course changes
  useEffect(() => {
    if (!courseId || !isOpen) {
      setAvailableBatches(batches);
      return;
    }

    const loadAvailableBatches = async () => {
      try {
        setIsLoadingBatches(true);
        const res = await courseAPI.getAvailableBatchesForCourse(
          Number(courseId),
          divisionId,
          academicYearId
        );
        setAvailableBatches(Array.isArray(res.data) ? res.data : []);
      } catch (err) {
        console.error('Failed to load available batches:', err);
        setAvailableBatches(batches);
      } finally {
        setIsLoadingBatches(false);
      }
    };

    loadAvailableBatches();
  }, [courseId, isOpen, divisionId, academicYearId, batches]);

  // Load available rooms and teachers when day/slot changes
  useEffect(() => {
    if (!dayOfWeek || !timeSlotId) {
      setAvailableRooms(rooms.filter((r: any) => r.roomType === 'LAB'));
      setAvailableTeachers(teachers);
      return;
    }

    const loadAvailability = async () => {
      try {
        setIsLoadingAvailability(true);
        const [roomsRes, teachersRes] = await Promise.all([
          timetableAPI.getAvailableRooms(dayOfWeek, Number(timeSlotId), academicYearId, divisionId),
          timetableAPI.getAvailableTeachers(dayOfWeek, Number(timeSlotId), academicYearId),
        ]);
        const labRooms = (Array.isArray(roomsRes.data) ? roomsRes.data : []).filter((r: any) => r.roomType === 'LAB');
        setAvailableRooms(labRooms);
        setAvailableTeachers(Array.isArray(teachersRes.data) ? teachersRes.data : []);
      } catch (err) {
        console.error('Failed to load availability:', err);
        setAvailableRooms(rooms.filter((r: any) => r.roomType === 'LAB'));
        setAvailableTeachers(teachers);
      } finally {
        setIsLoadingAvailability(false);
      }
    };

    loadAvailability();
  }, [dayOfWeek, timeSlotId, academicYearId, divisionId, rooms, teachers]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!courseId || !dayOfWeek || !timeSlotId || !batchId || !teacherId || !roomId) {
      toast.error('Please fill in all required fields');
      return;
    }

    try {
      setIsSaving(true);
      setConflicts([]);

      await timetableAPI.createLabSession({
        divisionId,
        academicYearId,
        courseId: Number(courseId),
        timeSlotId: Number(timeSlotId),
        dayOfWeek,
        semester: semester || undefined,
        batchAssignments: [{
          batchId: Number(batchId),
          teacherId: Number(teacherId),
          roomId: Number(roomId),
        }],
      });

      toast.success('Lab session created successfully!');
      onSaved();
      onClose();
    } catch (err: any) {
      if (err.response?.status === 409) {
        const data = err.response.data;
        setConflicts(data?.conflicts || [data?.message || 'Conflict detected']);
      } else {
        toast.error(getErrorMessage(err));
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create Lab Session (Single Batch)">
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-gray-600">
          Lab sessions occupy 2 consecutive lecture periods. The system will auto-book the next period.
          Schedule one batch at a time.
        </p>

        {/* Conflicts */}
        {conflicts.length > 0 && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-3">
            <p className="font-semibold text-red-800 mb-1"><FiAlertTriangle className="inline mr-1" /> Conflicts</p>
            {conflicts.map((c, i) => (
              <p key={i} className="text-sm text-red-700">- {c}</p>
            ))}
          </div>
        )}

        {/* Course + Day + Slot */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Lab Course <span className="text-red-500">*</span>
              {isLoadingCourses && <span className="text-xs text-gray-500 ml-2">(Loading...)</span>}
            </label>
            <select 
              className="w-full border rounded-lg px-3 py-2 text-sm" 
              value={courseId} 
              onChange={e => { setCourseId(e.target.value); setConflicts([]); }}
              disabled={isLoadingCourses}
            >
              <option value="">Select lab course...</option>
              {availableCourses.map((c: any) => (
                <option key={c.id} value={c.id}>{c.displayName || c.name}</option>
              ))}
            </select>
            {availableCourses.length === 0 && !isLoadingCourses && (
              <p className="text-xs text-orange-600 mt-1">
                All batches have been allocated lab courses.
              </p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Day <span className="text-red-500">*</span>
            </label>
            <select className="w-full border rounded-lg px-3 py-2 text-sm" value={dayOfWeek} onChange={e => { setDayOfWeek(e.target.value); setConflicts([]); }}>
              <option value="">Select day...</option>
              {DAYS.map(d => <option key={d} value={d}>{DAY_LABELS[d]}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Starting Period <span className="text-red-500">*</span>
              {isLoadingAvailability && <span className="text-xs text-gray-500 ml-2">(Checking...)</span>}
            </label>
            <select className="w-full border rounded-lg px-3 py-2 text-sm" value={timeSlotId} onChange={e => { setTimeSlotId(e.target.value); setConflicts([]); }}>
              <option value="">Select period...</option>
              {timeSlots.map((s: any) => (
                <option key={s.id} value={s.id}>{s.slotName} ({s.startTime?.substring(0,5)})</option>
              ))}
            </select>
          </div>
        </div>

        {/* Semester */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Semester</label>
          <select className="w-full border rounded-lg px-3 py-2 text-sm" value={semester} onChange={e => setSemester(e.target.value)}>
            <option value="">Select semester...</option>
            {['SEM_1','SEM_2','SEM_3','SEM_4','SEM_5','SEM_6','SEM_7','SEM_8'].map(s => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </select>
        </div>

        {/* Batch Assignment */}
        <div className="border-t pt-4">
          <label className="block text-sm font-medium text-gray-700 mb-2">Batch Assignment</label>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <div>
              <label className="block text-xs text-gray-600 mb-1">
                Batch <span className="text-red-500">*</span>
                {isLoadingBatches && <span className="text-xs text-gray-500 ml-2">(Loading...)</span>}
              </label>
              <select
                className="w-full border rounded px-3 py-2 text-sm"
                value={batchId}
                onChange={e => setBatchId(e.target.value)}
                disabled={isLoadingBatches || !courseId}
              >
                <option value="">Select batch...</option>
                {availableBatches.map((b: any) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
              {courseId && availableBatches.length === 0 && !isLoadingBatches && (
                <p className="text-xs text-orange-600 mt-1">
                  All batches allocated for this course.
                </p>
              )}
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">
                Teacher <span className="text-red-500">*</span>
              </label>
              <select
                className="w-full border rounded px-3 py-2 text-sm"
                value={teacherId}
                onChange={e => setTeacherId(e.target.value)}
                disabled={isLoadingAvailability}
              >
                <option value="">Select teacher...</option>
                {availableTeachers.map((t: any) => (
                  <option key={t.id} value={t.id}>{t.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">
                Lab Room <span className="text-red-500">*</span>
              </label>
              <select
                className="w-full border rounded px-3 py-2 text-sm"
                value={roomId}
                onChange={e => setRoomId(e.target.value)}
                disabled={isLoadingAvailability}
              >
                <option value="">Select lab room...</option>
                {availableRooms.map((r: any) => (
                  <option key={r.id} value={r.id}>{r.roomNumber} ({r.capacity})</option>
                ))}
              </select>
            </div>
          </div>
          {dayOfWeek && timeSlotId && !isLoadingAvailability && (
            <>
              {availableTeachers.length === 0 && (
                <p className="text-xs text-orange-600 mt-2">
                  ⚠️ No teachers available at this time.
                </p>
              )}
              {availableRooms.length === 0 && (
                <p className="text-xs text-orange-600 mt-2">
                  ⚠️ No lab rooms available at this time.
                </p>
              )}
            </>
          )}
        </div>

        <div className="flex gap-3 pt-2">
          <Button type="button" variant="outline" onClick={onClose} className="flex-1">Cancel</Button>
          <Button type="submit" variant="primary" isLoading={isSaving} className="flex-1">
            Create Lab Session
          </Button>
        </div>
      </form>
    </Modal>
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
}

const EntryFormModal: React.FC<EntryFormModalProps> = ({
  isOpen, onClose, editingEntry, divisionId, academicYearId,
  courses, teachers, rooms, timeSlots, batches,
  prefillDay, prefillSlotId, defaultSemester, conflicts, setConflicts, onSaved,
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

  // Phase 3: Load available rooms and teachers when day/slot changes
  useEffect(() => {
    if (!form.dayOfWeek || !form.timeSlotId) {
      setAvailableRooms(rooms);
      setAvailableTeachers(teachers);
      return;
    }

    const loadAvailability = async () => {
      try {
        setIsLoadingAvailability(true);
        const [roomsRes, teachersRes] = await Promise.all([
          timetableAPI.getAvailableRooms(form.dayOfWeek, Number(form.timeSlotId), academicYearId, divisionId),
          timetableAPI.getAvailableTeachers(form.dayOfWeek, Number(form.timeSlotId), academicYearId),
        ]);
        setAvailableRooms(Array.isArray(roomsRes.data) ? roomsRes.data : []);
        setAvailableTeachers(Array.isArray(teachersRes.data) ? teachersRes.data : []);
      } catch (err) {
        console.error('Failed to load availability:', err);
        // Fallback to showing all
        setAvailableRooms(rooms);
        setAvailableTeachers(teachers);
      } finally {
        setIsLoadingAvailability(false);
      }
    };

    loadAvailability();
  }, [form.dayOfWeek, form.timeSlotId, academicYearId, divisionId, rooms, teachers]);

  // Load available batches when a lab course is selected
  useEffect(() => {
    if (!form.courseId || !isOpen) {
      setAvailableBatches(batches);
      return;
    }

    const selectedCourse = editingEntry 
      ? courses.find((c: any) => c.id === Number(form.courseId))
      : availableCourses.find((c: any) => c.id === Number(form.courseId));

    // Only filter batches for lab courses
    if (selectedCourse?.courseType !== 'LAB') {
      setAvailableBatches(batches);
      return;
    }

    const loadAvailableBatches = async () => {
      try {
        setIsLoadingBatches(true);
        const res = await courseAPI.getAvailableBatchesForCourse(
          Number(form.courseId),
          divisionId,
          academicYearId
        );
        let filteredBatches = Array.isArray(res.data) ? res.data : [];
        
        // If editing, include the current batch even if it's allocated
        if (editingEntry && editingEntry.batch?.id) {
          const currentBatch = batches.find((b: any) => b.id === editingEntry.batch.id);
          if (currentBatch && !filteredBatches.find((b: any) => b.id === currentBatch.id)) {
            filteredBatches = [currentBatch, ...filteredBatches];
          }
        }
        
        setAvailableBatches(filteredBatches);
      } catch (err) {
        console.error('Failed to load available batches:', err);
        setAvailableBatches(batches);
      } finally {
        setIsLoadingBatches(false);
      }
    };

    loadAvailableBatches();
  }, [form.courseId, isOpen, divisionId, academicYearId, batches, editingEntry, courses, availableCourses]);

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
    if (form.batchId) payload.batchId = Number(form.batchId);
    if (form.semester) payload.semester = form.semester;
    if (form.notes) payload.notes = form.notes;

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
          <div className="bg-red-50 border border-red-300 rounded-lg p-4">
            <div className="flex items-center gap-2 mb-2">
              <FiAlertTriangle className="text-red-600" />
              <span className="font-semibold text-red-800">Scheduling Conflicts Detected</span>
            </div>
            <ul className="text-sm text-red-700 space-y-1">
              {conflicts.map((c, i) => (
                <li key={i}>- {c}</li>
              ))}
            </ul>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Day */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Day <span className="text-red-500">*</span>
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={form.dayOfWeek}
              onChange={(e) => handleChange('dayOfWeek', e.target.value)}
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
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={form.timeSlotId}
              onChange={(e) => handleChange('timeSlotId', e.target.value)}
              required
            >
              <option value="">Select Time Slot</option>
              {timeSlots.map((s: any) => (
                <option key={s.id} value={s.id}>
                  {s.slotName} ({s.startTime?.substring(0, 5)} - {s.endTime?.substring(0, 5)})
                </option>
              ))}
            </select>
          </div>

          {/* Course - Phase 4: Credit-based selection */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Course <span className="text-red-500">*</span>
              {isLoadingCourses && <span className="text-xs text-gray-500 ml-2">(Loading...)</span>}
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={form.courseId}
              onChange={(e) => handleChange('courseId', e.target.value)}
              required
              disabled={isLoadingCourses}
            >
              <option value="">Select Course</option>
              {displayCourses.map((c: any) => (
                <option key={c.id} value={c.id}>
                  {c.displayName || c.name} {c.courseType === 'LAB' ? '(Lab)' : ''} {c.code ? `[${c.code}]` : ''}
                </option>
              ))}
            </select>
            {!editingEntry && displayCourses.length === 0 && !isLoadingCourses && (
              <p className="text-xs text-orange-600 mt-1">
                All course credits have been allocated. Delete an entry to free up a credit slot.
              </p>
            )}
          </div>

          {/* Teacher - Phase 3: Availability filtering */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Teacher <span className="text-red-500">*</span>
              {isLoadingAvailability && <span className="text-xs text-gray-500 ml-2">(Checking...)</span>}
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={form.teacherId}
              onChange={(e) => handleChange('teacherId', e.target.value)}
              required
              disabled={isLoadingAvailability}
            >
              <option value="">Select Teacher</option>
              {availableTeachers.map((t: any) => (
                <option key={t.id} value={t.id}>
                  {t.name} {t.department?.name ? `(${t.department.name})` : ''}
                </option>
              ))}
            </select>
            {form.dayOfWeek && form.timeSlotId && availableTeachers.length === 0 && !isLoadingAvailability && (
              <p className="text-xs text-orange-600 mt-1">
                No teachers available at this time. All teachers are occupied.
              </p>
            )}
          </div>

          {/* Room - Phase 3: Availability filtering */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Room <span className="text-red-500">*</span>
              {isLoadingAvailability && <span className="text-xs text-gray-500 ml-2">(Checking...)</span>}
            </label>
            <select
              className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              value={form.roomId}
              onChange={(e) => handleChange('roomId', e.target.value)}
              required
              disabled={isLoadingAvailability}
            >
              <option value="">Select Room</option>
              {availableRooms.map((r: any) => (
                <option key={r.id} value={r.id}>
                  {r.roomNumber} {r.name ? `- ${r.name}` : ''} ({r.roomType || 'classroom'}, cap: {r.capacity || '?'})
                </option>
              ))}
            </select>
            {form.dayOfWeek && form.timeSlotId && availableRooms.length === 0 && !isLoadingAvailability && (
              <p className="text-xs text-orange-600 mt-1">
                No rooms available at this time. All rooms are occupied.
              </p>
            )}
          </div>

          {/* Batch - Hidden for theory, required for lab */}
          {isLabCourse ? (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Batch <span className="text-red-500">*</span>
                {isLoadingBatches && <span className="text-xs text-gray-500 ml-2">(Loading...)</span>}
              </label>
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                value={form.batchId}
                onChange={(e) => handleChange('batchId', e.target.value)}
                required
                disabled={isLoadingBatches}
              >
                <option value="">Select batch...</option>
                {availableBatches.map((b: any) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
              {availableBatches.length === 0 && !isLoadingBatches && (
                <p className="text-xs text-orange-600 mt-1">
                  All batches have been allocated this lab course.
                </p>
              )}
            </div>
          ) : (
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Batch
              </label>
              <select
                className="w-full border border-gray-300 rounded-lg px-3 py-2 bg-gray-100 cursor-not-allowed"
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
