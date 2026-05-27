import React, { useState, useEffect } from 'react';
import { teacherAdminAPI, timetableAPI, academicYearAPI, timeSlotAPI } from '../../services/api';
import { Teacher, TimetableEntry, AcademicYear, TimeSlot } from '../../types';
import { FiSearch, FiDownload, FiInfo, FiTrendingUp, FiClock, FiBook, FiCheckCircle, FiActivity } from 'react-icons/fi';
import toast from 'react-hot-toast';

export const FacultyTimetablesPage: React.FC = () => {
  const [teachers, setTeachers] = useState<Teacher[]>([]);
  const [filteredTeachers, setFilteredTeachers] = useState<Teacher[]>([]);
  const [selectedTeacherId, setSelectedTeacherId] = useState<number | null>(null);
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  // Root-level calculations to prevent complex JSX scoping
  const selectedTeacher = teachers.find(t => t.id === selectedTeacherId);
  const theoryHours = entries.filter(e => e.type !== 'LAB').length;
  const labHours = entries.filter(e => e.type === 'LAB').length * 2;
  const totalHours = theoryHours + labHours;
  const maxHours = selectedTeacher?.maxWeeklyHours || 30;
  const remainingHours = maxHours - totalHours;

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedTeacherId && selectedYearId) {
      fetchTeacherTimetable();
    } else {
      setEntries([]);
    }
  }, [selectedTeacherId, selectedYearId]);

  useEffect(() => {
    let result = teachers;
    if (searchQuery) {
      result = result.filter(t => 
        t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        t.email.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (t.designation && t.designation.toLowerCase().includes(searchQuery.toLowerCase()))
      );
    }
    setFilteredTeachers(result);
  }, [searchQuery, teachers]);

  const fetchInitialData = async () => {
    try {
      const [teachersRes, yearsRes, slotsRes] = await Promise.all([
        teacherAdminAPI.getAll(),
        academicYearAPI.getAll(),
        timeSlotAPI.getAll()
      ]);
      setTeachers(teachersRes.data || []);
      setFilteredTeachers(teachersRes.data || []);
      setAcademicYears(yearsRes.data || []);
      
      const sortedSlots = (slotsRes.data || [])
        .filter((s: TimeSlot) => s.type === 'TYPE_1') 
        .sort((a: TimeSlot, b: TimeSlot) => a.startTime.localeCompare(b.startTime));
      setTimeSlots(sortedSlots);

      const currentYear = (yearsRes.data || []).find((y: AcademicYear) => y.isActive);
      if (currentYear) {
        setSelectedYearId(currentYear.id);
      } else if ((yearsRes.data || []).length > 0) {
        setSelectedYearId(yearsRes.data[0].id);
      }
    } catch (error) {
      toast.error('Failed to load faculty records');
    }
  };

  const fetchTeacherTimetable = async () => {
    if (!selectedTeacherId || !selectedYearId) return;
    try {
      const res = await timetableAPI.getByTeacher(selectedTeacherId, selectedYearId);
      setEntries(res.data || []);
    } catch (error) {
      toast.error('Failed to load teacher timetable schedule');
    }
  };

  const handleExportPDF = async () => {
    if (!selectedTeacherId || !selectedYearId) {
      toast.error('Please select a teacher first');
      return;
    }
    try {
      toast('Generating PDF export...');
      const res = await timetableAPI.exportTeacherPDF(selectedTeacherId, selectedYearId);
      
      // Trigger file download
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_teacher_${selectedTeacherId}.pdf`;
      link.click();
      toast.success('PDF downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download PDF');
    }
  };

  const handleExportExcel = async () => {
    if (!selectedTeacherId || !selectedYearId) {
      toast.error('Please select a teacher first');
      return;
    }
    try {
      toast('Generating Excel export...');
      const res = await timetableAPI.exportTeacherExcel(selectedTeacherId, selectedYearId);
      
      // Trigger file download
      const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_teacher_${selectedTeacherId}.xlsx`;
      link.click();
      toast.success('Excel spreadsheet downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download Excel spreadsheet');
    }
  };

  const getEntryForCell = (day: string, slotId: number) => {
    return entries.filter(e => e.dayOfWeek === day && e.timeSlotId === slotId);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Faculty Schedule Tracker</h1>
          <p className="text-gray-500 text-sm mt-1">
            Search, inspect, and evaluate individual weekly teacher workload allocations and teaching hours.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleExportExcel}
            disabled={!selectedTeacherId}
            className="flex items-center gap-2 px-4 py-2.5 bg-green-700 text-white rounded-xl font-medium hover:bg-green-800 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
          >
            <FiDownload size={18} />
            Export Excel
          </button>
          <button
            onClick={handleExportPDF}
            disabled={!selectedTeacherId}
            className="flex items-center gap-2 px-4 py-2.5 bg-primary-800 text-white rounded-xl font-medium hover:bg-primary-900 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
          >
            <FiDownload size={18} />
            Export PDF
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <div className="lg:col-span-1 bg-white p-5 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-4 max-h-[800px]">
          <h2 className="text-lg font-bold text-gray-800 flex items-center gap-2">
            Faculty Members
          </h2>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-500">Academic Year</label>
            <select
              value={selectedYearId || ''}
              onChange={(e) => setSelectedYearId(Number(e.target.value))}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            >
              {academicYears.map((y) => (
                <option key={y.id} value={y.id}>{y.name}</option>
              ))}
            </select>
          </div>

          <div className="relative">
            <FiSearch className="absolute left-3 top-2.5 text-gray-400" size={18} />
            <input
              type="text"
              placeholder="Search faculty..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            />
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredTeachers.length === 0 ? (
              <p className="text-center py-8 text-gray-400 text-sm">No faculty found</p>
            ) : (
              filteredTeachers.map((teacher) => (
                <button
                  key={teacher.id}
                  onClick={() => setSelectedTeacherId(teacher.id)}
                  className={`w-full text-left px-4 py-3 rounded-xl transition-all border ${
                    selectedTeacherId === teacher.id
                      ? 'bg-primary-50 border-primary-300 text-primary-800'
                      : 'border-gray-100 hover:bg-gray-50 text-gray-700'
                  }`}
                >
                  <div className="font-semibold text-sm">{teacher.name}</div>
                  <div className="text-xs text-gray-500 mt-1 flex flex-col gap-0.5">
                    <span>{teacher.email}</span>
                    <span className="text-[10px] text-gray-400 uppercase font-semibold mt-0.5">
                      {teacher.designation || 'Lecturer'}
                    </span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        <div className="lg:col-span-3 bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-6">
          {!selectedTeacherId ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-12">
              <FiInfo className="text-gray-300 mb-4" size={48} />
              <h3 className="text-lg font-bold text-gray-700">No Teacher Selected</h3>
              <p className="text-gray-500 text-sm max-w-sm mt-1">
                Select a faculty member from the left sidebar to inspect their weekly workload summary and active teaching slots.
              </p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
                <div className="p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl border border-blue-100 flex items-center gap-3">
                  <div className="p-2 bg-blue-500 rounded-lg text-white">
                    <FiBook size={18} />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-blue-600 uppercase tracking-wider">Theory Hours</div>
                    <div className="text-lg font-bold text-gray-800 mt-0.5">{theoryHours} Hrs</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-purple-50 to-pink-50 rounded-2xl border border-purple-100 flex items-center gap-3">
                  <div className="p-2 bg-purple-50 rounded-lg text-white">
                    <FiTrendingUp size={18} />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-purple-600 uppercase tracking-wider">Lab Hours</div>
                    <div className="text-lg font-bold text-gray-800 mt-0.5">{labHours} Hrs</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-2xl border border-emerald-100 flex items-center gap-3">
                  <div className="p-2 bg-emerald-500 rounded-lg text-white">
                    <FiClock size={18} />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">Total Workload</div>
                    <div className="text-lg font-bold text-gray-800 mt-0.5">{totalHours} Hrs</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-orange-50 to-amber-50 rounded-2xl border border-orange-100 flex items-center gap-3">
                  <div className="p-2 bg-orange-500 rounded-lg text-white">
                    <FiCheckCircle size={18} />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-orange-600 uppercase tracking-wider">Max Hours</div>
                    <div className="text-lg font-bold text-gray-800 mt-0.5">{maxHours} Hrs</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-rose-50 to-red-50 rounded-2xl border border-rose-100 flex items-center gap-3">
                  <div className="p-2 bg-rose-500 rounded-lg text-white">
                    <FiActivity size={18} />
                  </div>
                  <div>
                    <div className="text-[10px] font-bold text-rose-600 uppercase tracking-wider">Remaining</div>
                    <div className="text-lg font-bold text-gray-800 mt-0.5">
                      <span className={remainingHours < 0 ? 'text-red-600 font-extrabold' : ''}>
                        {remainingHours} Hrs
                      </span>
                    </div>
                  </div>
                </div>
              </div>

              <div className="overflow-x-auto border border-gray-200 rounded-2xl mt-4">
                <table className="w-full border-collapse text-sm">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-200">
                      <th className="px-4 py-3 text-left font-bold text-gray-600 w-32 border-r border-gray-200">Time</th>
                      {DAYS.map((_, idx) => (
                        <th key={idx} className="px-4 py-3 text-center font-bold text-gray-600 min-w-[150px] border-r border-gray-200">
                          {DAY_LABELS[idx]}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {timeSlots.map((slot) => (
                      <tr key={slot.id} className="border-b border-gray-200 hover:bg-gray-50">
                        <td className="px-4 py-3 font-semibold text-gray-700 bg-gray-50 border-r border-gray-200">
                          <div className="text-sm">{slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}</div>
                          <div className="text-xs text-gray-400 mt-0.5">{slot.startTime.localeCompare('12:00') >= 0 ? 'PM' : 'AM'}</div>
                        </td>

                        {DAYS.map((day) => {
                          const cellEntries = getEntryForCell(day, slot.id);
                          return (
                            <td key={day} className="p-2 border-r border-gray-200 align-top min-h-[80px]">
                              {cellEntries.length === 0 ? (
                                <div className="h-full flex items-center justify-center py-4 text-gray-300 font-medium italic text-xs">
                                  Free
                                </div>
                              ) : (
                                cellEntries.map((entry) => (
                                  <div
                                    key={entry.id}
                                    className={`p-2.5 rounded-lg border text-xs flex flex-col gap-1 shadow-xs transition-all hover:scale-[1.02] ${
                                      entry.type === 'LAB'
                                        ? 'bg-purple-50 border-purple-200 text-purple-800'
                                        : 'bg-blue-50 border-blue-200 text-blue-800'
                                    }`}
                                  >
                                    <div className="font-bold truncate">Course ID: {entry.courseId}</div>
                                    <div className="flex justify-between items-center text-[10px] text-gray-500 font-medium">
                                      <span>Rm: {entry.roomId}</span>
                                      <span className="px-1 bg-white border border-gray-200 rounded font-semibold scale-90 uppercase">
                                        Div: {entry.divisionId}
                                      </span>
                                    </div>
                                  </div>
                                ))
                              )}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
