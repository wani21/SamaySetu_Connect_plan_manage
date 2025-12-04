import React, { useState, useEffect } from 'react';
import { FiCheck, FiX, FiSave, FiBook } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../../components/common/Card';
import { Button } from '../../components/common/Button';
import { timeSlotPublicAPI } from '../../services/api';

export const AvailabilityPage: React.FC = () => {
  const days = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
  
  const [timeSlots, setTimeSlots] = useState<any[]>([]);
  const [availability, setAvailability] = useState<any>({});
  const [isLoading, setIsLoading] = useState(false);
  const [isFetchingSlots, setIsFetchingSlots] = useState(true);

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
      
      // Initialize availability based on timetable
      initializeAvailability(classSlots);
    } catch (error) {
      console.error('Failed to fetch time slots:', error);
      setTimeSlots([]);
    } finally {
      setIsFetchingSlots(false);
    }
  };

  const formatTime = (time: string) => {
    if (!time) return '';
    return time.substring(0, 5); // Convert "HH:MM:SS" to "HH:MM"
  };

  const getSlotKey = (slot: any) => {
    return `${formatTime(slot.startTime)} - ${formatTime(slot.endTime)}`;
  };

  // Sample timetable - same as TimetablePage (should be fetched from API in production)
  const getTimetable = () => {
    if (timeSlots.length === 0) return {};
    
    const timetable: any = {};
    
    // Regular lectures (1 hour each)
    if (timeSlots.length > 0) {
      const slot1 = getSlotKey(timeSlots[0]);
      timetable.Monday = { [slot1]: { subject: 'Data Structures', type: 'lecture' } };
      timetable.Thursday = { [slot1]: { subject: 'Data Structures', type: 'lecture' } };
    }
    
    if (timeSlots.length > 1) {
      const slot2 = getSlotKey(timeSlots[1]);
      timetable.Monday = { ...timetable.Monday, [slot2]: { subject: 'Algorithms', type: 'lecture' } };
      timetable.Wednesday = { [slot2]: { subject: 'Software Engineering', type: 'lecture' } };
      timetable.Friday = { [slot2]: { subject: 'Operating Systems', type: 'lecture' } };
    }
    
    if (timeSlots.length > 2) {
      const slot3 = getSlotKey(timeSlots[2]);
      timetable.Tuesday = { [slot3]: { subject: 'Computer Networks', type: 'lecture' } };
      timetable.Thursday = { ...timetable.Thursday, [slot3]: { subject: 'Algorithms', type: 'lecture' } };
    }
    
    // Labs (2 hours - spanning 2 consecutive slots)
    if (timeSlots.length > 4) {
      const slot5 = getSlotKey(timeSlots[4]);
      const slot6 = timeSlots.length > 5 ? getSlotKey(timeSlots[5]) : null;
      
      // Regular lectures
      timetable.Monday = { ...timetable.Monday, [slot5]: { subject: 'Database Systems', type: 'lecture' } };
      timetable.Wednesday = { ...timetable.Wednesday, [slot5]: { subject: 'Web Technologies', type: 'lecture' } };
      timetable.Thursday = { ...timetable.Thursday, [slot5]: { subject: 'Database Systems', type: 'lecture' } };
      
      // Labs spanning 2 hours
      timetable.Tuesday = { 
        ...timetable.Tuesday, 
        [slot5]: { subject: 'Data Structures Lab', type: 'lab' }
      };
      if (slot6) {
        timetable.Tuesday = { ...timetable.Tuesday, [slot6]: 'SKIP' }; // Mark next slot as occupied
      }
      
      timetable.Friday = { 
        ...timetable.Friday, 
        [slot5]: { subject: 'Database Lab', type: 'lab' }
      };
      if (slot6) {
        timetable.Friday = { ...timetable.Friday, [slot6]: 'SKIP' }; // Mark next slot as occupied
      }
    }
    
    if (timeSlots.length > 6) {
      const slot7 = getSlotKey(timeSlots[6]);
      timetable.Wednesday = { ...timetable.Wednesday, [slot7]: { subject: 'Project Work', type: 'lecture' } };
    }
    
    if (timeSlots.length > 0) {
      const slot1 = getSlotKey(timeSlots[0]);
      timetable.Saturday = { [slot1]: { subject: 'Seminar', type: 'lecture' } };
    }
    
    return timetable;
  };

  const timetable = getTimetable();

  const initializeAvailability = (slots: any[]) => {
    // Initialize all slots as available by default
    const initialAvailability: any = {};
    days.forEach((day) => {
      slots.forEach((slot) => {
        const slotKey = getSlotKey(slot);
        initialAvailability[`${day}-${slotKey}`] = true;
      });
    });
    setAvailability(initialAvailability);
  };

  const hasClass = (day: string, slotKey: string) => {
    const classData = timetable[day]?.[slotKey];
    return classData !== undefined && classData !== 'SKIP';
  };

  const getClassInfo = (day: string, slotKey: string) => {
    const classData = timetable[day]?.[slotKey];
    return classData === 'SKIP' ? null : classData;
  };

  const isSkippedSlot = (day: string, slotKey: string) => {
    return timetable[day]?.[slotKey] === 'SKIP';
  };

  const toggleAvailability = (day: string, slotKey: string) => {
    // Don't allow toggling if there's a class scheduled
    if (hasClass(day, slotKey)) {
      toast.error('Cannot change availability - you have a class scheduled at this time');
      return;
    }
    
    const key = `${day}-${slotKey}`;
    setAvailability((prev: any) => ({
      ...prev,
      [key]: !prev[key],
    }));
  };

  const handleSave = async () => {
    setIsLoading(true);
    try {
      // API call would go here
      await new Promise((resolve) => setTimeout(resolve, 1000));
      toast.success('Availability updated successfully!');
    } catch (error) {
      toast.error('Failed to update availability');
    } finally {
      setIsLoading(false);
    }
  };

  const isAvailable = (day: string, slotKey: string) => {
    return availability[`${day}-${slotKey}`] || false;
  };

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
          <div className="text-blue-600 text-2xl">ℹ️</div>
          <div>
            <p className="font-medium text-blue-900 mb-1">How to use:</p>
            <ul className="text-sm text-blue-800 space-y-1">
              <li>• <span className="font-semibold text-blue-900">Blue slots</span> - You have a class scheduled (cannot be changed)</li>
              <li>• <span className="font-semibold text-green-700">Green slots</span> - You are available for classes</li>
              <li>• <span className="font-semibold text-red-700">Red slots</span> - You are not available</li>
              <li>• Click on green/red slots to toggle your availability</li>
              <li>• Don't forget to save your changes!</li>
            </ul>
          </div>
        </div>
      </Card>

      {/* Availability Grid */}
      <Card>
        {isFetchingSlots ? (
          <div className="text-center py-12">
            <p className="text-gray-500">Loading time slots...</p>
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
                    <tr key={slot.id}>
                      <td className="border border-gray-300 p-3 font-medium text-gray-700 bg-gray-50">
                        <div>
                          <div>{slotKey}</div>
                          <div className="text-xs text-gray-500">{slot.slotName}</div>
                        </div>
                      </td>
                      {days.map((day) => {
                        const skipped = isSkippedSlot(day, slotKey);
                        
                        // Don't render cell if it's part of a multi-hour class
                        if (skipped) {
                          return null;
                        }
                        
                        const hasScheduledClass = hasClass(day, slotKey);
                        const classInfo = getClassInfo(day, slotKey);
                        const available = isAvailable(day, slotKey);
                        const isLab = classInfo?.type === 'lab';
                        
                        return (
                          <td 
                            key={`${day}-${slot.id}`} 
                            className="border border-gray-300 p-2"
                            rowSpan={isLab ? 2 : 1}
                          >
                            {hasScheduledClass ? (
                              // Show scheduled class - cannot be changed
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
                                    {classInfo?.subject}
                                  </span>
                                  <span className={`text-xs ${
                                    isLab ? 'text-purple-600' : 'text-blue-600'
                                  }`}>
                                    {isLab ? '🔬 Lab' : 'Scheduled'}
                                  </span>
                                </div>
                              </div>
                            ) : (
                              // Allow toggling availability
                              <button
                                onClick={() => toggleAvailability(day, slotKey)}
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
            const allAvailable: any = {};
            days.forEach((day) => {
              timeSlots.forEach((slot) => {
                const slotKey = getSlotKey(slot);
                // Only mark as available if no class is scheduled
                if (!hasClass(day, slotKey)) {
                  allAvailable[`${day}-${slotKey}`] = true;
                }
              });
            });
            setAvailability((prev: any) => ({ ...prev, ...allAvailable }));
            toast.success('Marked all free slots as available');
          }}
          className="w-full"
        >
          Mark All Free Slots Available
        </Button>
        <Button
          variant="outline"
          onClick={() => {
            const allUnavailable: any = {};
            days.forEach((day) => {
              timeSlots.forEach((slot) => {
                const slotKey = getSlotKey(slot);
                // Only mark as unavailable if no class is scheduled
                if (!hasClass(day, slotKey)) {
                  allUnavailable[`${day}-${slotKey}`] = false;
                }
              });
            });
            setAvailability((prev: any) => ({ ...prev, ...allUnavailable }));
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
