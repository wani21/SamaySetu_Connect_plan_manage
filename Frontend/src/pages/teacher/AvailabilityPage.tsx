import React, { useState, useEffect } from 'react';
import { FiCheck, FiX, FiSave, FiBook } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { Loading } from '../../components/common/Loading';
import { teacherAPI, timetableAPI, timeSlotPublicAPI, academicYearPublicAPI, staffAPI } from '../../services/api';

const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const DAY_ENUM: Record<string, string> = {
  Monday: 'MONDAY', Tuesday: 'TUESDAY', Wednesday: 'WEDNESDAY',
  Thursday: 'THURSDAY', Friday: 'FRIDAY', Saturday: 'SATURDAY',
};

export const AvailabilityPage: React.FC = () => {
  const [timeSlots, setTimeSlots] = useState<any[]>([]);
  const [timetableEntries, setTimetableEntries] = useState<any[]>([]);
  const [availability, setAvailability] = useState<Record<string, boolean>>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isFetchingData, setIsFetchingData] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setIsFetchingData(true);

      // Fetch time slots, teacher profile, and academic years in parallel
      const [slotsRes, profileRes, yearsRes] = await Promise.all([
        timeSlotPublicAPI.getAll(),
        teacherAPI.getProfile(),
        academicYearPublicAPI.getAll(),
      ]);

      const slots = Array.isArray(slotsRes.data) ? slotsRes.data : [];
      const classSlots = slots
        .filter((slot: any) => !slot.isBreak)
        .sort((a: any, b: any) => a.startTime.localeCompare(b.startTime));
      setTimeSlots(classSlots);

      const teacher = profileRes.data;
      const years = Array.isArray(yearsRes.data) ? yearsRes.data : [];
      const currentYear = years.find((y: any) => y.isCurrent);

      let entries: any[] = [];
      if (teacher?.id && currentYear?.id) {
        try {
          const ttRes = await timetableAPI.getByTeacher(teacher.id, currentYear.id);
          entries = Array.isArray(ttRes.data) ? ttRes.data : [];
        } catch {
          entries = [];
        }
      }
      setTimetableEntries(entries);

      // Initialize all slots as available by default
      initializeAvailability(classSlots);
    } catch (error) {
      if (import.meta.env.DEV) console.error('Failed to fetch availability data:', error);
      setTimeSlots([]);
    } finally {
      setIsFetchingData(false);
    }
  };

  const formatTime = (time: string) => (time ? time.substring(0, 5) : '');
  const getSlotKey = (slot: any) => `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;

  // Build timetable lookup: { "MONDAY-slotId": entry }
  const timetableLookup: Record<string, any> = {};
  timetableEntries.forEach((entry: any) => {
    const key = `${entry.dayOfWeek}-${entry.timeSlot?.id}`;
    if (key) timetableLookup[key] = entry;
  });

  const hasClass = (day: string, slotId: number) => {
    return !!timetableLookup[`${DAY_ENUM[day]}-${slotId}`];
  };

  const getClassInfo = (day: string, slotId: number) => {
    return timetableLookup[`${DAY_ENUM[day]}-${slotId}`] || null;
  };

  const initializeAvailability = (slots: any[]) => {
    const initial: Record<string, boolean> = {};
    DAYS.forEach((day) => {
      slots.forEach((slot) => {
        initial[`${day}-${slot.id}`] = true;
      });
    });
    setAvailability(initial);
  };

  const toggleAvailability = (day: string, slotId: number) => {
    if (hasClass(day, slotId)) {
      toast.error('Cannot change availability - you have a class scheduled at this time');
      return;
    }
    const key = `${day}-${slotId}`;
    setAvailability((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const isAvailable = (day: string, slotId: number) => {
    return availability[`${day}-${slotId}`] || false;
  };

  const handleSave = async () => {
    setIsLoading(true);
    try {
      // Convert availability map to backend format
      const entries: any[] = [];
      for (const slot of timeSlots) {
        for (const day of DAYS) {
          const dayEnum = DAY_ENUM[day];
          const key = `${day}-${slot.id}`;
          entries.push({
            dayOfWeek: dayEnum,
            startTime: slot.startTime,
            endTime: slot.endTime,
            isAvailable: availability[key] || false,
          });
        }
      }
      await staffAPI.saveAvailability(entries);
      toast.success('Availability updated successfully!');
    } catch {
      toast.error('Failed to update availability');
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetchingData) return <Loading />;

  return (
    <div>
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Availability Management</h1>
          <p className="text-gray-600">Mark your available time slots for scheduling</p>
        </div>
        <Button
          variant="primary"
          onClick={handleSave}
          isLoading={isLoading}
          className="flex items-center gap-2"
        >
          <FiSave /> Save Changes
        </Button>
      </div>

      {/* Instructions */}
      <Card className="mb-6 bg-blue-50 border border-blue-200">
        <div className="flex items-start gap-3">
          <div className="text-blue-600 text-2xl">i</div>
          <div>
            <p className="font-medium text-blue-900 mb-1">How to use:</p>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>- <span className="font-semibold text-blue-900">Blue slots</span> - You have a class scheduled (cannot be changed)</li>
              <li>- <span className="font-semibold text-green-700">Green slots</span> - You are available for classes</li>
              <li>- <span className="font-semibold text-red-700">Red slots</span> - You are not available</li>
              <li>- Click on green/red slots to toggle your availability</li>
              <li>- Don't forget to save your changes!</li>
            </ul>
          </div>
        </div>
      </Card>

      {/* Availability Grid */}
      <Card>
        {timeSlots.length === 0 ? (
          <div className="text-center py-12">
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
                  return (
                    <tr key={slot.id}>
                      <td className="border border-gray-300 p-3 font-medium text-gray-700 bg-gray-50">
                        <div>
                          <div>{slotKey}</div>
                          <div className="text-xs text-gray-500">{slot.slotName}</div>
                        </div>
                      </td>
                      {DAYS.map((day) => {
                        const hasScheduledClass = hasClass(day, slot.id);
                        const classInfo = getClassInfo(day, slot.id);
                        const available = isAvailable(day, slot.id);
                        const isLab = classInfo?.course?.courseType === 'LAB';

                        return (
                          <td key={`${day}-${slot.id}`} className="border border-gray-300 p-2">
                            {hasScheduledClass ? (
                              <div className={`w-full h-20 rounded-lg border-2 flex items-center justify-center cursor-not-allowed ${
                                isLab
                                  ? 'bg-purple-100 border-purple-500'
                                  : 'bg-blue-100 border-blue-500'
                              }`}>
                                <div className="text-center">
                                  <FiBook className={`w-6 h-6 mx-auto mb-1 ${
                                    isLab ? 'text-purple-600' : 'text-blue-600'
                                  }`} />
                                  <span className={`text-xs font-medium block ${
                                    isLab ? 'text-purple-700' : 'text-blue-700'
                                  }`}>
                                    {classInfo?.course?.name || 'Class'}
                                  </span>
                                  <span className={`text-xs ${
                                    isLab ? 'text-purple-600' : 'text-blue-600'
                                  }`}>
                                    {isLab ? 'Lab' : 'Scheduled'}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <button
                                onClick={() => toggleAvailability(day, slot.id)}
                                className={`w-full h-20 rounded-lg transition-all flex items-center justify-center ${
                                  available
                                    ? 'bg-green-100 hover:bg-green-200 border-2 border-green-500'
                                    : 'bg-red-100 hover:bg-red-200 border-2 border-red-500'
                                }`}
                              >
                                {available ? (
                                  <div className="text-center">
                                    <FiCheck className="w-8 h-8 text-green-600 mx-auto mb-1" />
                                    <span className="text-xs font-medium text-green-700">Available</span>
                                  </div>
                                ) : (
                                  <div className="text-center">
                                    <FiX className="w-8 h-8 text-red-600 mx-auto mb-1" />
                                    <span className="text-xs font-medium text-red-700">Not Available</span>
                                  </div>
                                )}
                              </button>
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

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-6">
        <Button
          variant="outline"
          onClick={() => {
            const allAvailable: Record<string, boolean> = {};
            DAYS.forEach((day) => {
              timeSlots.forEach((slot) => {
                if (!hasClass(day, slot.id)) {
                  allAvailable[`${day}-${slot.id}`] = true;
                }
              });
            });
            setAvailability((prev) => ({ ...prev, ...allAvailable }));
            toast.success('Marked all free slots as available');
          }}
          className="w-full"
        >
          Mark All Free Slots Available
        </Button>
        <Button
          variant="outline"
          onClick={() => {
            const allUnavailable: Record<string, boolean> = {};
            DAYS.forEach((day) => {
              timeSlots.forEach((slot) => {
                if (!hasClass(day, slot.id)) {
                  allUnavailable[`${day}-${slot.id}`] = false;
                }
              });
            });
            setAvailability((prev) => ({ ...prev, ...allUnavailable }));
            toast.success('Marked all free slots as unavailable');
          }}
          className="w-full"
        >
          Mark All Free Slots Unavailable
        </Button>
      </div>
    </div>
  );
};
