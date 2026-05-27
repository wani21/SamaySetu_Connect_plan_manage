import { useState, useEffect } from 'react';
import api from '../services/api';

interface DashboardStats {
  teachersCount: number;
  coursesCount: number;
  divisionsCount: number;
  roomsCount: number;
  overallUtilization: number;
  recentActivities: any[];
}

export const useDashboardStats = () => {
  const [stats, setStats] = useState<DashboardStats>({
    teachersCount: 0,
    coursesCount: 0,
    divisionsCount: 0,
    roomsCount: 0,
    overallUtilization: 0,
    recentActivities: [],
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Get current academic year
        const currentYearRes = await api.get('/api/academic-years/current');
        const yearId = currentYearRes.data?.id;
        if (!yearId) {
          throw new Error('Active academic year not configured. Configure it in Academic Structure first.');
        }

        // Call the unified, role-based dashboard stats endpoint
        const statsRes = await api.get(`/api/timetable/dashboard-stats?academicYearId=${yearId}`);
        setStats(statsRes.data || {
          teachersCount: 0,
          coursesCount: 0,
          divisionsCount: 0,
          roomsCount: 0,
          overallUtilization: 0,
          recentActivities: [],
        });
      } catch (err: any) {
        console.error('Error fetching dashboard stats:', err);
        setError(err.response?.data?.message || err.message || 'Failed to fetch statistics');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, []);

  return { stats, isLoading, error };
};
