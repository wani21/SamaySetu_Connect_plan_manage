import { useState, useEffect } from 'react';
import api from '../services/api';

interface DashboardStats {
  totalTeachers: number;
  totalCourses: number;
  totalDepartments: number;
  totalRooms: number;
  totalDivisions: number;
  totalAcademicYears: number;
  totalTimeSlots: number;
}

export const useDashboardStats = () => {
  const [stats, setStats] = useState<DashboardStats>({
    totalTeachers: 0,
    totalCourses: 0,
    totalDepartments: 0,
    totalRooms: 0,
    totalDivisions: 0,
    totalAcademicYears: 0,
    totalTimeSlots: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        setIsLoading(true);
        setError(null);

        // Fetch all data in parallel
        const [
          teachersRes,
          coursesRes,
          departmentsRes,
          roomsRes,
          divisionsRes,
          academicYearsRes,
          timeSlotsRes,
        ] = await Promise.all([
          api.get('/api/teachers'),
          api.get('/admin/api/courses'),
          api.get('/admin/api/departments'),
          api.get('/admin/api/rooms'),
          api.get('/admin/api/divisions'),
          api.get('/admin/api/academic-years'),
          api.get('/admin/api/time-slots'),
        ]);

        // Filter to count only teachers (exclude admins)
        const teachersData = Array.isArray(teachersRes.data) ? teachersRes.data : [];
        const teachersCount = teachersData.filter((user: any) => user.role === 'TEACHER').length;

        setStats({
          totalTeachers: teachersCount,
          totalCourses: coursesRes.data?.length || 0,
          totalDepartments: departmentsRes.data?.length || 0,
          totalRooms: roomsRes.data?.length || 0,
          totalDivisions: divisionsRes.data?.length || 0,
          totalAcademicYears: academicYearsRes.data?.length || 0,
          totalTimeSlots: timeSlotsRes.data?.length || 0,
        });
      } catch (err: any) {
        console.error('Error fetching dashboard stats:', err);
        setError(err.response?.data?.message || 'Failed to fetch statistics');
      } finally {
        setIsLoading(false);
      }
    };

    fetchStats();
  }, []);

  return { stats, isLoading, error };
};
