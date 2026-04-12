import React, { useState, useEffect } from 'react';
import { FiCalendar, FiClock, FiBook, FiCheckCircle } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { StatsCard } from '../components/dashboard/StatsCard';
import { Card } from '../components/common/Card';
import { Loading } from '../components/common/Loading';
import { useAuthStore } from '../store/authStore';
import { teacherAPI, timetableAPI, academicYearPublicAPI } from '../services/api';

// Map backend DayOfWeek enum to display names
const DAY_ORDER = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
const DAY_LABELS: Record<string, string> = {
  MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday',
  THURSDAY: 'Thursday', FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday',
};

export const DashboardPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const navigate = useNavigate();
  const [teacherData, setTeacherData] = useState<any>(null);
  const [timetableEntries, setTimetableEntries] = useState<any[]>([]);
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
        const currentYear = years.find((y: any) => y.isCurrent);

        if (teacher?.id && currentYear?.id) {
          // 3. Fetch this teacher's published timetable
          try {
            const ttRes = await timetableAPI.getByTeacher(teacher.id, currentYear.id);
            setTimetableEntries(Array.isArray(ttRes.data) ? ttRes.data : []);
          } catch {
            // No published timetable yet — that's fine
            setTimetableEntries([]);
          }
        }
      } catch (error) {
        if (import.meta.env.DEV) console.error('Error fetching dashboard data:', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (user?.email) {
      fetchData();
    }
  }, [user?.email]);

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
      return dayIndex > todayIndex; // Only future days this week
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
    return sum + 1; // default 1 hour per class
  }, 0);

  const formatTime = (time: string) => time ? time.substring(0, 5) : '';
  const getTimeRange = (entry: any) =>
    `${formatTime(entry.timeSlot?.startTime)} - ${formatTime(entry.timeSlot?.endTime)}`;

  const getUserName = () => {
    if (teacherData?.name) return teacherData.name;
    if (user?.name) return user.name;
    if (user?.email) return user.email.split('@')[0].replace(/\./g, ' ').replace(/\d+/g, '').trim();
    return 'User';
  };

  const formatName = (name: string) =>
    name.split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()).join(' ');

  if (isLoading) {
    return <Loading />;
  }

  return (
    <div>
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Welcome back, {formatName(getUserName())}!
        </h1>
        <p className="text-gray-600">Here's what's happening with your schedule today.</p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
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
          <h3 className="card-header">Today's Schedule - {DAY_LABELS[getCurrentDay()] || getCurrentDay()}</h3>
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
                    className={`p-4 rounded-lg border-l-4 ${
                      isLab ? 'bg-purple-50 border-purple-500' : 'bg-blue-50 border-blue-500'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <FiClock className="text-gray-600" size={14} />
                          <span className="text-sm font-medium text-gray-700">{getTimeRange(entry)}</span>
                          {isLab && (
                            <span className="px-2 py-0.5 bg-purple-200 text-purple-800 text-xs rounded-full">Lab</span>
                          )}
                        </div>
                        <p className="font-semibold text-gray-900 mb-1">
                          {entry.course?.name || 'Unknown Course'}
                        </p>
                        <div className="flex items-center gap-3 text-xs text-gray-600">
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
          <h3 className="card-header">Quick Actions</h3>
          <div className="grid grid-cols-2 gap-4">
            {[
              { label: 'View Timetable', icon: FiCalendar, color: 'bg-blue-50 text-blue-700 hover:bg-blue-100', path: '/dashboard/timetable' },
              { label: 'Availability', icon: FiClock, color: 'bg-green-50 text-green-700 hover:bg-green-100', path: '/dashboard/availability' },
              { label: 'My Courses', icon: FiBook, color: 'bg-purple-50 text-purple-700 hover:bg-purple-100', path: '/dashboard/timetable' },
              { label: 'Profile', icon: FiCheckCircle, color: 'bg-orange-50 text-orange-700 hover:bg-orange-100', path: '/dashboard/profile' },
            ].map((action, index) => (
              <button
                key={index}
                onClick={() => navigate(action.path)}
                className={`${action.color} p-4 rounded-lg transition-all text-left`}
              >
                <action.icon className="w-6 h-6 mb-2" />
                <p className="font-medium text-sm">{action.label}</p>
              </button>
            ))}
          </div>
        </Card>
      </div>

      {/* Upcoming Classes */}
      <Card className="mt-6">
        <h3 className="card-header">Upcoming This Week</h3>
        {upcomingEntries.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <p>No upcoming classes this week</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 font-semibold text-gray-700">Day</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-700">Time</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-700">Subject</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-700">Room</th>
                  <th className="text-left py-3 px-4 font-semibold text-gray-700">Division</th>
                </tr>
              </thead>
              <tbody>
                {upcomingEntries.map((entry: any, index: number) => (
                  <tr key={entry.id || index} className="border-b border-gray-100 hover:bg-gray-50">
                    <td className="py-3 px-4">
                      <span className="inline-block px-2 py-1 bg-primary-100 text-primary-800 rounded text-sm font-medium">
                        {DAY_LABELS[entry.dayOfWeek] || entry.dayOfWeek}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-gray-700">
                      <div className="flex items-center gap-2">
                        <FiClock size={14} />
                        {getTimeRange(entry)}
                      </div>
                    </td>
                    <td className="py-3 px-4 font-medium text-gray-900">
                      {entry.course?.name || 'Unknown'}
                    </td>
                    <td className="py-3 px-4 text-gray-600">
                      {entry.room?.roomNumber || '-'}
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-block px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs">
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
    </div>
  );
};
