import React, { useState, useRef, useEffect } from 'react';
import { FiLogOut, FiUser, FiMenu, FiSettings } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import toast from 'react-hot-toast';
import logo from '../../assets/logo.png';

interface NavbarProps {
  onMenuClick?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onMenuClick }) => {
  const navigate = useNavigate();
  const { user, logout } = useAuthStore();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Get user display name
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

  const handleLogout = () => {
    logout();
    toast.success('Logged out successfully');
    navigate('/login');
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <nav className="bg-white shadow-md border-b border-gray-200 sticky top-0 z-50">
      <div className="px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Left Section */}
          <div className="flex items-center gap-4">
            {onMenuClick && (
              <button
                onClick={onMenuClick}
                className="lg:hidden p-2 rounded-lg hover:bg-gray-100 text-gray-600"
              >
                <FiMenu size={24} />
              </button>
            )}
            <div className="flex items-center gap-3">
              <img src={logo} alt="MIT AOE" className="h-10" />
              <div className="h-8 w-px bg-gray-300"></div>
              <div>
                <h1 className="text-xl font-bold text-primary-900">SamaySetu</h1>
                <p className="text-xs text-gray-600">MIT Academy of Engineering</p>
              </div>
            </div>
          </div>

          {/* Right Section */}
          <div className="flex items-center gap-4">
            <div className="hidden sm:block text-right">
              <p className="text-sm font-medium text-gray-900">{formatName(getUserName())}</p>
              <p className="text-xs text-gray-500 capitalize">{user?.role}</p>
            </div>
            
            <div className="relative" ref={dropdownRef}>
              <button 
                onClick={() => setShowDropdown(!showDropdown)}
                className="p-2 rounded-lg hover:bg-gray-100 text-gray-600 transition-colors"
              >
                <FiUser size={20} />
              </button>

              {/* Dropdown Menu */}
              {showDropdown && (
                <div className="absolute right-0 mt-2 w-64 bg-white rounded-lg shadow-strong border border-gray-200 py-2 z-50">
                  {/* User Info */}
                  <div className="px-4 py-3 border-b border-gray-200">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 rounded-full bg-gradient-to-br from-primary-600 to-primary-800 flex items-center justify-center text-white font-bold text-lg">
                        {getUserName().charAt(0).toUpperCase()}
                      </div>
                      <div className="flex-1">
                        <p className="font-semibold text-gray-900">{formatName(getUserName())}</p>
                        <p className="text-xs text-gray-500 capitalize">{user?.role}</p>
                      </div>
                    </div>
                    <p className="text-xs text-gray-600 mt-2 truncate">{user?.email}</p>
                  </div>

                  {/* Menu Items */}
                  <div className="py-1">
                    <button
                      onClick={() => {
                        setShowDropdown(false);
                        const adminRoles = ['ADMIN', 'HOD', 'TIMETABLE_COORDINATOR'];
                        navigate(adminRoles.includes(user?.role || '') ? '/admin/profile' : '/dashboard/profile');
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-3"
                    >
                      <FiUser size={16} />
                      <span>My Profile</span>
                    </button>
                    <button
                      onClick={() => {
                        setShowDropdown(false);
                        toast('Settings coming soon!', { icon: 'ℹ️' });
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-gray-100 flex items-center gap-3"
                    >
                      <FiSettings size={16} />
                      <span>Settings</span>
                    </button>
                  </div>

                  {/* Logout */}
                  <div className="border-t border-gray-200 pt-1">
                    <button
                      onClick={() => {
                        setShowDropdown(false);
                        handleLogout();
                      }}
                      className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-3"
                    >
                      <FiLogOut size={16} />
                      <span>Logout</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};
