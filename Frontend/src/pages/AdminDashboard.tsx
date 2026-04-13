import React, { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from '../components/layout/Navbar';
import { Sidebar } from '../components/layout/Sidebar';
import { AdminDashboardHome } from '../components/admin/AdminDashboardHome';
import { AcademicStructurePage } from '../components/admin/AcademicStructurePage';
import { StaffManagementPage } from '../components/admin/StaffManagementPage';
import { RoomsPage } from '../components/admin/RoomsPage';
import { TimeSlotsPage } from '../components/admin/TimeSlotsPage';
import { TimetableManagementPage } from '../components/admin/TimetableManagementPage';
import { AdminProfilePage } from './admin/AdminProfilePage';
import { DepartmentsPage } from '../components/admin/DepartmentsPage';
import { CoursesPage } from '../components/admin/CoursesPage';
import { DivisionsPage } from '../components/admin/DivisionsPage';

export const AdminDashboard: React.FC = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar onMenuClick={() => setSidebarOpen(!sidebarOpen)} />

      <div className="flex">
        <Sidebar
          isOpen={sidebarOpen}
          onClose={() => setSidebarOpen(false)}
          isAdmin={true}
        />

        <main className="flex-1 p-6 lg:p-8">
          <Routes>
            <Route path="dashboard" element={<AdminDashboardHome />} />
            <Route path="academic-structure" element={<AcademicStructurePage />} />
            <Route path="staff" element={<StaffManagementPage />} />
            <Route path="rooms" element={<RoomsPage />} />
            <Route path="time-slots" element={<TimeSlotsPage />} />
            <Route path="timetable" element={<TimetableManagementPage />} />
            <Route path="departments" element={<DepartmentsPage />} />
            <Route path="courses" element={<CoursesPage />} />
            <Route path="divisions" element={<DivisionsPage />} />
            <Route path="teachers" element={<Navigate to="/admin/staff" replace />} />
            <Route path="profile" element={<AdminProfilePage />} />
            <Route path="/" element={<Navigate to="dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
};
