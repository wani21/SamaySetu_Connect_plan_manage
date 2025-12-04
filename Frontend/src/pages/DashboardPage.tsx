import React, { useState, useEffect } from 'react';
import { FiCalendar, FiClock, FiBook, FiCheckCircle } from 'react-icons/fi';
import { StatsCard } from '../components/dashboard/StatsCard';
import { Card } from '../components/common/Card';
import { useAuthStore } from '../store/authStore';
import api from '../services/api';

export const DashboardPage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  const [teacherData, setTeacherData] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchTeacherData = async () => {
      try {
        setIsLoading(true);
        // Try to fetch teacher data by email
        const response = await api.get('/api/teachers');
        const teachers = response.data;
        const currentTeacher = teachers.find((t: any) => t.email === user?.email);
        setTeacherData(currentTeacher);
      } catch (error) {
        console.error('Error fetching teacher data:', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (user?.email) {
      fetchTeacherData();
    }
  }, [user?.email]);

  // Sample data for presentation
  const todayClasses = [
    { time: '09:00 - 10:00', subject: 'Data Structures', room: 'H202', division: 'SY-A', type: 'lecture' },
    { time: '10:00 - 11:00', subject: 'Algorithms', room: 'H203', division: 'TY-B', type: 'lecture' },
    { time: '02:00 - 04:00', subject: 'Database Lab', room: 'Lab-2', division: 'TY-B', type: 'lab' },
  ];

  const upcomingClasses = [
    { day: 'Tuesday', time: '09:00 - 10:00', subject: 'Operating Systems', room: 'H204', division: 'TY-C' },
    { day: 'Tuesday', time: '02:00 - 04:00', subject: 'Data Structures Lab', room: 'Lab-1', division: 'SY-A' },
    { day: 'Wednesday', time: '10:00 - 11:00', subject: 'Software Engineering', room: 'H205', division: 'TY-B' },
    { day: 'Wednesday', time: '02:00 - 03:00', subject: 'Web Technologies', room: 'H208', division: 'BTech-A' },
    { day: 'Thursday', time: '09:00 - 10:00', subject: 'Data Structures', room: 'H202', division: 'SY-B' },
    { day: 'Friday', time: '10:00 - 11:00', subject: 'Operating Systems', room: 'H205', division: 'TY-A' },
  ];

  const getCurrentDay = () => {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    return days[new Date().getDay()];
  };

  const getUserName = () => {
    if (user?.name) return user.name;
    if (user?.email) {
      const emailName = user.email.split('@')[0];
      return emailName.replace(/\./g, ' ').replace(/\d+/g, '').trim();
    }
    return 'User';
  };

  const formatName = (name: string) => {
    return name
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  };

  return (
    <div>
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">
          Welcome back, {formatName(getUserName())}! 👋
        </h1>
        <p className="text-gray-600">Here's what's happening with your schedule today.</p>
      </div>

          {/* Stats Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
            <StatsCard
              title="Total Classes"
              value={isLoading ? '...' : '24'}
              icon={FiBook}
              color="blue"
            />
            <StatsCard
              title="This Week"
              value={isLoading ? '...' : '18'}
              icon={FiCalendar}
              color="green"
            />
            <StatsCard
              title="Hours/Week"
              value={isLoading ? '...' : teacherData?.weeklyHoursLimit || '25'}
              icon={FiClock}
              color="orange"
            />
            <StatsCard
              title="Completed"
              value={isLoading ? '...' : '12'}
              icon={FiCheckCircle}
              color="purple"
            />
          </div>

          {/* Content Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Today's Schedule */}
            <Card>
              <h3 className="card-header">Today's Schedule - {getCurrentDay()}</h3>
              <div className="space-y-3">
                {todayClasses.map((classItem, index) => (
                  <div 
                    key={index} 
                    className={`p-4 rounded-lg border-l-4 ${
                      classItem.type === 'lab' 
                        ? 'bg-purple-50 border-purple-500' 
                        : 'bg-blue-50 border-blue-500'
                    }`}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <FiClock className="text-gray-600" size={14} />
                          <span className="text-sm font-medium text-gray-700">{classItem.time}</span>
                          {classItem.type === 'lab' && (
                            <span className="px-2 py-0.5 bg-purple-200 text-purple-800 text-xs rounded-full">
                              🔬 Lab
                            </span>
                          )}
                        </div>
                        <p className="font-semibold text-gray-900 mb-1">{classItem.subject}</p>
                        <div className="flex items-center gap-3 text-xs text-gray-600">
                          <span>📍 {classItem.room}</span>
                          <span>👥 {classItem.division}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </Card>

            {/* Quick Actions */}
            <Card>
              <h3 className="card-header">Quick Actions</h3>
              <div className="grid grid-cols-2 gap-4">
                {[
                  { label: 'View Timetable', icon: '📅', color: 'bg-blue-50 text-blue-700' },
                  { label: 'Update Availability', icon: '⏰', color: 'bg-green-50 text-green-700' },
                  { label: 'My Courses', icon: '📚', color: 'bg-purple-50 text-purple-700' },
                  { label: 'Profile Settings', icon: '⚙️', color: 'bg-orange-50 text-orange-700' },
                ].map((action, index) => (
                  <button
                    key={index}
                    className={`${action.color} p-4 rounded-lg hover:shadow-md transition-all text-left`}
                  >
                    <div className="text-2xl mb-2">{action.icon}</div>
                    <p className="font-medium text-sm">{action.label}</p>
                  </button>
                ))}
              </div>
            </Card>
          </div>

          {/* Upcoming Classes */}
          <Card className="mt-6">
            <h3 className="card-header">Upcoming This Week</h3>
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
                  {upcomingClasses.map((classItem, index) => (
                    <tr key={index} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-3 px-4">
                        <span className="inline-block px-2 py-1 bg-primary-100 text-primary-800 rounded text-sm font-medium">
                          {classItem.day}
                        </span>
                      </td>
                      <td className="py-3 px-4 text-gray-700">
                        <div className="flex items-center gap-2">
                          <FiClock size={14} />
                          {classItem.time}
                        </div>
                      </td>
                      <td className="py-3 px-4 font-medium text-gray-900">{classItem.subject}</td>
                      <td className="py-3 px-4 text-gray-600">{classItem.room}</td>
                      <td className="py-3 px-4">
                        <span className="inline-block px-2 py-1 bg-blue-100 text-blue-800 rounded text-xs">
                          {classItem.division}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
    </div>
  );
};
