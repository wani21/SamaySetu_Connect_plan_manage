import React, { useState, useEffect } from 'react';
import { NavLink } from 'react-router-dom';
import {
  FiHome,
  FiUsers,
  FiCalendar,
  FiGrid,
  FiSettings,
  FiX,
  FiClock,
  FiLayers,
  FiChevronLeft,
  FiChevronRight
} from 'react-icons/fi';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '../../store/authStore';

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  isAdmin?: boolean;
  collapsed?: boolean;
  onCollapsedChange?: (collapsed: boolean) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  isOpen,
  onClose,
  isAdmin = false,
  collapsed: controlledCollapsed,
  onCollapsedChange,
}) => {
  const user = useAuthStore((state) => state.user);
  const role = user?.role || 'TEACHER';

  // Internal collapsed state with optional controlled mode
  const [internalCollapsed, setInternalCollapsed] = useState(false);
  const collapsed = controlledCollapsed !== undefined ? controlledCollapsed : internalCollapsed;

  const toggleCollapse = () => {
    const next = !collapsed;
    setInternalCollapsed(next);
    onCollapsedChange?.(next);
  };

  // Persist collapse preference in localStorage
  useEffect(() => {
    const saved = localStorage.getItem('sidebar-collapsed');
    if (saved !== null) {
      const val = saved === 'true';
      setInternalCollapsed(val);
      onCollapsedChange?.(val);
    }
  }, []);

  useEffect(() => {
    localStorage.setItem('sidebar-collapsed', String(collapsed));
  }, [collapsed]);

  const teacherLinks = [
    { to: '/dashboard', icon: FiHome, label: 'Dashboard' },
    { to: '/dashboard/timetable', icon: FiCalendar, label: 'My Timetable' },
    { to: '/dashboard/availability', icon: FiGrid, label: 'Availability' },
    { to: '/dashboard/profile', icon: FiSettings, label: 'Profile' },
  ];

  // Build admin-layout links based on role
  const getAdminLinks = () => {
    const isInstitutionAdmin = role === 'ADMIN' || role === 'SUPER_ADMIN';

    const links = [
      { to: '/admin/dashboard', icon: FiHome, label: 'Dashboard' },
    ];

    // Academic Structure — all admin-layout roles
    links.push({ to: '/admin/academic-structure', icon: FiLayers, label: 'Academic Structure' });

    // Faculty management — ADMIN, SUPER_ADMIN, DEPARTMENT_ADMIN, and HOD
    if (isInstitutionAdmin || role === 'DEPARTMENT_ADMIN' || role === 'HOD') {
      links.push({ to: '/admin/staff', icon: FiUsers, label: 'Faculty' });
    }

    // Rooms — all admin-layout roles
    links.push({ to: '/admin/rooms', icon: FiGrid, label: 'Rooms' });

    // Resource Timetables (rooms + labs + faculty) — all admin-layout roles
    links.push({ to: '/admin/resource-timetables', icon: FiCalendar, label: 'Resource Timetables' });

    // Time Slots — ADMIN, SUPER_ADMIN, DEPARTMENT_ADMIN, and TIMETABLE_COORDINATOR
    if (isInstitutionAdmin || role === 'DEPARTMENT_ADMIN' || role === 'TIMETABLE_COORDINATOR') {
      links.push({ to: '/admin/time-slots', icon: FiClock, label: 'Time Slots' });
    }

    // Timetable — all admin-layout roles
    links.push({ to: '/admin/timetable', icon: FiCalendar, label: 'Timetable Builder' });

    return links;
  };

  const links = isAdmin ? getAdminLinks() : teacherLinks;

  return (
    <>
      {/* Mobile Overlay */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <aside
        className={`
          fixed lg:sticky lg:top-16 inset-y-0 left-0 z-50
          bg-white border-r border-gray-200
          lg:h-[calc(100vh-4rem)]
          transition-all duration-300 ease-in-out
          ${collapsed ? 'w-[68px]' : 'w-64'}
          ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
        `}
      >
        <div className="flex flex-col h-full relative">
          {/* Close Button (Mobile) */}
          <div className="lg:hidden flex justify-end p-4">
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-gray-100 text-gray-600"
            >
              <FiX size={24} />
            </button>
          </div>

          {/* Collapse Toggle Button (Desktop) */}
          <button
            onClick={toggleCollapse}
            className="
              hidden lg:flex items-center justify-center
              absolute -right-3 top-6
              w-6 h-6 rounded-full
              bg-white border border-gray-300
              shadow-sm hover:shadow-md
              text-gray-500 hover:text-primary-700
              transition-all duration-200
              z-10
            "
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? <FiChevronRight size={14} /> : <FiChevronLeft size={14} />}
          </button>

          {/* Navigation Links */}
          <nav className={`flex-1 py-6 space-y-1 overflow-y-auto overflow-x-hidden ${collapsed ? 'px-2' : 'px-3'}`}>
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                onClick={() => window.innerWidth < 1024 && onClose()}
                title={collapsed ? link.label : undefined}
                className={({ isActive }) =>
                  `
                    group relative flex items-center rounded-lg transition-all duration-200
                    ${collapsed
                      ? 'justify-center px-0 py-3'
                      : 'gap-3 px-4 py-3'
                    }
                    ${isActive
                      ? 'bg-primary-800 text-white shadow-sm'
                      : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                    }
                  `
                }
              >
                {() => (
                  <>
                    <link.icon size={20} className="flex-shrink-0" />

                    {/* Label — hidden when collapsed */}
                    <span
                      className={`
                        font-medium whitespace-nowrap
                        transition-all duration-300
                        ${collapsed ? 'w-0 opacity-0 overflow-hidden' : 'w-auto opacity-100'}
                      `}
                    >
                      {link.label}
                    </span>

                    {/* Tooltip — only when collapsed, on hover */}
                    {collapsed && (
                      <span
                        className="
                          absolute left-full ml-2 px-2.5 py-1.5
                          bg-gray-900 text-white text-xs font-medium
                          rounded-md shadow-lg
                          whitespace-nowrap
                          opacity-0 group-hover:opacity-100
                          pointer-events-none
                          transition-opacity duration-200
                          z-50
                        "
                      >
                        {link.label}
                        <span className="absolute top-1/2 -left-1 -translate-y-1/2 w-2 h-2 bg-gray-900 rotate-45" />
                      </span>
                    )}
                  </>
                )}
              </NavLink>
            ))}
          </nav>

          {/* Footer */}
          <div className={`p-3 border-t border-gray-200 ${collapsed ? 'px-1' : ''}`}>
            <p className={`text-xs text-gray-400 text-center transition-opacity duration-300 ${collapsed ? 'opacity-0 h-0 overflow-hidden' : 'opacity-100'}`}>
              © 2026 MIT Academy of Engineering
            </p>
            {collapsed && (
              <p className="text-xs text-gray-400 text-center">©</p>
            )}
          </div>
        </div>
      </aside>
    </>
  );
};
