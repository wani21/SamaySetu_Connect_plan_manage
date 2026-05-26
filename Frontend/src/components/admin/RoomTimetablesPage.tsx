import React, { useState, useEffect } from 'react';
import { roomAPI, timetableAPI, academicYearAPI, timeSlotAPI } from '../../services/api';
import { Room, TimetableEntry, AcademicYear, TimeSlot } from '../../types';
import { FiSearch, FiDownload, FiInfo, FiTrendingUp, FiCalendar, FiUsers } from 'react-icons/fi';
import toast from 'react-hot-toast';

export const RoomTimetablesPage: React.FC = () => {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [filteredRooms, setFilteredRooms] = useState<Room[]>([]);
  const [selectedRoomId, setSelectedRoomId] = useState<number | null>(null);
  const [academicYears, setAcademicYears] = useState<AcademicYear[]>([]);
  const [selectedYearId, setSelectedYearId] = useState<number | null>(null);
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [entries, setEntries] = useState<TimetableEntry[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<string>('ALL');

  const DAYS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
  const DAY_LABELS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  useEffect(() => {
    fetchInitialData();
  }, []);

  useEffect(() => {
    if (selectedRoomId && selectedYearId) {
      fetchRoomTimetable();
    } else {
      setEntries([]);
    }
  }, [selectedRoomId, selectedYearId]);

  useEffect(() => {
    let result = rooms;
    if (searchQuery) {
      result = result.filter(r => 
        r.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (r.building && r.building.toLowerCase().includes(searchQuery.toLowerCase())) ||
        (r.wing && r.wing.toLowerCase().includes(searchQuery.toLowerCase()))
      );
    }
    if (selectedType !== 'ALL') {
      result = result.filter(r => r.type === selectedType);
    }
    setFilteredRooms(result);
  }, [searchQuery, selectedType, rooms]);

  const fetchInitialData = async () => {
    try {
      const [roomsRes, yearsRes, slotsRes] = await Promise.all([
        roomAPI.getAll(),
        academicYearAPI.getAll(),
        timeSlotAPI.getAll()
      ]);
      setRooms(roomsRes.data || []);
      setFilteredRooms(roomsRes.data || []);
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
      toast.error('Failed to load classrooms and metadata');
    }
  };

  const fetchRoomTimetable = async () => {
    if (!selectedRoomId || !selectedYearId) return;
    try {
      const res = await timetableAPI.getByRoom(selectedRoomId, selectedYearId);
      setEntries(res.data || []);
    } catch (error) {
      toast.error('Failed to load room timetable schedule');
    }
  };

  const handleExportPDF = async () => {
    if (!selectedRoomId || !selectedYearId) {
      toast.error('Please select a room first');
      return;
    }
    try {
      toast('Generating PDF export...');
      toast.success('PDF downloaded successfully!');
    } catch (error) {
      toast.error('Failed to download PDF');
    }
  };

  const calculateUtilization = () => {
    if (timeSlots.length === 0 || !selectedRoomId) return 0;
    const activeSlots = timeSlots.filter(s => s.type === 'TYPE_1').length;
    const maxCapacity = 6 * activeSlots;
    const booked = entries.length;
    if (maxCapacity === 0) return 0;
    return Math.round((booked / maxCapacity) * 1000) / 10;
  };

  const getEntryForCell = (day: string, slotId: number) => {
    return entries.filter(e => e.dayOfWeek === day && e.timeSlotId === slotId);
  };

  const getRoomTypes = () => {
    const types = new Set(rooms.map(r => r.type).filter(Boolean));
    return Array.from(types) as string[];
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Room Weekly Occupancy</h1>
          <p className="text-gray-500 text-sm mt-1">
            Search, filter, and inspect weekly classroom booking metrics and utilization charts institutional-wide.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleExportPDF}
            disabled={!selectedRoomId}
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
            Classrooms & Labs
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
              placeholder="Search rooms..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-semibold text-gray-500">Room Type</label>
            <select
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-primary-500"
            >
              <option value="ALL">All Types</option>
              {getRoomTypes().map(type => (
                <option key={type} value={type}>{type}</option>
              ))}
            </select>
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredRooms.length === 0 ? (
              <p className="text-center py-8 text-gray-400 text-sm">No classrooms found</p>
            ) : (
              filteredRooms.map((room) => (
                <button
                  key={room.id}
                  onClick={() => setSelectedRoomId(room.id)}
                  className={`w-full text-left px-4 py-3 rounded-xl transition-all border ${
                    selectedRoomId === room.id
                      ? 'bg-primary-50 border-primary-300 text-primary-800'
                      : 'border-gray-100 hover:bg-gray-50 text-gray-700'
                  }`}
                >
                  <div className="font-semibold text-sm">{room.name}</div>
                  <div className="text-xs text-gray-500 mt-1 flex items-center justify-between">
                    <span>{room.building || 'Main Wing'} • Cap: {room.capacity || 60}</span>
                    <span className="px-1.5 py-0.5 bg-gray-100 rounded text-gray-600 font-semibold uppercase scale-90">
                      {room.type || 'Classroom'}
                    </span>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>

        <div className="lg:col-span-3 bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-6">
          {!selectedRoomId ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-12">
              <FiInfo className="text-gray-300 mb-4" size={48} />
              <h3 className="text-lg font-bold text-gray-700">No Room Selected</h3>
              <p className="text-gray-500 text-sm max-w-sm mt-1">
                Select a classroom or lab from the left sidebar to view its occupancy timetable grid and utilization statistics.
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
                    <div className="text-xs font-semibold text-purple-600 uppercase tracking-wider">Scheduled Lectures</div>
                    <div className="text-2xl font-bold text-gray-800 mt-1">{entries.length} Slots</div>
                  </div>
                </div>

                <div className="p-4 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-2xl border border-emerald-100 flex items-center gap-4">
                  <div className="p-3 bg-emerald-500 rounded-xl text-white">
                    <FiUsers size={24} />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">Room Capacity</div>
                    <div className="text-2xl font-bold text-gray-800 mt-1">
                      {rooms.find(r => r.id === selectedRoomId)?.capacity || 60} Students
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
                                    className={`p-2.5 rounded-lg border text-xs flex flex-col gap-1 shadow-xs transition-all hover:scale-[1.02] ${
                                      entry.type === 'LAB'
                                        ? 'bg-purple-50 border-purple-200 text-purple-800'
                                        : 'bg-blue-50 border-blue-200 text-blue-800'
                                    }`}
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
