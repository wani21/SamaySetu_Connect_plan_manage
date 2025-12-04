import React, { useState, useEffect } from 'react';
import { FiUser, FiMail, FiPhone, FiBriefcase, FiLock, FiSave } from 'react-icons/fi';
import toast from 'react-hot-toast';
import { Card } from '../../components/common/Card';
import { Input } from '../../components/common/Input';
import { Button } from '../../components/common/Button';
import { useAuthStore } from '../../store/authStore';
import { teacherAPI } from '../../services/api';

export const ProfilePage: React.FC = () => {
  const user = useAuthStore((state) => state.user);
  
  const [profileData, setProfileData] = useState({
    name: '',
    email: user?.email || '',
    phone: '',
    employeeId: '',
    specialization: '',
    weeklyHoursLimit: '25',
    department: null as any,
  });

  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const [isLoading, setIsLoading] = useState(false);
  const [isFetching, setIsFetching] = useState(true);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setIsFetching(true);
      const response = await teacherAPI.getProfile();
      const data = response.data;
      setProfileData({
        name: data.name || '',
        email: data.email || '',
        phone: data.phone || '',
        employeeId: data.employeeId || '',
        specialization: data.specialization || '',
        weeklyHoursLimit: data.weeklyHoursLimit?.toString() || '25',
        department: data.department,
      });
    } catch (error: any) {
      console.error('Error fetching profile:', error);
      toast.error('Failed to load profile data');
    } finally {
      setIsFetching(false);
    }
  };

  const handleProfileUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    try {
      const updateData = {
        name: profileData.name,
        phone: profileData.phone,
        specialization: profileData.specialization,
        weeklyHoursLimit: parseInt(profileData.weeklyHoursLimit),
        email: profileData.email,
        employeeId: profileData.employeeId,
        departmentId: profileData.department?.id || null,
        // Don't include password unless changing it
      };
      
      await teacherAPI.updateProfile(updateData);
      toast.success('Profile updated successfully!');
      await fetchProfile(); // Refresh profile data
    } catch (error: any) {
      console.error('Error updating profile:', error);
      const message = error.response?.data?.message || error.response?.data || 'Failed to update profile';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  const handlePasswordChange = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!passwordData.currentPassword || !passwordData.newPassword) {
      toast.error('Please fill in all password fields');
      return;
    }
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      toast.error('New passwords do not match');
      return;
    }

    if (passwordData.newPassword.length < 6) {
      toast.error('Password must be at least 6 characters');
      return;
    }

    setIsLoading(true);
    try {
      // Update profile with new password
      const updateData = {
        name: profileData.name,
        phone: profileData.phone,
        specialization: profileData.specialization,
        weeklyHoursLimit: parseInt(profileData.weeklyHoursLimit),
        email: profileData.email,
        employeeId: profileData.employeeId,
        departmentId: profileData.department?.id || null,
        password: passwordData.newPassword, // Include password for change
      };
      
      await teacherAPI.updateProfile(updateData);
      toast.success('Password changed successfully!');
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (error: any) {
      console.error('Error changing password:', error);
      const message = error.response?.data?.message || error.response?.data || 'Failed to change password';
      toast.error(message);
    } finally {
      setIsLoading(false);
    }
  };

  if (isFetching) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Loading profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div>
      {/* Header */}
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Profile Settings</h1>
        <p className="text-gray-600">Manage your account information</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Profile Picture */}
        <Card>
          <div className="text-center">
            <div className="w-32 h-32 bg-gradient-to-br from-primary-800 to-primary-900 rounded-full mx-auto mb-4 flex items-center justify-center text-white text-4xl font-bold">
              {profileData.name.charAt(0).toUpperCase()}
            </div>
            <h3 className="text-xl font-bold text-gray-900 mb-1">{profileData.name}</h3>
            <p className="text-sm text-gray-500 mt-1">{profileData.email}</p>
            <span className="inline-block px-3 py-1 bg-primary-100 text-primary-800 rounded-full text-sm font-medium mt-2 capitalize">
              {user?.role}
            </span>
            
            <div className="mt-6 pt-6 border-t border-gray-200">
              <div className="space-y-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-gray-600">Status:</span>
                  <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs font-medium">
                    Active
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Role:</span>
                  <span className="font-medium text-gray-900 capitalize">{user?.role}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Member Since:</span>
                  <span className="font-medium text-gray-900">
                    {new Date().getFullYear()}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </Card>

        {/* Profile Information */}
        <Card className="lg:col-span-2">
          <h3 className="card-header">Personal Information</h3>
          <form onSubmit={handleProfileUpdate} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Full Name"
                icon={<FiUser />}
                value={profileData.name}
                onChange={(e) => setProfileData({ ...profileData, name: e.target.value })}
              />
              <Input
                label="Employee ID"
                icon={<FiBriefcase />}
                value={profileData.employeeId}
                disabled
              />
            </div>

            <Input
              label="Email Address"
              type="email"
              icon={<FiMail />}
              value={profileData.email}
              disabled
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Phone Number"
                icon={<FiPhone />}
                value={profileData.phone}
                onChange={(e) => setProfileData({ ...profileData, phone: e.target.value })}
              />
              <Input
                label="Weekly Hours Limit"
                type="number"
                value={profileData.weeklyHoursLimit}
                onChange={(e) => setProfileData({ ...profileData, weeklyHoursLimit: e.target.value })}
              />
            </div>

            <Input
              label="Specialization"
              icon={<FiBriefcase />}
              value={profileData.specialization}
              onChange={(e) => setProfileData({ ...profileData, specialization: e.target.value })}
            />

            <Button
              type="submit"
              variant="primary"
              isLoading={isLoading}
              className="flex items-center gap-2"
            >
              <FiSave /> Save Changes
            </Button>
          </form>
        </Card>
      </div>

      {/* Change Password */}
      <Card className="mt-6">
        <h3 className="card-header">Change Password</h3>
        <form onSubmit={handlePasswordChange} className="space-y-4 max-w-2xl">
          <Input
            label="Current Password"
            type="password"
            icon={<FiLock />}
            value={passwordData.currentPassword}
            onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
            placeholder="Enter current password"
          />

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              label="New Password"
              type="password"
              icon={<FiLock />}
              value={passwordData.newPassword}
              onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
              placeholder="Enter new password"
            />
            <Input
              label="Confirm New Password"
              type="password"
              icon={<FiLock />}
              value={passwordData.confirmPassword}
              onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
              placeholder="Confirm new password"
            />
          </div>

          <Button
            type="submit"
            variant="secondary"
            isLoading={isLoading}
            className="flex items-center gap-2"
          >
            <FiLock /> Change Password
          </Button>
        </form>
      </Card>

      {/* Account Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mt-6">
        <Card>
          <div className="text-center">
            <p className="text-3xl font-bold text-primary-800 mb-1">
              {profileData.weeklyHoursLimit || '25'}
            </p>
            <p className="text-sm text-gray-600">Weekly Hours Limit</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-bold text-green-600 mb-1">
              {profileData.department?.name ? '1' : '0'}
            </p>
            <p className="text-sm text-gray-600">Department</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-bold text-orange-600 mb-1">
              {profileData.employeeId ? '✓' : '-'}
            </p>
            <p className="text-sm text-gray-600">Employee ID</p>
          </div>
        </Card>
        <Card>
          <div className="text-center">
            <p className="text-3xl font-bold text-purple-600 mb-1">
              {profileData.specialization ? '✓' : '-'}
            </p>
            <p className="text-sm text-gray-600">Specialization</p>
          </div>
        </Card>
      </div>
    </div>
  );
};
