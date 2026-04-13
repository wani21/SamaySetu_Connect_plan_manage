import React, { useState, useEffect } from 'react';
import { FiCalendar, FiClock, FiMapPin, FiDownload } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Loading } from '../../components/common/Loading';
import { teacherAPI, timetableAPI, timeSlotPublicAPI, academicYearPublicAPI } from '../../services/api';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const DAY_ENUM: Record<string, string> = {
  Monday: 'MONDAY', Tuesday: 'TUESDAY', Wednesday: 'WEDNESDAY',
  Thursday: 'THURSDAY', Friday: 'FRIDAY', Saturday: 'SATURDAY',
};

export const TimetablePage: React.FC = () => {
  const [timeSlots, setTimeSlots] = useState<any[]>([]);
  const [timetableEntries, setTimetableEntries] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [exportContext, setExportContext] = useState<{ teacherId: number; academicYearId: number } | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setIsLoading(true);
      setError(null);

      // Fetch time slots, teacher profile, and academic years in parallel
      const [slotsRes, profileRes, yearsRes] = await Promise.all([
        timeSlotPublicAPI.getAll(),
        teacherAPI.getProfile(),
        academicYearPublicAPI.getAll(),
      ]);

      const slots = Array.isArray(slotsRes.data) ? slotsRes.data : [];
      // Filter by TYPE_1 (default schedule) to avoid duplicates, keep breaks for display
      const filteredSlots = slots
        .filter((slot: any) => !slot.type || slot.type === 'TYPE_1')
        .sort((a: any, b: any) => a.startTime.localeCompare(b.startTime));
      setTimeSlots(filteredSlots);

      const teacher = profileRes.data;
      const years = Array.isArray(yearsRes.data) ? yearsRes.data : [];
      const currentYear = years.find((y: any) => y.isCurrent);

      if (teacher?.id && currentYear?.id) {
        setExportContext({ teacherId: teacher.id, academicYearId: currentYear.id });
        try {
          const ttRes = await timetableAPI.getByTeacher(teacher.id, currentYear.id);
          setTimetableEntries(Array.isArray(ttRes.data) ? ttRes.data : []);
        } catch {
          // No published timetable yet
          setTimetableEntries([]);
        }
      }
    } catch (err) {
      if (import.meta.env.DEV) console.error('Failed to fetch timetable data:', err);
      setError('Failed to load timetable. Please try again later.');
    } finally {
      setIsLoading(false);
    }
  };

  const formatTime = (time: string) => (time ? time.substring(0, 5) : '');

  const getSlotKey = (slot: any) =>
    `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;

  // Build timetable lookup: { "MONDAY": { slotId: entry } }
  const timetableLookup: Record<string, Record<number, any>> = {};
  timetableEntries.forEach((entry: any) => {
    const day = entry.dayOfWeek;
    const slotId = entry.timeSlot?.id;
    if (day && slotId) {
      if (!timetableLookup[day]) timetableLookup[day] = {};
      timetableLookup[day][slotId] = entry;
    }
  });

  // Compute real stats
  const totalClasses = timetableEntries.length;
  const totalHours = timetableEntries.reduce((sum: number, e: any) => {
    if (e.timeSlot?.durationMinutes) return sum + e.timeSlot.durationMinutes / 60;
    return sum + 1;
  }, 0);
  const uniqueRooms = new Set(timetableEntries.map((e: any) => e.room?.id).filter(Boolean)).size;

  if (isLoading) return <Loading />;

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">My Timetable</h1>
          <p className="text-gray-600">View your weekly class schedule</p>
        </div>
        {exportContext && timetableEntries.length > 0 && (
          <Button variant="outline" onClick={async () => {
            try {
              const res = await timetableAPI.exportTeacherPDF(exportContext.teacherId, exportContext.academicYearId);
              const blob = new Blob([res.data]);
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              a.href = url; a.download = 'my_timetable.pdf'; a.click();
              URL.revokeObjectURL(url);
              toast.success('PDF downloaded!');
            } catch { toast.error('Failed to download PDF'); }
          }}>
            <FiDownload className="mr-2" /> Download PDF
          </Button>
        )}
      </div>

      {error && (
        <Card className="mb-6 bg-red-50 border border-red-200">
          <p className="text-red-700">{error}</p>
        </Card>
      )}

      {/* Timetable Grid */}
      <Card>
        {timeSlots.length === 0 ? (
          <div className="text-center py-12">
            <FiCalendar className="w-12 h-12 text-gray-300 mx-auto mb-3" />
            <p className="text-gray-500">No time slots configured. Please contact admin.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-primary-800 text-white">
                  <th className="border border-gray-300 p-3 text-left font-semibold">Time</th>
                  {DAYS.map((day) => (
                    <th key={day} className="border border-gray-300 p-3 text-center font-semibold">
                      {day}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {timeSlots.map((slot) => {
                  const slotKey = getSlotKey(slot);
                  const isBreak = slot.isBreak;

                  // ── Break row: grey banner spanning all day columns ──
                  if (isBreak) {
                    return (
                      <tr key={slot.id} className="bg-gray-100">
                        <td className="border border-gray-300 p-2 font-medium text-gray-500">
                          <div className="flex items-center gap-2">
                            <FiClock className="text-gray-400" />
                            <div>
                              <div className="text-sm">{slotKey}</div>
                              <div className="text-xs text-gray-400">{slot.slotName}</div>
                            </div>
                          </div>
                        </td>
                        <td colSpan={6} className="border border-gray-300 p-2 text-center text-gray-400 italic text-sm">
                          {slot.slotName}
                        </td>
                      </tr>
                    );
                  }

                  // ── Regular slot row ──
                  return (
                    <tr key={slot.id} className="hover:bg-gray-50">
                      <td className="border border-gray-300 p-3 font-medium text-gray-700 bg-gray-50">
                        <div className="flex items-center gap-2">
                          <FiClock className="text-primary-800" />
                          <div>
                            <div>{slotKey}</div>
                            <div className="text-xs text-gray-500">{slot.slotName}</div>
                          </div>
                        </div>
                      </td>
                      {DAYS.map((day) => {
                        const dayEnum = DAY_ENUM[day];
                        const entry = timetableLookup[dayEnum]?.[slot.id];
                        const isLab = entry?.course?.courseType === 'LAB';

                        return (
                          <td key={`${day}-${slot.id}`} className="border border-gray-300 p-2">
                            {entry ? (
                              <div className={`p-3 rounded-lg ${
                                isLab
                                  ? 'bg-gradient-to-br from-purple-50 to-purple-100'
                                  : 'bg-gradient-to-br from-primary-50 to-primary-100'
                              }`}>
                                <p className={`font-semibold mb-1 ${
                                  isLab ? 'text-purple-900' : 'text-primary-900'
                                }`}>
                                  {entry.course?.name || 'Unknown'}
                                </p>
                                <div className="flex items-center gap-2 text-xs text-gray-600 mb-1">
                                  <FiMapPin size={12} />
                                  <span>{entry.room?.roomNumber || '-'}</span>
                                </div>
                                <div className="flex gap-1 flex-wrap">
                                  <span className={`inline-block px-2 py-1 text-white text-xs rounded ${
                                    isLab ? 'bg-purple-700' : 'bg-primary-800'
                                  }`}>
                                    {entry.division?.name || '-'}
                                  </span>
                                  {isLab && (
                                    <span className="inline-block px-2 py-1 bg-purple-700 text-white text-xs rounded">
                                      Lab
                                    </span>
                                  )}
                                </div>
                              </div>
                            ) : (
                              <div className="text-center text-gray-400 py-4">-</div>
                            )}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      {/* Summary */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-6">
        <Card>
          <div className="text-center">
            <FiCalendar className="w-12 h-12 text-primary-800 mx-auto mb-3" />
            <p className="text-3xl font-bold text-gray-900 mb-1">{totalClasses}</p>
            <p className="text-gray-600">Total Classes/Week</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <FiClock className="w-12 h-12 text-green-600 mx-auto mb-3" />
            <p className="text-3xl font-bold text-gray-900 mb-1">{Math.round(totalHours)}</p>
            <p className="text-gray-600">Hours/Week</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <FiMapPin className="w-12 h-12 text-orange-600 mx-auto mb-3" />
            <p className="text-3xl font-bold text-gray-900 mb-1">{uniqueRooms}</p>
            <p className="text-gray-600">Different Rooms</p>
          </div>
        </Card>
      </div>
    </div>
  );
};
