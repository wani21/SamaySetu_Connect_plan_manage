import React, { useState, useEffect, useMemo } from 'react';
import { roomAPI, timetableAPI, academicYearAPI, timeSlotAPI, teacherAdminAPI } from '../../services/api';
import { Room, Teacher, AcademicYear, TimeSlot } from '../../types';
import {
  FiSearch, FiDownload, FiInfo, FiTrendingUp, FiCalendar, FiUsers,
  FiClock, FiBook, FiCheckCircle, FiActivity, FiGrid, FiMonitor, FiCpu
} from 'react-icons/fi';
import toast from 'react-hot-toast';

// ---------------------------------------------------------------------------
// Types for the API response (backend returns nested objects, NOT flat IDs)
// ---------------------------------------------------------------------------
interface ApiTimetableEntry {
  id: number;
  dayOfWeek: string;
  // Backend returns full nested objects:
  timeSlot?: { id: number; startTime: string; endTime: string; slotName?: string };
  course?: { id: number; name: string; shortName?: string; courseType?: string };
  teacher?: { id: number; name: string; shortName?: string };
  room?: { id: number; name: string; roomNumber?: string };
  division?: { id: number; name: string };
  batch?: { id: number; name: string };
  // Fallback flat IDs (may exist in some endpoints):
  timeSlotId?: number;
  courseId?: number;
  teacherId?: number;
  roomId?: number;
  divisionId?: number;
  batchId?: number;
  type?: string;
  isLabSession?: boolean;
  status?: string;
  semester?: string;
}

type ViewMode = 'classrooms' | 'labs' | 'faculty';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------
function getSlotId(e: ApiTimetableEntry): number {
  return e.timeSlot?.id ?? e.timeSlotId ?? 0;
}

function getRoomType(r: Room): string {
  return (r.roomType || r.type || '').toUpperCase();
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------
export const ResourceTimetablesPage: React.FC = () => {
  const [mode, setMode] = useState<ViewMode>('classrooms');

  // Shared data
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [allTimeSlots, setAllTimeSlots] = useState<TimeSlot[]>([]);
  const [entries, setEntries] = useState<ApiTimetableEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);

  // Room / Lab data
  const [rooms, setRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [selectedRoomType, setSelectedRoomType] = useState<string>('ALL');

  // Faculty data
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | null>(null);

  // Mobile sidebar toggle
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
  const DAY_LABELS_FULL = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  // -----------------------------------------------------------------------
  // Derived state
  // -----------------------------------------------------------------------
  const isRoomMode = mode === 'classrooms' || mode === 'labs';
  const selectedId = isRoomMode ? selectedRoomId : selectedTeacherId;

  const filteredRooms = useMemo(() => {
    let list = rooms;
    if (mode === 'labs') {
      list = list.filter(r => getRoomType(r) === 'LAB');
    } else if (mode === 'classrooms') {
      list = list.filter(r => getRoomType(r) === 'CLASSROOM');
    }
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      const bw = (r: Room) => r.buildingWing || r.building || r.wing || '';
      list = list.filter(r =>
        r.name.toLowerCase().includes(q) ||
        bw(r).toLowerCase().includes(q)
      );
    }
    if (selectedRoomType !== 'ALL' && mode === 'classrooms') {
      list = list.filter(r => getRoomType(r) === selectedRoomType);
    }
    return list;
  }, [rooms, mode, searchQuery, selectedRoomType]);

  const filteredTeachers = useMemo(() => {
    if (!searchQuery) return teachers;
    const q = searchQuery.toLowerCase();
    return teachers.filter(t =>
      t.name.toLowerCase().includes(q) ||
      t.email.toLowerCase().includes(q) ||
      (t.designation && t.designation.toLowerCase().includes(q))
    );
  }, [teachers, searchQuery]);

  const classroomTypes = useMemo(() => {
    const classroomRooms = rooms.filter(r => getRoomType(r) === 'CLASSROOM');
    const types = new Set(classroomRooms.map(r => getRoomType(r)).filter(Boolean));
    return Array.from(types) as string[];
  }, [rooms]);

  // Faculty workload calcs
  const selectedTeacher = teachers.find(t => t.id === selectedTeacherId);
  const theoryHours = entries.filter(e => {
    const ct = e.course?.courseType ?? e.type;
    return ct !== 'LAB';
  }).length;
  const labHours = entries.filter(e => {
    const ct = e.course?.courseType ?? e.type;
    return ct === 'LAB';
  }).length * 2;
  const totalHours = theoryHours + labHours;
  const maxHours = selectedTeacher?.maxWeeklyHours || 30;
  const remainingHours = maxHours - totalHours;

  // Determine visible time slots based on what the entries actually use
  const visibleTimeSlots = useMemo(() => {
    if (entries.length === 0) {
      // No entries — show TYPE_1 slots by default (non-break only)
      return allTimeSlots
        .filter((s: any) => !s.isBreak && (s.type === 'TYPE_1' || !s.type))
        .sort((a, b) => a.startTime.localeCompare(b.startTime));
    }
    // Detect which slot type the entries use
    const entrySlotIds = new Set(entries.map(e => getSlotId(e)));
    const usedSlots = allTimeSlots.filter(s => entrySlotIds.has(s.id));
    const detectedType = usedSlots.length > 0 ? (usedSlots[0] as any).type : 'TYPE_1';
    return allTimeSlots
      .filter((s: any) => !s.isBreak && (s.type === detectedType || !s.type))
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [allTimeSlots, entries]);

  // Room utilization
  const roomUtilization = useMemo(() => {
    if (visibleTimeSlots.length === 0 || !selectedRoomId) return 0;
    const maxCapacity = 6 * visibleTimeSlots.length;
    if (maxCapacity === 0) return 0;
    return Math.round((entries.length / maxCapacity) * 1000) / 10;
  }, [entries, visibleTimeSlots, selectedRoomId]);

  // -----------------------------------------------------------------------
  // Data fetching
  // -----------------------------------------------------------------------
  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (isRoomMode && selectedRoomId && selectedYearId) {
      fetchRoomTimetable();
    } else if (!isRoomMode && selectedTeacherId && selectedYearId) {
      fetchTeacherTimetable();
    } else {
      setEntries([]);
    }
  }, [selectedRoomId, selectedTeacherId, selectedYearId, mode]);

  // Clear selection on mode change
  useEffect(() => {
    setSelectedRoomId(null);
    setSelectedTeacherId(null);
    setEntries([]);
    setSearchQuery('');
    setSelectedRoomType('ALL');
  }, [mode]);

  const fetchInitialData = async () => {
    try {
      const [roomsRes, teachersRes, yearsRes, slotsRes] = await Promise.all([
        roomAPI.getAll(),
        teacherAdminAPI.getAll(),
        academicYearAPI.getAll(),
        timeSlotAPI.getAll()
      ]);
      setRooms(roomsRes.data || []);
      setTeachers(teachersRes.data || []);
      setAcademicYears(yearsRes.data || []);

      // Store ALL slots (don't filter by type — we detect the right type from entries)
      const sortedSlots = (slotsRes.data || [])
        .sort((a: TimeSlot, b: TimeSlot) => a.startTime.localeCompare(b.startTime));
      setAllTimeSlots(sortedSlots);

      const currentYear = (yearsRes.data || []).find((y: AcademicYear) => y.isCurrent || y.isActive);
      if (currentYear) {
        setSelectedYearId(currentYear.id);
      } else if ((yearsRes.data || []).length > 0) {
        setSelectedYearId(yearsRes.data[0].id);
      }
    } catch {
      toast.error('Failed to load initial data');
    }
  };

  const fetchRoomTimetable = async () => {
    if (!selectedRoomId || !selectedYearId) return;
    setLoading(true);
    try {
      const res = await timetableAPI.getByRoom(selectedRoomId, selectedYearId);
      setEntries(res.data || []);
    } catch {
      toast.error('Failed to load timetable');
    } finally {
      setLoading(false);
    }
  };

  const fetchTeacherTimetable = async () => {
    if (!selectedTeacherId || !selectedYearId) return;
    setLoading(true);
    try {
      const res = await timetableAPI.getByTeacher(selectedTeacherId, selectedYearId);
      setEntries(res.data || []);
    } catch {
      toast.error('Failed to load timetable');
    } finally {
      setLoading(false);
    }
  };

  // -----------------------------------------------------------------------
  // Grid lookup — FIXED: use nested object IDs
  // -----------------------------------------------------------------------
  const getEntryForCell = (day: string, slotId: number): ApiTimetableEntry[] => {
    return entries.filter(e => e.dayOfWeek === day && getSlotId(e) === slotId);
  };

  // -----------------------------------------------------------------------
  // Export handlers
  // -----------------------------------------------------------------------
  const handleExportPDF = async () => {
    if (!selectedId || !selectedYearId) {
      toast.error('Please select an item first');
      return;
    }
    try {
      toast('Generating PDF export...');
      let res;
      if (isRoomMode) {
        res = await timetableAPI.exportRoomPDF(selectedId, selectedYearId, 'SEM_6');
      } else {
        res = await timetableAPI.exportTeacherPDF(selectedId, selectedYearId);
      }
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_${mode}_${selectedId}.pdf`;
      link.click();
      toast.success('PDF downloaded!');
    } catch {
      toast.error('Failed to download PDF');
    }
  };

  const handleExportExcel = async () => {
    if (!selectedId || !selectedYearId) {
      toast.error('Please select an item first');
      return;
    }
    try {
      toast('Generating Excel export...');
      let res;
      if (isRoomMode) {
        res = await timetableAPI.exportRoomExcel(selectedId, selectedYearId, 'SEM_6');
      } else {
        res = await timetableAPI.exportTeacherExcel(selectedId, selectedYearId);
      }
      const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_${mode}_${selectedId}.xlsx`;
      link.click();
      toast.success('Excel downloaded!');
    } catch {
      toast.error('Failed to download Excel');
    }
  };

  // -----------------------------------------------------------------------
  // Selection handler (auto-close sidebar on mobile)
  // -----------------------------------------------------------------------
  const handleSelect = (id: number) => {
    if (isRoomMode) {
      setSelectedRoomId(id);
    } else {
      setSelectedTeacherId(id);
    }
    setSidebarOpen(false); // close on mobile
  };

  // -----------------------------------------------------------------------
  // Render helpers
  // -----------------------------------------------------------------------
  const modeConfig = {
    classrooms: { icon: FiMonitor, label: 'Classrooms', color: 'blue' },
    labs: { icon: FiCpu, label: 'Laboratories', color: 'purple' },
    faculty: { icon: FiUsers, label: 'Faculty', color: 'emerald' },
  };

  const selectedItemName = isRoomMode
    ? rooms.find(r => r.id === selectedRoomId)?.name
    : teachers.find(t => t.id === selectedTeacherId)?.name;

  // -----------------------------------------------------------------------
  // Render: Entry cell content — FIXED: uses nested object names
  // -----------------------------------------------------------------------
  const renderEntryCell = (entry: ApiTimetableEntry) => {
    const courseName = entry.course?.shortName || entry.course?.name || `Course #${entry.courseId || '?'}`;
    const courseType = entry.course?.courseType || entry.type;
    const isLab = courseType === 'LAB' || entry.isLabSession;

    // In faculty mode show room, in room mode show teacher
    const secondaryInfo = mode === 'faculty'
      ? (entry.room?.name || `Room #${entry.roomId || '?'}`)
      : (entry.teacher?.shortName || entry.teacher?.name || `Faculty #${entry.teacherId || '?'}`);

    const divisionName = entry.division?.name || `Div ${entry.divisionId || '?'}`;
    const batchName = entry.batch?.name;

    return (
      <div
        key={entry.id}
        className={`p-2 sm:p-2.5 rounded-lg border text-xs flex flex-col gap-0.5 shadow-xs transition-all hover:scale-[1.02] ${
          isLab
            ? 'bg-purple-50 border-purple-200 text-purple-800'
            : 'bg-blue-50 border-blue-200 text-blue-800'
        }`}
      >
        <div className="font-bold truncate text-[11px] sm:text-xs">{courseName}</div>
        <div className="flex flex-wrap justify-between items-center gap-1 text-[9px] sm:text-[10px] text-gray-500 font-medium">
          <span className="truncate max-w-[80px] sm:max-w-none">{secondaryInfo}</span>
          <span className="px-1 bg-white border border-gray-200 rounded font-semibold uppercase shrink-0">
            {batchName || divisionName}
          </span>
        </div>
      </div>
    );
  };

  // -----------------------------------------------------------------------
  // RENDER
  // -----------------------------------------------------------------------
  return (
    <div className="space-y-4 sm:space-y-6">
      {/* ===== Header Bar ===== */}
      <div className="flex flex-col gap-4 bg-white p-4 sm:p-6 rounded-2xl shadow-sm border border-gray-100">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold text-gray-800">Resource Timetables</h1>
            <p className="text-gray-500 text-xs sm:text-sm mt-1">
              View weekly schedules for classrooms, laboratories, and faculty members.
            </p>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              onClick={handleExportExcel}
              disabled={!selectedId}
              className="flex items-center gap-2 px-3 sm:px-4 py-2 sm:py-2.5 bg-green-700 text-white rounded-xl text-xs sm:text-sm font-medium hover:bg-green-800 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
            >
              <FiDownload size={16} />
              <span className="hidden sm:inline">Export</span> Excel
            </button>
            <button
              onClick={handleExportPDF}
              disabled={!selectedId}
              className="flex items-center gap-2 px-3 sm:px-4 py-2 sm:py-2.5 bg-primary-800 text-white rounded-xl text-xs sm:text-sm font-medium hover:bg-primary-900 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
            >
              <FiDownload size={16} />
              <span className="hidden sm:inline">Export</span> PDF
            </button>
          </div>
        </div>

        {/* ===== Tab Switcher ===== */}
        <div className="flex gap-1 p-1 bg-gray-100 rounded-xl w-full sm:w-fit">
          {(Object.keys(modeConfig) as ViewMode[]).map((m) => {
            const cfg = modeConfig[m];
            const Icon = cfg.icon;
            const isActive = mode === m;
            return (
              <button
                key={m}
                onClick={() => setMode(m)}
                className={`flex items-center gap-1.5 px-3 sm:px-4 py-2 rounded-lg text-xs sm:text-sm font-semibold transition-all flex-1 sm:flex-none justify-center ${
                  isActive
                    ? 'bg-white text-gray-800 shadow-sm'
                    : 'text-gray-500 hover:text-gray-700 hover:bg-gray-50'
                }`}
              >
                <Icon size={16} />
                <span>{cfg.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* ===== Main Content Grid ===== */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-4 sm:gap-6">

        {/* --- Mobile Sidebar Toggle --- */}
        <button
          onClick={() => setSidebarOpen(!sidebarOpen)}
          className="lg:hidden flex items-center justify-between w-full px-4 py-3 bg-white rounded-xl border border-gray-200 shadow-sm"
        >
          <span className="font-semibold text-sm text-gray-700">
            {selectedItemName ? `Selected: ${selectedItemName}` : `Select ${modeConfig[mode].label}`}
          </span>
          <FiGrid size={18} className="text-gray-400" />
        </button>

        {/* ===== Left Sidebar ===== */}
        <div
          className={`lg:col-span-1 bg-white p-4 sm:p-5 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-3 sm:gap-4 max-h-[70vh] lg:max-h-[800px] ${
            sidebarOpen ? 'block' : 'hidden lg:flex'
          }`}
        >
          <h2 className="text-base sm:text-lg font-bold text-gray-800 flex items-center gap-2">
            {React.createElement(modeConfig[mode].icon, { size: 20 })}
            {modeConfig[mode].label}
          </h2>

          {/* Academic Year */}
          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-500">Academic Year</label>
            <select
              value={selectedYearId || ''}
              onChange={(e) => setSelectedYearId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            >
              {academicYears.map((y) => (
                <option key={y.id} value={y.id}>{y.yearName || y.name || `Year ${y.id}`}</option>
              ))}
            </select>
          </div>

          {/* Search */}
          <div className="relative">
            <FiSearch className="absolute left-3 top-2.5 text-gray-400" size={18} />
            <input
              type="text"
              placeholder={`Search ${modeConfig[mode].label.toLowerCase()}...`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            />
          </div>

          {/* Room Type filter (classrooms only) */}
          {mode === 'classrooms' && classroomTypes.length > 0 && (
            <div className="space-y-1">
              <label className="text-xs font-semibold text-gray-500">Room Type</label>
              <select
                value={selectedRoomType}
                onChange={(e) => setSelectedRoomType(e.target.value)}
                className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
              >
                <option value="ALL">All Types</option>
                {classroomTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>
          )}

          {/* Item List */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1 -mr-1">
            {isRoomMode ? (
              filteredRooms.length === 0 ? (
                <p className="text-center py-8 text-gray-400 text-sm">No {modeConfig[mode].label.toLowerCase()} found</p>
              ) : (
                filteredRooms.map((room) => (
                  <button
                    key={room.id}
                    onClick={() => handleSelect(room.id)}
                    className={`w-full text-left px-3 sm:px-4 py-2.5 sm:py-3 rounded-xl transition-all border ${
                      selectedRoomId === room.id
                        ? 'bg-primary-50 border-primary-300 text-primary-800'
                        : 'border-gray-100 hover:bg-gray-50 text-gray-700'
                    }`}
                  >
                    <div className="font-semibold text-sm">{room.name}</div>
                    <div className="text-xs text-gray-500 mt-1 flex items-center justify-between">
                      <span>{room.buildingWing || room.building || 'Main Wing'} • Cap: {room.capacity || 60}</span>
                      <span className="px-1.5 py-0.5 bg-gray-100 rounded text-gray-600 font-semibold uppercase text-[10px]">
                        {room.roomType || room.type || 'Classroom'}
                      </span>
                    </div>
                  </button>
                ))
              )
            ) : (
              filteredTeachers.length === 0 ? (
                <p className="text-center py-8 text-gray-400 text-sm">No faculty found</p>
              ) : (
                filteredTeachers.map((teacher) => (
                  <button
                    key={teacher.id}
                    onClick={() => handleSelect(teacher.id)}
                    className={`w-full text-left px-3 sm:px-4 py-2.5 sm:py-3 rounded-xl transition-all border ${
                      selectedTeacherId === teacher.id
                        ? 'bg-primary-50 border-primary-300 text-primary-800'
                        : 'border-gray-100 hover:bg-gray-50 text-gray-700'
                    }`}
                  >
                    <div className="font-semibold text-sm">{teacher.name}</div>
                    <div className="text-xs text-gray-500 mt-1 flex flex-col gap-0.5">
                      <span className="truncate">{teacher.email}</span>
                      <span className="text-[10px] text-gray-400 uppercase font-semibold">
                        {teacher.designation || 'Lecturer'}
                      </span>
                    </div>
                  </button>
                ))
              )
            )}
          </div>
        </div>

        {/* ===== Right Panel ===== */}
        <div className="lg:col-span-3 bg-white p-4 sm:p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-4 sm:gap-6">
          {!selectedId ? (
            /* --- Empty State --- */
            <div className="flex-1 flex flex-col items-center justify-center text-center p-8 sm:p-12">
              <FiInfo className="text-gray-300 mb-4" size={48} />
              <h3 className="text-lg font-bold text-gray-700">No {mode === 'faculty' ? 'Faculty' : 'Room'} Selected</h3>
              <p className="text-gray-500 text-sm max-w-sm mt-1">
                Select a {modeConfig[mode].label.toLowerCase().slice(0, -1)} from the
                <button onClick={() => setSidebarOpen(true)} className="text-primary-600 font-semibold mx-1 lg:hidden underline">sidebar</button>
                <span className="hidden lg:inline">left panel</span> to view its weekly timetable.
              </p>
            </div>
          ) : (
            <>
              {/* --- Stats Cards --- */}
              {mode === 'faculty' ? (
                /* Faculty stats: 5 cards */
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3 sm:gap-4">
                  <StatCard icon={FiBook} label="Theory Hours" value={`${theoryHours} Hrs`} gradient="from-blue-50 to-indigo-50" border="blue" iconBg="bg-blue-500" labelColor="text-blue-600" />
                  <StatCard icon={FiTrendingUp} label="Lab Hours" value={`${labHours} Hrs`} gradient="from-purple-50 to-pink-50" border="purple" iconBg="bg-purple-500" labelColor="text-purple-600" />
                  <StatCard icon={FiClock} label="Total Workload" value={`${totalHours} Hrs`} gradient="from-emerald-50 to-teal-50" border="emerald" iconBg="bg-emerald-500" labelColor="text-emerald-600" />
                  <StatCard icon={FiCheckCircle} label="Max Hours" value={`${maxHours} Hrs`} gradient="from-orange-50 to-amber-50" border="orange" iconBg="bg-orange-500" labelColor="text-orange-600" />
                  <StatCard
                    icon={FiActivity} label="Remaining" gradient="from-rose-50 to-red-50" border="rose" iconBg="bg-rose-500" labelColor="text-rose-600"
                    value={<span className={remainingHours < 0 ? 'text-red-600 font-extrabold' : ''}>{remainingHours} Hrs</span>}
                  />
                </div>
              ) : (
                /* Room stats: 3 cards */
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4">
                  <StatCard icon={FiTrendingUp} label="Weekly Utilization" value={`${roomUtilization}%`} gradient="from-blue-50 to-indigo-50" border="blue" iconBg="bg-blue-500" labelColor="text-blue-600" />
                  <StatCard icon={FiCalendar} label="Scheduled Slots" value={`${entries.length} Slots`} gradient="from-purple-50 to-pink-50" border="purple" iconBg="bg-purple-500" labelColor="text-purple-600" />
                  <StatCard icon={FiUsers} label={mode === 'labs' ? 'Lab Capacity' : 'Room Capacity'} value={`${rooms.find(r => r.id === selectedRoomId)?.capacity || 60} Students`} gradient="from-emerald-50 to-teal-50" border="emerald" iconBg="bg-emerald-500" labelColor="text-emerald-600" />
                </div>
              )}

              {/* --- Loading State --- */}
              {loading && (
                <div className="flex items-center justify-center py-8">
                  <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
                </div>
              )}

              {/* --- Timetable Grid --- */}
              {!loading && (
                <div className="overflow-x-auto border border-gray-200 rounded-2xl">
                  <table className="w-full border-collapse text-xs sm:text-sm min-w-[700px]">
                    <thead>
                      <tr className="bg-gray-50 border-b border-gray-200">
                        <th className="px-2 sm:px-4 py-2 sm:py-3 text-left font-bold text-gray-600 w-24 sm:w-32 border-r border-gray-200 sticky left-0 bg-gray-50 z-10">
                          Time
                        </th>
                        {DAYS.map((_, idx) => (
                          <th key={idx} className="px-2 sm:px-4 py-2 sm:py-3 text-center font-bold text-gray-600 min-w-[100px] sm:min-w-[130px] border-r border-gray-200">
                            <span className="hidden sm:inline">{DAY_LABELS_FULL[idx]}</span>
                            <span className="sm:hidden">{DAY_LABELS[idx]}</span>
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {visibleTimeSlots.map((slot) => (
                        <tr key={slot.id} className="border-b border-gray-200 hover:bg-gray-50/50">
                          <td className="px-2 sm:px-4 py-2 sm:py-3 font-semibold text-gray-700 bg-gray-50 border-r border-gray-200 sticky left-0 z-10">
                            <div className="text-[11px] sm:text-sm whitespace-nowrap">
                              {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                            </div>
                            <div className="text-[9px] sm:text-xs text-gray-400 mt-0.5">
                              {slot.startTime.localeCompare('12:00') >= 0 ? 'PM' : 'AM'}
                            </div>
                          </td>

                          {DAYS.map((day) => {
                            const cellEntries = getEntryForCell(day, slot.id);
                            return (
                              <td key={day} className="p-1 sm:p-2 border-r border-gray-200 align-top">
                                {cellEntries.length === 0 ? (
                                  <div className="h-full flex items-center justify-center py-3 sm:py-4 text-gray-300 font-medium italic text-[10px] sm:text-xs">
                                    Free
                                  </div>
                                ) : (
                                  <div className="flex flex-col gap-1">
                                    {cellEntries.map((entry) => renderEntryCell(entry))}
                                  </div>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

// ---------------------------------------------------------------------------
// StatCard sub-component
// ---------------------------------------------------------------------------
interface StatCardProps {
  icon: React.ElementType;
  label: string;
  value: React.ReactNode;
  gradient: string;
  border: string;
  iconBg: string;
  labelColor: string;
}

const StatCard: React.FC<StatCardProps> = ({ icon: Icon, label, value, gradient, border, iconBg, labelColor }) => (
  <div className={`p-3 sm:p-4 bg-gradient-to-r ${gradient} rounded-2xl border border-${border}-100 flex items-center gap-3`}>
    <div className={`p-2 ${iconBg} rounded-lg text-white shrink-0`}>
      <Icon size={18} />
    </div>
    <div className="min-w-0">
      <div className={`text-[9px] sm:text-[10px] font-bold ${labelColor} uppercase tracking-wider truncate`}>{label}</div>
      <div className="text-base sm:text-lg font-bold text-gray-800 mt-0.5 truncate">{value}</div>
    </div>
  </div>
);
