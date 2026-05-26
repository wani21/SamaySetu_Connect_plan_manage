import React from 'react';
import { FiUsers, FiBook, FiGrid, FiTrendingUp, FiActivity } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { StatsCard } from '../dashboard/StatsCard';
import { Card } from '../common/Card';
import { useDashboardStats } from '../../hooks/useDashboardStats';
import { RecentActivity } from '../../types';

export const AdminDashboardHome: React.FC = () => {
  const navigate = useNavigate();
  const { stats, isLoading } = useDashboardStats();

  const quickActions = [
    { label: 'Timetable Builder', icon: '🗓️', path: '/admin/timetable' },
    { label: 'Room Occupancy', icon: '🏫', path: '/admin/room-timetables' },
    { label: 'Manage Batches', icon: '👥', path: '/admin/batches' },
    { label: 'Faculty List', icon: '👨‍🏫', path: '/admin/staff' },
  ];

  return (
    <div className="space-y-8">
      {/* Welcome Section */}
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-extrabold text-gray-800 tracking-tight">
            Dashboard 👑
          </h1>
          <p className="text-gray-500 mt-1">
            Real-time department scheduling statistics, classroom utilization, and recent builder events.
          </p>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatsCard
          title="Total Faculty"
          value={isLoading ? '...' : stats.teachersCount}
          icon={FiUsers}
          color="blue"
        />
        <StatsCard
          title="Total Courses"
          value={isLoading ? '...' : stats.coursesCount}
          icon={FiBook}
          color="green"
        />
        <StatsCard
          title="Divisions"
          value={isLoading ? '...' : stats.divisionsCount}
          icon={FiGrid}
          color="purple"
        />
        <StatsCard
          title="Room/Lab Utilization"
          value={isLoading ? '...' : `${stats.overallUtilization}%`}
          icon={FiTrendingUp}
          color="orange"
        />
      </div>

      {/* Quick Actions & System Summary */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <Card>
            <h3 className="text-lg font-bold text-gray-800 mb-4">Quick Actions</h3>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {quickActions.map((action, index) => (
                <button
                  key={index}
                  onClick={() => navigate(action.path)}
                  className="bg-gradient-to-br from-primary-50 to-primary-100 p-5 rounded-2xl hover:shadow-md hover:scale-[1.02] transition-all text-center flex flex-col items-center justify-center gap-2 border border-primary-200"
                >
                  <div className="text-3xl">{action.icon}</div>
                  <p className="font-semibold text-xs text-primary-900 leading-tight">{action.label}</p>
                </button>
              ))}
            </div>
          </Card>
        </div>

        <div className="lg:col-span-1">
          <Card>
            <h3 className="text-lg font-bold text-gray-800 mb-4">Department Summary</h3>
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 bg-blue-50 rounded-xl border border-blue-100">
                <div className="flex items-center gap-3">
                  <FiUsers className="text-blue-600" size={18} />
                  <div>
                    <p className="text-xs font-semibold text-gray-500 uppercase">Faculty members</p>
                  </div>
                </div>
                <span className="text-sm font-bold text-blue-800">{isLoading ? '...' : stats.teachersCount}</span>
              </div>

              <div className="flex items-center justify-between p-3 bg-green-50 rounded-xl border border-green-100">
                <div className="flex items-center gap-3">
                  <FiBook className="text-green-600" size={18} />
                  <div>
                    <p className="text-xs font-semibold text-gray-500 uppercase">Courses</p>
                  </div>
                </div>
                <span className="text-sm font-bold text-green-800">{isLoading ? '...' : stats.coursesCount}</span>
              </div>

              <div className="flex items-center justify-between p-3 bg-purple-50 rounded-xl border border-purple-100">
                <div className="flex items-center gap-3">
                  <FiGrid className="text-purple-600" size={18} />
                  <div>
                    <p className="text-xs font-semibold text-gray-500 uppercase">Classrooms</p>
                  </div>
                </div>
                <span className="text-sm font-bold text-purple-800">{isLoading ? '...' : stats.roomsCount}</span>
              </div>
            </div>
          </Card>
        </div>
      </div>

      {/* Recent Activities */}
      <Card>
        <div className="flex items-center gap-2 mb-4">
          <FiActivity className="text-primary-800" size={20} />
          <h3 className="text-lg font-bold text-gray-800">Recent Timetable Activities</h3>
        </div>

        {isLoading ? (
          <p className="text-center py-6 text-gray-400 text-sm">Loading activity feed...</p>
        ) : !stats.recentActivities || stats.recentActivities.length === 0 ? (
          <p className="text-center py-8 text-gray-400 text-sm italic">
            No recent changes recorded in this academic year yet.
          </p>
        ) : (
          <div className="overflow-x-auto border border-gray-100 rounded-xl">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-gray-50 border-b border-gray-200">
                  <th className="px-4 py-3 font-bold text-gray-600">Division</th>
                  <th className="px-4 py-3 font-bold text-gray-600">Course</th>
                  <th className="px-4 py-3 font-bold text-gray-600">Teacher</th>
                  <th className="px-4 py-3 font-bold text-gray-600">Room</th>
                  <th className="px-4 py-3 font-bold text-gray-600">Day & Slot</th>
                  <th className="px-4 py-3 font-bold text-gray-600">Status</th>
                  <th className="px-4 py-3 font-bold text-gray-600 text-right">Modified At</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {stats.recentActivities.map((act: RecentActivity, idx: number) => (
                  <tr key={idx} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 font-semibold text-gray-900">{act.divisionName}</td>
                    <td className="px-4 py-3">
                      <div className="font-semibold text-gray-800">{act.courseName}</div>
                      <div className="text-[10px] text-gray-400 mt-0.5">{act.courseCode}</div>
                    </td>
                    <td className="px-4 py-3 font-medium text-gray-700">{act.teacherName}</td>
                    <td className="px-4 py-3">
                      <span className="px-1.5 py-0.5 bg-gray-100 rounded text-gray-600 font-bold uppercase">
                        {act.roomName}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-600">
                      <div className="font-medium">{act.dayOfWeek}</div>
                      <div className="text-[10px] text-gray-400 mt-0.5">{act.slotName}</div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-1.5 py-0.5 rounded font-bold scale-90 ${
                        act.status === 'PUBLISHED' 
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-amber-50 text-amber-700 border border-amber-200'
                      }`}>
                        {act.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right text-gray-500 font-mono">
                      {act.updatedAt ? new Date(act.updatedAt).toLocaleString() : 'N/A'}
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
