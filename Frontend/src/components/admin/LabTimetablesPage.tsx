import React, { useState, useEffect } from 'react';
import { roomAPI, timetableAPI, academicYearAPI, timeSlotAPI } from '../../services/api';
import { Room, TimetableEntry, AcademicYear, TimeSlot } from '../../types';
import { FiSearch, FiDownload, FiInfo, FiTrendingUp, FiCalendar, FiUsers } from 'react-icons/fi';
import toast from 'react-hot-toast';

export const LabTimetablesPage: React.FC = () => {
  const [labs, setLabs] = useState<Room[]>([]);
  const [filteredLabs, setFilteredLabs] = useState<Room[]>([]);
  const [selectedLabId, setSelectedLabId] = useState<number | null>(null);
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');

  const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedLabId && selectedYearId) {
      fetchLabTimetable();
    } else {
      setEntries([]);
    }
  }, [selectedLabId, selectedYearId]);

  useEffect(() => {
    let result = labs;
    if (searchQuery) {
      result = result.filter(r => 
        r.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (r.building && r.building.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (r.wing && r.wing.toLowerCase().includes(searchQuery.toLowerCase()))
      );
    }
    setFilteredLabs(result);
  }, [searchQuery, labs]);

  const fetchInitialData = async () => {
    try {
      const [roomsRes, yearsRes, slotsRes] = await Promise.all([
        roomAPI.getAll(),
        academicYearAPI.getAll(),
        timeSlotAPI.getAll()
      ]);
      
      // Filter strictly for rooms of type 'LAB'
      const labRooms = (roomsRes.data || []).filter((r: Room) => r.type?.toUpperCase() === 'LAB');
      setLabs(labRooms);
      setFilteredLabs(labRooms);
      
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
      toast.error('Failed to load laboratory structures');
    }
  };

  const fetchLabTimetable = async () => {
    if (!selectedLabId || !selectedYearId) return;
    try {
      const res = await timetableAPI.getByRoom(selectedLabId, selectedYearId);
      setEntries(res.data || []);
    } catch (error) {
      toast.error('Failed to load laboratory timetable schedule');
    }
  };

  const handleExportPDF = async () => {
    if (!selectedLabId || !selectedYearId) {
      toast.error('Please select a laboratory first');
      return;
    }
    try {
      toast('Generating PDF export...');
      const res = await timetableAPI.exportRoomPDF(selectedLabId, selectedYearId, 'SEM_3');
      
      // Trigger file download
      const blob = new Blob([res.data], { type: 'application/pdf' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_lab_${selectedLabId}.pdf`;
      link.click();
      toast.success('PDF downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download PDF');
    }
  };

  const handleExportExcel = async () => {
    if (!selectedLabId || !selectedYearId) {
      toast.error('Please select a laboratory first');
      return;
    }
    try {
      toast('Generating Excel export...');
      const res = await timetableAPI.exportRoomExcel(selectedLabId, selectedYearId, 'SEM_3');
      
      // Trigger file download
      const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = `timetable_lab_${selectedLabId}.xlsx`;
      link.click();
      toast.success('Excel spreadsheet downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download Excel spreadsheet');
    }
  };

  const calculateUtilization = () => {
    if (timeSlots.length === 0 || !selectedLabId) return 0;
    const activeSlots = timeSlots.filter(s => s.type === 'TYPE_1').length;
    const maxCapacity = 6 * activeSlots;
    const booked = entries.length;
    if (maxCapacity === 0) return 0;
    return Math.round((booked / maxCapacity) * 1000) / 10;
  };

  const getEntryForCell = (day: string, slotId: number) => {
    return entries.filter(e => e.dayOfWeek === day && e.timeSlotId === slotId);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Laboratory Timetables</h1>
          <p className="text-gray-500 text-sm mt-1">
            Search, filter, and inspect weekly laboratory schedules and utilization metrics.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleExportExcel}
            disabled={!selectedLabId}
            className="flex items-center gap-2 px-4 py-2.5 bg-green-700 text-white rounded-xl font-medium hover:bg-green-800 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-sm"
          >
            <FiDownload size={18} />
            Export Excel
          </button>
          <button
            onClick={handleExportPDF}
            disabled={!selectedLabId}
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
            Laboratories
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
              placeholder="Search labs..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            />
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredLabs.length === 0 ? (
              <p className="text-center py-8 text-gray-400 text-sm">No laboratories found</p>
            ) : (
              filteredLabs.map((lab) => (
                <button
                  key={lab.id}
                  onClick={() => setSelectedLabId(lab.id)}
                  className={`w-full text-left px-4 py-3 rounded-xl transition-all border ${
                    selectedLabId === lab.id
                      ? 'bg-primary-50 border-primary-300 text-primary-800'
                      : 'border-gray-100 hover:bg-gray-50 text-gray-700'
                  }`}
                >
                  <div className="font-semibold text-sm">{lab.name}</div>
                  <div className="text-xs text-gray-500 mt-1 flex items-center justify-between">
                    <span>{lab.building || 'Main Wing'} • Cap: {lab.capacity || 30}</span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        <div className="lg:col-span-3 bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-6">
          {!selectedLabId ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-12">
              <FiInfo className="text-gray-300 mb-4" size={48} />
              <h3 className="text-lg font-bold text-gray-700">No Laboratory Selected</h3>
              <p className="text-gray-500 text-sm max-w-sm mt-1">
                Select a laboratory from the left sidebar to view its weekly occupancy timetable grid and utilization statistics.
              </p>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="p-4 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl border border-blue-100 flex items-center gap-4">
                  <div className="p-3 bg-blue-500 rounded-xl text-white">
                    <FiTrendingUp size={24} />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-blue-600 uppercase tracking-wider">Weekly Utilization</div>
                    <div className="text-2xl font-bold text-gray-800 mt-1">{calculateUtilization()}%</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-purple-50 to-pink-50 rounded-2xl border border-purple-100 flex items-center gap-4">
                  <div className="p-3 bg-purple-500 rounded-xl text-white">
                    <FiCalendar size={24} />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-purple-600 uppercase tracking-wider">Scheduled Labs</div>
                    <div className="text-2xl font-bold text-gray-800 mt-1">{entries.length} Slots</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-2xl border border-emerald-100 flex items-center gap-4">
                  <div className="p-3 bg-emerald-500 rounded-xl text-white">
                    <FiUsers size={24} />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">Lab Capacity</div>
                    <div className="text-2xl font-bold text-gray-800 mt-1">
                      {labs.find(r => r.id === selectedLabId)?.capacity || 30} Students
                    </div>
                  </div>
                </div>
              </div>

              <div className="overflow-x-auto border border-gray-200 rounded-2xl">
                <table className="w-full border-collapse text-sm">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-200">
                      <th className="px-4 py-3 text-left font-bold text-gray-600 w-32 border-r border-gray-200">Time</th>
                      {DAY_LABELS.map((day, idx) => (
                        <th key={idx} className="px-4 py-3 text-center font-bold text-gray-600 min-w-[150px] border-r border-gray-200">
                          {day}
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
                                    className="p-2.5 rounded-lg border text-xs flex flex-col gap-1 shadow-xs transition-all hover:scale-[1.02] bg-purple-50 border-purple-200 text-purple-800"
                                  >
                                    <div className="font-bold truncate">Course ID: {entry.courseId}</div>
                                    <div className="flex justify-between items-center text-[10px] text-gray-500 font-medium">
                                      <span>Fac: {entry.teacherId}</span>
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
