import React, { useState, useEffect } from 'react';
import { FiCalendar, FiClock, FiBook, FiCheckCircle } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { StatsCard } from '../components/dashboard/StatsCard';
import { Card } from '../components/common/Card';
import { Loading } from '../components/common/Loading';
import { useAuthStore } from '../store/authStore';
import { teacherAPI, timetableAPI, academicYearPublicAPI, timeSlotPublicAPI } from '../services/api';
import { TimeSlot } from '../types';

// Map backend DayOfWeek enum to display names
const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
const DAY_LABELS_ARR = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday', FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday',
};

export const DashboardPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const navigate = useNavigate();
  const [teacherData, setTeacherData] = useState<any>(null);
  const [timetableEntries, setTimetableEntries] = useState<any[]>([]);
  const [deptEntries, setDeptEntries] = useState<any[]>([]);
  const [activeTab, setActiveTab] = useState<'MY_SCHEDULE' | 'DEPT_SCHEDULE'>('MY_SCHEDULE');
  const [timeSlots, setTimeSlots] = useState<TimeSlot[]>([]);
  const [selectedDivisionId, setSelectedDivisionId] = useState<number | null>(null);
  const [selectedSemester, setSelectedSemester] = useState<string>('SEM_3');
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);

        // 1. Get current teacher's profile
        const profileRes = await teacherAPI.getProfile();
        const teacher = profileRes.data;
        setTeacherData(teacher);

        // 2. Get current academic year
        const yearsRes = await academicYearPublicAPI.getAll();
        const years = Array.isArray(yearsRes.data) ? yearsRes.data : [];
        const currentYear = years.find((y: any) => y.isActive || y.isCurrent);

        // 3. Get time slots
        const slotsRes = await timeSlotPublicAPI.getAll();
        const slots = Array.isArray(slotsRes.data) ? slotsRes.data : [];

        let myEntries: any[] = [];
        let dEntries: any[] = [];

        if (teacher?.id && currentYear?.id) {
          // 4. Fetch this teacher's published timetable
          try {
            const ttRes = await timetableAPI.getMyTimetable(currentYear.id);
            myEntries = Array.isArray(ttRes.data) ? ttRes.data : [];
            setTimetableEntries(myEntries);
          } catch {
            setTimetableEntries([]);
          }

          // 5. Fetch department-wide published timetable
          if (teacher?.department?.id) {
            try {
              const deptTTRes = await timetableAPI.getFacultyDepartmentTimetable(currentYear.id, selectedSemester);
              dEntries = Array.isArray(deptTTRes.data) ? deptTTRes.data : [];
              setDeptEntries(dEntries);
              
              // Select first division by default
              const divIds = Array.from(new Set(dEntries.map(e => e.division?.id).filter(Boolean)));
              if (divIds.length > 0) {
                setSelectedDivisionId(divIds[0] as number);
              } else {
                setSelectedDivisionId(null);
              }
            } catch {
              setDeptEntries([]);
            }
          }
        }

        // Detect slot type from entries (teacher's own or department entries)
        let detectedType = 'TYPE_1';
        const allEntries = [...myEntries, ...dEntries];
        for (const entry of allEntries) {
          const type = entry.timeSlot?.type;
          if (type) {
            detectedType = type;
            break;
          }
        }

        // Filter and sort time slots of detected type
        const sortedSlots = slots
          .filter((s: TimeSlot) => s.type === detectedType || (!s.type && detectedType === 'TYPE_1'))
          .sort((a: TimeSlot, b: TimeSlot) => a.startTime.localeCompare(b.startTime));
        setTimeSlots(sortedSlots);
      } catch (error) {
        if (import.meta.env.DEV) console.error('Error fetching dashboard data:', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (user?.email) {
      fetchData();
    }
  }, [user?.email, selectedSemester]);

  // Derive today's and upcoming classes from real timetable data
  const getCurrentDay = () => {
    const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    return days[new Date().getDay()];
  };

  const todayEntries = timetableEntries
    .filter((e: any) => e.dayOfWeek === getCurrentDay())
    .sort((a: any, b: any) => (a.timeSlot?.startTime || '').localeCompare(b.timeSlot?.startTime || ''));

  const upcomingEntries = timetableEntries
    .filter((e: any) => {
      const dayIndex = DAY_ORDER.indexOf(e.dayOfWeek);
      const todayIndex = DAY_ORDER.indexOf(getCurrentDay());
      return dayIndex > todayIndex;
    })
    .sort((a: any, b: any) => {
      const dayDiff = DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek);
      if (dayDiff !== 0) return dayDiff;
      return (a.timeSlot?.startTime || '').localeCompare(b.timeSlot?.startTime || '');
    })
    .slice(0, 6);

  // Compute real stats
  const totalClasses = timetableEntries.length;
  const uniqueDays = new Set(timetableEntries.map((e: any) => e.dayOfWeek)).size;
  const totalHours = timetableEntries.reduce((sum: number, e: any) => {
    if (e.timeSlot?.durationMinutes) return sum + e.timeSlot.durationMinutes / 60;
    return sum + 1;
  }, 0);

  const formatTime = (time: string) => time ? time.substring(0, 5) : '';
  const getTimeRange = (entry: any) =>
    `${formatTime(entry.timeSlot?.startTime)} - ${formatTime(entry.timeSlot?.endTime)}`;

  const getUserName = () => {
    if (teacherData?.name) return teacherData.name;
    if (user?.name) return user.name;
    return 'Professor';
  };

  const formatName = (name: string) =>
    name.split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');

  // Department timetable filtering
  const getDeptDivisions = () => {
    const divsMap = new Map<number, string>();
    deptEntries.forEach(e => {
      if (e.division?.id && e.division?.name) {
        divsMap.set(e.division.id, e.division.name);
      }
    });
    return Array.from(divsMap.entries()).map(([id, name]) => ({ id, name }));
  };

  const getFilteredDeptEntries = () => {
    if (!selectedDivisionId) return [];
    return deptEntries.filter(e => e.division?.id === selectedDivisionId);
  };

  const getDeptEntryForCell = (day: string, slotId: number) => {
    return getFilteredDeptEntries().filter(e => e.dayOfWeek === day && e.timeSlot?.id === slotId);
  };

  if (isLoading) {
    return <Loading />;
  }

  return (
    <div className="space-y-6">
      {/* Welcome & Tabs Header */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-800 tracking-tight">
            Welcome back, {formatName(getUserName())}!
          </h1>
          <p className="text-gray-500 mt-1">Here is your academic timetable planner and department tracker.</p>
        </div>

        {/* Tab Switcher */}
        <div className="flex bg-gray-100 p-1.5 rounded-xl border border-gray-200">
          <button
            onClick={() => setActiveTab('MY_SCHEDULE')}
            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${
              activeTab === 'MY_SCHEDULE'
                ? 'bg-primary-800 text-white shadow-sm'
                : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            My Schedule
          </button>
          <button
            onClick={() => setActiveTab('DEPT_SCHEDULE')}
            className={`px-4 py-2 rounded-lg text-sm font-bold transition-all ${
              activeTab === 'DEPT_SCHEDULE'
                ? 'bg-primary-800 text-white shadow-sm'
                : 'text-gray-600 hover:text-gray-800'
            }`}
          >
            Department Timetable
          </button>
        </div>
      </div>

      {activeTab === 'MY_SCHEDULE' ? (
        <>
          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <StatsCard title="Total Classes" value={totalClasses} icon={FiBook} color="blue" />
            <StatsCard title="Teaching Days" value={uniqueDays} icon={FiCalendar} color="green" />
            <StatsCard title="Hours/Week" value={Math.round(totalHours)} icon={FiClock} color="orange" />
            <StatsCard
              title="Max Hours"
              value={teacherData?.maxWeeklyHours || '-'}
              icon={FiCheckCircle}
              color="purple"
            />
          </div>

          {/* Content Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Today's Schedule */}
            <Card>
              <h3 className="text-lg font-bold text-gray-800 mb-4">Today's Schedule - {DAY_LABELS[getCurrentDay()] || getCurrentDay()}</h3>
              {todayEntries.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <FiCalendar className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                  <p>No classes scheduled for today</p>
                </div>
              ) : (
                <div className="space-y-3">
                  {todayEntries.map((entry: any, index: number) => {
                    const isLab = entry.course?.courseType === 'LAB';
                    return (
                      <div
                        key={entry.id || index}
                        className={`p-4 rounded-xl border-l-4 shadow-xs transition-all hover:scale-[1.01] ${
                          isLab ? 'bg-purple-50 border-purple-500 text-purple-900' : 'bg-blue-50 border-blue-500 text-blue-900'
                        }`}
                      >
                        <div className="flex items-start justify-between">
                          <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <FiClock className="text-gray-500" size={14} />
                              <span className="text-xs font-semibold text-gray-600">{getTimeRange(entry)}</span>
                              {isLab && (
                                <span className="px-2 py-0.5 bg-purple-200 text-purple-800 text-[10px] font-bold rounded-full uppercase">Lab</span>
                              )}
                            </div>
                            <p className="font-bold text-gray-900 mb-1">
                              {entry.course?.name || 'Unknown Course'}
                            </p>
                            <div className="flex items-center gap-3 text-xs text-gray-500 font-semibold">
                              <span>Room: {entry.room?.roomNumber || '-'}</span>
                              <span>Div: {entry.division?.name || '-'}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </Card>

            {/* Quick Actions */}
            <Card>
              <h3 className="text-lg font-bold text-gray-800 mb-4">Quick Actions</h3>
              <div className="grid grid-cols-2 gap-4">
                {[
                  { label: 'View Timetable', icon: FiCalendar, color: 'bg-blue-50 text-blue-700 hover:bg-blue-100 border border-blue-200', path: '/dashboard/timetable' },
                  { label: 'Availability Slots', icon: FiClock, color: 'bg-green-50 text-green-700 hover:bg-green-100 border border-green-200', path: '/dashboard/availability' },
                  { label: 'My Courses', icon: FiBook, color: 'bg-purple-50 text-purple-700 hover:bg-purple-100 border border-purple-200', path: '/dashboard/timetable' },
                  { label: 'Profile Settings', icon: FiCheckCircle, color: 'bg-orange-50 text-orange-700 hover:bg-orange-100 border border-orange-200', path: '/dashboard/profile' },
                ].map((action, index) => (
                  <button
                    key={index}
                    onClick={() => navigate(action.path)}
                    className={`${action.color} p-5 rounded-2xl transition-all text-left flex flex-col gap-2`}
                  >
                    <action.icon className="w-6 h-6" />
                    <p className="font-bold text-xs leading-tight">{action.label}</p>
                  </button>
                ))}
              </div>
            </Card>
          </div>

          {/* Upcoming Classes */}
          <Card>
            <h3 className="text-lg font-bold text-gray-800 mb-4">Upcoming This Week</h3>
            {upcomingEntries.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <p>No upcoming classes this week</p>
              </div>
            ) : (
              <div className="overflow-x-auto border border-gray-100 rounded-xl">
                <table className="w-full text-left border-collapse text-xs">
                  <thead>
                    <tr className="bg-gray-50 border-b border-gray-200">
                      <th className="py-3 px-4 font-bold text-gray-600">Day</th>
                      <th className="py-3 px-4 font-bold text-gray-600">Time</th>
                      <th className="py-3 px-4 font-bold text-gray-600">Subject</th>
                      <th className="py-3 px-4 font-bold text-gray-600">Room</th>
                      <th className="py-3 px-4 font-bold text-gray-600">Division</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    {upcomingEntries.map((entry: any, index: number) => (
                      <tr key={entry.id || index} className="hover:bg-gray-50 transition-colors">
                        <td className="py-3 px-4">
                          <span className="inline-block px-2 py-1 bg-primary-100 text-primary-800 rounded font-bold uppercase scale-90">
                            {DAY_LABELS[entry.dayOfWeek] || entry.dayOfWeek}
                          </span>
                        </td>
                        <td className="py-3 px-4 text-gray-700 font-semibold">
                          <div className="flex items-center gap-2">
                            <FiClock size={14} className="text-gray-400" />
                            {getTimeRange(entry)}
                          </div>
                        </td>
                        <td className="py-3 px-4 font-bold text-gray-900">
                          {entry.course?.name || 'Unknown'}
                        </td>
                        <td className="py-3 px-4">
                          <span className="px-1.5 py-0.5 bg-gray-100 rounded font-bold text-gray-600">
                            {entry.room?.roomNumber || '-'}
                          </span>
                        </td>
                        <td className="py-3 px-4">
                          <span className="inline-block px-2 py-1 bg-blue-100 text-blue-800 rounded font-bold uppercase scale-90">
                            {entry.division?.name || '-'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      ) : (
        /* Read-Only Department Timetable Tab */
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col gap-6">
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
            <div>
              <h2 className="text-xl font-bold text-gray-800">Department Divisions</h2>
              <p className="text-gray-500 text-xs mt-0.5">Select a division to inspect its active published weekly timetable schedule.</p>
            </div>
            
            <div className="flex flex-wrap gap-3">
              {/* Semester Selector */}
              <select
                value={selectedSemester}
                onChange={(e) => setSelectedSemester(e.target.value)}
                className="px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:border-primary-500 text-sm font-semibold"
              >
                <option value="SEM_1">Semester 1</option>
                <option value="SEM_2">Semester 2</option>
                <option value="SEM_3">Semester 3</option>
                <option value="SEM_4">Semester 4</option>
                <option value="SEM_5">Semester 5</option>
                <option value="SEM_6">Semester 6</option>
                <option value="SEM_7">Semester 7</option>
                <option value="SEM_8">Semester 8</option>
              </select>

              {/* Division Selector */}
              <select
                value={selectedDivisionId || ''}
                onChange={(e) => setSelectedDivisionId(Number(e.target.value))}
                disabled={getDeptDivisions().length === 0}
                className="px-4 py-2 border border-gray-200 rounded-xl focus:outline-none focus:border-primary-500 text-sm font-semibold disabled:bg-gray-50"
              >
                {getDeptDivisions().length === 0 ? (
                  <option>No division schedule published</option>
                ) : (
                  getDeptDivisions().map(div => (
                    <option key={div.id} value={div.id}>Division: {div.name}</option>
                  ))
                )}
              </select>
            </div>
          </div>

          {!selectedDivisionId ? (
            <p className="text-center py-12 text-gray-400 text-sm italic">
              No published timetable entries found for this department's divisions yet.
            </p>
          ) : (
            <div className="overflow-x-auto border border-gray-200 rounded-2xl">
              <table className="w-full border-collapse text-sm">
                <thead>
                  <tr className="bg-gray-50 border-b border-gray-200">
                    <th className="px-4 py-3 text-left font-bold text-gray-600 w-32 border-r border-gray-200">Time</th>
                    {DAY_LABELS_ARR.map((day, idx) => (
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

                      {DAY_ORDER.map((day) => {
                        const cellEntries = getDeptEntryForCell(day, slot.id);
                        return (
                          <td key={day} className="p-2 border-r border-gray-200 align-top min-h-[80px]">
                            {cellEntries.length === 0 ? (
                              <div className="h-full flex items-center justify-center py-4 text-gray-300 font-medium italic text-xs">
                                -
                              </div>
                            ) : (
                              cellEntries.map((entry) => {
                                const isLab = entry.isLabSession || entry.course?.courseType === 'LAB';
                                return (
                                  <div
                                    key={entry.id}
                                    className={`p-2.5 rounded-lg border text-xs flex flex-col gap-1 shadow-xs ${
                                      isLab
                                        ? 'bg-purple-50 border-purple-200 text-purple-800'
                                        : 'bg-blue-50 border-blue-200 text-blue-800'
                                    }`}
                                  >
                                    <div className="font-bold truncate">{entry.course?.name || 'Course'}</div>
                                    <div className="flex justify-between items-center text-[10px] text-gray-500 font-semibold">
                                      <span>Fac: {entry.teacher?.name || '-'}</span>
                                      <span className="px-1 bg-white border border-gray-200 rounded font-bold">
                                        Rm: {entry.room?.roomNumber || '-'}
                                      </span>
                                    </div>
                                    {entry.batch && (
                                      <div className="text-[10px] text-purple-700 font-bold bg-white px-1 py-0.5 rounded border border-purple-200 w-max mt-0.5">
                                        Batch: {entry.batch.name}
                                      </div>
                                    )}
                                  </div>
                                );
                              })
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
        </div>
      )}
    </div>
  );
};
