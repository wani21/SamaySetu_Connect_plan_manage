import React, { useState, useEffect } from 'react';
import { FiCalendar, FiClock, FiMapPin } from 'react-icons/fi';
import { Card } from '../../components/common/Card';
import { timeSlotPublicAPI } from '../../services/api';

export const TimetablePage: React.FC = () => {
  const [selectedWeek, setSelectedWeek] = useState('current');
  const [timeSlots, setTimeSlots] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

  useEffect(() => {
    fetchTimeSlots();
  }, []);

  const fetchTimeSlots = async () => {
    try {
      const response = await timeSlotPublicAPI.getAll();
      const slots = Array.isArray(response.data) ? response.data : [];
      // Filter out break times and sort by start time
      const classSlots = slots
        .filter((slot: any) => !slot.isBreak)
        .sort((a: any, b: any) => a.startTime.localeCompare(b.startTime));
      setTimeSlots(classSlots);
    } catch (error) {
      console.error('Failed to fetch time slots:', error);
      setTimeSlots([]);
    } finally {
      setIsLoading(false);
    }
  };

  const formatTime = (time: string) => {
    if (!time) return '';
    return time.substring(0, 5); // Convert "HH:MM:SS" to "HH:MM"
  };

  const getSlotKey = (slot: any) => {
    return `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;
  };

  // Sample timetable data - will be replaced with API data later
  // Format: { day: { slotKey: { subject, room, division, year, type, duration } } }
  // type: 'lecture' (1 hour) or 'lab' (2 hours)
  // duration: number of consecutive slots
  const getSampleTimetable = () => {
    if (timeSlots.length === 0) return {};
    
    const timetable: any = {};
    
    // Regular lectures (1 hour each)
    if (timeSlots.length > 0) {
      const slot1 = getSlotKey(timeSlots[0]);
      timetable.Monday = { [slot1]: { subject: 'Data Structures', room: 'H202', division: 'A', year: 'SY', type: 'lecture', duration: 1 } };
      timetable.Thursday = { [slot1]: { subject: 'Data Structures', room: 'H202', division: 'B', year: 'SY', type: 'lecture', duration: 1 } };
    }
    
    if (timeSlots.length > 1) {
      const slot2 = getSlotKey(timeSlots[1]);
      timetable.Monday = { ...timetable.Monday, [slot2]: { subject: 'Algorithms', room: 'H203', division: 'B', year: 'TY', type: 'lecture', duration: 1 } };
      timetable.Wednesday = { [slot2]: { subject: 'Software Engineering', room: 'H204', division: 'B', year: 'TY', type: 'lecture', duration: 1 } };
      timetable.Friday = { [slot2]: { subject: 'Operating Systems', room: 'H205', division: 'A', year: 'TY', type: 'lecture', duration: 1 } };
    }
    
    if (timeSlots.length > 2) {
      const slot3 = getSlotKey(timeSlots[2]);
      timetable.Tuesday = { [slot3]: { subject: 'Computer Networks', room: 'H206', division: 'A', year: 'BTech', type: 'lecture', duration: 1 } };
      timetable.Thursday = { ...timetable.Thursday, [slot3]: { subject: 'Algorithms', room: 'H203', division: 'A', year: 'TY', type: 'lecture', duration: 1 } };
    }
    
    // Labs (2 hours - spanning 2 consecutive slots)
    if (timeSlots.length > 4) {
      const slot5 = getSlotKey(timeSlots[4]);
      const slot6 = timeSlots.length > 5 ? getSlotKey(timeSlots[5]) : null;
      
      // Regular lectures
      timetable.Monday = { ...timetable.Monday, [slot5]: { subject: 'Database Systems', room: 'H207', division: 'A', year: 'TY', type: 'lecture', duration: 1 } };
      timetable.Wednesday = { ...timetable.Wednesday, [slot5]: { subject: 'Web Technologies', room: 'H208', division: 'A', year: 'BTech', type: 'lecture', duration: 1 } };
      timetable.Thursday = { ...timetable.Thursday, [slot5]: { subject: 'Database Systems', room: 'H207', division: 'C', year: 'TY', type: 'lecture', duration: 1 } };
      
      // Labs spanning 2 hours
      timetable.Tuesday = { 
        ...timetable.Tuesday, 
        [slot5]: { subject: 'Data Structures Lab', room: 'Lab-1', division: 'A', year: 'SY', type: 'lab', duration: 2 }
      };
      if (slot6) {
        timetable.Tuesday = { ...timetable.Tuesday, [slot6]: 'SKIP' }; // Mark next slot as occupied
      }
      
      timetable.Friday = { 
        ...timetable.Friday, 
        [slot5]: { subject: 'Database Lab', room: 'Lab-2', division: 'B', year: 'TY', type: 'lab', duration: 2 }
      };
      if (slot6) {
        timetable.Friday = { ...timetable.Friday, [slot6]: 'SKIP' }; // Mark next slot as occupied
      }
    }
    
    if (timeSlots.length > 6) {
      const slot7 = getSlotKey(timeSlots[6]);
      timetable.Wednesday = { ...timetable.Wednesday, [slot7]: { subject: 'Project Work', room: 'H209', division: 'All', year: 'BTech', type: 'lecture', duration: 1 } };
    }
    
    if (timeSlots.length > 0) {
      const slot1 = getSlotKey(timeSlots[0]);
      timetable.Saturday = { [slot1]: { subject: 'Seminar', room: 'Auditorium', division: 'All', year: 'All', type: 'lecture', duration: 1 } };
    }
    
    return timetable;
  };

  const timetable = getSampleTimetable();

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">My Timetable</h1>
        <p className="text-gray-600">View your weekly class schedule</p>
      </div>

      {/* Week Selector */}
      <div className="flex gap-4 mb-6">
        <button
          onClick={() => setSelectedWeek('previous')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            selectedWeek === 'previous'
              ? 'bg-primary-800 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-100'
          }`}
        >
          Previous Week
        </button>
        <button
          onClick={() => setSelectedWeek('current')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            selectedWeek === 'current'
              ? 'bg-primary-800 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-100'
          }`}
        >
          Current Week
        </button>
        <button
          onClick={() => setSelectedWeek('next')}
          className={`px-4 py-2 rounded-lg font-medium transition-colors ${
            selectedWeek === 'next'
              ? 'bg-primary-800 text-white'
              : 'bg-white text-gray-700 hover:bg-gray-100'
          }`}
        >
          Next Week
        </button>
      </div>

      {/* Timetable Grid */}
      <Card>
        {isLoading ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading timetable...</p>
          </div>
        ) : timeSlots.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-gray-500">No time slots configured. Please contact admin.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse">
              <thead>
                <tr className="bg-primary-800 text-white">
                  <th className="border border-gray-300 p-3 text-left font-semibold">Time</th>
                  {days.map((day) => (
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
                      {days.map((day) => {
                        const classInfo = timetable[day]?.[slotKey];
                        
                        // Skip rendering if this slot is part of a multi-hour class
                        if (classInfo === 'SKIP') {
                          return null;
                        }
                        
                        const isLab = classInfo?.type === 'lab';
                        const rowSpan = classInfo?.duration || 1;
                        
                        return (
                          <td 
                            key={`${day}-${slot.id}`} 
                            className="border border-gray-300 p-2"
                            rowSpan={rowSpan}
                          >
                            {classInfo ? (
                              <div className={`p-3 rounded-lg ${
                                isLab 
                                  ? 'bg-gradient-to-br from-purple-50 to-purple-100' 
                                  : 'bg-gradient-to-br from-primary-50 to-primary-100'
                              }`}>
                                <p className={`font-semibold mb-1 ${
                                  isLab ? 'text-purple-900' : 'text-primary-900'
                                }`}>
                                  {classInfo.subject}
                                </p>
                                <div className="flex items-center gap-2 text-xs text-gray-600 mb-1">
                                  <FiMapPin size={12} />
                                  <span>{classInfo.room}</span>
                                </div>
                                <div className="flex gap-1 flex-wrap">
                                  <span className={`inline-block px-2 py-1 text-white text-xs rounded ${
                                    isLab ? 'bg-purple-700' : 'bg-primary-800'
                                  }`}>
                                    {classInfo.year}
                                  </span>
                                  <span className={`inline-block px-2 py-1 text-white text-xs rounded ${
                                    isLab ? 'bg-purple-700' : 'bg-primary-800'
                                  }`}>
                                    Div {classInfo.division}
                                  </span>
                                  {isLab && (
                                    <span className="inline-block px-2 py-1 bg-purple-700 text-white text-xs rounded">
                                      🔬 Lab
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
            <p className="text-3xl font-bold text-gray-900 mb-1">24</p>
            <p className="text-gray-600">Total Classes/Week</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <FiClock className="w-12 h-12 text-green-600 mx-auto mb-3" />
            <p className="text-3xl font-bold text-gray-900 mb-1">16</p>
            <p className="text-gray-600">Hours/Week</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <FiMapPin className="w-12 h-12 text-orange-600 mx-auto mb-3" />
            <p className="text-3xl font-bold text-gray-900 mb-1">8</p>
            <p className="text-gray-600">Different Rooms</p>
          </div>
        </Card>
      </div>
    </div>
  );
};
